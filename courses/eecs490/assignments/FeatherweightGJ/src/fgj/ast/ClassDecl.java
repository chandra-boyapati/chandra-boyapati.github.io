package fgj.ast;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * AST node representing a class declaration.
 */
public class ClassDecl {

	/**
	 * The name of this class.
	 */
	public final String name;
	
	/**
	 * The type parameters for this parameterized class.
	 */
	public final List typeParams;
	
	/**
	 * The type extended by this class.
	 */
	public final NonVariableType superType;
	
	/**
	 * The list of {@linkplain Declaration field declarations}
	 * for this class.
	 */
	public final List fields;
	
	/**
	 * The constructor of this class.
	 */
	public final Constructor constructor;
	
	/**
	 * The list of {@link Method} objects for this class.
	 */
	public final List methods;

	/**
	 * The non-variable type of this class together with its type parameters.
	 */
	public final NonVariableType thisType;
	
	/**
	 * Construct a new class declaration.
	 * @param name the name of the class
	 * @param typeParams the type parameters to the class
	 * @param superType the type extended by this class
	 * @param fields the {@linkplain Declaration field declarations}
	 * of this class
	 * @param constructor the constructor of this class
	 * @param methods the {@link Method} objects of this class
	 */
	public ClassDecl(String name, List typeParams, 
			NonVariableType superType, List fields,
			Constructor constructor, List methods) {
		this.name = name;
		this.typeParams = Collections.unmodifiableList(typeParams);
		this.superType = superType;
		this.fields = Collections.unmodifiableList(fields);
		this.constructor = constructor;
		this.methods = Collections.unmodifiableList(methods);

		List typeArgs = new LinkedList();
		for (Iterator i = typeParams.iterator(); i.hasNext();) {
			TypeParam typeParam = (TypeParam) i.next();
			typeArgs.add(new TypeVariable(typeParam.varName));
		}
		this.thisType = new NonVariableType(name, typeArgs);
	}
}
