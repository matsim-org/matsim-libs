package ch.sbb.matsim.analysis.skims;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class FloatMatrixTest {

    @Test
    void testCreateFloatMatrixWithDefaultValue() {
        Set<String> zones = new HashSet<>(Arrays.asList("A", "B", "C"));
        float defaultValue = 1.5f;
        FloatMatrix<String> matrix = FloatMatrix.createFloatMatrix(zones, defaultValue);
        for (String from : matrix.getIdentifiers()) {
            for (String to : matrix.getIdentifiers()) {
                assertEquals(defaultValue, matrix.get(from, to), 1e-6);
            }
        }
    }

    @Test
    void testCreateFloatMatrixWithDataArray() {
        List<String> ids = Arrays.asList("X", "Y");
        float[][] data = {{1.0f, 2.0f}, {3.0f, 4.0f}};
        FloatMatrix<String> matrix = FloatMatrix.createFloatMatrix(ids, data);
        assertEquals(1.0f, matrix.get("X", "X"), 1e-6);
        assertEquals(2.0f, matrix.get("X", "Y"), 1e-6);
        assertEquals(3.0f, matrix.get("Y", "X"), 1e-6);
        assertEquals(4.0f, matrix.get("Y", "Y"), 1e-6);
    }

    @Test
    void testCreateFloatMatrixWithNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.createFloatMatrix(null, null));
    }

    @Test
    void testCreateFloatMatrixWithMismatchedDimensions() {
        List<String> ids = Arrays.asList("A", "B");
        float[][] data = {{1.0f}};
        assertThrows(IllegalArgumentException.class, () -> FloatMatrix.createFloatMatrix(ids, data));
    }
}

