package fgj.ast;

/**
 * AST node representing a field initialization statement in
 * a constructor.
 */
public class FieldInit {

	/**
	 * The name of the field being initialized.
	 */
	public final String fieldName;
	
	/**
	 * The name of the constructor argument being assigned to the
	 * field.
	 */
	public final String initName;
	
	/**
	 * Construct a new field initialization statement.
	 * @param fieldName the name of the field to assign
	 * @param initName the name of the constructor argument
	 */
	public FieldInit(String fieldName, String initName) {
		this.fieldName = fieldName;
		this.initName = initName;
	}
}
