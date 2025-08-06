package fj.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fj.ast.ClassDecl;
import fj.ast.Method;
import fj.ast.Type;

/**
 * The class table (the <i>CT</i> in the FeatherweightJava paper)
 * used as a database of class information throughout typechecking
 * and evaluation.  The class table is used by both the type checker
 * and the evaluator, and thus must be built first.  During its
 * construction, we detect five different errors:
 * 
 * <ol>
 * <li>declaring a class which has already been declared</li>
 * <li>declaring a field which has already been declared
 * in the given class or its superclass (transitively)</li>
 * <li>declaring a method which has already been declared
 * in the given class</li>
 * <li>extending an unknown class</li>
 * <li>introducing a cycle in the subtype relation</li>
 * </ol>
 *
 * This class also supplies definitions of the <code>mbody</code>,
 * <code>mtype</code>, <code>fields</code>, and <code>override</code>
 * functions defined in the <i>auxiliary definitions</i> table in
 * the FeatherweightJava paper.
 */
public class ClassTable {

	/**
	 * A mapping from class names (String) to ClassInfo structures.
	 * The ClassInfo structure is private to this package; use the
	 * public methods of this class to get a class's information.
	 */
	private Map classMap;
	
	/**
	 * Build a class table from a list of classes.  This object can
	 * then be used in typechecking and evaluation.
	 * @param classes the list of class declarations
	 * @throws ClassTableException a class, field, or method is multiply
	 * defined, a class extends an unknown class, or a cycle in the
	 * subtyping relation is introduced
	 * @see fj.ast.ClassDecl
	 */
	public ClassTable(List classes) throws ClassTableException {
		final Map declMap = new HashMap();

		class FieldFixer {
			private final Map fieldMap;
			FieldFixer() {
				this.fieldMap = new HashMap();
			}
			List fixFields(String name) throws ClassTableException {
				ClassDecl classDecl = (ClassDecl) declMap.get(name);
				if (classDecl == null) {
					throw new ClassTableException(
						"unknown class \"" + name + "\"");
				}
				else if (classDecl.superType.isObject()) {
					return classDecl.fields;
				}
				List fieldList = new LinkedList();
				fieldList.addAll(fixFields(classDecl.superType.className));
				fieldMap.put(name, fieldList);
				return fieldList;
			}
		}
		
		// Construct a temporary classMap for below calls to subtype
		classMap = new HashMap();
		
		// Phase 1: Construct ClassInfos with only superType
		for (Iterator i = classes.iterator(); i.hasNext(); ) {
			ClassDecl classDecl = (ClassDecl) i.next();
			String name = classDecl.name;
			
			// Check for repeated classes
			if (classMap.containsKey(name)) {
				throw new ClassTableException(
						"duplicate class name \"" + name + "\"");				
			}
			
			// Check for subtyping cycles
			Type superType = classDecl.superType;
			if (subtype(superType, classDecl.type)) {
				throw new ClassTableException(
					"class \"" + classDecl.name +
					"\" introduces a subtyping cycle");					
			}
			
			// Insert a pretty blank ClassInfo to satisfy subtype
			classMap.put(name, new ClassInfo(superType));
			declMap.put(name, classDecl);
		}

		// Prepare the class map
		FieldFixer fixer = new FieldFixer();
		Map tmpClassMap = new HashMap();
		for (Iterator i = classes.iterator(); i.hasNext(); ) {
			ClassDecl classDecl = (ClassDecl) i.next();
			String name = classDecl.name;

			// Associate method names with method information
			Map methodMap = new HashMap();
			for (Iterator j = classDecl.methods.iterator(); j.hasNext(); ) {
				Method method = (Method) j.next();
				String methodName = method.name;
				if (methodMap.containsKey(methodName)) {
					throw new ClassTableException(
						"duplicate method name \"" + name + "\"");
				}
				methodMap.put(methodName, method);
			}

			// Construct the ClassInfo structure
			tmpClassMap.put(name, new ClassInfo(classDecl.superType,
				new FieldInfo(fixer.fixFields(name)), methodMap));
		}

		// Finally we're set to assign to our field
		classMap = Collections.unmodifiableMap(tmpClassMap);
	}
	
