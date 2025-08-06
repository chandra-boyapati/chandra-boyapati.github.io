package fgj.parse;

import fgj.main.FeatherweightGJException;

/**
 * An exception raised during source file parsing.
 */
public class ParseException extends FeatherweightGJException {
	
	/**
	 * Construct a new parse exception.
	 * @param lineNumber the line of the source file where the
	 * exception occurred
	 * @param str the reason for the exception
	 */
	ParseException(int lineNumber, String str) {
		super("parse error on line " + lineNumber + ": " + str);
	}
}
