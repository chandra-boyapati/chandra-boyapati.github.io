package fgj.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;

import fgj.ast.Program;
import fgj.eval.Evaluator;
import fgj.parse.Lexer;
import fgj.parse.Parser;
import fgj.typecheck.TypeChecker;
import fgj.types.ClassTable;

/**
 * Site of the <code>main</code> method for the FeatherweightGJ
 * system.
 */
public class Main {

	/**
	 * The main program.  Calls the parser, class table builder,
	 * typechecker, and evaluator.  Prints out the results of
	 * evaluation if successful, or the first error discovered during
	 * any of the above steps.
	 * @param args one argument: the name of the source file to use
	 */
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("usage: fgj filename.fgj");
			return;
		}
		
		try {
			Reader reader = new BufferedReader(new FileReader(args[0]));
			Program program = new Parser(new Lexer(reader)).parse();
			ClassTable types = new ClassTable(program.classDecls);
			TypeChecker checker = new TypeChecker(types);
			checker.checkProgram(program);
			Evaluator eval = new Evaluator(types);
			System.out.println(eval.evalTerm(program.term));
		}
		catch (FileNotFoundException e) {
			System.err.println("can't find file \"" + args[0] + "\"");
		}
		catch (FeatherweightGJException e) {
			System.err.println(e.getMessage());
		}
	}

}
