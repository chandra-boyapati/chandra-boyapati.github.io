package fgj.ast;

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
	
	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		Declaration d = (Declaration) o;
		return this.name.equals(d.name) && this.type.equals(d.type);
	}
}
