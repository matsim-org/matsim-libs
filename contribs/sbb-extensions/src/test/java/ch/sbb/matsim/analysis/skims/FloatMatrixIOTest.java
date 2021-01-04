/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package ch.sbb.matsim.analysis.skims;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author mrieser / SBB
 */
public class FloatMatrixIOTest {

    @Test
    public void testIO() throws IOException {
        Set<String> zoneIds = new HashSet<>();
        zoneIds.add("un");
        zoneIds.add("dos");
        zoneIds.add("tres");
        FloatMatrix<String> matrix = new FloatMatrix<>(zoneIds, 0.0f);

        matrix.set("un", "un", 2.0f);
        matrix.set("un", "dos", 3.0f);
        matrix.set("un", "tres", 4.0f);
        matrix.set("dos", "un", 4.0f);
        matrix.set("dos", "dos", 9.0f);
        matrix.set("dos", "tres", 16.0f);
        matrix.set("tres", "un", 8.0f);
        matrix.set("tres", "dos", 27.0f);
        matrix.set("tres", "tres", 64.0f);

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        FloatMatrixIO.writeAsCSV(matrix, outStream);
        outStream.close();

        ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
        FloatMatrix<String> matrix2 = new FloatMatrix<>(zoneIds, Float.NaN);
        FloatMatrixIO.readAsCSV(matrix2, inStream, id -> id);
        inStream.close();

        float epsilon = 1e-6f;
        Assert.assertEquals(2.0f, matrix2.get("un", "un"), epsilon);
        Assert.assertEquals(3.0f, matrix2.get("un", "dos"), epsilon);
        Assert.assertEquals(4.0f, matrix2.get("un", "tres"), epsilon);
        Assert.assertEquals(4.0f, matrix2.get("dos", "un"), epsilon);
        Assert.assertEquals(9.0f, matrix2.get("dos", "dos"), epsilon);
        Assert.assertEquals(16.0f, matrix2.get("dos", "tres"), epsilon);
        Assert.assertEquals(8.0f, matrix2.get("tres", "un"), epsilon);
        Assert.assertEquals(27.0f, matrix2.get("tres", "dos"), epsilon);
        Assert.assertEquals(64.0f, matrix2.get("tres", "tres"), epsilon);
    }
}