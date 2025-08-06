package fj.ast;

import java.util.Collections;
import java.util.List;

/**
 * AST node representing a method invocation expression.
 */
public class CallTerm extends Term {

	/**
	 * An expression which must evaluate to an object which
	 * accepts the <code>methodName</code> message.
	 */
	public final Term base;
	
	/**
	 * The method to invoke on <code>base</code>.
	 */
	public final String methodName;
	
	/**
	 * The list of {@link Term} expressions to apply as arguments
	 * to the method.
	 */
	public final List args;
	
	/**
	 * Construct a new method invocation term.
	 * @param base the base object
	 * @param methodName the message to send
	 * @param args the {@link Term} arguments to the method
	 */
	public CallTerm(Term base, String methodName, List args) {
		this.base = base;
		this.methodName = methodName;
		this.args = Collections.unmodifiableList(args);
	}
}
