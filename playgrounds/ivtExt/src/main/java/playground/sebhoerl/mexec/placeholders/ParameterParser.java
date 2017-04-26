package playground.sebhoerl.mexec.placeholders;

import playground.sebhoerl.mexec.placeholders.ConstantElement;
import playground.sebhoerl.mexec.placeholders.Parameter;
import playground.sebhoerl.mexec.placeholders.PlaceholderElement;

public class ParameterParser {
    private enum State {
        EXTERNAL, INTERNAL, PLACEHOLDER
    }

    public class ParsingException extends RuntimeException {
        final public String what;
        final public String parameterName;

        public ParsingException(String what, String parameterName) {
            this.what = what;
            this.parameterName = parameterName;
        }

        @Override
        public String toString() {
            return String.format("%s in parameter %s", what,  parameterName);
        }
    }

    public Parameter parse(String parameterName, String parameterValue) throws ParsingException {
        Parameter parameter = new Parameter(parameterName);
        State state = State.EXTERNAL;

        String current = "";
        int length;

        for (int i = 0; i < parameterValue.length(); i++) {
            current += parameterValue.charAt(i);
            length = current.length();
            boolean nextEof = i == parameterValue.length() - 1;

            switch (state) {
                case EXTERNAL:
                    if (length > 1 && current.charAt(length - 2) == '%' && current.charAt(length - 1) == '{') {
                        if (current.length() - 2 > 1) {
                            parameter.addElement(new ConstantElement(current.substring(0, length - 2)));
                        }

                        state = State.INTERNAL;
                        current = "";
                    }
                    break;
                case INTERNAL:
                    if (current.charAt(length - 1) == '}') {
                        state = State.EXTERNAL;
                        current = "";
                    } else  {
                        state = State.PLACEHOLDER;
                        current = "";
                        i -= 1;
                    }

                    break;
                case PLACEHOLDER:
                    if (current.charAt(length - 1) == '}') {
                        parameter.addElement(new PlaceholderElement(current.substring(0, length - 1).trim()));

                        state = State.INTERNAL;
                        current = "";
                        i -= 1;
                    }

                    break;
            }
        }

        if (state != State.EXTERNAL) {
            throw new ParsingException("Unclosed placeholder", parameterName);
        } else if (current.length() > 0) {
            parameter.addElement(new ConstantElement(current));
        }

        return parameter;
    }
}
