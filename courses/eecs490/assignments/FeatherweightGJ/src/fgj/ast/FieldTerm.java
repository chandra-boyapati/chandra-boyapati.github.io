package fgj.ast;

/**
 * AST node representing the accessing of a field of an object.
 */
public class FieldTerm extends Term {

	/**
	 * An expression which must evaluate to an object which has
	 * a field named <code>fieldName</code>.
	 */
	public final Term base;
	
	/**
	 * The name of the field being accessed.
	 */
	public final String fieldName;
	
	/**
	 * Construct a new field access expression.
	 * @param base the expression to dereference
	 * @param fieldName the name of the field to access
	 */
	public FieldTerm(Term base, String fieldName) {
		this.base = base;
		this.fieldName = fieldName;
	}
	
	public String toString() {
		return base + "." + fieldName;
	}
}
