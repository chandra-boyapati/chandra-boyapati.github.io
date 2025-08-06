package fgj.parse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import fgj.ast.*;

/**
 * Convert a stream of tokens into a FeatherweightGJ abstract syntax tree.
 */
public class Parser {
	
	/**
	 * The token provider.
	 */
	private final Lexer lexer;
	
	/**
	 * A pushdown store for lookahead tokens.
	 */
	private final Stack pushedTokens;
	
	/**
	 * Sets of type variables at different nesting levels.
	 */
	private final Stack typeVarSets;

	/**
	 * Construct a new parser.
	 * @param lexer the token provider
	 */
	public Parser(Lexer lexer) {
		this.lexer = lexer;
		this.pushedTokens = new Stack();
		this.typeVarSets = new Stack();
	}
	
	/**
	 * Convert the token stream into a FeatherweightGJ program.
	 * @return the abstract syntax of the program
	 * @throws ParseException an I/O exception occurred, the lexer
	 * encountered an invalid lexeme, or the parser encountered a
	 * bad token sequence
	 */
	public Program parse() throws ParseException {
		
		// Read class declarations first
		List classList = new LinkedList();
		Token token;
		while ((token = nextToken()).id == Token.CLASS) {
			classList.add(parseClass());
		}
		
		// Read a term
		pushToken(token);
		Term term = parseTerm();
		
		// Should be end-of-file
		eat(Token.EOF, "end of file");
		
		// All done
		return new Program(classList, term);
	}
	
	/**
	 * Parse a class declaration.
	 * @return class declaration abstract syntax
	 * @throws ParseException a parse error occurred
	 */
	private ClassDecl parseClass() throws ParseException {
		String className = eat(Token.IDENTIFIER, "class name");
		List typeParams = parseTypeParams();
		eat(Token.EXTENDS, "\"extends\" after class type");
		NonVariableType superType = parseNonVariableType("super type");
		eat(Token.LBRACE, "'{' after super class type");
		List fields = parseFields();
		Constructor cons = parseConstructor(className);
		List methods = parseMethods();
		typeVarSets.pop();
		return new ClassDecl(className, typeParams,
			superType, fields, cons, methods);
	}
	
	/**
	 * Parse the type parameter list of a class or method.
	 * @return the list of {@link TypeParam} objects.
	 * @throws ParseException a parse error occurred
	 */
	private List parseTypeParams() throws ParseException {
		
		// We use this as a surrogate for TypeParam
		// because we don't have NonVariableTypes yet
		class Param {
			final String varName;
			final Type bound;
			/**
			 * Construct a new param
			 * @param varName the name of the variable
			 * @param bound its associated bound
			 */
			Param(String varName, Type bound) {
				this.varName = varName;
				this.bound = bound;
			}
		}
		
		// Add a new nesting level to the type mapping
		Set typeVarSet = new HashSet();
		typeVarSets.push(typeVarSet);
		
		// Quit if there are no parameters
		Token token = nextToken();
		if (token.id != fgj.parse.Token.LT) {
			pushToken(token);
			return Collections.EMPTY_LIST;
		}		
		List typeParams = new LinkedList();
		
		// Read the parameters
		do {
			// "variable name" extends ...
			String typeVar = eat(Token.IDENTIFIER, "type variable");
			eat(Token.EXTENDS, "\"extends\" in type parameter");
			
			// This is the tricky part.  Type variable bounds can be mutually
			// recursive, so there might be type variables that we haven't seen
			// yet embedded in these bounds.  However, names without parameters
			// might be top-level class names (which we also haven't seen yet).
			
			// Therefore, we do this in two passes.  First, we assume any name
			// not followed by a '<' is a type variable.  After we have parsed
			// the whole thing, we go back and replace unknown type variables
			// with nonvariable types with no type parameters.  Whew!
			
			Type bound = parseBound();
			typeParams.add(new Param(typeVar, bound));
			if (!typeVarSet.add(typeVar)) {
				die("duplicate type variable");
			}	
		} while ((token = nextToken()).id == Token.COMMA);
		if (token.id != fgj.parse.Token.GT) {
			die("expected '>' at end of type parameter list");
		}
		
		// Walk the type parameters and reconstruct the bounds
		List newTypeParams = new LinkedList();
		for (Iterator i = typeParams.iterator(); i.hasNext(); ) {
			Param p = (Param) i.next();
			NonVariableType type;
			
			// The base bound name must not be a type variable
			if (p.bound instanceof TypeVariable) {
				TypeVariable ty = (TypeVariable) p.bound;
				if (isTypeVar(ty.name)) {
					die("using type variable as a nonvariable type");
				}
				type = new NonVariableType(ty.name, Collections.EMPTY_LIST);
			}
			else {
				type = cleanupBounds((NonVariableType) p.bound);
			}
			newTypeParams.add(new TypeParam(p.varName, type));
		}		
		
		// All done and remapped
		return newTypeParams;
	}

