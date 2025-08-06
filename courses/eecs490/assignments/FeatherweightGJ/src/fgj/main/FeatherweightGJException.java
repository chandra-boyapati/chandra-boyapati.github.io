package fgj.main;

/**
 * Base class of exceptions thrown by the FeatherweightGJ
 * typechecker and evaluator.
 */
public abstract class FeatherweightGJException extends Exception {
	
	/**
	 * Construct a new FGJ exception.
	 * @param message the reason for the exception
	 */
	public FeatherweightGJException(String message) {
		super(message);
	}
	
	/**
	 * Construct a FGJ exception which is a wrapper around another,
	 * lower-level exception.
	 * @param e the low level exception
	 */
	protected FeatherweightGJException(Exception e) {
		super(e);
	}
}
