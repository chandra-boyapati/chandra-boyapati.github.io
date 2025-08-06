package fj.types;

import java.util.Collections;
import java.util.List;

import fj.ast.Term;

/**
 * A structure providing information about the body of a method.
 * Specifically, the names of the method's parameters, and the
 * method body expression, are provided here as a single unit.
 */
public class MethodBody {

	/**
	 * The names of the formal parameters of the method.
	 */
	public final List paramNames;
	
	/**
	 * The body expression of the method.
	 */
	public final Term term;
	
	/**
	 * Construct a new method body structure.
	 * @param paramNames the names of the parameters
	 * @param term the method body expression
	 */
	public MethodBody(List paramNames, Term term) {
		this.paramNames = Collections.unmodifiableList(paramNames);
		this.term = term;
	}
}
