package fgj.ast;

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
	 * The list of {@link Type} arguments to apply to the
	 * parameterized method.
	 */
	public final List typeArgs;
	
	/**
	 * The list of {@link Term} expressions to apply as arguments
	 * to the method.
	 */
	public final List args;
	
	/**
	 * Construct a new method invocation term.
	 * @param base the base object
	 * @param methodName the message to send
	 * @param typeArgs the {@link Type} arguments to the method
	 * @param args the {@link Term} arguments to the method
	 */
	public CallTerm(Term base, String methodName, List typeArgs, List args) {
		this.base = base;
		this.methodName = methodName;
		this.typeArgs = Collections.unmodifiableList(typeArgs);
		this.args = Collections.unmodifiableList(args);
	}
	
	public String toString() {
		return base + methodName + typeArgs + args; 
	}
}
