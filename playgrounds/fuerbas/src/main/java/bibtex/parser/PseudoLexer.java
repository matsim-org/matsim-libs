/*
 * Created on Mar 19, 2003
 * 
 * @author henkel@cs.colorado.edu
 *  
 */
package bibtex.parser;

import java.io.IOException;
import java.io.Reader;

/**
 * this is not a real lexer, since bibtex is such an insane format...
 * 
 * @author henkel
 */
final class PseudoLexer {

	static final class Token {
		Token(int choice, String content, int line, int column) {
			this.choice = choice;
			this.content = content;
			this.line = line;
			this.column = column;
		}

		final int choice;
		final String content;
		final int line, column;
	}

	private final LookAheadReader input;
	private Token eofToken = null;

	PseudoLexer(Reader input) throws IOException {
		this.input = new LookAheadReader(input);
	}

	Token getNextToken() {
		return null;
	}

	/**
	 * if it's a top level comment, result.choice will be 0, for @ 1, for EOF 2.
	 * 
	 * @return Token
	 */
	public Token scanTopLevelCommentOrAtOrEOF() throws IOException {
		skipWhitespace();
		if (eofToken != null) {
			return new Token(2, eofToken.content, eofToken.line, eofToken.column);
		}

		final int column = input.getColumn(), line = input.getLine();
		if (input.getCurrent() == '@') {
			input.step();
			return new Token(1, "@", line, column);
		}
		StringBuffer content = new StringBuffer();
		while (!input.eof() && input.getCurrent() != '@') {
			content.append(input.getCurrent());
			input.step();
		}
		return new Token(0, content.toString(), line, column);
	}

	/**
	 * the return value is an index into alternatives. If lookAhead is true we
	 * will not move forward ...
	 * 
	 * @param alternatives
	 * @return int
	 */
	public final int scanAlternatives(char[] alternatives, boolean lookAhead)
		throws IOException, ParseException {
		skipWhitespace();
		if (eofToken != null)
			throw new ParseException(
				eofToken.line,
				eofToken.column,
				"[EOF]",
				alternativesToString(alternatives));
		final int line = input.getLine(), column = input.getColumn();
		for (int i = 0; i < alternatives.length; i++) {
			if (alternatives[i] == input.getCurrent()) {
				if (!lookAhead)
					input.step();
				return i;
			}
		}
		if (!lookAhead)
			input.step();
		throw new ParseException(line, column, "" + input.getCurrent(), alternativesToString(alternatives));
	}

	//	/**
	//	 * this one is case insensitive!
	//	 *
	//	 * @param alternatives
	//	 * @return Token
	//	 * @throws ParseException
	//	 * @throws IOException
	//	 */
	//
	//	public final Token scanAlternatives(String[] alternatives)
	//		throws ParseException, IOException {
	//		skipWhitespace();
	//		if (eofToken != null)
	//			throw new ParseException(
	//				eofToken.line,
	//				eofToken.column,
	//				"[EOF]",
	//				alternativesToString(alternatives));
	//		final int line = input.getLine();
	//		final int column = input.getColumn();
	//		HashMap amap = new HashMap();
	//		int maxLength = 0;
	//		for (int i = 0; i < alternatives.length; i++) {
	//			amap.put(alternatives[i], new Integer(i));
	//			if (alternatives[i].length() > maxLength)
	//				maxLength = alternatives[i].length();
	//		}
	//		String content = "";
	//		String lowerCaseContent = "";
	//		for (int length = 1; length <= maxLength; length++) {
	//			content += input.getCurrent();
	//			lowerCaseContent += Character.toLowerCase(input.getCurrent());
	//			input.step();
	//
	//			if (amap.containsKey(lowerCaseContent)) {
	//				return new Token(
	//					((Integer) amap.get(lowerCaseContent)).intValue(),
	//					content,
	//					line,
	//					column);
	//			}
	//		}
	//		throw new ParseException(line, column, content,
	// alternativesToString(alternatives));
	//	}

	public String scanLiteral(char[] terminationSet, boolean excludeWhitespace, boolean enforceNonzero)
		throws ParseException, IOException {
		StringBuffer buffer = new StringBuffer();
		scanLiteral(terminationSet, excludeWhitespace, enforceNonzero, buffer);
		return buffer.toString();
	}

