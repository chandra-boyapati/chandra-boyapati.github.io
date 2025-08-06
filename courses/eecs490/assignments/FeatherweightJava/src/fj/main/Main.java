package fj.main;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import fj.ast.Program;
import fj.eval.Evaluator;
import fj.parse.Lexer;
import fj.parse.Parser;
import fj.typecheck.TypeChecker;
import fj.types.ClassTable;
import fj.types.ClassTableException;

/**
 * Site of the <code>main</code> method for the FeatherweightJava
 * system.
 */
public class Main {

	protected static class Factory {
		public Program parse(InputStream in) throws Exception {
			return (Program) new Parser(new Lexer(in)).parse().value;
		}
		public ClassTable ClassTable(List classDecls) throws ClassTableException {
			return new ClassTable(classDecls);
		}
		public TypeChecker TypeChecker(ClassTable types) {
			return new TypeChecker(types);
		}
		public Evaluator Evaluator(ClassTable types) {
			return new Evaluator(types);
		}
	}
	
	/**
	 * The main program.  Calls the parser, class table builder,
	 * typechecker, and evaluator.  Prints out the results of
	 * evaluation if successful, or the first error discovered during
	 * any of the above steps.
	 * @param args one argument: the name of the source file to use
	 */
	public static void main(String[] args) {
		runMain("fj", args, new Factory());
	}

	protected static void runMain(String name, String[] args, Factory factory) {
		// We expect exactly one argument
		if (args.length != 1) {
			System.err.println("usage: " + name + " filename." + name);
		}
		else {
			try {
				// Read the input
				InputStream in = new BufferedInputStream(new FileInputStream(args[0]));
				Program program = factory.parse(in);
				
				// Build class table and do rudimentary checks
				ClassTable types = factory.ClassTable(program.classDecls);
				
				// Type check
				TypeChecker check = factory.TypeChecker(types);
				check.checkProgram(program);
				
				// Run the evaluator
				Evaluator eval = factory.Evaluator(types);
				System.out.println(eval.evalTerm(program.term));
			}
			catch (FileNotFoundException e) {
				System.err.println(name + ": can't find file \"" + args[0] + "\"");
			}
			catch (FeatherweightJavaException e) {
				System.out.println(e.getMessage());
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
