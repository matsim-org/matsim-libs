package org.matsim.api.core.v01;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdMapTest {

	@Test
	public void testPutGetRemoveSize() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());

		Assert.assertNull(map.put(Id.create(1, Person.class), "one"));

		Assert.assertEquals(1, map.size());
		Assert.assertFalse(map.isEmpty());

		Assert.assertNull(map.put(Id.create(2, Person.class), "two"));

		Assert.assertEquals(2, map.size());
		Assert.assertFalse(map.isEmpty());

		Assert.assertEquals("one", map.put(Id.create(1, Person.class), "also-one"));
		Assert.assertEquals(2, map.size());

		Assert.assertNull(map.put(Id.create(3, Person.class), "three"));
		Assert.assertEquals(3, map.size());

		Assert.assertNull(map.put(Id.create(4, Person.class), null));
		Assert.assertEquals(3, map.size());

		Assert.assertEquals("also-one", map.get(Id.create(1, Person.class)));
		Assert.assertEquals("two", map.get(Id.create(2, Person.class)));
		Assert.assertEquals("three", map.get(Id.create(3, Person.class)));
		Assert.assertNull(map.get(Id.create(4, Person.class)));

		Assert.assertEquals("two", map.remove(Id.create(2, Person.class)));
		Assert.assertEquals(2, map.size());
		Assert.assertNull(map.get(Id.create(2, Person.class)));
	}

	@Test
	public void testIterable() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		int i = 0;
		for (String data : map) {
			if (i == 0) {
				Assert.assertEquals("one", data);
			} else if (i == 1) {
				Assert.assertEquals("two", data);
			} else if (i == 2) {
				Assert.assertEquals("four", data);
			} else if (i == 3) {
				Assert.assertEquals("five", data);
			} else {
				throw new RuntimeException("unexpected element: " + data);
			}
			i++;
		}
		Assert.assertEquals(4, i);
	}

	@Test
	public void testForEach() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		List<Tuple<Id<Person>, String>> data = new ArrayList<>();

		map.forEach((k, v) -> data.add(new Tuple<>(k, v)));

		Assert.assertEquals(Id.create(1, Person.class), data.get(0).getFirst());
		Assert.assertEquals("one", data.get(0).getSecond());

		Assert.assertEquals(Id.create(2, Person.class), data.get(1).getFirst());
		Assert.assertEquals("two", data.get(1).getSecond());

		Assert.assertEquals(Id.create(4, Person.class), data.get(2).getFirst());
		Assert.assertEquals("four", data.get(2).getSecond());

		Assert.assertEquals(Id.create(5, Person.class), data.get(3).getFirst());
		Assert.assertEquals("five", data.get(3).getSecond());
	}

	@Test
	public void testContainsKey() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Assert.assertTrue(map.containsKey(Id.create(1, Person.class)));
		Assert.assertTrue(map.containsKey(Id.create(2, Person.class)));
		Assert.assertFalse(map.containsKey(Id.create(3, Person.class)));
		Assert.assertTrue(map.containsKey(Id.create(4, Person.class)));
		Assert.assertTrue(map.containsKey(Id.create(5, Person.class)));
		Assert.assertFalse(map.containsKey(Id.create(6, Person.class)));

		Assert.assertTrue(map.containsKey(Id.create(1, Person.class).index()));
		Assert.assertTrue(map.containsKey(Id.create(2, Person.class).index()));
		Assert.assertFalse(map.containsKey(Id.create(3, Person.class).index()));
		Assert.assertTrue(map.containsKey(Id.create(4, Person.class).index()));
		Assert.assertTrue(map.containsKey(Id.create(5, Person.class).index()));
		Assert.assertFalse(map.containsKey(Id.create(6, Person.class).index()));

		Assert.assertTrue(map.containsKey((Object) Id.create(1, Person.class)));
		Assert.assertTrue(map.containsKey((Object) Id.create(2, Person.class)));
		Assert.assertFalse(map.containsKey((Object) Id.create(3, Person.class)));
		Assert.assertTrue(map.containsKey((Object) Id.create(4, Person.class)));
		Assert.assertTrue(map.containsKey((Object) Id.create(5, Person.class)));
		Assert.assertFalse(map.containsKey((Object) Id.create(6, Person.class)));
	}

	@Test
	public void testContainsValue() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Assert.assertTrue(map.containsValue("one"));
		Assert.assertTrue(map.containsValue("two"));
		Assert.assertFalse(map.containsValue("three"));
		Assert.assertTrue(map.containsValue("four"));
		Assert.assertTrue(map.containsValue("five"));
		Assert.assertFalse(map.containsValue("six"));
	}

	@Test
	public void testPutAll_IdMap() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		IdMap<Person, String> map2 = new IdMap<>(Person.class, 10);
		map2.put(Id.create(1, Person.class), "one");
		map2.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map2.put(Id.create(4, Person.class), "four");
		map2.put(Id.create(5, Person.class), "five");

		map.putAll(map2);

		Assert.assertEquals(4, map.size());

		Assert.assertEquals("one", map.get(Id.create(1, Person.class)));
		Assert.assertEquals("two", map.get(Id.create(2, Person.class)));
		Assert.assertNull(map.get(Id.create(3, Person.class)));
		Assert.assertEquals("four", map.get(Id.create(4, Person.class)));
		Assert.assertEquals("five", map.get(Id.create(5, Person.class)));
	}

	@Test
	public void testPutAll_GenericMap() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		Map<Id<Person>, String> map2 = new HashMap<>();
		map2.put(Id.create(1, Person.class), "one");
		map2.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map2.put(Id.create(4, Person.class), "four");
		map2.put(Id.create(5, Person.class), "five");

		map.putAll(map2);

		Assert.assertEquals(4, map.size());

		Assert.assertEquals("one", map.get(Id.create(1, Person.class)));
		Assert.assertEquals("two", map.get(Id.create(2, Person.class)));
		Assert.assertNull(map.get(Id.create(3, Person.class)));
		Assert.assertEquals("four", map.get(Id.create(4, Person.class)));
		Assert.assertEquals("five", map.get(Id.create(5, Person.class)));
	}

	@Test
	public void testClear() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Assert.assertEquals(4, map.size());

		map.clear();

		Assert.assertEquals(0, map.size());

		Assert.assertNull(map.get(Id.create(1, Person.class)));
		Assert.assertNull(map.get(Id.create(2, Person.class)));
		Assert.assertNull(map.get(Id.create(3, Person.class)));

		Assert.assertFalse(map.containsKey(Id.create(1, Person.class)));
	}

	@Test
	public void testValues() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Collection<String> coll = map.values();
		Assert.assertEquals(4, coll.size());

		map.put(Id.create(6, Person.class), "six");
		Assert.assertEquals(5, coll.size());

		Assert.assertTrue(coll.remove("one"));
		Assert.assertFalse(coll.remove("null"));

		Assert.assertFalse(map.containsValue("one"));
		Assert.assertTrue(map.containsValue("two"));

		Assert.assertTrue(coll.contains("two"));
		Assert.assertFalse(coll.contains("one"));

		Set<String> values = new HashSet<>();
		coll.forEach(v -> values.add(v));

		Assert.assertEquals(4, values.size());
		Assert.assertTrue(values.contains("two"));
		Assert.assertTrue(values.contains("four"));
		Assert.assertTrue(values.contains("five"));
		Assert.assertTrue(values.contains("six"));
	}

	@Test
	public void testKeySet() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(id1, "one");
		map.put(id2, "two");
		map.put(id4, "four");
		map.put(id5, "five");

		Set<Id<Person>> set = map.keySet();
		Assert.assertEquals(4, set.size());

		map.put(id6, "six");
		Assert.assertEquals(5, set.size());

		Assert.assertTrue(set.remove(id1));
		Assert.assertFalse(set.remove(id3));

		Assert.assertFalse(map.containsKey(id1));
		Assert.assertTrue(map.containsKey(id2));

		Assert.assertTrue(set.contains(id2));
		Assert.assertFalse(set.contains(id1));

		Set<Id<Person>> keys = new HashSet<>();
		set.forEach(k -> keys.add(k));

		Assert.assertEquals(4, keys.size());
		Assert.assertTrue(keys.contains(id2));
		Assert.assertTrue(keys.contains(id4));
		Assert.assertTrue(keys.contains(id5));
		Assert.assertTrue(keys.contains(id6));
	}

	@Test
	public void testEntrySet() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(id1, "one");
		map.put(id2, "two");
		map.put(id4, "four");
		map.put(id5, "five");

		Set<Map.Entry<Id<Person>, String>> set = map.entrySet();
		Assert.assertEquals(4, set.size());

		map.put(id6, "six");
		Assert.assertEquals(5, set.size());

		Map<Id<Person>, Map.Entry<Id<Person>, String>> entries = new HashMap<>();

		for (Map.Entry<Id<Person>, String> e : set) {
			entries.put(e.getKey(), e);
		}

		Assert.assertEquals(id1, entries.get(id1).getKey());
		Assert.assertEquals("one", entries.get(id1).getValue());
		Assert.assertEquals("two", entries.get(id2).getValue());
		Assert.assertEquals("four", entries.get(id4).getValue());
		Assert.assertEquals("five", entries.get(id5).getValue());

		Assert.assertTrue(set.remove(entries.get(id1)));
		Assert.assertFalse(set.remove(new Object()));

		Assert.assertFalse(map.containsKey(id1));
		Assert.assertTrue(map.containsKey(id2));

		Assert.assertTrue(set.contains(entries.get(id2)));
		Assert.assertFalse(set.contains(entries.get(id1)));

		// test forEach
		Set<Map.Entry<Id<Person>, String>> es = new HashSet<>();
		set.forEach(k -> es.add(k));

		Assert.assertEquals(4, es.size());
		Assert.assertTrue(es.contains(entries.get(id2)));
		Assert.assertTrue(es.contains(entries.get(id4)));
		Assert.assertTrue(es.contains(entries.get(id5)));
		Assert.assertTrue(es.contains(entries.get(id6)));
	}

	@Test
	public void testIterator_iterate() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(id1, "one");
		map.put(id2, "two");
		map.put(id4, "four");
		map.put(id5, "five");

		Iterator<String> iter = map.iterator();
		Assert.assertNotNull(iter);

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("one", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("two", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("four", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("five", iter.next());

		Assert.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assert.fail("Expected exception, got none.");
		} catch (NoSuchElementException ignore) {
		}

		Assert.assertFalse(iter.hasNext());
	}

	@Test
	public void testIterator_remove() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(id1, "one");
		map.put(id2, "two");
		map.put(id4, "four");
		map.put(id5, "five");

		Iterator<String> iter = map.iterator();
		Assert.assertNotNull(iter);

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("one", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("two", iter.next());

		iter.remove();

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("four", iter.next());

		Assert.assertEquals(3, map.size());
		Assert.assertTrue(map.containsValue("one"));
		Assert.assertFalse(map.containsValue("two"));
		Assert.assertTrue(map.containsValue("four"));
		Assert.assertTrue(map.containsValue("five"));
	}

	@Test
	public void testKeySetToArray() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(id1, "one");
		map.put(id2, "two");
		map.put(id4, "four");
		map.put(id5, "five");

		Id<Person>[] array = (Id<Person>[]) map.keySet().toArray();
		Assert.assertEquals(id1, array[0]);
		Assert.assertEquals(id2, array[1]);
		Assert.assertEquals(id4, array[2]);
		Assert.assertEquals(id5, array[3]);

	}

	@Test
	public void testEqualsAndHashCode() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> mapA = new IdMap<>(Person.class, 4);
		IdMap<Person, String> mapB = new IdMap<>(Person.class, 4);
		IdMap<Person, String> mapC = new IdMap<>(Person.class, 10);

		Assert.assertEquals(mapA, mapA);
		Assert.assertEquals(mapA, mapB);
		Assert.assertEquals(mapA, mapC);
		Assert.assertEquals(mapA.hashCode(), mapB.hashCode());
		Assert.assertEquals(mapA.hashCode(), mapC.hashCode());

		mapA.put(id1, "one");
		mapB.put(id1, "one");

		Assert.assertEquals(mapA, mapA);
		Assert.assertEquals(mapA, mapB);
		Assert.assertNotEquals(mapA, mapC);
		Assert.assertEquals(mapA.hashCode(), mapB.hashCode());
		Assert.assertNotEquals(mapA.hashCode(), mapC.hashCode());

		mapC.put(id1, "one");

		Assert.assertEquals(mapA, mapC);
		Assert.assertEquals(mapA.hashCode(), mapC.hashCode());

		mapA.put(id2, "two");
		mapB.put(id3, "three");

		Assert.assertNotEquals(mapA, mapB);
		Assert.assertNotEquals(mapA.hashCode(), mapB.hashCode());

		mapA.put(id3, "three");
		mapB.put(id2, "two");

		Assert.assertEquals(mapA, mapB);
		Assert.assertEquals(mapA.hashCode(), mapB.hashCode());

		mapA.put(id4, "four");
		mapA.put(id5, "five");
		mapA.put(id6, "six");
		mapA.remove(id4, "four");
		mapA.remove(id5, "five");
		mapA.remove(id6, "six");

		Assert.assertEquals(mapA, mapB);
		Assert.assertEquals(mapA.hashCode(), mapB.hashCode());

		Assert.assertEquals(mapA.entrySet(), mapB.entrySet());
		Assert.assertEquals(mapA.entrySet().hashCode(), mapB.entrySet().hashCode());
		Assert.assertEquals(mapA.keySet(), mapB.keySet());
		Assert.assertEquals(mapA.keySet().hashCode(), mapB.keySet().hashCode());

		mapA.put(id4, "four");
		mapB.put(id4, "DifferentFour");

		Assert.assertNotEquals(mapA, mapB);
		Assert.assertNotEquals(mapA.hashCode(), mapB.hashCode());

		Assert.assertNotEquals(mapA.entrySet(), mapB.entrySet());
		Assert.assertNotEquals(mapA.entrySet().hashCode(), mapB.entrySet().hashCode());
		Assert.assertEquals(mapA.keySet(), mapB.keySet());
		Assert.assertEquals(mapA.keySet().hashCode(), mapB.keySet().hashCode());

		HashMap<Id<Person>, String> hMapA = new HashMap<Id<Person>, String>(mapA);

		// The commented out tests will fail right now because the hashCode of IdImpl is based on the id, not the index
		Assert.assertEquals(hMapA, mapA);
