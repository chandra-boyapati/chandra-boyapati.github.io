package fgj.ast;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * AST node representing a method declaration.
 */
public class Method {

	/**
	 * The list of {@linkplain TypeParam type parameters} which this
	 * method accepts.
	 */
	public final List typeParams;
	
	/**
	 * The return type of the method.
	 */
	public final Type returnType;
	
	/**
	 * The name of the method.
	 */
	public final String name;
	
	/**
	 * The list of {@linkplain Declaration parameters} which this
	 * method accepts.
	 */
	public final List params;
	
	/**
	 * The expression evaluated as the body of the method.
	 */
	public final Term body;
	
	/**
	 * The list of the types of <code>params</code>,
	 * provided as a convenience.
	 */
	public final List paramTypes;
	
	/**
	 * The list of the names of <code>params</code>,
	 * provided as a convenience.
	 */
	public final List paramNames;
	
	/**
	 * Construct a new method declaration.
	 * @param typeParams the type parameter list
	 * @param returnType the return type of the method
	 * @param name the name of the method
	 * @param params the {@linkplain Declaration parameter} list
	 * @param body the body of the method
	 */
	public Method(List typeParams, Type returnType, String name,
			List params, Term body) {
		this.typeParams = Collections.unmodifiableList(typeParams);
		this.returnType = returnType;
		this.name = name;
		this.params = Collections.unmodifiableList(params);
		this.body = body;
		
		// Build the param names and types lists for later use
		LinkedList paramTypes = new LinkedList();
		LinkedList paramNames = new LinkedList();
		for (Iterator i = params.iterator(); i.hasNext(); ) {
			Declaration p = (Declaration) i.next();
			paramTypes.addLast(p.type);
			paramNames.addLast(p.name);
		}
		this.paramTypes = Collections.unmodifiableList(paramTypes);
		this.paramNames = Collections.unmodifiableList(paramNames);
	}	
}