	/**
	 * the return value is an index into the termination set the result is
	 * appended in the resultTargetBuffer
	 * 
	 * @return Token
	 */
	public int scanLiteral(
		char[] terminationSet,
		boolean excludeWhitespace,
		boolean enforceNonzero,
		StringBuffer resultTargetBuffer)
		throws ParseException, IOException {
		if (excludeWhitespace) {
			skipWhitespace();

			if (eofToken != null)
				throw new ParseException(
					eofToken.line,
					eofToken.column,
					"[EOF]",
					"not (" + alternativesToString(terminationSet) + " or [whitespace])");
		} else
			enforceNoEof("not (" + alternativesToString(terminationSet) + ")", false);
		final int line = input.getLine(), column = input.getColumn();
		int indexIntoTerminationSet = -1;
		final int initialResultTargetBufferLength = resultTargetBuffer.length();
		while (true) {
			if (input.eof())
				break;
			final char inputChar = input.getCurrent();

			indexIntoTerminationSet = index(terminationSet, inputChar);
			if (indexIntoTerminationSet >= 0 || excludeWhitespace && Character.isWhitespace(inputChar)) {
				break;
			} else {
				input.step();
				resultTargetBuffer.append(inputChar);
			}
		}

		if (resultTargetBuffer.length() > initialResultTargetBufferLength || !enforceNonzero) {
			return indexIntoTerminationSet;
		} else {
			throw new ParseException(
				line,
				column,
				"" + input.getCurrent(),
				"not (" + alternativesToString(terminationSet) + " or [whitespace])");
		}
	}

	private static final char[] QUOTE_OR_LBRACE = new char[] { '\"', '{' };

	public String scanQuotedString() throws IOException, ParseException {
		StringBuffer content = new StringBuffer();
		scan('"');
		while (true) {
			final int choice = this.scanLiteral(QUOTE_OR_LBRACE, false, false, content);
			if (choice == 0) { // we terminated with '"'
				break;
			} else { // we found a '{'
				scanBracketedString(content, true);
			}
		}
		scan('"');
		return content.toString();
	}

	private final char[] RBRACE_LBRACE = new char[] { '}', '{' };

	public void scanBracketedString(StringBuffer targetBuffer, boolean includeOuterBraces)
		throws ParseException, IOException {
		scan('{');
		if (includeOuterBraces)
			targetBuffer.append('{');
		while (true) {
			final int choice = this.scanLiteral(RBRACE_LBRACE, false, false, targetBuffer);

			if (choice == 0) { // we terminated with '}'
				break;
			} else { // we terminated with '{'
				scanBracketedString(targetBuffer, true);
			}
		}
		scan('}');
		if (includeOuterBraces)
			targetBuffer.append("}");
	}

	public String scanEntryTypeName() throws ParseException, IOException {
		skipWhitespace();
		if (eofToken != null)
			throw new ParseException(eofToken.line, eofToken.column, "[EOF]", "[a..z,A..Z]");
		final int line = input.getLine(), column = input.getColumn();
		StringBuffer result = new StringBuffer();
		while (true) {
			enforceNoEof("[a..z,A..Z]", false);
			char inputChar = input.getCurrent();

			if (inputChar >= 'a' && inputChar <= 'z' || inputChar >= 'A' && inputChar <= 'Z') {
				result.append(inputChar);
				input.step();
			} else {
				break;
			}
		}
		if (result.length() == 0) {
			throw new ParseException(line, column, "" + input.getCurrent(), "[a..z,A..Z]");
		}
		return result.toString();

	}

	public void scan(char expected) throws ParseException, IOException {
		skipWhitespace();
		if (eofToken != null)
			throw new ParseException(eofToken.line, eofToken.column, "[EOF]", "" + expected);
		final char encountered = input.getCurrent();
		if (encountered != expected) {
			final int line = input.getLine(), column = input.getColumn();
			input.step();
			throw new ParseException(line, column, "" + encountered, "" + expected);
		} else input.step();
	}

	public void skipWhitespace() throws IOException {
		if (eofToken != null)
			return;
		while (!input.eof() && Character.isWhitespace(input.getCurrent()))
			input.step();
		if (input.eof()) {
			eofToken = new Token(-1, null, input.getLine(), input.getColumn());
		}
	}

	/**
	 * make sure you call
	 * 
	 * @return boolean
	 */
	public void enforceNoEof(String expected, boolean skipWhiteSpace) throws ParseException, IOException {
		if (skipWhiteSpace)
			skipWhitespace();
		else if (input.eof()) {
			eofToken = new Token(-1, null, input.getLine(), input.getColumn());
		}
		if (eofToken != null)
			throw new ParseException(eofToken.line, eofToken.column, "[EOF]", "" + expected);
	}

	/**
	 * make sure to query enforceNoEof first!
	 * 
	 * @return char
	 */
	public char currentInputChar() {
		return input.getCurrent();
	}

	private static String alternativesToString(char[] alternatives) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("one of ");
		for (int i = 0; i < alternatives.length; i++) {
			if (i != 0)
				buffer.append(',');
			buffer.append('\'');
			buffer.append(alternatives[i]);
			buffer.append('\'');
		}

		return buffer.toString();
	}

	private static String alternativesToString(Object[] alternatives) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("one of ");
		for (int i = 0; i < alternatives.length; i++) {
			if (i != 0)
				buffer.append(',');
			buffer.append('\'');
			buffer.append(alternatives[i]);
			buffer.append('\'');
		}

		return buffer.toString();
	}

	private static int index(char[] container, char element) {
		for (int i = 0; i < container.length; i++) {
			if (container[i] == element)
				return i;
		}
		return -1;

	}
}
