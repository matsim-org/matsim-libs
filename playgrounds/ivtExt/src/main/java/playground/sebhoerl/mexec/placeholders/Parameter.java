package playground.sebhoerl.mexec.placeholders;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Parameter {
    final private List<ParameterElement> elements = new LinkedList<>();
    final private String name;

    public Parameter(String name) {
        this.name = name;
    }

    public void addElement(ParameterElement element) {
        elements.add(element);
    }

    public List<ParameterElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public String process(Map<String, String> parameterValues) throws PlaceholderSubstitutionException {
        StringBuilder builder = new StringBuilder();

        try {
            for (ParameterElement e : elements) {
                builder.append(e.process(parameterValues));
            }
        } catch (ParameterElement.PlaceholderSubstitutionException e) {
            throw new PlaceholderSubstitutionException(name, e.placeholderName);
        }

        return builder.toString();
    }

    public class PlaceholderSubstitutionException extends ParameterElement.PlaceholderSubstitutionException {
        final public String parameterName;

        public PlaceholderSubstitutionException(String parameterName, String placeholderName) {
            super(placeholderName);
            this.parameterName = parameterName;
        }

        @Override
        public String toString() {
            return super.toString() + " in " + parameterName;
        }
    }
}
