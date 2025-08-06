package fj.ast;

/**
 * AST node representing the reference of a variable.
 */
public class VarTerm extends Term {

	/**
	 * The name of the variable being referenced.
	 */
	public final String name;
	
	/**
	 * Construct a new variable reference.
	 * @param name the name of the variable being referenced
	 */
	public VarTerm(String name) {
		this.name = name;
	}
}
