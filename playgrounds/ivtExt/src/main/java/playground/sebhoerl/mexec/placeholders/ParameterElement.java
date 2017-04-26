package playground.sebhoerl.mexec.placeholders;

import java.util.Map;

public interface ParameterElement {
    String process(Map<String, String> placeholderValues) throws PlaceholderSubstitutionException;

    class PlaceholderSubstitutionException extends Exception {
        final public String placeholderName;

        public PlaceholderSubstitutionException(String placeholderName) {
            this.placeholderName = placeholderName;
        }

        @Override
        public String toString() {
            return String.format("Placeholder \"%s\" not found", placeholderName);
        }
    }
}
