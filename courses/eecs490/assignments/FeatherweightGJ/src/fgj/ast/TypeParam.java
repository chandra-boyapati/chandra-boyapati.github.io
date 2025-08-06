package fgj.ast;

/**
 * AST node representing a type parameter; that is, a type
 * variable name together with a non-variable bound.  Both classes
 * and methods possess lists of these.
 */
public class TypeParam {

	/**
	 * The name of the type parameter.
	 */
	public final String varName;
	
	/**
	 * The non-variable type bound.
	 */
	public final NonVariableType bound;
	
	/**
	 * Construct a new type parameter.
	 * @param varName the type parameter name
	 * @param bound its non-variable bound
	 */
	public TypeParam(String varName, NonVariableType bound) {
		this.varName = varName;
		this.bound = bound;
	}
}
