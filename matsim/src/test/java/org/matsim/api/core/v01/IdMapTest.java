package org.matsim.api.core.v01;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdMapTest {

	@Test
	void testPutGetRemoveSize() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		Assertions.assertEquals(0, map.size());
		Assertions.assertTrue(map.isEmpty());

		Assertions.assertNull(map.put(Id.create(1, Person.class), "one"));

		Assertions.assertEquals(1, map.size());
		Assertions.assertFalse(map.isEmpty());

		Assertions.assertNull(map.put(Id.create(2, Person.class), "two"));

		Assertions.assertEquals(2, map.size());
		Assertions.assertFalse(map.isEmpty());

		Assertions.assertEquals("one", map.put(Id.create(1, Person.class), "also-one"));
		Assertions.assertEquals(2, map.size());

		Assertions.assertNull(map.put(Id.create(3, Person.class), "three"));
		Assertions.assertEquals(3, map.size());

		Assertions.assertNull(map.put(Id.create(4, Person.class), null));
		Assertions.assertEquals(3, map.size());

		Assertions.assertEquals("also-one", map.get(Id.create(1, Person.class)));
		Assertions.assertEquals("two", map.get(Id.create(2, Person.class)));
		Assertions.assertEquals("three", map.get(Id.create(3, Person.class)));
		Assertions.assertNull(map.get(Id.create(4, Person.class)));

		Assertions.assertEquals("two", map.remove(Id.create(2, Person.class)));
		Assertions.assertEquals(2, map.size());
		Assertions.assertNull(map.get(Id.create(2, Person.class)));
	}

	@Test
	void testIterable() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		int i = 0;
		for (String data : map) {
			if (i == 0) {
				Assertions.assertEquals("one", data);
			} else if (i == 1) {
				Assertions.assertEquals("two", data);
			} else if (i == 2) {
				Assertions.assertEquals("four", data);
			} else if (i == 3) {
				Assertions.assertEquals("five", data);
			} else {
				throw new RuntimeException("unexpected element: " + data);
			}
			i++;
		}
		Assertions.assertEquals(4, i);
	}

	@Test
	void testForEach() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		List<Tuple<Id<Person>, String>> data = new ArrayList<>();

		map.forEach((k, v) -> data.add(new Tuple<>(k, v)));

		Assertions.assertEquals(Id.create(1, Person.class), data.get(0).getFirst());
		Assertions.assertEquals("one", data.get(0).getSecond());

		Assertions.assertEquals(Id.create(2, Person.class), data.get(1).getFirst());
		Assertions.assertEquals("two", data.get(1).getSecond());

		Assertions.assertEquals(Id.create(4, Person.class), data.get(2).getFirst());
		Assertions.assertEquals("four", data.get(2).getSecond());

		Assertions.assertEquals(Id.create(5, Person.class), data.get(3).getFirst());
		Assertions.assertEquals("five", data.get(3).getSecond());
	}

	@Test
	void testContainsKey() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Assertions.assertTrue(map.containsKey(Id.create(1, Person.class)));
		Assertions.assertTrue(map.containsKey(Id.create(2, Person.class)));
		Assertions.assertFalse(map.containsKey(Id.create(3, Person.class)));
		Assertions.assertTrue(map.containsKey(Id.create(4, Person.class)));
		Assertions.assertTrue(map.containsKey(Id.create(5, Person.class)));
		Assertions.assertFalse(map.containsKey(Id.create(6, Person.class)));

		Assertions.assertTrue(map.containsKey(Id.create(1, Person.class).index()));
		Assertions.assertTrue(map.containsKey(Id.create(2, Person.class).index()));
		Assertions.assertFalse(map.containsKey(Id.create(3, Person.class).index()));
		Assertions.assertTrue(map.containsKey(Id.create(4, Person.class).index()));
		Assertions.assertTrue(map.containsKey(Id.create(5, Person.class).index()));
		Assertions.assertFalse(map.containsKey(Id.create(6, Person.class).index()));

		Assertions.assertTrue(map.containsKey((Object) Id.create(1, Person.class)));
		Assertions.assertTrue(map.containsKey((Object) Id.create(2, Person.class)));
		Assertions.assertFalse(map.containsKey((Object) Id.create(3, Person.class)));
		Assertions.assertTrue(map.containsKey((Object) Id.create(4, Person.class)));
		Assertions.assertTrue(map.containsKey((Object) Id.create(5, Person.class)));
		Assertions.assertFalse(map.containsKey((Object) Id.create(6, Person.class)));
	}

	@Test
	void testContainsValue() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Assertions.assertTrue(map.containsValue("one"));
		Assertions.assertTrue(map.containsValue("two"));
		Assertions.assertFalse(map.containsValue("three"));
		Assertions.assertTrue(map.containsValue("four"));
		Assertions.assertTrue(map.containsValue("five"));
		Assertions.assertFalse(map.containsValue("six"));
	}

	@Test
	void testPutAll_IdMap() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		IdMap<Person, String> map2 = new IdMap<>(Person.class, 10);
		map2.put(Id.create(1, Person.class), "one");
		map2.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map2.put(Id.create(4, Person.class), "four");
		map2.put(Id.create(5, Person.class), "five");

		map.putAll(map2);

		Assertions.assertEquals(4, map.size());

		Assertions.assertEquals("one", map.get(Id.create(1, Person.class)));
		Assertions.assertEquals("two", map.get(Id.create(2, Person.class)));
		Assertions.assertNull(map.get(Id.create(3, Person.class)));
		Assertions.assertEquals("four", map.get(Id.create(4, Person.class)));
		Assertions.assertEquals("five", map.get(Id.create(5, Person.class)));
	}

	@Test
	void testPutAll_GenericMap() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		Map<Id<Person>, String> map2 = new HashMap<>();
		map2.put(Id.create(1, Person.class), "one");
		map2.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map2.put(Id.create(4, Person.class), "four");
		map2.put(Id.create(5, Person.class), "five");

		map.putAll(map2);

		Assertions.assertEquals(4, map.size());

		Assertions.assertEquals("one", map.get(Id.create(1, Person.class)));
		Assertions.assertEquals("two", map.get(Id.create(2, Person.class)));
		Assertions.assertNull(map.get(Id.create(3, Person.class)));
		Assertions.assertEquals("four", map.get(Id.create(4, Person.class)));
		Assertions.assertEquals("five", map.get(Id.create(5, Person.class)));
	}

	@Test
	void testClear() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Assertions.assertEquals(4, map.size());

		map.clear();

		Assertions.assertEquals(0, map.size());

		Assertions.assertNull(map.get(Id.create(1, Person.class)));
		Assertions.assertNull(map.get(Id.create(2, Person.class)));
		Assertions.assertNull(map.get(Id.create(3, Person.class)));

		Assertions.assertFalse(map.containsKey(Id.create(1, Person.class)));
	}

	@Test
	void testValues() {
		IdMap<Person, String> map = new IdMap<>(Person.class, 10);

		map.put(Id.create(1, Person.class), "one");
		map.put(Id.create(2, Person.class), "two");
		Id unused = Id.create(3, Person.class);
		map.put(Id.create(4, Person.class), "four");
		map.put(Id.create(5, Person.class), "five");

		Collection<String> coll = map.values();
		Assertions.assertEquals(4, coll.size());

		map.put(Id.create(6, Person.class), "six");
		Assertions.assertEquals(5, coll.size());

		Assertions.assertTrue(coll.remove("one"));
		Assertions.assertFalse(coll.remove("null"));

		Assertions.assertFalse(map.containsValue("one"));
		Assertions.assertTrue(map.containsValue("two"));

		Assertions.assertTrue(coll.contains("two"));
		Assertions.assertFalse(coll.contains("one"));

		Set<String> values = new HashSet<>();
		coll.forEach(v -> values.add(v));

		Assertions.assertEquals(4, values.size());
		Assertions.assertTrue(values.contains("two"));
		Assertions.assertTrue(values.contains("four"));
		Assertions.assertTrue(values.contains("five"));
		Assertions.assertTrue(values.contains("six"));
	}

	@Test
	void testKeySet() {
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
		Assertions.assertEquals(4, set.size());

		map.put(id6, "six");
		Assertions.assertEquals(5, set.size());

		Assertions.assertTrue(set.remove(id1));
		Assertions.assertFalse(set.remove(id3));

		Assertions.assertFalse(map.containsKey(id1));
		Assertions.assertTrue(map.containsKey(id2));

		Assertions.assertTrue(set.contains(id2));
		Assertions.assertFalse(set.contains(id1));

		Set<Id<Person>> keys = new HashSet<>();
		set.forEach(k -> keys.add(k));

		Assertions.assertEquals(4, keys.size());
		Assertions.assertTrue(keys.contains(id2));
		Assertions.assertTrue(keys.contains(id4));
		Assertions.assertTrue(keys.contains(id5));
		Assertions.assertTrue(keys.contains(id6));
	}

	@Test
	void testEntrySet() {
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
		Assertions.assertEquals(4, set.size());

		map.put(id6, "six");
		Assertions.assertEquals(5, set.size());

		Map<Id<Person>, Map.Entry<Id<Person>, String>> entries = new HashMap<>();

		for (Map.Entry<Id<Person>, String> e : set) {
			entries.put(e.getKey(), e);
		}

		Assertions.assertEquals(id1, entries.get(id1).getKey());
		Assertions.assertEquals("one", entries.get(id1).getValue());
		Assertions.assertEquals("two", entries.get(id2).getValue());
		Assertions.assertEquals("four", entries.get(id4).getValue());
		Assertions.assertEquals("five", entries.get(id5).getValue());

		Assertions.assertTrue(set.remove(entries.get(id1)));
		Assertions.assertFalse(set.remove(new Object()));

		Assertions.assertFalse(map.containsKey(id1));
		Assertions.assertTrue(map.containsKey(id2));

		Assertions.assertTrue(set.contains(entries.get(id2)));
		Assertions.assertFalse(set.contains(entries.get(id1)));

		// test forEach
		Set<Map.Entry<Id<Person>, String>> es = new HashSet<>();
		set.forEach(k -> es.add(k));

		Assertions.assertEquals(4, es.size());
		Assertions.assertTrue(es.contains(entries.get(id2)));
		Assertions.assertTrue(es.contains(entries.get(id4)));
		Assertions.assertTrue(es.contains(entries.get(id5)));
		Assertions.assertTrue(es.contains(entries.get(id6)));
	}

	@Test
	void testIterator_iterate() {
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
		Assertions.assertNotNull(iter);

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("one", iter.next());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("two", iter.next());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("four", iter.next());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("five", iter.next());

		Assertions.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assertions.fail("Expected exception, got none.");
		} catch (NoSuchElementException ignore) {
		}

		Assertions.assertFalse(iter.hasNext());
	}

	@Test
	void testIterator_remove() {
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
		Assertions.assertNotNull(iter);

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("one", iter.next());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("two", iter.next());

		iter.remove();

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("four", iter.next());

		Assertions.assertEquals(3, map.size());
		Assertions.assertTrue(map.containsValue("one"));
		Assertions.assertFalse(map.containsValue("two"));
		Assertions.assertTrue(map.containsValue("four"));
		Assertions.assertTrue(map.containsValue("five"));
	}

	@Test
	void testKeySetToArray() {
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
		Assertions.assertEquals(id1, array[0]);
		Assertions.assertEquals(id2, array[1]);
		Assertions.assertEquals(id4, array[2]);
		Assertions.assertEquals(id5, array[3]);

	}

	@Test
	void testEqualsAndHashCode() {
		Id<Person> id1 = Id.create(1, Person.class);
		Id<Person> id2 = Id.create(2, Person.class);
		Id<Person> id3 = Id.create(3, Person.class);
		Id<Person> id4 = Id.create(4, Person.class);
		Id<Person> id5 = Id.create(5, Person.class);
		Id<Person> id6 = Id.create(6, Person.class);

		IdMap<Person, String> mapA = new IdMap<>(Person.class, 4);
		IdMap<Person, String> mapB = new IdMap<>(Person.class, 4);
		IdMap<Person, String> mapC = new IdMap<>(Person.class, 10);

		Assertions.assertEquals(mapA, mapA);
		Assertions.assertEquals(mapA, mapB);
		Assertions.assertEquals(mapA, mapC);
		Assertions.assertEquals(mapA.hashCode(), mapB.hashCode());
		Assertions.assertEquals(mapA.hashCode(), mapC.hashCode());

		mapA.put(id1, "one");
		mapB.put(id1, "one");

		Assertions.assertEquals(mapA, mapA);
		Assertions.assertEquals(mapA, mapB);
		Assertions.assertNotEquals(mapA, mapC);
		Assertions.assertEquals(mapA.hashCode(), mapB.hashCode());
		Assertions.assertNotEquals(mapA.hashCode(), mapC.hashCode());

		mapC.put(id1, "one");

		Assertions.assertEquals(mapA, mapC);
		Assertions.assertEquals(mapA.hashCode(), mapC.hashCode());

		mapA.put(id2, "two");
		mapB.put(id3, "three");

		Assertions.assertNotEquals(mapA, mapB);
		Assertions.assertNotEquals(mapA.hashCode(), mapB.hashCode());

		mapA.put(id3, "three");
		mapB.put(id2, "two");

		Assertions.assertEquals(mapA, mapB);
		Assertions.assertEquals(mapA.hashCode(), mapB.hashCode());

		mapA.put(id4, "four");
		mapA.put(id5, "five");
		mapA.put(id6, "six");
		mapA.remove(id4, "four");
		mapA.remove(id5, "five");
		mapA.remove(id6, "six");

		Assertions.assertEquals(mapA, mapB);
		Assertions.assertEquals(mapA.hashCode(), mapB.hashCode());

		Assertions.assertEquals(mapA.entrySet(), mapB.entrySet());
		Assertions.assertEquals(mapA.entrySet().hashCode(), mapB.entrySet().hashCode());
		Assertions.assertEquals(mapA.keySet(), mapB.keySet());
		Assertions.assertEquals(mapA.keySet().hashCode(), mapB.keySet().hashCode());

		mapA.put(id4, "four");
		mapB.put(id4, "DifferentFour");

		Assertions.assertNotEquals(mapA, mapB);
		Assertions.assertNotEquals(mapA.hashCode(), mapB.hashCode());

		Assertions.assertNotEquals(mapA.entrySet(), mapB.entrySet());
		Assertions.assertNotEquals(mapA.entrySet().hashCode(), mapB.entrySet().hashCode());
		Assertions.assertEquals(mapA.keySet(), mapB.keySet());
		Assertions.assertEquals(mapA.keySet().hashCode(), mapB.keySet().hashCode());

		HashMap<Id<Person>, String> hMapA = new HashMap<Id<Person>, String>(mapA);

		// The commented out tests will fail right now because the hashCode of IdImpl is based on the id, not the index
		Assertions.assertEquals(hMapA, mapA);
//		Assert.assertEquals(hMapA.hashCode(), mapA.hashCode());
		Assertions.assertEquals(hMapA.entrySet(), mapA.entrySet());
//		Assert.assertEquals(hMapA.entrySet().hashCode(), mapA.entrySet().hashCode());
		Assertions.assertEquals(hMapA.keySet(), mapA.keySet());
//		Assert.assertEquals(hMapA.keySet().hashCode(), mapA.keySet().hashCode());
		Assertions.assertNotEquals(hMapA, mapB);
		Assertions.assertNotEquals(hMapA.hashCode(), mapB.hashCode());
		Assertions.assertNotEquals(hMapA.entrySet(), mapB.entrySet());
		Assertions.assertNotEquals(hMapA.entrySet().hashCode(), mapB.entrySet().hashCode());
		Assertions.assertEquals(hMapA.keySet(), mapB.keySet());
//		Assert.assertEquals(hMapA.keySet().hashCode(), mapB.keySet().hashCode());

		// Best way I could think of to explicitly test the equals() of Entry (i.e. not inside the EntrySet)
		Iterator<Entry<Id<Person>, String>> iter = mapA.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<Id<Person>, String> e = iter.next();
			if (e.getKey() != id4) {
				Assertions.assertTrue(mapB.entrySet().contains(e));
			} else {
				Assertions.assertFalse(mapB.entrySet().contains(e));
			}
			Assertions.assertTrue(hMapA.entrySet().contains(e));
		}
	}

}
