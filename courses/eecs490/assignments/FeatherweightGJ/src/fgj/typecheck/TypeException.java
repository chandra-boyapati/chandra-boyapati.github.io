package fgj.typecheck;

import fgj.main.FeatherweightGJException;
import fgj.types.ClassTableException;

/**
 * An exception raised during type checking.
 */
public class TypeException extends FeatherweightGJException {

	/**
	 * Construct a type checking exception.
	 * @param message the reason for the exception
	 */
	TypeException(String message) {
		super("type check error: " + message);
	}
	
	/**
	 * Construct an exception which is a wrapper around a class
	 * table exception.
	 * @param e the low level exception
	 */
	TypeException(ClassTableException e) {
		super(e);
	}
}
