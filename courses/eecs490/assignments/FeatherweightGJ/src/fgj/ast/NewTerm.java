package fgj.ast;

import java.util.Collections;
import java.util.List;

/**
 * AST node representing a constructor invocation through
 * <code>new</code>.
 */
public class NewTerm extends Term {

	/**
	 * The type of the object being constructed.
	 */
	public final NonVariableType type;
	
	/**
	 * The list of {@link Term} arguments to the constructor.
	 */
	public final List args;
	
	/**
	 * Construct a new constructor invocation expression.
	 * @param type the type to construct
	 * @param args the {@link Term} arguments to the constructor
	 */
	public NewTerm(NonVariableType type, List args) {
		this.type = type;
		this.args = Collections.unmodifiableList(args);
	}
	
	public String toString() {
		return "new " + type + args;
	}
}