	/**
	 * Perform a second pass on the bounds in a type parameter.
	 * @param type the type to cleanup
	 * @return the cleaned-up type
	 * @throws ParseException a type variable was used as a nonvariable type
	 */
	private NonVariableType cleanupBounds(NonVariableType type) throws ParseException {
		if (isTypeVar(type.className)) {
			die("using type variable as a nonvariable type");
		}
		List typeArgs = new LinkedList();
		for (Iterator j = type.typeArgs.iterator(); j.hasNext(); ) {
			Type arg = (Type) j.next();
			if (arg instanceof TypeVariable) {
				TypeVariable t = (TypeVariable) arg;
				if (!isTypeVar(t.name)) {
					arg = new NonVariableType(t.name, Collections.EMPTY_LIST);
				}
			}
			else {
				arg = cleanupBounds((NonVariableType) arg);
			}
			typeArgs.add(arg);
		}
		return new NonVariableType(type.className, typeArgs);
	}

	/**
	 * Determine if a given name refers to a type variable in this scope.
	 * @param name the name to lookup
	 * @return whether or not that name is a type variable in this scope
	 */
	private boolean isTypeVar(String name) {
		for (Iterator i = typeVarSets.iterator(); i.hasNext(); ) {
			Set typeVarSet = (Set) i.next();
			if (typeVarSet.contains(name)) return true;
		}
		return false;
	}
	
	/**
	 * Parse the bound of a type parameter.
	 * @return the parsed type
	 * @throws ParseException a parse error occurred
	 */
	private Type parseBound() throws ParseException {
		// See the comment in parseTypeParams for the reason for this
		// function's existence
		String name = eat(Token.IDENTIFIER, "type or type variable name");
		Token token = nextToken();
		if (token.id != Token.LT) {
			pushToken(token);
			return new TypeVariable(name);
		}
		List typeArgs = new LinkedList();
		do {
			typeArgs.add(parseBound());
		} while ((token = nextToken()).id == Token.COMMA);
		if (token.id != Token.GT) {
			die("expected '>' at end of type bound argument list");
		}
		return new NonVariableType(name, typeArgs);
	}
	
	/**
	 * Parse the field list of a class.
	 * @return the list of fields
	 * @throws ParseException a parse error occurred
	 */
	private List parseFields() throws ParseException {
		List fields = new LinkedList();
		for (;;) {
			// Two tokens of lookahead tell us if this is a constructor
			Token t1 = nextToken(); Token t2 = nextToken();
			pushToken(t2); pushToken(t1);
			if (t2.id == Token.LPAREN) break;
			
			// Read a type and a field name
			Type type = parseType("field type");
			String name = eat(Token.IDENTIFIER, "field name");
			fields.add(new Declaration(type, name));
			eat(Token.SEMI, "';' after field declaration");
		}
		return fields;
	}
	
	/**
	 * Parse the constructor of a class.
	 * @param name the name of the enclosing class
	 * @return the parsed constructor
	 * @throws ParseException a parse error occurred, the constructor
	 * name does not match the class name, or a field initializer did
	 * does not have matching names
	 */
	private Constructor parseConstructor(String name) throws ParseException {
		Token token;
		String typeName = eat(Token.IDENTIFIER, "class name in constructor");
		if (!typeName.equals(name)) {
			die("constructor name must match class name");
		}
		
		// Constructor params
		List params = parseParams();
		
		// Call to super constructor
		eat(Token.LBRACE, "'{' after constructor parameter list");
		eat(Token.SUPER, "\"super\" invocation");
		eat(Token.LPAREN, "'(' after \"super\" in constructor");
		List superArgs = new LinkedList();
		if ((token = nextToken()).id != Token.RPAREN) {
			pushToken(token);
			for (;;) {
				superArgs.add(eat(Token.IDENTIFIER, "field name"));
				if ((token = nextToken()).id == Token.RPAREN) break;
				if (token.id != Token.COMMA) die("malformed \"super\"");				
			}
		}
		eat(Token.SEMI, "';' after \"super\" invocation");
		
		// Field initialization, plus closing brace
		List fieldInits = new LinkedList();
		while ((token = nextToken()).id != Token.RBRACE) {
			pushToken(token);
			eat(Token.THIS, "\"this\" in field initialization");
			eat(Token.DOT, "'.' after \"this\"");
			String field = eat(Token.IDENTIFIER, "field name");
			eat(Token.EQUAL, "'=' in field initialization");
			String param = eat(Token.IDENTIFIER, "parameter name");
			eat(Token.SEMI, "';' after field initialization");
			if (!field.equals(param)) {
				die("field initialization name mismatch");
			}
			fieldInits.add(new FieldInit(field, param));
		}
		
		// Finally done with this silly thing
		return new Constructor(typeName, params, superArgs, fieldInits);
	}

