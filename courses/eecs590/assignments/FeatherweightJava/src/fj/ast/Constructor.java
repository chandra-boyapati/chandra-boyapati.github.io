package fj.ast;

import java.util.Collections;
import java.util.List;

/**
 * AST node representing a constructor of objects of a class.
 */
public class Constructor {

	/**
	 * The name of the class whose objects this constructor
	 * constructs.
	 */
	public final String className;
	
	/**
	 * The list of {@linkplain Declaration parameters} which this
	 * constructor accepts.
	 */
	public final List params;
	
	/**
	 * The list of {@link Term} expressions applied to the
	 * <code>super</code> constructor.
	 */
	public final List superArgs;
	
	/**
	 * The list of {@link FieldInit} objects initializing the
	 * fields of this class.
	 */
	public final List inits;
	
	/**
	 * Construct a new constructor node.
	 * @param className the name of the class to construct
	 * @param params the list of {@linkplain Declaration parameters}
	 * @param superArgs the {@link Term} arguments to
	 * <code>super</code>
	 * @param inits the list of {@link FieldInit} statements
	 */
	public Constructor(String className, List params,
			List superArgs, List inits) {
		this.className = className;
		this.params = Collections.unmodifiableList(params);
		this.superArgs = Collections.unmodifiableList(superArgs);
		this.inits = Collections.unmodifiableList(inits);
	}
}
