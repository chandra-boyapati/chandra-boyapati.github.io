package fgj.ast;

/**
 * AST node representing the use of a type variable as a type.
 * The use of a type variable always occurs in the scope of a class
 * or method which declared it in its {@link TypeParam} list.
 */
public class TypeVariable extends Type {

	/**
	 * The name of the type variable.
	 */
	public final String name;
	
	/**
	 * Construct a new type variable.
	 * @param name the name of the type variable
	 */
	public TypeVariable(String name) {
		this.name = name;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (!(o instanceof TypeVariable)) return false;
		return ((TypeVariable) o).name.equals(this.name);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return name;
	}
}
