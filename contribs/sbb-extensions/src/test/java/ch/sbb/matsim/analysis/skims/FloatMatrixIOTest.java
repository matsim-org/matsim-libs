/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package ch.sbb.matsim.analysis.skims;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author mrieser / SBB
 */
public class FloatMatrixIOTest {

	@Test
	void testIO() throws IOException {
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
        Assertions.assertEquals(2.0f, matrix2.get("un", "un"), epsilon);
        Assertions.assertEquals(3.0f, matrix2.get("un", "dos"), epsilon);
        Assertions.assertEquals(4.0f, matrix2.get("un", "tres"), epsilon);
        Assertions.assertEquals(4.0f, matrix2.get("dos", "un"), epsilon);
        Assertions.assertEquals(9.0f, matrix2.get("dos", "dos"), epsilon);
        Assertions.assertEquals(16.0f, matrix2.get("dos", "tres"), epsilon);
        Assertions.assertEquals(8.0f, matrix2.get("tres", "un"), epsilon);
        Assertions.assertEquals(27.0f, matrix2.get("tres", "dos"), epsilon);
        Assertions.assertEquals(64.0f, matrix2.get("tres", "tres"), epsilon);
    }
}
