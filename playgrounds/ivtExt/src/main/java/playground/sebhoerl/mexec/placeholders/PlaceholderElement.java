package playground.sebhoerl.mexec.placeholders;

import java.util.Map;

public class PlaceholderElement implements ParameterElement {
    final private String placeholderName;

    public PlaceholderElement(String placeholderName) {
        this.placeholderName = placeholderName;
    }

    @Override
    public String process(Map<String, String> placeholderValues) throws PlaceholderSubstitutionException {
        if (placeholderValues.containsKey(placeholderName)) {
            return placeholderValues.get(placeholderName);
        }

        throw new PlaceholderSubstitutionException(placeholderName);
    }

    public String getPlaceholderName() {
        return placeholderName;
    }

    @Override
    public String toString() {
        return "PLACEHOLDER(" + placeholderName + ")";
    }
}