//		Assert.assertEquals(hMapA.hashCode(), mapA.hashCode());
		Assert.assertEquals(hMapA.entrySet(), mapA.entrySet());
//		Assert.assertEquals(hMapA.entrySet().hashCode(), mapA.entrySet().hashCode());
		Assert.assertEquals(hMapA.keySet(), mapA.keySet());
//		Assert.assertEquals(hMapA.keySet().hashCode(), mapA.keySet().hashCode());
		Assert.assertNotEquals(hMapA, mapB);
		Assert.assertNotEquals(hMapA.hashCode(), mapB.hashCode());
		Assert.assertNotEquals(hMapA.entrySet(), mapB.entrySet());
		Assert.assertNotEquals(hMapA.entrySet().hashCode(), mapB.entrySet().hashCode());
		Assert.assertEquals(hMapA.keySet(), mapB.keySet());
//		Assert.assertEquals(hMapA.keySet().hashCode(), mapB.keySet().hashCode());

		// Best way I could think of to explicitly test the equals() of Entry (i.e. not inside the EntrySet)
		Iterator<Entry<Id<Person>, String>> iter = mapA.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Id<Person>, String> e = iter.next();
			if (e.getKey() != id4) {
				Assert.assertTrue(mapB.entrySet().contains(e));
			} else {
				Assert.assertFalse(mapB.entrySet().contains(e));
			}
			Assert.assertTrue(hMapA.entrySet().contains(e));
		}
	}

}
