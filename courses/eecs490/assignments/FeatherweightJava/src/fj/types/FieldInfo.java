package fj.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fj.ast.Declaration;

/**
 * Information about the fields of a class.  This structure provides
 * two views of the fields: as an ordered list, and as a mapping of
 * names to field indices.  The map view is not strictly necessary, but
 * it facilitates the implementation of several portions of the
 * typechecker and evaluator.
 */
public class FieldInfo {

	/**
	 * The list of fields of a class.  This list is in declaration
	 * order, with the fields of a superclass in front of the
	 * fields of a class.
	 */
	public final List list;
	
	/**
	 * The mapping of field names to their indices in the field list.
	 */
	public final Map map;
	
	/**
	 * Construct a FieldInfo structure with the given list.
	 * @param list the list of fields
	 */
	public FieldInfo(List list) throws ClassTableException {
		Map tempMap = new HashMap();
		int k = 0;
		for (Iterator i = list.iterator(); i.hasNext(); ++k) {
			Declaration field = (Declaration) i.next();
			String name = field.name;			
			if (tempMap.containsKey(name)) {
				throw new ClassTableException(
					"duplicate field name \"" + name + "\"");
			}
			tempMap.put(name, new Integer(k));
		}		
		
		this.list = Collections.unmodifiableList(list);
		this.map = Collections.unmodifiableMap(tempMap);		
	}
	
	/**
	 * Construct an empty field information structure.
	 */
	private FieldInfo() {
		this.list = Collections.EMPTY_LIST;
		this.map = Collections.EMPTY_MAP;
	}
	
	/** The field info of <code>Object</code> */
	public static final FieldInfo EMPTY = new FieldInfo();
}
