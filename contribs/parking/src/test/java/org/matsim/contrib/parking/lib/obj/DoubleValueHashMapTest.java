package org.matsim.contrib.parking.lib.obj;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.matsim.contrib.parking.parkingchoice.lib.obj.DoubleValueHashMap;

public class DoubleValueHashMapTest {

	@Test
	void testBasic(){
		DoubleValueHashMap<Integer> dhm=new DoubleValueHashMap<Integer>();

		dhm.put(0, 5.2);

		dhm.incrementBy(0, 3.0);

		dhm.decrementBy(0, 5.0);

		dhm.increment(0);

		dhm.decrement(0);

		assertEquals(3.2, dhm.get(0),0.0001);

		assertEquals(0.0, dhm.get(1), 0);

	}

}
