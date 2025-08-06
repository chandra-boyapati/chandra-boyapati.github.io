package fgj.parse;

/**
 * The return type of {@link Lexer#nextToken()}.  Every token produced
 * by the lexer has an integer identifier, along with a string value
 * if it is a FeatherweightGJ identifier.
 */
public class Token {

	/**
	 * The identifying number of this type of token.
	 */
	public final int id;
	
	/**
	 * The value of this token; only valid when
	 * <code>id == IDENTIFIER</code>.
	 */
	public final String value;
	
	/**
	 * Construct a new non-identifier token.
	 * @param id the number of this token
	 */
	public Token(int id) {
		this.id = id;
		this.value = null;
	}
	
	/**
	 * Construct an identifier token.
	 * @param value the identifier to associate with this token
	 */
	public Token(String value) {
		this.id = IDENTIFIER;
		this.value = value;
	}

	/** Token representing end-of-file. */
	public static final int EOF = 0;
	
	/** Token representing the <code>class</code> keyword. */
	public static final int CLASS = 1;

	/** Token representing FeatherweightGJ identifiers. */
	public static final int IDENTIFIER = 2;
	
	/** Token representing the <code>extends</code> keyword. */
	public static final int EXTENDS = 3;
	
	/** Token representing the '{' punctuation. */
	public static final int LBRACE = 4;
	
	/** Token representing the '}' punctuation. */
	public static final int RBRACE = 5;
	
	/** Token representing the '(' punctuation. */
	public static final int LPAREN = 6;
	
	/** Token representing the ')' punctuation. */
	public static final int RPAREN = 7;
	
	/** Token representing the <code>super</code> keyword. */
	public static final int SUPER = 8;
	
	/** Token representing the ',' punctuation. */
	public static final int COMMA = 9;
	
	/** Token representing the ';' punctuation. */
	public static final int SEMI = 10;
	
	/** Token representing the <code>this</code> keyword. */
	public static final int THIS = 11;
	
	/** Token representing the '.' punctuation. */
	public static final int DOT = 12;
	
	/** Token representing the '=' punctuation. */
	public static final int EQUAL = 13;
	
	/** Token representing the <code>return</code> keyword. */
	public static final int RETURN = 14;
	
	/** Token representing the <code>new</code> keyword. */
	public static final int NEW = 15;
	
	/** Token representing the '<' punctuation. */
	public static final int LT = 16;
	
	/** Token representing the '>' punctuation. */
	public static final int GT = 17;
}
