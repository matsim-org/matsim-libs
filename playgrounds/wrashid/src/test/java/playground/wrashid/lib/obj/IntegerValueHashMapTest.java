package playground.wrashid.lib.obj;

import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;

import junit.framework.TestCase;

public class IntegerValueHashMapTest extends TestCase{

	public void testBasic(){
		IntegerValueHashMap<Integer> dhm=new IntegerValueHashMap<Integer>();
		
		dhm.set(0, 5);
		
		dhm.incrementBy(0, 3);
		
		dhm.decrementBy(0, 5);
		
		dhm.increment(0);
		
		dhm.decrement(0);
		
		assertEquals(3, dhm.get(0));
		
		assertEquals(0, dhm.get(1));
		
	}
	
}
