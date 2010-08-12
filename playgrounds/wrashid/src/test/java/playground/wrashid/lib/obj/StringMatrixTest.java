package playground.wrashid.lib.obj;

import java.util.Arrays;
import java.util.LinkedList;

import ucar.ma2.ArraySequence;

import junit.framework.TestCase;

public class StringMatrixTest extends TestCase{

	public void testBasic(){
		StringMatrix matrix=new StringMatrix();
		
		assertEquals(0, matrix.getNumberOfRows());
		
		LinkedList<String> row=new LinkedList<String>();
		row.add("1");
		
		matrix.addRow(row);
		
		assertEquals(1, matrix.getNumberOfRows());
		assertEquals("1", matrix.getString(0, 0));
		
	}
	
}
