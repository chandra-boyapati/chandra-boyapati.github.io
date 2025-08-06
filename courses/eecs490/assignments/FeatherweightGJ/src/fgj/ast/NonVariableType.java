package fgj.ast;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * AST node representing a non-variable type.  A non-variable
 * type consists of an actual class name, together with type
 * parameters which may be variable or non-variable.  Casts,
 * <code>new</code> expressions, and type parameter bounds all
 * require non-variable types.
 */
public class NonVariableType extends Type {

	/**
	 * The name of the actual class being referenced.
	 */
	public final String className;
	
	/**
	 * The list of type arguments to supply to <code>className</code>.
	 */
	public final List typeArgs;
	
	/**
	 * Construct a new non-variable type.
	 * @param className the name of the actual class
	 * @param typeArgs the list of type arguments 
	 */
	public NonVariableType(String className, List typeArgs) {
		this.className = className;
		this.typeArgs = Collections.unmodifiableList(typeArgs);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String str = className;
		if (!typeArgs.isEmpty()) {
			Iterator i = typeArgs.iterator();
			str += "<" + i.next();
			while (i.hasNext()) str += "," + i.next();
			str += ">";
		}
		return str;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		if (!(o instanceof NonVariableType)) return false;
		NonVariableType t = (NonVariableType) o;
		if (!t.className.equals(this.className)) return false;
		return t.typeArgs.equals(this.typeArgs);
	}

	/**
	 * Return <code>true</code> if this non-variable type
	 * refers to <code>Object</code>, the root of the
	 * FeatherweightGJ class hierarchy.
	 * @return <code>true</code> if this non-variable type
	 * refers to <code>Object</code>; <code>false</code> otherwise
	 */
	public boolean isObject() {
		return equals(OBJECT);
	}
	
	/**
	 * The non-variable type <code>Object</code>. 
	 */
	private static final NonVariableType OBJECT =
		new NonVariableType("Object", Collections.EMPTY_LIST);
}
