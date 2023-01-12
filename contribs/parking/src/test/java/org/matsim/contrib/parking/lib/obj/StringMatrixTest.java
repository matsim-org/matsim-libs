package org.matsim.contrib.parking.lib.obj;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;
import org.matsim.contrib.parking.parkingchoice.lib.obj.Matrix;

public class StringMatrixTest {

	@Test public void testBasic(){
		Matrix matrix=new Matrix();

		assertEquals(0, matrix.getNumberOfRows());

		ArrayList<String> row=new ArrayList<String>();
		row.add("1");

		matrix.addRow(row);

		assertEquals(1, matrix.getNumberOfRows());
		assertEquals("1", matrix.getString(0, 0));

	}

}
