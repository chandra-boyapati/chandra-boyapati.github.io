package fgj.eval;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fgj.ast.*;
import fgj.types.ClassTableException;
import fgj.types.MethodBody;
import fgj.types.ClassTable;
import fgj.typecheck.TypeEnvironment;

/**
 * The core of the FeatherweightGJ evaluation engine.  Given a class
 * table, an evaluator maps abstract {@link Term} objects into
 * concrete {@link Value} objects, which may then be printed nicely.
 */
public class Evaluator {
	
	/**
	 * Mapping of variables to values.  In a method
	 * invocation, the values of the arguments passed must be mapped
	 * to the names of the formal parameters, for use in
	 * {@link VarTerm} expressions.
	 */
	private final Map env;
	
	/**
	 * The parent of this evaluator.  When evaluating a method
	 * invocation within a method body, additional variable-to-value
	 * mappings must be created, and the sets of variable names may
	 * overlap.  We handle this by evaluating the inner method in
	 * a new evaluator, with its new mappings, as well as a pointer
	 * to the outer evaluator with its old mappings.
	 */
	private final Evaluator parent;	

	/**
	 * The class table.
	 */
	private final ClassTable types;

	/**
	 * Construct an evaluator with the given class table.
	 * @param types the class table
	 */
	public Evaluator(ClassTable types) {
		this.env = Collections.EMPTY_MAP;
		this.parent = null;
		this.types = types;
	}
	
	/**
	 * Construct a nested class table.
	 * @param env the new environment to append
	 * @param parent the evaluator with the outer environment
	 */
	private Evaluator(Map env, Evaluator parent) {
		this.env = env;
		this.parent = parent;
		this.types = parent.types;
	}

	/**
	 * Evaluate an expression to a value.
	 * @param t the expression to evaluate
	 * @return the value of the expression
	 * @throws EvaluationException the expression could not be
	 * evaluated
	 */
	public Value evalTerm(Term t) throws EvaluationException {
		try {
			if (t instanceof VarTerm) {
				return evalVarTerm((VarTerm) t);	
			}
			else if (t instanceof FieldTerm) {
				return evalFieldTerm((FieldTerm) t);
			}
			else if (t instanceof CallTerm) {
				return evalCallTerm((CallTerm) t);
			}
			else if (t instanceof NewTerm) {
				return evalNewTerm((NewTerm) t);
			}
			else {
				return evalCastTerm((CastTerm) t);
			}
		}
		catch (ClassTableException e) {
			throw new EvaluationException(e);
		}
	}
	
	/**
	 * Evaluate a variable access expression.
	 * @param t the expression to evaluate
	 * @return the value of the expression
	 * @throws EvaluationException <code>t</code> references
	 * an unknown variable
	 */
	private Value evalVarTerm(VarTerm t) throws EvaluationException {
		
		// Rule E-InvkNew: variables are mapped to values on invocation
		Value value = (Value) env.get(t.name);
		if (value == null) {
			if (parent != null) {
				return parent.evalVarTerm(t);
			}
			else {
				throw new EvaluationException("unknown variable");
			}
		}
		else {
			return value;
		}
	}
	
	/**
	 * Evaluate a field access expression.
	 * @param t the expression to evaluate
	 * @return the value of the expression
	 * @throws EvaluationException the base expression could not
	 * be evaluated, or did not have the requested field
	 * @throws ClassTableException the base expression had an
	 * unknown type
	 */
	private Value evalFieldTerm(FieldTerm t)
		throws EvaluationException, ClassTableException {
		
		// Rule E-Field: evaluate the base first
		Value base = evalTerm(t.base);
		
		// Rule E-ProjNew: project out the field we want
		Iterator i = types.fields(base.type).iterator();
		Iterator j = base.args.iterator();
		while (i.hasNext() && j.hasNext()) {
			Declaration field = (Declaration) i.next();
			Value value = (Value) j.next();
			if (field.name.equals(t.fieldName)) return value;
		}
		throw new EvaluationException("unknown field");
	}
	
	/**
	 * Evaluate a method invocation expression.
	 * @param t the expression to evaluate
	 * @return the value of the expression
	 * @throws EvaluationException the message receiver or any of the
	 * message arguments could not be evaluated, the expression provided
	 * an incorrect number of arguments
	 * @throws ClassTableException the receiver did not
	 * implement the requested method
	 */
	private Value evalCallTerm(CallTerm t)
		throws EvaluationException, ClassTableException {
		
		// Rule E-Invk-Recv: evaluate the message recipient
		Value base = evalTerm(t.base);
		
		// Rule E-Invk-Arg: evaluate the arguments to the method
		List args = evalArgs(t.args);
		
		// Rule E-InvkNew: get the method arguments and body
		MethodBody body = types.mbody(t.methodName, t.typeArgs, base.type);
	
		// Rule E-InvkNew: perform the substitution on arguments 				
		Map newEnv = new HashMap();
		Iterator i = body.paramNames.iterator();
		Iterator j = args.iterator();
		while (i.hasNext() && j.hasNext()) {
			String varName = (String) i.next();
			Value value = (Value) j.next();
			newEnv.put(varName, value);
		}
		if (i.hasNext() || j.hasNext()) {
			throw new EvaluationException("incorrect number of arguments");
		}

		// Rule E-InvkNew: perform the "this" substitution
		newEnv.put("this", base);
		
		// Rule E-InvkNew: evaluate the body in the new environment 			
		return (new Evaluator(newEnv, this)).evalTerm(body.term);
	}
	
	/**
	 * Evaluate an object creation expression.
	 * @param t the expression to evaluate
	 * @return the value of the expression
	 * @throws EvaluationException an argument to the constructor
	 * could not be evaluated
	 */
	private Value evalNewTerm(NewTerm t) throws EvaluationException {
		
		// Rule E-New-Arg: evaluate the arguments to the constructor
		List args = evalArgs(t.args);
		
		// "New"s are terminal in this semantics
		return new Value(t.type, args);
	}
	
	/**
	 * Evaluate a type cast expression.
	 * @param t the expression to evaluate
	 * @return the value of the expression
	 * @throws EvaluationException the base expression could not
	 * be evaluated, or the cast failed
	 * @throws ClassTableException a class table error occurred
	 */
	private Value evalCastTerm(CastTerm t)
			throws EvaluationException, ClassTableException {
		
		// Rule E-Cast: evaluate the term being casted
		Value value = evalTerm(t.term);
		
		// Rule E-CastNew: just return the value if the cast is okay
		if (new TypeEnvironment(types).subtype(value.type, t.type)) {
			return value;
		}
		else {
			throw new EvaluationException("invalid cast");
		}
	}
	
	/**
	 * Evaluate a list of expressions.
	 * @param terms the {@link Term} list to evaluate
	 * @return the resulting {@link Value} list
	 * @throws EvaluationException an expression in the list could
	 * not be evaluated
	 */
	private List evalArgs(List terms) throws EvaluationException {
		LinkedList values = new LinkedList();
		
		// Both E-Invk-Arg and E-New-Arg process terms left to right		
		for (Iterator i = terms.iterator(); i.hasNext(); ) {
			values.addLast(evalTerm((Term) i.next()));
		}
		return values;
	}
}
