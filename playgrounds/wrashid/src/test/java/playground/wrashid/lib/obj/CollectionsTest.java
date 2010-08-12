package playground.wrashid.lib.obj;


import java.util.Collection;
import java.util.LinkedList;

import junit.framework.TestCase;


public class CollectionsTest extends TestCase{

	public void testConvertDoubleCollectionToArray(){
		Collection<Double> values=new LinkedList<Double>();
		
		values.add(1.0);
		values.add(2.0);
		
		double[] doubleArray=Collections.convertDoubleCollectionToArray(values);
		
		assertEquals(2, doubleArray.length);
		assertEquals(1.0, doubleArray[0]);
		assertEquals(2.0, doubleArray[1]);
		
	}
	
}
