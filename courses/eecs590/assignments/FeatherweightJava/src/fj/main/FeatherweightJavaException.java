package fj.main;

/**
 * Base class of exceptions thrown by the FeatherweightJava
 * typechecker and evaluator.
 */
public abstract class FeatherweightJavaException extends Exception {
	
	/**
	 * Construct a new FJ exception.
	 * @param str the reason for the exception
	 */
	protected FeatherweightJavaException(String str) {
		super(str);
	}
}
