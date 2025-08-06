package fgj.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fgj.ast.*;

/**
 * The class table (the <i>CT</i> in the FeatherweightGJ paper)
 * used as a database of class information throughout typechecking
 * and evaluation.  The class table is used by both the type checker
 * and the evaluator, and thus must be built first.  During its
 * construction, we detect six different errors:
 * 
 * <ol>
 * <li>declaring a class which has already been declared</li>
 * <li>declaring a field which has already been declared
 * in the given class or its superclass (transitively)</li>
 * <li>declaring a method which has already been declared
 * in the given class</li>
 * <li>declaring a type variable which has already been declared
 * in the given class or method</li>
 * <li>extending an unknown class</li>
 * <li>introducing a cycle in the subtype relation</li>
 * </ol>
 *
 * This class also supplies definitions of the <code>mbody</code>,
 * <code>mtype</code>, and <code>fields</code>
 * functions defined in the <i>auxiliary definitions</i> table in
 * the FeatherweightGJ paper.
 */
public class ClassTable {
	
	/**
	 * A mapping from class names (String) to their
	 * corresponding {@link ClassDecl} structures.
	 */
	private final Map classMap;

	/**
	 * Build a class table from a list of classes.  This object can
	 * then be used in typechecking and evaluation.
	 * @param classes the list of class declarations
	 * @throws ClassTableException a class, type variable,
	 * field, or method is multiply defined, a class extends an
	 * unknown class, or a cycle in the subtyping relation is introduced
	 */
	public ClassTable(List classes) throws ClassTableException {
		classMap = new HashMap();
		
		// Add the classes, and check for duplicates
		for (Iterator i = classes.iterator(); i.hasNext();) {
			ClassDecl cd = (ClassDecl) i.next();
			if (classMap.containsKey(cd.name)) {
				throw new ClassTableException("multiply defined class");
			}
			if (cd.name.equals("Object")) {
				throw new ClassTableException("cannot define Object");
			}
			classMap.put(cd.name, cd);
		}
	}
	
	/**
	 * Find the class declaration corresponding to the given class name.
	 * @param className the name of the class to lookup
	 * @return its class declaration AST node
	 * @throws ClassTableException the class doesn't exist
	 */
	private ClassDecl lookup(String className) throws ClassTableException {
		ClassDecl cd = (ClassDecl) classMap.get(className);
		if (cd != null) return cd;
		throw new ClassTableException("unknown class \"" + className + "\"");
	}
	
	/**
	 * Return the list of type parameters of the given class.
	 * @param className the class to lookup
	 * @return its type parameters
	 * @throws ClassTableException the class doesn't exist
	 */
	public List typeParams(String className) throws ClassTableException {
		return lookup(className).typeParams;
	}
	
	/**
	 * Return the super type of a given class.
	 * @param className the class to lookup
	 * @return its super type
	 * @throws ClassTableException the class doesn't exist
	 */
	public NonVariableType superType(String className) throws ClassTableException {
		return lookup(className).superType;
	}
		
	/**
	 * Return the list of fields of the given type. 
	 * @param type the type to lookup
	 * @return a list of {@link Declaration} structures corresponding
	 * to the fields of that type
	 * @throws ClassTableException the type could not be found, or it
	 * applied the incorrect number of type arguments
	 */
	public List fields(NonVariableType type) throws ClassTableException {
		
		// Object has no fields
		if (type.isObject()) return Collections.EMPTY_LIST;
		
		// Get our declaration
		ClassDecl cd = lookup(type.className);
		List fields = new LinkedList();
		
		// Create a substitution with our arguments
		Substitution subst = new Substitution(type.typeArgs, cd.typeParams);
		
		// Fetch the fields of our super type
		fields.addAll(fields(subst.apply(cd.superType)));
		
		// Add our fields, substituting on the way
		for (Iterator i = cd.fields.iterator(); i.hasNext();) {
			Declaration field = (Declaration) i.next();
			fields.add(new Declaration(subst.apply(field.type), field.name));
		}
		
		return fields;
	}
	
	/**
	 * Return the MethodType structure corresponding to the requested
	 * method.
	 * @param methodName the name of the method to lookup
	 * @param type the type to begin searching in
	 * @return the MethodType structure
	 * @throws ClassTableException the type could not be found, or it
	 * applied the incorrect number of type arguments
	 */
	public MethodType mtype(String methodName, NonVariableType type) throws ClassTableException {
		
		// Object has no methods
		if (type.isObject()) return null;
		ClassDecl cd = lookup(type.className);
		
		// Construct a substitution with the class parameters
		Substitution subst = new Substitution(type.typeArgs, cd.typeParams);
		
		// Look for it in this class
		for (Iterator i = cd.methods.iterator(); i.hasNext();) {
			Method method = (Method) i.next();
			if (method.name.equals(methodName)) {
				
				// We found it; return a substituted method type
				List typeParams = new LinkedList();
				for (Iterator j = method.typeParams.iterator(); j.hasNext();) {
					TypeParam typeParam = (TypeParam) j.next();
					NonVariableType bound = subst.apply(typeParam.bound);
					typeParams.add(new TypeParam(typeParam.varName, bound));
				}
				List paramTypes = new LinkedList();
				for (Iterator j = method.paramTypes.iterator(); j.hasNext();) {
					Type paramType = (Type) j.next();
					paramTypes.add(subst.apply(paramType));
				}
				Type returnType = subst.apply(method.returnType);
				return new MethodType(typeParams, paramTypes, returnType);
			}
		}
		
		// We didn't find it; try the substituted parent
		return mtype(methodName, subst.apply(cd.superType));		
	}
	
	/**
	 * Return the MethodBody structure corresponding to the requested
	 * method.
	 * @param methodName the name of the method to lookup
	 * @param typeArgs the type arguments passed to the method call
	 * @param type the type to begin searching in
	 * @return the MethodBody structure
	 * @throws ClassTableException the type could not be found, or it
	 * applied the incorrect number of type arguments
	 */
	public MethodBody mbody(String methodName, List typeArgs,
			NonVariableType type) throws ClassTableException {
		
		// Object has no methods
		if (type.isObject()) {
			throw new ClassTableException("unknown method \"" +
					methodName + "\"");
		}
		ClassDecl cd = lookup(type.className);
		
		// Construct a substitution with the class parameters
		Substitution subst = new Substitution(type.typeArgs, cd.typeParams);
		
		// Look for it in this class
		for (Iterator i = cd.methods.iterator(); i.hasNext();) {
			Method method = (Method) i.next();
			if (method.name.equals(methodName)) {
				
				// We found it; add the method substitutions and go
				Substitution methSubst =
					new Substitution(typeArgs, method.typeParams);
				return new MethodBody(method.paramNames,
						methSubst.apply(subst.apply(method.body)));
			}
		}
		
		// We didn't find it; try the substituted parent
		return mbody(methodName, typeArgs, subst.apply(cd.superType));
	}

}
