package fj.typecheck;

import fj.main.FeatherweightJavaException;

/**
 * An exception raised during type checking.
 */
public class TypeException extends FeatherweightJavaException {
	
	/**
	 * Construct a type checking exception.
	 * @param str the reason for the exception
	 */
	TypeException(String str) {
		super("type check error: " + str);
	}
}
