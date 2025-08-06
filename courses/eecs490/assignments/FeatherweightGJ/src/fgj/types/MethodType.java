package fgj.types;

import java.util.Collections;
import java.util.List;

import fgj.ast.Method;
import fgj.ast.Type;

/**
 * A structure providing method type information.  Specifically,
 * the names and bounds of the type parameters of the method,
 * the types of the method's parameters, and its return type,
 * are provided here as a single unit.
 */
public class MethodType {

	/**
	 * The type parameters of the method (names and bounds).
	 */
	public final List typeParams;

	/**
	 * The types of the method's parameters.
	 */
	public final List paramTypes;
	
	/**
	 * The return type of the method.
	 */
	public final Type returnType;
	
	/**
	 * Construct a new method type structure.
	 * @param typeParams the type parameters (names and bounds)
	 * @param paramTypes the parameter types
	 * @param returnType the return type
	 */
	MethodType(List typeParams, List paramTypes, Type returnType) {
		this.typeParams = Collections.unmodifiableList(typeParams);
		this.paramTypes = Collections.unmodifiableList(paramTypes);
		this.returnType = returnType;
	}
	
	/**
	 * Construct a new method type structure directly from a method.
	 * @param method the method
	 */
	public MethodType(Method method) {
		this(method.typeParams, method.paramTypes, method.returnType);
	}
}
