package fj.typecheck;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fj.ast.*;
import fj.types.FieldInfo;
import fj.types.MethodType;
import fj.types.ClassTable;

/**
 * Typechecking of FeatherweightJava programs.  Given a class table,
 * the typechecker ensures that the program is well-typed, such that
 * evaluation of it should not get stuck unless a bad cast is executed.
 *
 * The result of typechecking is either a normal return, indicating
 * success, or throwing a {@link TypeException} with a description of
 * the reason for typechecking failure.  In addition, if typechecking
 * discovers a stupid cast, a message indicating the participating types
 * is emitted on {@link java.lang.System#err}.
 */
public class TypeChecker {

	/**
	 * The class table.
	 */
	protected final ClassTable types;
	
	// TODO: Add additional variables if necessary
	
	/**
	 * Construct a typechecker with the given class table.
	 * @param types the class table
	 */
	public TypeChecker(ClassTable types) {
		this.types = types;
		// TODO: Perform additional initialization if necessary
	}

	/**
	 * Typecheck a {@link Program}.
	 * @param program the program to check
	 * @throws TypeException the program failed typechecking
	 */
	public void checkProgram(Program program) throws TypeException {
		for (Iterator i = program.classDecls.iterator(); i.hasNext(); ) {
			checkClass((ClassDecl) i.next());
		}
		checkTerm(program.term);
	}
	
	/**
	 * Typecheck an expression.
	 * @param t the expression to check
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	protected Type checkTerm(Term t) throws TypeException {
		if (t instanceof VarTerm) {
			return checkVarTerm((VarTerm) t);	
		}
		else if (t instanceof FieldTerm) {
			return checkFieldTerm((FieldTerm) t);
		}
		else if (t instanceof CallTerm) {
			return checkCallTerm((CallTerm) t);
		}
		else if (t instanceof NewTerm) {
			return checkNewTerm((NewTerm) t);
		}
		else if (t instanceof CastTerm) {
			return checkCastTerm((CastTerm) t);
		}
		else {
			throw new Error("Internal typechecker error: invalid term type");
		}
	}
	
	/**
	 * Typecheck a variable access expression.
	 * @param t the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	protected Type checkVarTerm(VarTerm t) throws TypeException {
		// TODO: Implement variable term checking
		throw new TypeException("checkVarTerm unimplemented");
	}
	
	/**
	 * Typecheck a field access expression.
	 * @param t the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	private Type checkFieldTerm(FieldTerm t) throws TypeException {
		// TODO: Implement field term checking
		throw new TypeException("checkFieldTerm unimplemented");
	}
	
	/**
	 * Typecheck a method invocation expression.
	 * @param t the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	private Type checkCallTerm(CallTerm t) throws TypeException {
		// TODO: Implement method invocation term checking
		throw new TypeException("checkCallTerm unimplemented");
	}
	
	/**
	 * Typecheck an object creation expression.
	 * @param t the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	private Type checkNewTerm(NewTerm t) throws TypeException {
		// TODO: Implement constructor invocation term checking
		throw new TypeException("checkNewTerm unimplemented");
	}
	
	/**
	 * Typecheck a type cast expression.  If the cast is stupid,
	 * emit a warning on System.err, specifying the types involved
	 * in the stupid cast.
	 * @param t the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	private Type checkCastTerm(CastTerm t) throws TypeException {
		// TODO: Implement type cast term checking
		throw new TypeException("checkCastTerm unimplemented");
	}

	/**
	 * Typecheck a method declaration in a class.
	 * @param method the method declaration
	 * @param type the type <code>method</code> is declared in
	 * @throws TypeException the method failed typechecking
	 */
	private void checkMethod(Method method, Type type)
		throws TypeException {
		// TODO: Implement method declaration type checking
		throw new TypeException("checkMethod unimplemented");
	}

	/**
	 * Typecheck a class declaration.
	 * @param classDecl the class declaration to check
	 * @throws TypeException the class declaration failed typechecking
	 */
	private void checkClass(ClassDecl classDecl) throws TypeException {
		
		// Get the fields of our parent
		FieldInfo parentInfo = types.fields(classDecl.superType);
		if (parentInfo == null) {
			throw new TypeException("unknown type " + classDecl.superType); 
		}

		// Do some simple checks
		Constructor cons = classDecl.constructor;
		String constructorName = cons.className;
		String className = classDecl.name;
		if (!constructorName.equals(className)) {
			throw new TypeException("constructor name " + constructorName
				+ " does not match class name " + className);
		}
		
		// Make sure the first arguments to the constructor and super() match
		Iterator params = cons.params.iterator();
		Iterator args = cons.superArgs.iterator();
		Iterator superFields = parentInfo.list.iterator();
		
		while (params.hasNext() && args.hasNext() && superFields.hasNext()) {
			Declaration param = (Declaration) params.next();
			String arg = (String) args.next();
			Declaration superField = (Declaration) superFields.next();
			if (!param.type.equals(superField.type) ||
				!param.name.equals(superField.name) ||
				!param.name.equals(arg))
			{
				throw new TypeException("constructor name mismatch");
			}
		}
		
		// At this point, we should have exhausted the super fields
		// and super args
		if (superFields.hasNext() || args.hasNext()) {
			throw new TypeException("didn't account for all constructor"
					+ "fields");
		}
		
		// Now iterate over this class's fields for the remaining
		// constructor args
		Iterator fields = classDecl.fields.listIterator(parentInfo.list.size());
		Iterator inits = cons.inits.iterator();
		while (fields.hasNext() && params.hasNext()) {
			Declaration field = (Declaration) fields.next();
			Declaration param = (Declaration) params.next();
			FieldInit init = (FieldInit) inits.next();
			if (!param.type.equals(field.type) ||
				!param.name.equals(field.name) ||
				!param.name.equals(init.fieldName) ||
				!param.name.equals(init.initName))
			{
				throw new TypeException("bogus initialization");
			}
		}
		
		// If any still have more to go, we have a number mismatch
		if (fields.hasNext() || params.hasNext() || inits.hasNext()) {
			throw new TypeException("incorrect number of arguments" +
					"to constructor");
		}
		
		// Now check all our methods
		for (Iterator i = classDecl.methods.iterator(); i.hasNext(); ) {
			checkMethod((Method) i.next(), classDecl.type);
		}
	}
}
