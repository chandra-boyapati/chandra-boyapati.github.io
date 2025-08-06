package fj.types;

import fj.main.FeatherweightJavaException;

/**
 * An exception raised during class table construction.
 */
public class ClassTableException extends FeatherweightJavaException {
	
	/**
	 * Construct a class table exception.
	 * @param str the reason for the exception
	 */
	ClassTableException(String str) {
		super("CT: " + str);
	}
}
