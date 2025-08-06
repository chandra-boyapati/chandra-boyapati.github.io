package fj.ast;

import java.util.Collections;
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
	 * The type of this class.
	 */
	public final Type type;
	
	/**
	 * The type extended by this class.
	 */
	public final Type superType;
	
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
	 * Construct a new class declaration.
	 * @param name the name of the class
	 * @param superType the type extended by this class
	 * @param fields the {@linkplain Declaration field declarations}
	 * of this class
	 * @param constructor the constructor of this class
	 * @param methods the {@link Method} objects of this class
	 */
	public ClassDecl(String name, Type superType, List fields,
			Constructor constructor, List methods) {
		this.name = name;
		this.type = new Type(name);
		this.superType = superType;
		this.fields = Collections.unmodifiableList(fields);
		this.constructor = constructor;
		this.methods = Collections.unmodifiableList(methods);
	}
}
