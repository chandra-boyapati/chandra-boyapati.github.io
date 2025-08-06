package fgj.typecheck;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import fgj.ast.NonVariableType;
import fgj.ast.Type;
import fgj.ast.TypeParam;
import fgj.ast.TypeVariable;
import fgj.types.ClassTable;
import fgj.types.ClassTableException;
import fgj.types.MethodType;
import fgj.types.Substitution;

/**
 * The mapping of type variables to their bounds, denoted &Delta; in the
 * FeatherweightGJ paper.
 */
public class TypeEnvironment {

	/**
	 * The class table.
	 */
	private final ClassTable types;

	/**
	 * The underlying type variable mapping.
	 */
	private final Map env;

	/**
	 * Construct a new type environment using the given class table.
	 * @param types the class table
	 */
	public TypeEnvironment(ClassTable types) {
		this.types = types;
		this.env = new HashMap();
	}

	/**
	 * Add a list of {@link TypeParam} bounds to the environment.
	 * @param bounds the list of bounds to add
	 */
	public void addBounds(List bounds) {
		for (Iterator i = bounds.iterator(); i.hasNext();) {
			TypeParam typeParam = (TypeParam) i.next();
			env.put(typeParam.varName, typeParam.bound);
		}				
	}

	/**
	 * Evaluate the subtype relation on two given types.
	 * @param subType the sub type
	 * @param superType the super type
	 * @return <code>true</code> if <code>subType &lt;: superType</code>;
	 * <code>false</code>otherwise
	 * @throws ClassTableException a class table error occurred
	 */
	public boolean subtype(Type subType, Type superType) 
			throws ClassTableException {
 		// TODO implement subtype
 		return true;
	}

	/**
	 * Check that a given non-variable type meets the well-formedness
	 * criteria.
	 * @param type the non-variable type to check
	 * @throws TypeException the type is not well formed
	 * @throws ClassTableException a class table method threw an exception
	 */
	public void checkWellFormed(Type type) throws TypeException, ClassTableException {		
		if (type instanceof TypeVariable) {
			TypeVariable ty = (TypeVariable) type;
			if (!env.containsKey(ty.name)) {
				throw new TypeException("type variable \"" + ty.name
						+ "\" is not well formed");
			}
		}
		else {
			NonVariableType ty = (NonVariableType) type;
			if (!ty.isObject()) {
				for (Iterator i = ty.typeArgs.iterator(); i.hasNext();) {
					Type typeArg = (Type) i.next();
					checkWellFormed(typeArg);
				}
				List typeParams = types.typeParams(ty.className);
				Substitution subst = new Substitution(ty.typeArgs, typeParams);
				Iterator i = ty.typeArgs.iterator();
				Iterator j = typeParams.iterator();
				while (i.hasNext() && j.hasNext()) {
					Type typeArg = (Type) i.next();
					TypeParam typeParam = (TypeParam) j.next();
					if (!subtype(typeArg, subst.apply(typeParam.bound))) {
						throw new TypeException("type argument must be subtype of bound");
					}
				}
			}
		}
	}	

	/**
	 * Compute the bound of a type.
	 * @param type the type whose bound we seek
	 * @return the bound
	 */
	public NonVariableType bound(Type type) {
		if (type instanceof NonVariableType) {
			return (NonVariableType) type;
		}
		TypeVariable ty = (TypeVariable) type;
		return (NonVariableType) env.get(ty.name);
	}
	
	/**
	 * Determine if a particular method override is valid.
	 * @param methodName the method name
	 * @param superType the type containing the method being overridden
	 * @param methodType the type of the subtype's method
	 * @return <code>true</code> if the override is valid; <code>false</code>
	 * otherwise
	 * @throws ClassTableException a class table method failed
	 */
	public boolean override(String methodName, NonVariableType superType,
			MethodType methodType) throws ClassTableException {
		
		// Get the method type in the super type
		MethodType superMethod = types.mtype(methodName, superType);
		if (superMethod == null) return true;
		
		// Construct a substitution
		List typeArgs = new LinkedList();
		for (Iterator i = methodType.typeParams.iterator(); i.hasNext();) {
			TypeParam typeParam = (TypeParam) i.next();
			typeArgs.add(new TypeVariable(typeParam.varName));
		}
		Substitution subst = new Substitution(typeArgs, superMethod.typeParams);
		
		// The bounds and argument types must be the same under subst
		{
			Iterator i = methodType.typeParams.iterator();
			Iterator j = superMethod.typeParams.iterator();
			while (i.hasNext() && j.hasNext()) {
				TypeParam subParam = (TypeParam) i.next();
				TypeParam superParam = (TypeParam) j.next();
				if (!subParam.bound.equals(subst.apply(superParam.bound))) {
					return false;
				}
			}
			// They must have the same number (subst would have gotten it)
		}
		{
			Iterator i = methodType.paramTypes.iterator();
			Iterator j = superMethod.paramTypes.iterator();
			while (i.hasNext() && j.hasNext()) {
				Type subParam = (Type) i.next();
				Type superParam = (Type) j.next();
				if (!subParam.equals(subst.apply(superParam))) {
					return false;
				}
			}
			// Check for same number of arguments
			if (i.hasNext() || j.hasNext()) {
				return false;
			}
		}
		
		// The return type must be a subtype (doesn't have to be identical)
		if (!subtype(methodType.returnType, subst.apply(superMethod.returnType))) {
			return false;
		}
				
		// Passed all the tests
		return true;
	}
	
	/**
	 * Determine if a given downcast is valid.
	 * @param castType the type being cast to
	 * @param termType the original type of the term
	 * @return <code>true</code> if the downcast is valid;
	 * <code>false</code> otherwise
	 */
	public boolean downcast(NonVariableType castType, Type termType) {
		return true;
	}
}
