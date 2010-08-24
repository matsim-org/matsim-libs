/*
 * Created on Mar 27, 2003
 *
 * @author henkel@cs.colorado.edu
 * 
 */
package bibtex.expansions;

import java.util.LinkedList;
import java.util.List;

import bibtex.dom.BibtexFile;
import bibtex.dom.BibtexPerson;
import bibtex.dom.BibtexPersonList;
import bibtex.dom.BibtexString;

/**
 * @author henkel
 */
final class BibtexPersonListParser {

    static final class StringIterator {

        private final char[] chars;

        private int pos;

        StringIterator(String string) {
            chars = string.toCharArray();
            pos = 0;
        }

        char next() {
            return chars[pos++];
        }

        char current() {
            return chars[pos];
        }

        void step() {
            pos++;
        }

        void skipWhiteSpace() {
            while (pos < chars.length && Character.isWhitespace(chars[pos]))
                pos++;
        }

        boolean hasNext() {
            return pos + 1 < chars.length;
        }
    }

    public static BibtexPersonList parse(BibtexString personList,String entryKey) throws PersonListParserException {

        String content = personList.getContent();
        String[] tokens = tokenize(content);

        BibtexPersonList result = personList.getOwnerFile().makePersonList();
        if (tokens.length == 0) {
            return result;
        }
        int begin = 0;
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].toLowerCase().equals(AND) && begin < i) {
                result.add(makePerson(tokens, begin, i, content, entryKey, personList.getOwnerFile()));
                begin = i + 1;
            }
        }
        if (begin < tokens.length)
            result.add(makePerson(tokens, begin, tokens.length, content, entryKey, personList.getOwnerFile()));
        return result;
    }

    private static boolean firstCharAtBracelevel0IsLowerCase(final String string) {
        StringIterator stringIt =new StringIterator(string);
        if (Character.isLowerCase(stringIt.current()))
            return true;
        while (stringIt.hasNext()) {
            stringIt.skipWhiteSpace();
            if (Character.isLowerCase(stringIt.current()))
                return true;
            if (Character.isUpperCase(stringIt.current()))
                return false;
            if (stringIt.current() == '{') {
                stringIt.step();
                stringIt.skipWhiteSpace();

                if (stringIt.current() == '\\') {
                    scanCommandOrAccent: while (true) {
                        stringIt.step();
                        stringIt.skipWhiteSpace();
                        if (Character.isLetter(stringIt.current())) {
                            while (stringIt.hasNext() && stringIt.current() != '{'
                                    && !Character.isWhitespace(stringIt.current()) && stringIt.current() != '}')
                                stringIt.step();
                            stringIt.skipWhiteSpace();
                            if (!stringIt.hasNext())
                                return false;
                            if (stringIt.current() == '}')
                                return false;
                            if (stringIt.current() == '{') {
                                stringIt.step();
                                stringIt.skipWhiteSpace();
                                if (!stringIt.hasNext())
                                    return false;
                                if (Character.isLowerCase(stringIt.current()))
                                    return true;
                                if (Character.isUpperCase(stringIt.current()))
                                    return false;
                                if (stringIt.current() == '\\') {
                                    continue scanCommandOrAccent;
                                }
                            }
                        } else {
                            while(stringIt.hasNext() && !Character.isLetter(stringIt.current())){
                                stringIt.step();
                            }
                            if(!stringIt.hasNext()) return false;
                            if(Character.isLowerCase(stringIt.current())) return true;
                            if(Character.isUpperCase(stringIt.current())) return false;
                            return false;
                        }
                    }
                } else {
                    // brace level 1
                    int braces = 1;
                    while (braces > 0) {
                        if (!stringIt.hasNext())
                            return false;
                        else if (stringIt.current() == '{')
                            braces++;
                        else if (stringIt.current() == '}')
                            braces--;
                        stringIt.step();
                    }
                    // back at brace level 0
                }
            } else stringIt.step();
        }
        return false;
    }

    private static String getString(String[] tokens, int beginIndex, int endIndex) {
        if (!(beginIndex < endIndex))
            return null;
        assert beginIndex >= 0;
        assert endIndex >= 0;
        StringBuffer result = new StringBuffer();
        for (int i = beginIndex; i < endIndex; i++) {
            if (tokens[i] == MINUS) {
                if (i == beginIndex || i == endIndex - 1)
                    continue;
                result.append('-');
                continue;
            }
            if (i > beginIndex && tokens[i - 1] != MINUS)
                result.append(' ');
            result.append(tokens[i]);
        }
        return result.toString();
    }

    private static BibtexPerson makePerson(String[] tokens, int begin, int end, String fullEntry, String entryKey, BibtexFile factory)
            throws PersonListParserException {
        if (tokens[begin].equals("others")) {
            return factory.makePerson(null, null, null, null, true);
        } else if (tokens[end - 1] == COMMA)
            throw new PersonListParserException("Name ends with comma: '" + fullEntry + "' - in '"+entryKey+"'");
        else {
            int numberOfCommas = 0;
            for (int i = begin; i < end; i++) {
                if (tokens[i] == COMMA)
                    numberOfCommas++;
            }
            if (numberOfCommas == 0) {
                int lastNameBegin = end - 1;
                while (true) {
                    if (lastNameBegin - 1 >= begin && !firstCharAtBracelevel0IsLowerCase(tokens[lastNameBegin - 1])) {
                        lastNameBegin -= 1;
                    } else if (lastNameBegin - 2 >= begin && tokens[lastNameBegin - 1] == MINUS
                            && !firstCharAtBracelevel0IsLowerCase(tokens[lastNameBegin - 2])) {
                        lastNameBegin -= 2;
                    } else
                        break;
                }
                int firstLowerCase = -1;
                for (int i = begin; i < end; i++) {
                    if (tokens[i] == MINUS)
                        continue;
                    if (firstCharAtBracelevel0IsLowerCase(tokens[i])) {
                        firstLowerCase = i;
                        break;
                    }
                }
                final String last, first, lineage, preLast;
                if (lastNameBegin == begin || firstLowerCase == -1) {
                    //there is no preLast part

                    lastNameBegin = end - 1;
                    while (lastNameBegin - 2 >= begin && tokens[lastNameBegin - 1] == MINUS
                            && !firstCharAtBracelevel0IsLowerCase(tokens[lastNameBegin - 2]))
                        lastNameBegin -= 2;
                    last = getString(tokens, lastNameBegin, end);
                    first = getString(tokens, begin, lastNameBegin);
                    lineage = null;
                    preLast = null;
                } else {
                    last = getString(tokens, lastNameBegin, end);
                    first = getString(tokens, begin, firstLowerCase);
                    lineage = null;
                    preLast = getString(tokens, firstLowerCase, lastNameBegin);
                }
                if (last == null)
                    throw new PersonListParserException("Found an empty last name in '" + fullEntry + "' in '"+entryKey+"'.");
                return factory.makePerson(first, preLast, last, lineage, false);
            } else if (numberOfCommas == 1 || numberOfCommas == 2) {

                if (numberOfCommas == 1) {
                    int commaIndex = -1;
                    for (int i = begin; i < end; i++) {
                        if (tokens[i] == COMMA) {
                            commaIndex = i;
                            break;
                        }
                    }
                    final int preLastBegin = begin;
                    int preLastEnd = begin;
                    for (int i = preLastEnd; i < commaIndex; i++) {
                        if (tokens[i] == MINUS)
                            continue;
                        if (firstCharAtBracelevel0IsLowerCase(tokens[i])) {
                            preLastEnd = i + 1;
                        }
                    }
                    if (preLastEnd == commaIndex && preLastEnd > preLastBegin) {
                        preLastEnd--;
                    }
                    final String preLast = getString(tokens, preLastBegin, preLastEnd);
                    final String last = getString(tokens, preLastEnd, commaIndex);
                    final String first = getString(tokens, commaIndex + 1, end);
                    if (last == null)
                        throw new PersonListParserException("Found an empty last name in '" + fullEntry + "' in '"+entryKey+"'.");
                    return factory.makePerson(first, preLast, last, null, false);
                } else { // 2 commas ...
                    int firstComma = -1;
                    int secondComma = -1;
                    for (int i = begin; i < end; i++) {
                        if (tokens[i] == COMMA) {
                            if (firstComma == -1) {
                                firstComma = i;
                            } else {
                                secondComma = i;
                                break;
                            }
                        }
                    }
                    final int preLastBegin = begin;
                    int preLastEnd = begin;
                    for (int i = preLastEnd; i < firstComma; i++) {
                        if (tokens[i] == MINUS)
                            continue;
                        if (firstCharAtBracelevel0IsLowerCase(tokens[i])) {
                            preLastEnd = i + 1;
                        }
                    }
                    if (preLastEnd == firstComma && preLastEnd > preLastBegin) {
                        preLastEnd--;
                    }
                    final String preLast = getString(tokens, preLastBegin, preLastEnd);
                    final String last = getString(tokens, preLastEnd, firstComma);
                    String lineage = getString(tokens, firstComma + 1, secondComma);
                    String first = getString(tokens, secondComma + 1, end);
                    if (first == null && lineage != null) {
                        String tmp = lineage;
                        lineage = first;
                        first = tmp;
                    }
                    if (last == null)
                        throw new PersonListParserException("Found an empty last name in '" + fullEntry + "' in '"+entryKey+"'.");
                    return factory.makePerson(first, preLast, last, lineage, false);
                }
            } else {
                throw new PersonListParserException("Too many commas in '" + fullEntry + "' in '"+entryKey+"'.");
            }
        }
    }

    private static final String COMMA = ",".intern();

    private static final String AND = "and".intern();

    private static final String MINUS = "-".intern();

    /**
     * 
     * 
     * @param stringContent
     * @return String[]
     */
    private static String[] tokenize(String stringContent) {
        int numberOfOpenBraces = 0;
        int tokenBegin = 0;
        stringContent = stringContent + " ";
        // make sure the last character is whitespace ;-)
        LinkedList tokens = new LinkedList(); // just some strings ...
        for (int currentPos = 0; currentPos < stringContent.length(); currentPos++) {
            switch (stringContent.charAt(currentPos)) {
            case '{':
                numberOfOpenBraces++;
                break;
            case '}':
                if(numberOfOpenBraces>0){
                    numberOfOpenBraces--;
                } else{
                    if (tokenBegin <= currentPos - 1) {
                        String potentialToken = stringContent.substring(tokenBegin, currentPos).trim();
                        if (!potentialToken.equals("")) {
                            tokens.add(potentialToken);
                        }
                    }
                    tokenBegin = currentPos + 1;
                }
                break;
            case ',':
                if (numberOfOpenBraces == 0) {
                    if (tokenBegin <= currentPos - 1) {
                        String potentialToken = stringContent.substring(tokenBegin, currentPos).trim();
                        if (!potentialToken.equals("")) {
                            tokens.add(potentialToken);
                        }
                    }
                    tokens.add(COMMA);
                    tokenBegin = currentPos + 1;
                }
            default:
                char currentChar = stringContent.charAt(currentPos);
                if (Character.isWhitespace(currentChar) || (currentChar == '~') || (currentChar == '-')) {
                    if (numberOfOpenBraces == 0 && tokenBegin <= currentPos) {
                        String potentialToken = stringContent.substring(tokenBegin, currentPos).trim();
                        if (!potentialToken.equals("")) {
                            tokens.add(potentialToken);
                            if (currentChar == '-')
                                tokens.add(MINUS);
                        }
                        tokenBegin = currentPos + 1;
                    }
                }
            }
        }
        String[] result = new String[tokens.size()];
        tokens.toArray(result);
        return result;
    }

    private static boolean isEqual(String str1, String str2) {
        if (str1 == null)
            return str2 == null;
        return str1.equals(str2);
    }

    private static boolean isEqual(BibtexPersonListParserTests.Test test, BibtexPerson person) {
        return isEqual(test.getFirst(), person.getFirst()) && isEqual(test.getLast(), person.getLast())
                && isEqual(test.getLineage(), person.getLineage()) && isEqual(test.getPreLast(), person.getPreLast());
    }

    private static void dumpCompare(BibtexPersonListParserTests.Test test, BibtexPerson person) {
        System.out.println("test.first=\"" + test.getFirst() + "\" person.first=\"" + person.getFirst() + "\"");
        System.out.println("test.last=\"" + test.getLast() + "\" person.last=\"" + person.getLast() + "\"");
        System.out.println("test.preLast=\"" + test.getPreLast() + "\" person.preLast=\"" + person.getPreLast() + "\"");
        System.out.println("test.lineage=\"" + test.getLineage() + "\" person.lineage=\"" + person.getLineage() + "\"");
    }

    public static void main(String args[]) {
        BibtexPersonListParserTests.Test [] tests = BibtexPersonListParserTests.tests;
        for (int i = 0; i < tests.length; i++) {
            BibtexFile file = new BibtexFile();
            
            BibtexString string = file.makeString(tests[i].getString());
            List personList;
            try {
                personList = parse(string,"testCase"+i).getList();
            } catch (PersonListParserException e) {
                e.printStackTrace();
                continue;
            }
            assert personList.size() == 1;
            BibtexPerson person = (BibtexPerson) personList.get(0);
            if (!isEqual(tests[i], person)) {
                System.err.println("\ntest failed:");
                System.err.println(tests[i].getString());
                dumpCompare(tests[i], person);
            }
        }
    }
}