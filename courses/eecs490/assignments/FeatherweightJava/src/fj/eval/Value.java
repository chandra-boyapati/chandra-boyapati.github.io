package fj.eval;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import fj.ast.Type;

/**
 * The result of a computation in FeatherweightJava.  A value in FJ
 * is an object, which has a type and values assigned to its fields,
 * or equivalently, is a constructor invocation with values for each
 * constructor parameter.
 */
public class Value {
	
	/**
	 * The type of this object.
	 */
	public final Type type;
	
	/**
	 * The values assigned to this object's fields, or equivalently,
	 * the values provided to <code>type</code>'s constructor.
	 */
	public final List args;

	/**
	 * Construct a new value.
	 * @param type the type of the value
	 * @param args the values of its fields
	 */
	public Value(Type type, List args) {
		this.type = type;
		this.args = Collections.unmodifiableList(args);
	}
	
	/**
	 * The string representation of a value.
	 */
	public String toString() {
		String str = "new " + type;
		if (args.isEmpty()) return str + "()";
		str += "(" + args.get(0).toString();
		for (Iterator i = args.listIterator(1); i.hasNext(); ) {
			str += ", " + i.next().toString();
		}
		return str + ")";
	}	
}
