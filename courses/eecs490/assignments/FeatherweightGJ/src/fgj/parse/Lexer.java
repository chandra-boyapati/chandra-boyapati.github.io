package fgj.parse;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Converts character input into a stream of tokens suitable for parsing
 * into FeatherweightGJ abstract syntax.
 */
public class Lexer {

	/**
	 * The underlying input stream.
	 */
	private final Reader input;
	
	/**
	 * The current line number of the input, starting from 1.
	 */
	private int lineNumber;
	
	/**
	 * One character of pushback store, required for lookahead.
	 * A value of -2 means there is no pushback character.
	 */
	private int singlePushback;
	
	/**
	 * The list of keywords of FeatherweightGJ.
	 */
	private static final Map keywords;
	static {
		Map tmpKeywords = new HashMap();
		tmpKeywords.put("class", new Token(Token.CLASS));
		tmpKeywords.put("extends", new Token(Token.EXTENDS));
		tmpKeywords.put("super", new Token(Token.SUPER));
		tmpKeywords.put("this", new Token(Token.THIS));
		tmpKeywords.put("return", new Token(Token.RETURN));
		tmpKeywords.put("new", new Token(Token.NEW));
		keywords = Collections.unmodifiableMap(tmpKeywords);
	}

	/**
	 * Construct a lexer with a given input stream.
	 * @param input the underlying input character stream
	 */
	public Lexer(Reader input) {
		this.input = input;
		this.lineNumber = 1;
		this.singlePushback = -2;
	}
	
	/**
	 * Return the line number the lexer is currently working on.
	 * @return the line number
	 */
	public int lineNumber() {
		return lineNumber;
	}
	
	/**
	 * Retrieve the next token in the input stream.
	 * @return a new token
	 * @throws ParseException an I/O exception occurred, or the input
	 * contained an illegal lexeme
	 */
	public Token nextToken() throws ParseException {
		int c = next();

		// Iterate while eliminating whitespace
		while (c != -1) {
			switch (c) {
			
			// Whitespace
			case '\n':
			case '\r':
			case '\t':
			case ' ':
				c = next(); continue;
			
			// Comments
			case '/':
				switch (c = next()) {
				
				// End-of-line comment (//)
				case '/':
					do {c = next();} while (c != -1 && c != '\n');
					c = next(); continue;

				// Multi-line comment (/*, */)
				case '*':
					c = next();
					while (c != -1) {
						while (c != -1 && c != '*') { c = next(); }
						if (c == '*') {
							if ((c = next()) == '/') break;
						}
					}
					c = next(); continue;
				
				// Something else; definitely bad
				default:
					die("invalid character after '/'");
				}
			
			// A real token
			default:
				return nextToken(c);				
			}
		}

		// End of file
		return new Token(Token.EOF);
	}

	/**
	 * Retrieve the next token, starting with the given character.
	 * Whitespace has already been removed by {@link #nextToken()}.
	 * @param c the character to start with
	 * @return the next token
	 * @throws ParseException an I/O exception occurred, or the input
	 * contained an illegal lexeme
	 */
	private Token nextToken(int c) throws ParseException {
		Token tok = punctuation(c);
		if (tok != null) return tok;
		StringBuffer sb = new StringBuffer();
		while ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
			sb.append((char)c);
			c = next();
		}
		if (sb.length() == 0) die("unexpected character");
		singlePushback = c;
		String str = sb.toString();
		Token token = (Token) keywords.get(str);
		return (token != null) ? token : new Token(str);
	}

	/**
	 * Return the punctuation token associated with the given character,
	 * or <code>null</code> if no such token exists.
	 * @param c the character to examine
	 * @return its corresponding token, or <code>null</code>
	 */
	private Token punctuation(int c) {
		switch (c) {
		case '{': return tok(Token.LBRACE);
		case '}': return tok(Token.RBRACE);
		case '(': return tok(Token.LPAREN);
		case ')': return tok(Token.RPAREN);
		case '=': return tok(Token.EQUAL);
		case '.': return tok(Token.DOT);
		case ',': return tok(Token.COMMA);
		case ';': return tok(Token.SEMI);	
		case '<': return tok(Token.LT);
		case '>': return tok(Token.GT);
		default: return null;
		}
	}

	/**
	 * Construct a {@link Token} object with the given identifier number.
	 * @param id the number of the token
	 * @return the new token object
	 */
	private Token tok(int id) {
		return new Token(id);
	}
	
	/**
	 * Retrieve the next character from the input stream.
	 * @return the next character; -1 means end-of-file
	 * @throws ParseException an I/O error occurred
	 */
	private int next() throws ParseException {
		try {
			if (singlePushback != -2) {
				int value = singlePushback;
				singlePushback = -2;
				return value;
			}
			int c = input.read();
			if (c == '\n') ++lineNumber;
			return c;
		}
		catch (IOException e) {
			die("input/output error");
			return -1;
		}
	}
	
	/**
	 * Throw a {@link ParseException} for the given reason
	 * @param reason the message to associate with the exception
	 * @throws ParseException always; that's the point
	 */
	private void die(String reason) throws ParseException {
		throw new ParseException(lineNumber, reason);
	}
}
