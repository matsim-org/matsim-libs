package playground.sebhoerl.mexec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import playground.sebhoerl.mexec.placeholders.ConstantElement;
import playground.sebhoerl.mexec.placeholders.Parameter;
import playground.sebhoerl.mexec.placeholders.ParameterParser;
import playground.sebhoerl.mexec.placeholders.PlaceholderElement;

public class ParameterParserTest {
    @Test
    public void testParser() throws Parameter.PlaceholderSubstitutionException {
        ParameterParser parser = new ParameterParser();
        Parameter parameter;

        parameter = parser.parse("", "abcdef");
        assertEquals(1, parameter.getElements().size());

        parameter = parser.parse("", "%{ abcdef }");
        assertTrue(parameter.getElements().get(0) instanceof PlaceholderElement);
        assertEquals(1, parameter.getElements().size());

        parameter = parser.parse("", "uvw %{ abcdef } xyz");

        assertTrue(parameter.getElements().get(0) instanceof ConstantElement);
        assertTrue(parameter.getElements().get(1) instanceof PlaceholderElement);
        assertTrue(parameter.getElements().get(2) instanceof ConstantElement);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("abcdef", "123");

        assertEquals("uvw 123 xyz", parameter.process(replacements));

        replacements.put("v1", "xy");
        replacements.put("v2", "z");
        parameter = parser.parse("", "a %{ v1    }%{v2} b");
        assertEquals("a xyz b", parameter.process(replacements));
    }
}
