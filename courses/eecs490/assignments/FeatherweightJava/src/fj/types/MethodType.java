package fj.types;

import java.util.Collections;
import java.util.List;

import fj.ast.Type;

/**
 * A structure providing method type information.  Specifically,
 * the types of the method's parameters, and its return type,
 * are provided here as a single unit.
 */
public class MethodType {

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
	 * @param paramTypes the parameter types
	 * @param returnType the return type
	 */
	public MethodType(List paramTypes, Type returnType) {
		this.paramTypes = Collections.unmodifiableList(paramTypes);
		this.returnType = returnType;
	}
}
