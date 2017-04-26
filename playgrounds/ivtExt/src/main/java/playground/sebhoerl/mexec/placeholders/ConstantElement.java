package playground.sebhoerl.mexec.placeholders;

import java.util.Map;

public class ConstantElement implements ParameterElement {
    final private String value;

    public ConstantElement(String value) {
        this.value = value;
    }

    @Override
    public String process(Map<String, String> placeholderValues) throws PlaceholderSubstitutionException {
        return value;
    }

    @Override
    public String toString() {
        return "CONSTANT(" + value + ")";
    }
}
