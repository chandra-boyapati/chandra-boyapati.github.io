package fj.eval;

import fj.main.FeatherweightJavaException;

/**
 * An exception raised during evaluation of a FeatherweightJava
 * program.  Well-typed FeatherweightJava programs can only throw
 * one exception during evaluation--executing a bad cast.  All other
 * evaluation problems should be trapped beforehand by the typechecker.
 */
public class EvaluationException extends FeatherweightJavaException {
	
	/**
	 * Construct a new evaluation exception.
	 * @param str the reason for the exception
	 */
	public EvaluationException(String str) {
		super("evaluation error: " + str);
	}
}