	protected ClassInfo lookup(Type type) {
		return (ClassInfo) classMap.get(type.className);
	}

	/**
	 * Find the method with the given name in the given class.
	 * Search up the class hierarchy until a match is found, or
	 * return <code>null</code> if no match exists.  This method
	 * is a factoring out of the common operation needed by both
	 * {@link #mtype} and {@link #mbody}.
	 * @param methodName the name of the method to find
	 * @param type the type to start looking in
	 * @return the requested method object, or <code>null</code>
	 * if no method can be found by that name
	 * @see fj.ast.Method
	 */
	private Method methodLookup(String methodName, Type type) {
		
		// Special case for Object: it has no methods
		if (type.isObject()) return null;
		
		// Get the class information
		ClassInfo info = lookup(type);
		if (info == null) return null;
		
		// Lookup the method
		Method method = (Method) info.methodMap.get(methodName);
		if (method == null) {
			return methodLookup(methodName, info.superType);
		}
		else {
			return method;
		}
	}
	
	/**
	 * Return the direct supertype of the given type.
	 * @param type the derived type
	 * @return its superclass, or <code>null</code> if
	 * the given class is not in the class table
	 */
	public Type superType(Type type) {
		ClassInfo info = lookup(type);
		if (info == null) return null;
		return info.superType;
	}
	
	/**
	 * Return the FieldInfo structure for the given type. 
	 * @param type the type to lookup
	 * @return its FieldInfo structure, or <code>null</code> if
	 * the given type is not in the class table
	 */
	public FieldInfo fields(Type type) {
		
		// Special case for Object: it has no fields
		if (type.isObject()) return FieldInfo.EMPTY;
		ClassInfo info = lookup(type);
		if (info == null) return null;
		return info.fieldInfo;	
	}
	
	/**
	 * Return the MethodType structure corresponding to the requested
	 * method.
	 * @param methodName the name of the method to lookup
	 * @param type the type to begin searching in
	 * @return the MethodType structure, or <code>null</code> if
	 * the method cannot be found
	 */
	public MethodType mtype(String methodName, Type type) {
		Method method = methodLookup(methodName, type);
		if (method == null) return null;
		return new MethodType(method.paramTypes, method.returnType);		
	}
	
	/**
	 * Return the MethodBody structure corresponding to the requested
	 * method.
	 * @param methodName the name of the method to lookup
	 * @param type the type to begin searching in
	 * @return the MethodBody structure, or <code>null</code> if
	 * the method cannot be found
	 */
	public MethodBody mbody(String methodName, Type type) {
		Method method = methodLookup(methodName, type);
		if (method == null) return null;
		return new MethodBody(method.paramNames, method.body);
	}
	
	/**
	 * Determine if a method is a valid overriding of a method of
	 * the same name in a superclass.
	 * @param methodName the name of the method
	 * @param superType the supertype to start looking in
	 * @param methodType the type of the method we are checking
	 * @return <code>true</code> if <code>methodType</code> is a valid
	 * override; <code>false</code> otherwise
	 */
	public boolean override(String methodName, Type superType,
		MethodType methodType) {
		// TODO: Implement method override checking
		// This method should not throw a ClassTableException
		throw new Error("CT: override unimplemented");
	}

	/**
	 * Determine whether <code>typeSub</code> is a subtype of
	 * <code>typeSuper</code> in the subtyping relation.
	 * @param typeSub the subtype to check
	 * @param typeSuper the supertype to check
	 * @return whether <code>typeSub &lt;: typeSuper</code>
	 */
	public boolean subtype(Type typeSub, Type typeSuper) {
		// TODO: Implement subtype checking
		// This method should not throw a ClassTableException
		throw new Error("CT: subtype unimplemented");
	}
}
