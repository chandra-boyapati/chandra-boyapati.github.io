package fgj.types;

import fgj.main.FeatherweightGJException;

/**
 * An exception raised during class table construction.
 */
public class ClassTableException extends FeatherweightGJException {
	
	/**
	 * Construct a class table exception.
	 * @param message the reason for the exception
	 */
	ClassTableException(String message) {
		super("class table: " + message);
	}
}
