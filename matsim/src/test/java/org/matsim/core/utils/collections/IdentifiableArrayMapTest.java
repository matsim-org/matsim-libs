/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.collections;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.core.basic.v01.IdImpl;

/**
 * @author mrieser / senozon
 */
public class IdentifiableArrayMapTest {

	private final static Logger log = Logger.getLogger(IdentifiableArrayMapTest.class);
	
	@Test
	public void testConstructor() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Assert.assertEquals(0, map.size());
	}
	
	@Test
	public void testPutGet() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		Assert.assertNull(map.put(id1, to1));
		Assert.assertEquals(1, map.size());
		Assert.assertEquals(to1, map.get(id1));

		Assert.assertNull(map.put(id2, to2));
		Assert.assertEquals(2, map.size());
		Assert.assertEquals(to2, map.get(id2));
		
		Assert.assertNull(map.put(id3, to3));
		Assert.assertEquals(3, map.size());
		Assert.assertEquals(to3, map.get(id3));
		Assert.assertEquals(to2, map.get(id2));
		Assert.assertEquals(to1, map.get(id1));
	}
	
	@Test
	public void testPutGet_identifiablePut() {
		IdentifiableArrayMap<TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		Assert.assertNull(map.put(to1));
		Assert.assertEquals(1, map.size());
		Assert.assertEquals(to1, map.get(id1));
		
		Assert.assertNull(map.put(to2));
		Assert.assertEquals(2, map.size());
		Assert.assertEquals(to2, map.get(id2));
		
		Assert.assertNull(map.put(to3));
		Assert.assertEquals(3, map.size());
		Assert.assertEquals(to3, map.get(id3));
		Assert.assertEquals(to2, map.get(id2));
		Assert.assertEquals(to1, map.get(id1));
	}
	
	@Test
	public void testPut_multiple() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		Assert.assertEquals(1, map.size());

		map.put(id1, to1);
		Assert.assertEquals(1, map.size());
		
		map.put(id2, to2);
		Assert.assertEquals(2, map.size());
		
		map.put(id2, to2);
		Assert.assertEquals(2, map.size());
		
		map.put(id3, to3);
		Assert.assertEquals(3, map.size());
		
		map.put(id2, to2);
		Assert.assertEquals(3, map.size());
		
		map.put(id1, to1);
		Assert.assertEquals(3, map.size());
	}
	
	@Test
	public void testGet_equalKeys() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id2a = new IdImpl(2);
		Id id2b = new IdImpl(2);
		
		TO to2 = new TO(id2a, "zwei");
		
		map.put(id2a, to2);
		Assert.assertEquals(1, map.size());
		Assert.assertEquals(to2, map.get(id2a));
		Assert.assertEquals(to2, map.get(id2b));
	}

	@Test
	public void testPut_Overwrite() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2a = new IdImpl(2);
		Id id2b = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2a = new TO(id2a, "zweiA");
		TO to2b = new TO(id2b, "zweiB");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		Assert.assertEquals(1, map.size());

		Assert.assertNull(map.put(id2a, to2a));
		Assert.assertEquals(2, map.size());

		map.put(id3, to3);
		Assert.assertEquals(3, map.size());
		
		Assert.assertEquals(to2a, map.get(id2a));
		Assert.assertEquals(to2a, map.put(id2b, to2b));
		Assert.assertEquals(to2b, map.get(id2b));
		Assert.assertEquals(to2b, map.get(id2a));
	}
	
	@Test
	public void testContainsKey() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id2b = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		Assert.assertFalse(map.containsKey(id1));
		Assert.assertFalse(map.containsKey(id2));
		Assert.assertFalse(map.containsKey(id2b));
		Assert.assertFalse(map.containsKey(id3));
		
		map.put(id1, to1);
		Assert.assertTrue(map.containsKey(id1));
		Assert.assertFalse(map.containsKey(id2));
		Assert.assertFalse(map.containsKey(id2b));
		Assert.assertFalse(map.containsKey(id3));

		map.put(id2, to2);
		Assert.assertTrue(map.containsKey(id1));
		Assert.assertTrue(map.containsKey(id2));
		Assert.assertTrue(map.containsKey(id2b));
		Assert.assertFalse(map.containsKey(id3));
		
		map.put(id3, to3);
		Assert.assertTrue(map.containsKey(id1));
		Assert.assertTrue(map.containsKey(id2));
		Assert.assertTrue(map.containsKey(id2b));
		Assert.assertTrue(map.containsKey(id3));
	}
	
	@Test
	public void testContainsValue() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id2b = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		Assert.assertFalse(map.containsValue(to1));
		Assert.assertFalse(map.containsValue(to2));
		Assert.assertFalse(map.containsValue(to3));
		
		map.put(id1, to1);
		Assert.assertTrue(map.containsValue(to1));
		Assert.assertFalse(map.containsValue(to2));
		Assert.assertFalse(map.containsValue(to3));
		
		map.put(id2, to2);
		Assert.assertTrue(map.containsValue(to1));
		Assert.assertTrue(map.containsValue(to2));
		Assert.assertFalse(map.containsValue(to3));
		
		map.put(id3, to3);
		Assert.assertTrue(map.containsValue(to1));
		Assert.assertTrue(map.containsValue(to2));
		Assert.assertTrue(map.containsValue(to3));
	}
	
	@Test
	public void testRemove_middle() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);

		Assert.assertTrue(map.containsValue(to2));
		Assert.assertEquals(to2, map.remove(id2));

		Assert.assertTrue(map.containsValue(to1));
		Assert.assertFalse(map.containsValue(to2));
		Assert.assertTrue(map.containsValue(to3));
	}
	
	@Test
	public void testRemove_start() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Assert.assertTrue(map.containsValue(to2));
		Assert.assertEquals(to1, map.remove(id1));
		
		Assert.assertFalse(map.containsValue(to1));
		Assert.assertTrue(map.containsValue(to2));
		Assert.assertTrue(map.containsValue(to3));
	}
	
	@Test
	public void testRemove_end() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Assert.assertTrue(map.containsValue(to2));
		Assert.assertEquals(to3, map.remove(id3));
		
		Assert.assertTrue(map.containsValue(to1));
		Assert.assertTrue(map.containsValue(to2));
		Assert.assertFalse(map.containsValue(to3));
	}
	
	@Test
	public void testClear() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Assert.assertEquals(3, map.size());
		
		map.clear();
		
		Assert.assertEquals(0, map.size());
		Assert.assertFalse(map.containsValue(to2));
		Assert.assertNull(map.get(id2));
	}
	
	@Test
	public void testValues() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Collection<TO> values = map.values();
		
		Assert.assertEquals(3, values.size());
		Assert.assertTrue(values.contains(to1));
		Assert.assertTrue(values.contains(to2));
		Assert.assertTrue(values.contains(to3));
	}
	
	@Test
	public void testKeySet() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Set<Id> keys = map.keySet();
		
		Assert.assertEquals(3, keys.size());
		Assert.assertTrue(keys.contains(id1));
		Assert.assertTrue(keys.contains(id2));
		Assert.assertTrue(keys.contains(id3));
	}
	
	@Test
	public void testEntrySet() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Set<Map.Entry<Id, TO>> entries = map.entrySet();
		
		Assert.assertEquals(3, entries.size());
	}
	
	@Test
	public void testValuesIterator() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		Id id2 = new IdImpl(2);
		Id id3 = new IdImpl(3);
		
		TO to1 = new TO(id1, "eins");
		TO to2 = new TO(id2, "zwei");
		TO to3 = new TO(id3, "drei");
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Iterator<TO> iter = map.values().iterator();
		Assert.assertNotNull(iter);
		
		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals(to1, iter.next());
		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals(to2, iter.next());
		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals(to3, iter.next());
		Assert.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assert.fail("expected NoSuchElementException.");
		} catch (NoSuchElementException e) {
			log.info("catched expected exception.");
		}
	}
	
	@Test
	public void testValuesIterator_SingleDiretor() {
		Map<Id, TO> map = new IdentifiableArrayMap<TO>();
		Id id1 = new IdImpl(1);
		
		TO to1 = new TO(id1, "eins");
		
		map.put(id1, to1);
		
		TO toX = map.values().iterator().next();
		
		Assert.assertEquals(to1, toX);
	}
	
	private static class TO implements Identifiable {

		private final Id id;
		private final String value;
		
		public TO(final Id id, final String value) {
			this.id = id;
			this.value = value;
		}
		
		@Override
		public Id getId() {
			return this.id;
		}
	}

}
