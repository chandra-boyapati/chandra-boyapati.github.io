package fj.ast;

/**
 * AST node representing a field or parameter declaration.
 */
public class Declaration {

	/**
	 * The type of the variable being declared.
	 */
	public final Type type;
	
	/**
	 * The name of the variable.
	 */
	public final String name;
	
	/**
	 * Construct a new variable declaration.
	 * @param type the type of the variable
	 * @param name the name of the variable
	 */
	public Declaration(Type type, String name) {
		this.type = type;
		this.name = name;
	}
}
