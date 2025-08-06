package fj.parse;
import java_cup.runtime.Symbol;

%%

%public
%class Lexer
%throws ParseException
%cupsym Symbols
%cup

%{
	private Symbol keyword(int id) {
		return new Symbol(id);
	}
	
	private Symbol ident(String str) {
		return new Symbol(Symbols.IDENTIFIER, str);
	}
%}

Identifier = [a-zA-Z]+
WhiteSpace = [ \n\t\r]+
Comment = "//".*

%%

"class"           { return keyword(Symbols.CLASS); }
"extends"         { return keyword(Symbols.EXTENDS); }
"super"           { return keyword(Symbols.SUPER); }
"this"            { return keyword(Symbols.THIS); }
"return"          { return keyword(Symbols.RETURN); }
"new"             { return keyword(Symbols.NEW); }

"{"   { return keyword(Symbols.LBRACE); }
"}"   { return keyword(Symbols.RBRACE); }
"("   { return keyword(Symbols.LPAREN); }
")"   { return keyword(Symbols.RPAREN); }
"="   { return keyword(Symbols.EQUALS); }
"."   { return keyword(Symbols.DOT); }
";"   { return keyword(Symbols.SEMI); }
","   { return keyword(Symbols.COMMA); }

{Identifier}  { return ident(yytext()); }
{WhiteSpace}  { }
{Comment}     { }

.   { throw new ParseException("illegal character"); }
