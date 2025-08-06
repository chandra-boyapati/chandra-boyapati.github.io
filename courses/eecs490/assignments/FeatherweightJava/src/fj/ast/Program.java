package fj.ast;

import java.util.Collections;
import java.util.List;

/**
 * AST node representing a FeatherweightJava source file.  This is
 * the root of the syntax tree created by the parser.
 */
public class Program {

	/**
	 * The list of {@link ClassDecl} class declarations.
	 */
	public final List classDecls;
	
	/**
	 * The expression to evaluate.
	 */
	public final Term term;
	
	/**
	 * Construct a new FeatherweightJava program.
	 * @param classDecls the {@link ClassDecl} list
	 * @param term the expression to evaluate
	 */
	public Program(List classDecls, Term term) {
		this.classDecls = Collections.unmodifiableList(classDecls);
		this.term = term;
	}
}
