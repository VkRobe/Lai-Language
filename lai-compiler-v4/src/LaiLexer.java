import java.util.ArrayList;

public class LaiLexer {

	public static String getDebugString(Token t) {
		String message = t.type.name();
		if (t instanceof LaiLexer.StringLiteral) {
			message += ": " + ((LaiLexer.StringLiteral) t).value;
		}

		if (t instanceof LaiLexer.IntegerLiteral) {
			message += ": " + ((LaiLexer.IntegerLiteral) t).value;
		}

		if (t instanceof LaiLexer.Identifier) {
			message += ": " + ((LaiLexer.Identifier) t).value;
		}
		return message;
	}

	public static ArrayList<TokenType> BASIC_TOKENS = new ArrayList<TokenType>();
	public static ArrayList<TokenType> OPERATOR_TOKENS = new ArrayList<TokenType>();

	// If directly identifiable tokentype is available, return it.
	public static TokenType getTokenDirect(String s) {
		for (TokenType t : BASIC_TOKENS) {
			if (t.name.equals(s)) {
				return t;
			}
		}
		return TokenType.UnknownToken;
	}

	public static enum TokenType {

		UnknownToken("#UNKNOWN"),

		OpTypeDec(":"), OpAssignValue("="), OpInferTypeAssignValue(":="),

		OpBoolEqual("=="), OpBoolNotEqual("!="),

		OpOpenBrace("{"), OpCloseBrace("}"), OpOpenParenthesis("("), OpCloseParenthesis(")"),

		TypeInt("int"), TypeString("string"),

		StringLiteral("#STRING_LITERAL"), IntegerLiteral("#INTEGER_LITERAL"),

		Identifier("#IDENTIFIER");

		public final String name;

		private TokenType(String name) {
			this.name = name;
			if (name.charAt(0) == '#') {
				// This is a ValuedToken<T> and cannot be checked by string match.
			} else if (Lexer.isAlphanumeric(name)) {
				// This is a normal token, so its name can be used to find it.
				BASIC_TOKENS.add(this);
			} else {
				// This is an operator token, so we can put it in the list for parsing operators by name.
				OPERATOR_TOKENS.add(this);
			}
		}
	}

	public static class Token {

		public final int lineNumber;
		public final int charNumber;

		protected TokenType type;

		public Token(int lineNumber, int charNumber, TokenType type) {
			this.lineNumber = lineNumber;
			this.charNumber = charNumber;
			this.type = type;
		}

	}

	public static abstract class ValuedToken<T> extends Token {
		public final T value;

		public ValuedToken(int lineNumber, int charNumber, TokenType type, T value) {
			super(lineNumber, charNumber, type);
			this.value = value;
		}
	}

	public static class StringLiteral extends ValuedToken<String> {
		public StringLiteral(int lineNumber, int charNumber, String value) {
			super(lineNumber, charNumber, TokenType.StringLiteral, value);
		}
	};

	public static class IntegerLiteral extends ValuedToken<Integer> {
		public IntegerLiteral(int lineNumber, int charNumber, Integer value) {
			super(lineNumber, charNumber, TokenType.IntegerLiteral, value);
		}
	};

	public static class Identifier extends ValuedToken<String> {
		public Identifier(int lineNumber, int charNumber, String value) {
			super(lineNumber, charNumber, TokenType.Identifier, value);
		}
	}

}