	/**
	 * Parse a list of methods and the terminating closing brace.
	 * @return the parsed method list
	 * @throws ParseException a parse error occurred
	 */
	private List parseMethods() throws ParseException {
		Token token;
		List methods = new LinkedList();
		while ((token = nextToken()).id != Token.RBRACE) {
			pushToken(token);
			methods.add(parseMethod());
		}
		return methods;
	}
	
	/**
	 * Parse the parameter list of a constructor or method.
	 * @return the parsed parameter list
	 * @throws ParseException a parse error occurred
	 */
	private List parseParams() throws ParseException {
		List paramList = new LinkedList();
		
		// Check for an empty list
		eat(Token.LPAREN, "'(' to begin parameter list");
		Token token = nextToken();
		if (token.id == Token.RPAREN) return paramList;
		pushToken(token);
		
		// Keep going till we hit the end
		for (;;) {
			Type type = parseType("parameter type");
			String name = eat(Token.IDENTIFIER, "variable name");
			paramList.add(new Declaration(type, name));
			if ((token = nextToken()).id == Token.RPAREN) break;
			if (token.id != Token.COMMA) die("malformed parameter list");
		}
		return paramList;
	}

	/**
	 * Parse a method.
	 * @return the parsed method
	 * @throws ParseException a parse error occurred
	 */
	private Method parseMethod() throws ParseException {
		List typeParams = parseTypeParams();
		Type returnType = parseType("return type");
		String name = eat(Token.IDENTIFIER, "method name");
		List params = parseParams();
		eat(Token.LBRACE, "'{' after method parameter list");
		eat(Token.RETURN, "\"return\" in method body");
		Term body = parseTerm();
		eat(Token.SEMI, "';' after return statement");
		eat(Token.RBRACE, "'}' after method body");
		typeVarSets.pop();
		return new Method(typeParams, returnType, name, params, body);
	}
	
	/**
	 * Parse a non-variable type.
	 * @param what a description of how the type is used; for error messages
	 * @return the parsed type
	 * @throws ParseException a parse error occurred, or the parsed type
	 * was a type variable
	 */
	private NonVariableType parseNonVariableType(String what) throws ParseException {		
		Type type = parseType(what);
		if (type instanceof TypeVariable) {
			die("cannot use a type variable as a nonvariable type");
		}
		return (NonVariableType) type;
	}
	
	/**
	 * Parse a type variable or non-variable type.
	 * @param what a description of how the type is used; for error messages
	 * @return the parsed type
	 * @throws ParseException a parse error occurred, or type parameters
	 * were applied to a type variable
	 */
	private Type parseType(String what) throws ParseException {
		
		// Type name, could be a variable
		String name = eat(Token.IDENTIFIER, what);
		boolean isTypeVar = isTypeVar(name);
		
		// Lookahead for a type argument list
		Token token = nextToken(); pushToken(token);	
		if (token.id == Token.LT) {
			
			// Type variables cannot take arguments
			if (isTypeVar) {
				die("cannot apply type parameters to a type variable");
			}
			List typeArgs = parseTypeArgs();
			return new NonVariableType(name, typeArgs);			
		}
		// Type variable name by itself
		else if (isTypeVar) {
			return new TypeVariable(name);
		}
		// Syntactic sugar: C for C<>
		else {
			return new NonVariableType(name, Collections.EMPTY_LIST);
		}
	}

	/**
	 * Parse an expression.
	 * @return the parsed expression
	 * @throws ParseException a parse error occurred
	 */
	private Term parseTerm() throws ParseException {
		Term term = parseTermFirst();
		Token token;
		while ((token = nextToken()).id == Token.DOT) {
			String name = eat(Token.IDENTIFIER, "field or method name");

			// Peek for a '(' or '<' to see if it's a method call
			pushToken(token = nextToken());
			if (token.id == Token.LPAREN || token.id == fgj.parse.Token.LT) {
				List typeArgs = parseTypeArgs();
				List args = parseTermList();
				term = new CallTerm(term, name, typeArgs, args);
			}
			else {
				term = new FieldTerm(term, name);
			}
		}	
		pushToken(token);
		return term;		
	}

