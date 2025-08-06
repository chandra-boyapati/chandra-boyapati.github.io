package fj.ast;

public class Type {
	public final String className;
	public Type(String className) {
		this.className = className.intern();
	}
	public boolean equals(Object o) {
		return ((Type) o).className == className;
	}
	public int hashCode() {
		return className.hashCode();
	}
	public String toString() {
		return className;
	}
	public boolean isObject() {
		return equals(OBJECT);
	}
	private static final Type OBJECT = new Type("Object");
}
