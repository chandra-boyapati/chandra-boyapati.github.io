package fj.parse;

import fj.main.FeatherweightJavaException;

/**
 * An exception raised during source file parsing.
 */
public class ParseException extends FeatherweightJavaException {
	
	/**
	 * Construct a new parse exception.
	 * @param str the reason for the exception
	 */
	ParseException(String str) {
		super("parse error: " + str);
	}
}