	/**
	 * Parse the beginning of a term; identifiable for identifiers,
	 * constructors, casts, and parenthesized terms
	 * @return a parsed term
	 * @throws ParseException a parse error occurred
	 */
	private Term parseTermFirst() throws ParseException {
		Token token = nextToken();
		
		// "this" is just a variable now
		if (token.id == Token.THIS) return new VarTerm("this");
		
		// Identifiers are just variables
		if (token.id == Token.IDENTIFIER) return new VarTerm(token.value);
		
		// New is pretty easy
		if (token.id == Token.NEW) {
			NonVariableType type = parseNonVariableType("type name in \"new\" expression");
			List args = parseTermList();
			return new NewTerm(type, args);
		}
		
		// Must be a cast or a (term) 
		pushToken(token);
		eat(Token.LPAREN, "'(' in expression");
		
		// Try to parse a cast first; if it's not one, it's a term
		Term term = parseCast();
		if (term == null) {
			term = parseTerm();
			eat(Token.RPAREN, "')' in expression");
		}
		return term;
	}

	/**
	 * Parse the list of type arguments passed in a method call or
	 * constructor invocation.
	 * @return the parsed type arguments
	 * @throws ParseException a prase error occurred
	 */
	private List parseTypeArgs() throws ParseException {
		List typeArgs = new LinkedList();
		Token token = nextToken();
		
		// No list
		if (token.id != fgj.parse.Token.LT) {
			pushToken(token);
			return typeArgs;
		}
		
		// Empty list
		if ((token = nextToken()).id == fgj.parse.Token.GT) {
			return typeArgs;
		}
		pushToken(token);
		
		// A real list!
		do {
			Type type = parseType("type parameter");
			typeArgs.add(type);
		} while ((token = nextToken()).id == Token.COMMA);
		if (token.id != fgj.parse.Token.GT) {
			die("expected '>' at end of type parameter list");
		}
		return typeArgs;
	}

	/**
	 * Parse a type cast expression.
	 * @return the parsed type cast, or <code>null</code> if it's not a cast
	 * @throws ParseException a parse error occurred, or an attempt was
	 * made to cast to a type variable
	 */
	private CastTerm parseCast() throws ParseException {
		
		// Three tokens lookahead
		Token t1 = nextToken(); Token t2 = nextToken();
		Token t3 = nextToken();
		pushToken(t3); pushToken(t2); pushToken(t1);
		
		// Parameterized type cast
		if (t2.id == fgj.parse.Token.LT) {
			NonVariableType type = parseNonVariableType("type in type cast");
			eat(Token.RPAREN, "')' at end of type cast");
			Term term = parseTerm();
			return new CastTerm(type, term);
		}
		
		// Unparameterized type cast
		if (t1.id == Token.IDENTIFIER && t2.id == Token.RPAREN) {
			switch (t3.id) {
			case Token.THIS:
			case Token.IDENTIFIER:
			case Token.NEW:
			case Token.LPAREN:
				// It is a cast!  Eat the tokens first.
				nextToken(); nextToken();				
				Term term = parseTerm();
				
				// Make sure it's a valid nonvariable type
				if (isTypeVar(t1.value)) {
					die("cannot cast to type variable \"" + t1.value + "\"");
				}
				return new CastTerm(
					new NonVariableType(t1.value, Collections.EMPTY_LIST), term);	
			}
		}
		
		// Not a cast
		return null;
	}		

	/**
	 * Parse a list of arguments to a method or constructor invocation.
	 * @return the parsed argument list
	 * @throws ParseException a parse error occurred
	 */
	private List parseTermList() throws ParseException {
		List termList = new LinkedList();
		
		// Check for an empty list
		eat(Token.LPAREN, "'(' to begin argument list");
		Token token = nextToken();
		if (token.id == Token.RPAREN) return termList;
		pushToken(token);
		
		// Keep going till we hit the end
		for (;;) {
			Term term = parseTerm();
			termList.add(term);
			if ((token = nextToken()).id == Token.RPAREN) break;
			if (token.id != Token.COMMA) die("malformed argument list");
		}
		return termList;
	}

	/**
	 * Retrieve the next token from the input stream.
	 * @return the next token
	 * @throws ParseException the input stream threw an exception
	 */
	private Token nextToken() throws ParseException {
		if (!pushedTokens.empty()) return (Token) pushedTokens.pop();
		return lexer.nextToken();
	}
	
	/**
	 * Push a token on the pushdown store.
	 * @param token the token to push
	 */
	private void pushToken(Token token) {
		pushedTokens.push(token);
	}
	
	/**
	 * Throw an exception.
	 * @param reason the reason for the exception
	 * @throws ParseException always thrown
	 */
	private void die(String reason) throws ParseException {
		throw new ParseException(lexer.lineNumber(), reason);
	}
	
	/**
	 * Require that the next token has the specified identifying number.
	 * @param id the identifying number to look for
	 * @param die a string description of what was expected
	 * @return the value of the token read
	 * @throws ParseException the given token type was not read
	 */
	private String eat(int id, String die) throws ParseException {
		Token token = nextToken();
		if (token.id != id) die("expected " + die);
		return token.value;
	}
}
