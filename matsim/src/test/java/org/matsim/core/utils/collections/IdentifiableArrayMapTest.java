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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * @author mrieser / senozon
 */
public class IdentifiableArrayMapTest {

	private final static Logger log = LogManager.getLogger(IdentifiableArrayMapTest.class);

	@Test
	void testConstructor() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Assertions.assertEquals(0, map.size());
		Assertions.assertTrue(map.isEmpty());
	}

	@Test
	void testPutGet() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		Assertions.assertNull(map.put(id1, to1));
		Assertions.assertEquals(1, map.size());
		Assertions.assertFalse(map.isEmpty());
		Assertions.assertEquals(to1, map.get(id1));

		Assertions.assertNull(map.put(id2, to2));
		Assertions.assertEquals(2, map.size());
		Assertions.assertEquals(to2, map.get(id2));
		
		Assertions.assertNull(map.put(id3, to3));
		Assertions.assertEquals(3, map.size());
		Assertions.assertEquals(to3, map.get(id3));
		Assertions.assertEquals(to2, map.get(id2));
		Assertions.assertEquals(to1, map.get(id1));
	}

	@Test
	void testPutGet_identifiablePut() {
		IdentifiableArrayMap<TO, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		Assertions.assertNull(map.put(to1));
		Assertions.assertEquals(1, map.size());
		Assertions.assertEquals(to1, map.get(id1));
		
		Assertions.assertNull(map.put(to2));
		Assertions.assertEquals(2, map.size());
		Assertions.assertEquals(to2, map.get(id2));
		
		Assertions.assertNull(map.put(to3));
		Assertions.assertEquals(3, map.size());
		Assertions.assertEquals(to3, map.get(id3));
		Assertions.assertEquals(to2, map.get(id2));
		Assertions.assertEquals(to1, map.get(id1));
	}

	@Test
	void testPut_multiple() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		Assertions.assertEquals(1, map.size());

		map.put(id1, to1);
		Assertions.assertEquals(1, map.size());
		
		map.put(id2, to2);
		Assertions.assertEquals(2, map.size());
		
		map.put(id2, to2);
		Assertions.assertEquals(2, map.size());
		
		map.put(id3, to3);
		Assertions.assertEquals(3, map.size());
		
		map.put(id2, to2);
		Assertions.assertEquals(3, map.size());
		
		map.put(id1, to1);
		Assertions.assertEquals(3, map.size());
	}

	@Test
	void testGet_equalKeys() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id2a = Id.create(2, TO.class);
		Id<TO> id2b = Id.create(2, TO.class);
		
		TO to2 = new TO(id2a);
		
		map.put(id2a, to2);
		Assertions.assertEquals(1, map.size());
		Assertions.assertEquals(to2, map.get(id2a));
		Assertions.assertEquals(to2, map.get(id2b));
	}

	@Test
	void testPut_Overwrite() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2a = Id.create(2, TO.class);
		Id<TO> id2b = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2a = new TO(id2a);
		TO to2b = new TO(id2b);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		Assertions.assertEquals(1, map.size());

		Assertions.assertNull(map.put(id2a, to2a));
		Assertions.assertEquals(2, map.size());

		map.put(id3, to3);
		Assertions.assertEquals(3, map.size());
		
		Assertions.assertEquals(to2a, map.get(id2a));
		Assertions.assertEquals(to2a, map.put(id2b, to2b));
		Assertions.assertEquals(to2b, map.get(id2b));
		Assertions.assertEquals(to2b, map.get(id2a));
	}

	@Test
	void testContainsKey() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id2b = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		Assertions.assertFalse(map.containsKey(id1));
		Assertions.assertFalse(map.containsKey(id2));
		Assertions.assertFalse(map.containsKey(id2b));
		Assertions.assertFalse(map.containsKey(id3));
		
		map.put(id1, to1);
		Assertions.assertTrue(map.containsKey(id1));
		Assertions.assertFalse(map.containsKey(id2));
		Assertions.assertFalse(map.containsKey(id2b));
		Assertions.assertFalse(map.containsKey(id3));

		map.put(id2, to2);
		Assertions.assertTrue(map.containsKey(id1));
		Assertions.assertTrue(map.containsKey(id2));
		Assertions.assertTrue(map.containsKey(id2b));
		Assertions.assertFalse(map.containsKey(id3));
		
		map.put(id3, to3);
		Assertions.assertTrue(map.containsKey(id1));
		Assertions.assertTrue(map.containsKey(id2));
		Assertions.assertTrue(map.containsKey(id2b));
		Assertions.assertTrue(map.containsKey(id3));
	}

	@Test
	void testContainsValue() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		Assertions.assertFalse(map.containsValue(to1));
		Assertions.assertFalse(map.containsValue(to2));
		Assertions.assertFalse(map.containsValue(to3));
		
		map.put(id1, to1);
		Assertions.assertTrue(map.containsValue(to1));
		Assertions.assertFalse(map.containsValue(to2));
		Assertions.assertFalse(map.containsValue(to3));
		
		map.put(id2, to2);
		Assertions.assertTrue(map.containsValue(to1));
		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertFalse(map.containsValue(to3));
		
		map.put(id3, to3);
		Assertions.assertTrue(map.containsValue(to1));
		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertTrue(map.containsValue(to3));
	}

	@Test
	void testRemove_middle() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);

		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertEquals(to2, map.remove(id2));

		Assertions.assertTrue(map.containsValue(to1));
		Assertions.assertFalse(map.containsValue(to2));
		Assertions.assertTrue(map.containsValue(to3));
	}

	@Test
	void testRemove_start() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertEquals(to1, map.remove(id1));
		
		Assertions.assertFalse(map.containsValue(to1));
		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertTrue(map.containsValue(to3));
	}

	@Test
	void testRemove_end() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);

		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertEquals(to3, map.remove(id3));
		
		Assertions.assertTrue(map.containsValue(to1));
		Assertions.assertTrue(map.containsValue(to2));
		Assertions.assertFalse(map.containsValue(to3));
	}

	@Test
	void testClear() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Assertions.assertEquals(3, map.size());
		
		map.clear();
		
		Assertions.assertEquals(0, map.size());
		Assertions.assertTrue(map.isEmpty());
		Assertions.assertFalse(map.containsValue(to2));
		Assertions.assertNull(map.get(id2));
	}

	@Test
	void testValues() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Collection<TO> values = map.values();
		
		Assertions.assertEquals(3, values.size());
		Assertions.assertTrue(values.contains(to1));
		Assertions.assertTrue(values.contains(to2));
		Assertions.assertTrue(values.contains(to3));
	}

	@Test
	void testKeySet() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Set<Id<TO>> keys = map.keySet();
		
		Assertions.assertEquals(3, keys.size());
		Assertions.assertTrue(keys.contains(id1));
		Assertions.assertTrue(keys.contains(id2));
		Assertions.assertTrue(keys.contains(id3));
	}

	@Test
	void testEntrySet() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Set<Map.Entry<Id<TO>, TO>> entries = map.entrySet();
		
		Assertions.assertEquals(3, entries.size());
	}

	@Test
	void testValuesIterator() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);
		
		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);
		
		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);
		
		Iterator<TO> iter = map.values().iterator();
		Assertions.assertNotNull(iter);
		
		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(to1, iter.next());
		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(to2, iter.next());
		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(to3, iter.next());
		Assertions.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assertions.fail("expected NoSuchElementException.");
		} catch (NoSuchElementException e) {
			log.info("catched expected exception.");
		}
	}

	@Test
	void testValuesToArray() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		Id<TO> id2 = Id.create(2, TO.class);
		Id<TO> id3 = Id.create(3, TO.class);

		TO to1 = new TO(id1);
		TO to2 = new TO(id2);
		TO to3 = new TO(id3);

		map.put(id1, to1);
		map.put(id2, to2);
		map.put(id3, to3);

		Object[] array1 = map.values().toArray();
		Assertions.assertEquals(3, array1.length);
		Assertions.assertEquals(to1, array1[0]);
		Assertions.assertEquals(to2, array1[1]);
		Assertions.assertEquals(to3, array1[2]);

		TO[] array2 = map.values().toArray(new TO[0]);
		Assertions.assertEquals(3, array2.length);
		Assertions.assertEquals(to1, array2[0]);
		Assertions.assertEquals(to2, array2[1]);
		Assertions.assertEquals(to3, array2[2]);

		TO[] array3 = map.values().toArray(new TO[3]);
		Assertions.assertEquals(3, array3.length);
		Assertions.assertEquals(to1, array3[0]);
		Assertions.assertEquals(to2, array3[1]);
		Assertions.assertEquals(to3, array3[2]);
	}

	@Test
	void testValuesIterator_SingleDiretor() {
		Map<Id<TO>, TO> map = new IdentifiableArrayMap<>();
		Id<TO> id1 = Id.create(1, TO.class);
		
		TO to1 = new TO(id1);
		
		map.put(id1, to1);
		
		TO toX = map.values().iterator().next();
		
		Assertions.assertEquals(to1, toX);
	}
	
	private static class TO implements Identifiable<TO> {

		private final Id<TO> id;
		
		TO(final Id<TO> id) {
			this.id = id;
		}
		
		@Override
		public Id<TO> getId() {
			return this.id;
		}
	}

}
