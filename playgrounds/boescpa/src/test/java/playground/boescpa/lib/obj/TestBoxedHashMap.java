/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.boescpa.lib.obj;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class TestBoxedHashMap {
	
	private BoxedHashMap<Integer, Integer> bhm;
	
	@Before
	public void prepareTest() {
		bhm = new BoxedHashMap<Integer, Integer>();
		bhm.put(1, 1);
		bhm.put(1, 2);
		bhm.put(256, 1000);
	}
	
	@Test
	public void testBasics() {
		assertEquals("Error in test of <size>", 2, bhm.size());
		assertTrue("Error in test of <containsValue>", bhm.containsValue(2));
		assertTrue("Error in test of <containsKey>", bhm.containsKey(256));
		bhm.clear();
		assertTrue("Error in test of <clear> and <isEmpty>.", bhm.isEmpty());
	}
	
	@Test
	public void testRetrieval() {
		// put
		bhm.put(10, 5);
		assertEquals("Error in test of <put>", 3, bhm.size());
		int i = bhm.getValue(10);
		assertEquals("Error in test of <getValue>", 5, i);
		// getValue
		i = bhm.getValue(1);
		assertEquals("Error in test of <getValue>", 1, i);
		assertNull("Error in test of <getValue>", bhm.getValue(8));
		// getValues
		List<Integer> al = bhm.getValues(1);
		assertEquals("Error in test of <getValues>", 2, al.size());
		assertNull("Error in test of <getValue>", bhm.getValues(8));
	}
	
	@Test
	public void testModification() {
		// removeLast
		bhm.put(1, 5);
		List<Integer> al = bhm.getValues(1);
		assertEquals("Error in test of <getValues>", 3, al.size());
		bhm.removeLast(1);
		al = bhm.getValues(1);
		assertEquals("Error in test of <getValues>", 2, al.size());
		int i = al.get(al.size()-1);
		assertEquals("Error in test of <removeLast>", 2, i);
		// remove
		bhm.remove(1);
		assertNull("Error in test of <getValue>", bhm.getValue(1));
	}
	
	@Test
	public void testOverviews() {
		// keySet
		Set<Integer> s = bhm.keySet();
		assertEquals("Error in test of <keySet>", 2, s.size());
		assertTrue("Error in test of <keySet>", s.contains(256));
		// values
		Collection<LinkedList<Integer>> c = bhm.values();
		assertEquals("Error in test of <values>", 2, c.size());
	}
}
