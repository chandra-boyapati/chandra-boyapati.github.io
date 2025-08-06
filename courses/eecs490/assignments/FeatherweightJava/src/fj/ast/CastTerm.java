package fj.ast;

/**
 * AST node representing casting of an object from one type
 * to another.
 */
public class CastTerm extends Term {

	/**
	 * The target type, or the type to which to cast.
	 */
	public final Type type;
	
	/**
	 * The expression to cast to <code>type</code>.
	 */
	public final Term term;
	
	/**
	 * Construct a new cast expression.
	 * @param type the type to cast to
	 * @param term the expression to cast
	 */
	public CastTerm(Type type, Term term) {
		this.type = type;
		this.term = term;
	}
}
