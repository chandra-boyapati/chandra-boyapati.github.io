package fgj.typecheck;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fgj.types.ClassTable;
import fgj.types.ClassTableException;
import fgj.types.MethodType;
import fgj.types.Substitution;
import fgj.typecheck.TypeEnvironment;
import fgj.ast.*;

/**
 * Typechecking of FeatherweightGJ programs.  Given a class table,
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
	private final ClassTable types;

	/**
	 * Construct a typechecker with the given class table.
	 * @param types the class table
	 */
	public TypeChecker(ClassTable types) {
		this.types = types;
	}
	
	/**
	 * Typecheck a {@link Program}.
	 * @param program the program to check
	 * @throws TypeException the program failed typechecking
	 */
	public void checkProgram(Program program) throws TypeException {
		try {
			for (Iterator i = program.classDecls.iterator(); i.hasNext();) {
				ClassDecl cd = (ClassDecl) i.next();
				checkClass(cd);
			}
			checkTerm(new TypeEnvironment(types), new HashMap(), program.term);
		}
		catch (ClassTableException e) {
			throw new TypeException(e);
		}
	}
	
	/**
	 * Typecheck a class declaration.
	 * @param classDecl the class declaration to check
	 * @throws TypeException the class declaration failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private void checkClass(ClassDecl classDecl)
		throws TypeException, ClassTableException {
		
		// Prepare a type environment
		TypeEnvironment typeEnv = new TypeEnvironment(types);
		typeEnv.addBounds(classDecl.typeParams);
		
		// Make sure all the types are well-formed
		for (Iterator i = classDecl.typeParams.iterator(); i.hasNext();) {
			TypeParam typeParam = (TypeParam) i.next();
			typeEnv.checkWellFormed(typeParam.bound);
		}
		typeEnv.checkWellFormed(classDecl.superType);
		for (Iterator i = classDecl.fields.iterator(); i.hasNext();) {
			Declaration field = (Declaration) i.next();
			typeEnv.checkWellFormed(field.type);
		}
		
		// Check constructor parameters
		Iterator i = classDecl.constructor.params.iterator();
		Iterator j = types.fields(classDecl.superType).iterator();
		Iterator k = classDecl.constructor.superArgs.iterator();
		Iterator l = classDecl.constructor.inits.iterator();
		while (i.hasNext() && j.hasNext() && k.hasNext()) {
			Declaration field = (Declaration) j.next();
			if (!i.next().equals(field)) {
				throw new TypeException("constructor parameter error");
			}
			if (!k.next().equals(field.name)) {
				throw new TypeException("constructor super argument error");
			}
		}
		if (j.hasNext() || k.hasNext()) {
			throw new TypeException("didn't cover all the supertype's fields");
		}
		while (i.hasNext() && l.hasNext()) {
			Declaration field = (Declaration) i.next();
			FieldInit init = (FieldInit) l.next();
			if (!field.name.equals(init.fieldName)) {
				throw new TypeException("constructor field parameter error");
			}
		}
		if (i.hasNext() || l.hasNext()) {
			throw new TypeException("too many fields or arguments");
		}
		
		// Make sure the methods are okay
		for (Iterator m = classDecl.methods.iterator(); m.hasNext();) {
			Method method = (Method) m.next();
			checkMethod(method, classDecl);
		}
	}
	
	/**
	 * Typecheck a method declaration.
	 * @param method the method to check
	 * @param classDecl the enclosing class
	 * @throws TypeException the method declaration failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private void checkMethod(Method method, ClassDecl classDecl)
		throws TypeException, ClassTableException {
 		// TODO implement checkMethod
 		throw new TypeException("checkMethod unimplemented");
	}
	
	/**
	 * Typecheck an expression.
	 * @param typeEnv the type environment
	 * @param varEnv the variable environment
	 * @param term the term to check
	 * @return the type of the expression
	 * @throws TypeException the term failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private Type checkTerm(TypeEnvironment typeEnv, Map varEnv,
			Term term) throws TypeException, ClassTableException {
		
		if (term instanceof VarTerm) {
			return checkVarTerm(typeEnv, varEnv, (VarTerm) term);
		}
		else if (term instanceof FieldTerm) {
			return checkFieldTerm(typeEnv, varEnv, (FieldTerm) term);
		}
		else if (term instanceof CallTerm) {
			return checkCallTerm(typeEnv, varEnv, (CallTerm) term);
		}
		else if (term instanceof NewTerm) {
			return checkNewTerm(typeEnv, varEnv, (NewTerm) term);
		}
		else {
			return checkCastTerm(typeEnv, varEnv, (CastTerm) term);
		}
	}
	
	/**
	 * Typecheck a variable access expression.
	 * @param typeEnv the type environment
	 * @param varEnv the variable environment
	 * @param term the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 */
	private Type checkVarTerm(TypeEnvironment typeEnv, Map varEnv,
			VarTerm term) throws TypeException {
		// TODO implement checkVarTerm
		throw new TypeException("checkVarTerm unimplemented");
	}

	/**
	 * Typecheck a field access expression.
	 * @param typeEnv the type environment
	 * @param varEnv the variable environment
	 * @param term the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private Type checkFieldTerm(TypeEnvironment typeEnv, Map varEnv,
			FieldTerm term)	throws TypeException, ClassTableException {
		// TODO implement checkFieldTerm
		throw new TypeException("checkFieldTerm unimplemented");
	}
	
	/**
	 * Typecheck a method invocation expression.
	 * @param typeEnv the type environment
	 * @param varEnv the variable environment
	 * @param term the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private Type checkCallTerm(TypeEnvironment typeEnv, Map varEnv,
			CallTerm term) throws TypeException, ClassTableException {
		// TODO implement checkCallTerm
		throw new TypeException("checkCallTerm unimplemented");
	}
	
	/**
	 * Typecheck an object creation expression.
	 * @param typeEnv the type environment
	 * @param varEnv the variable environment
	 * @param term the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private Type checkNewTerm(TypeEnvironment typeEnv, Map varEnv,
			NewTerm term) throws TypeException, ClassTableException {
		// TODO implement checkNewTerm
		throw new TypeException("checkNewTerm unimplemented");
	}
	
	/**
	 * Typecheck a type cast expression.  If the cast is stupid,
	 * emit a warning on {@link java.lang.System#err},
	 * specifying the types involved in the stupid cast.
	 * @param typeEnv the type environment
	 * @param varEnv the variable environment
	 * @param term the expression to typecheck
	 * @return the type of the expression
	 * @throws TypeException the expression failed typechecking
	 * @throws ClassTableException a class table operation failed
	 */
	private Type checkCastTerm(TypeEnvironment typeEnv, Map varEnv,
			CastTerm term) throws TypeException, ClassTableException {
		// TODO implement checkCastTerm
		throw new TypeException("checkCastTerm unimplemented");
	}
}
