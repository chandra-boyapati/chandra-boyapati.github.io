package fgj.types;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fgj.ast.*;

/**
 * Utility class which performs type substitutions.  Given type
 * arguments and a list of type parameters, objects of this class
 * can be used to perform type substitutions on types and terms.
 */
public class Substitution {
	
	/**
	 * Mapping of type variable names to types.
	 */
	private final Map varMap;
	
	/**
	 * Construct a substitution, mapping parameter names to arguments.
	 * @param typeArgs the arguments supplied to the type parameters
	 * @param typeParams the list of parameters (names and bounds)
	 * @throws ClassTableException an incorrect number of arguments was supplied
	 */
	public Substitution(List typeArgs, List typeParams) throws ClassTableException {
		varMap = new HashMap();
		Iterator i = typeArgs.iterator();
		Iterator j = typeParams.iterator();
		while (i.hasNext() && j.hasNext()) {
			Type type = (Type) i.next();
			TypeParam typeParam = (TypeParam) j.next();
			varMap.put(typeParam.varName, type);
		}
		if (i.hasNext() || j.hasNext()) {
			throw new ClassTableException("incorrect number of type arguments");
		}
	}
	
	/**
	 * Apply the substitution to the type arguments of a non-variable type.
	 * @param type the non-variable type on which to substitute
	 * @return the substituted type
	 */
	public NonVariableType apply(NonVariableType type) {
		List typeArgs = new LinkedList();
		for (Iterator i = type.typeArgs.iterator(); i.hasNext();) {
			Type t = (Type) i.next();
			typeArgs.add(apply(t));
		}
		return new NonVariableType(type.className, typeArgs);
	}
	
	/**
	 * Apply the substitution to a type variable or a non-variable type.
	 * @param type the type on which to perform the substitution
	 * @return the substituted type
	 */
	public Type apply(Type type) {
		
		// Non-variables are special
		if (type instanceof NonVariableType) {
			return apply((NonVariableType) type);
		}
		
		// Type variables get replaced if they're in the substitution
		Type ty = (Type) varMap.get(((TypeVariable) type).name);
		return (ty == null) ? type : ty;
	}

	/**
	 * Apply the substitution to the types embedded with an expression. 
	 * @param term the expression on which to perform the substitution
	 * @return the substituted term
	 */
	public Term apply(Term term) {
		
		// Fields: just update the base
		if (term instanceof FieldTerm) {
			FieldTerm t = (FieldTerm) term;
			return new FieldTerm(apply(t.base), t.fieldName);
		}
		
		// Calls: update the base, type arguments, and arguments
		else if (term instanceof CallTerm) {
			CallTerm t = (CallTerm) term;
			Term base = apply(t.base);
			List typeArgs = new LinkedList();
			for (Iterator i = t.typeArgs.iterator(); i.hasNext();) {
				Type typeArg = (Type) i.next();
				typeArgs.add(apply(typeArg));
			}
			List args = new LinkedList();
			for (Iterator i = t.args.iterator(); i.hasNext();) {
				Term arg = (Term) i.next();
				args.add(apply(arg));
			}
			return new CallTerm(base, t.methodName, typeArgs, args);
		}
		
		// New's: update the type and the arguments
		else if (term instanceof NewTerm) {
			NewTerm t = (NewTerm) term;
			List args = new LinkedList();
			for (Iterator i = t.args.iterator(); i.hasNext();) {
				Term arg = (Term) i.next();
				args.add(apply(arg));
			}
			return new NewTerm(apply(t.type), args);
		}
		
		// Casts: update the cast type and the base term
		else if (term instanceof CastTerm) {
			CastTerm t = (CastTerm) term;
			return new CastTerm(apply(t.type), apply(t.term));
		}
		
		// Everything else (basically Var's): do nothing
		else {
			return term;
		}
	}
}
