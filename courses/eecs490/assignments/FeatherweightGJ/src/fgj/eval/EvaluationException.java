package fgj.eval;

import fgj.main.FeatherweightGJException;
import fgj.types.ClassTableException;

/**
 * An exception raised during evaluation of a FeatherweightGJ
 * program.  Well-typed FeatherweightGJ programs can only throw
 * one exception during evaluation--executing a bad cast.  All other
 * evaluation problems should be trapped beforehand by the typechecker.
 */
public class EvaluationException extends FeatherweightGJException {
	
	/**
	 * Construct a new evaluation exception.
	 * @param str the reason for the exception
	 */
	EvaluationException(String str) {
		super("evaluation error: " + str);
	}
	
	/**
	 * Construct a exception which is a wrapper around a class
	 * table exception.
	 * @param e the low level exception
	 */
	EvaluationException(ClassTableException e) {
		super(e);
	}
}
