package fj.types;

import java.util.Collections;
import java.util.Map;

import fj.ast.Type;

/**
 * A structure providing easy access to class information.  The
 * methods of this class provide a way to get a method by name, and
 * fields either in order in a list, or by name from a hash. 
 */
public class ClassInfo {
	
	/**
	 * The superclass of this class.
	 */
	public final Type superType;
	
	/**
	 * Information about this class's fields.
	 */
	public final FieldInfo fieldInfo;
	
	/**
	 * A mapping from method names to method objects.
	 * @see fj.ast.Method
	 */
	public final Map methodMap;

	/**
	 * Construct a new ClassInfo object.
	 * @param superType the superclass of this class
	 * @param fieldInfo the field information
	 * @param methodMap the mapping of names to methods
	 */
	ClassInfo(Type superType, FieldInfo fieldInfo, Map methodMap) {
		this.superType = superType;
		this.fieldInfo = fieldInfo;
		this.methodMap = Collections.unmodifiableMap(methodMap);
	}
	
	ClassInfo(Type superType) {
		this.superType = superType;
		this.fieldInfo = null;
		this.methodMap = null;
	}
}
