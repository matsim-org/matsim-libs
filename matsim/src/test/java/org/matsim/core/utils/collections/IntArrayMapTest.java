package org.matsim.core.utils.collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * @author mrieser / Simunto GmbH
 */
public class IntArrayMapTest {

	@Test
	void testPutGetRemoveSize() {
		IntArrayMap<String> map = new IntArrayMap<>();

		Assertions.assertEquals(0, map.size());
		Assertions.assertTrue(map.isEmpty());

		Assertions.assertNull(map.put(1, "one"));

		Assertions.assertEquals(1, map.size());
		Assertions.assertFalse(map.isEmpty());

		Assertions.assertNull(map.put(2, "two"));

		Assertions.assertEquals(2, map.size());
		Assertions.assertFalse(map.isEmpty());

		Assertions.assertEquals("one", map.put(1, "also-one"));
		Assertions.assertEquals(2, map.size());

		Assertions.assertNull(map.put(3, "three"));
		Assertions.assertEquals(3, map.size());

		Assertions.assertNull(map.put(4, null));
		Assertions.assertEquals(4, map.size());

		Assertions.assertEquals("also-one", map.get(1));
		Assertions.assertEquals("two", map.get(2));
		Assertions.assertEquals("three", map.get(3));
		Assertions.assertNull(map.get(4));

		Assertions.assertEquals("two", map.remove(2));
		Assertions.assertEquals(3, map.size());
		Assertions.assertNull(map.get(2));
	}

	@Test
	void testValuesIterable() {
		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(1, "one");
		map.put(2, "two");
		map.put(4, "four");
		map.put(5, "five");

		int i = 0;
		for (String data : map.values()) {
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
		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(1, "one");
		map.put(2, "two");
		map.put(4, "four");
		map.put(5, "five");

		List<Tuple<Integer, String>> data = new ArrayList<>();

		map.forEach((k, v) -> data.add(new Tuple<>(k, v)));

		Assertions.assertEquals(1, data.get(0).getFirst().intValue());
		Assertions.assertEquals("one", data.get(0).getSecond());

		Assertions.assertEquals(2, data.get(1).getFirst().intValue());
		Assertions.assertEquals("two", data.get(1).getSecond());

		Assertions.assertEquals(4, data.get(2).getFirst().intValue());
		Assertions.assertEquals("four", data.get(2).getSecond());

		Assertions.assertEquals(5, data.get(3).getFirst().intValue());
		Assertions.assertEquals("five", data.get(3).getSecond());
	}

	@Test
	void testContainsKey() {
		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(1, "one");
		map.put(2, "two");
		map.put(4, "four");
		map.put(5, "five");

		Assertions.assertTrue(map.containsKey(1));
		Assertions.assertTrue(map.containsKey(2));
		Assertions.assertFalse(map.containsKey(3));
		Assertions.assertTrue(map.containsKey(4));
		Assertions.assertTrue(map.containsKey(5));
		Assertions.assertFalse(map.containsKey(6));

		Assertions.assertTrue(map.containsKey(1));
		Assertions.assertTrue(map.containsKey(2));
		Assertions.assertFalse(map.containsKey(3));
		Assertions.assertTrue(map.containsKey(4));
		Assertions.assertTrue(map.containsKey(5));
		Assertions.assertFalse(map.containsKey(6));
	}

	@Test
	void testContainsValue() {
		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(1, "one");
		map.put(2, "two");
		map.put(4, "four");
		map.put(5, "five");

		Assertions.assertTrue(map.containsValue("one"));
		Assertions.assertTrue(map.containsValue("two"));
		Assertions.assertFalse(map.containsValue("three"));
		Assertions.assertTrue(map.containsValue("four"));
		Assertions.assertTrue(map.containsValue("five"));
		Assertions.assertFalse(map.containsValue("six"));
	}

	@Test
	void testPutAll_ArrayMap() {
		IntArrayMap<String> map = new IntArrayMap<>();

		IntArrayMap<String> map2 = new IntArrayMap<>();
		map2.put(1, "one");
		map2.put(2, "two");
		map2.put(4, "four");
		map2.put(5, "five");

		map.putAll(map2);

		Assertions.assertEquals(4, map.size());

		Assertions.assertEquals("one", map.get(1));
		Assertions.assertEquals("two", map.get(2));
		Assertions.assertNull(map.get(3));
		Assertions.assertEquals("four", map.get(4));
		Assertions.assertEquals("five", map.get(5));
	}

	@Test
	void testPutAll_GenericMap() {
		IntArrayMap<String> map = new IntArrayMap<>();

		Map<Integer, String> map2 = new HashMap<>();
		map2.put(1, "one");
		map2.put(2, "two");
		map2.put(4, "four");
		map2.put(5, "five");

		map.putAll(map2);

		Assertions.assertEquals(4, map.size());

		Assertions.assertEquals("one", map.get(1));
		Assertions.assertEquals("two", map.get(2));
		Assertions.assertNull(map.get(3));
		Assertions.assertEquals("four", map.get(4));
		Assertions.assertEquals("five", map.get(5));
	}

	@Test
	void testClear() {
		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(1, "one");
		map.put(2, "two");
		map.put(4, "four");
		map.put(5, "five");

		Assertions.assertEquals(4, map.size());

		map.clear();

		Assertions.assertEquals(0, map.size());

		Assertions.assertNull(map.get(1));
		Assertions.assertNull(map.get(2));
		Assertions.assertNull(map.get(3));

		Assertions.assertFalse(map.containsKey(1));
	}

	@Test
	void testValues() {
		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(1, "one");
		map.put(2, "two");
		map.put(4, "four");
		map.put(5, "five");

		Collection<String> coll = map.values();
		Assertions.assertEquals(4, coll.size());

		map.put(6, "six");
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
		int key1 = 1;
		int key2 = 2;
		int key3 = 3;
		int key4 = 4;
		int key5 = 5;
		int key6 = 6;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Set<Integer> set = map.keySet();
		Assertions.assertEquals(4, set.size());

		map.put(key6, "six");
		Assertions.assertEquals(5, set.size());

		Assertions.assertTrue(set.remove(key1));
		Assertions.assertFalse(set.remove(key3));

		Assertions.assertFalse(map.containsKey(key1));
		Assertions.assertTrue(map.containsKey(key2));

		Assertions.assertTrue(set.contains(key2));
		Assertions.assertFalse(set.contains(key1));

		Set<Integer> keys = new HashSet<>();
		set.forEach(k -> keys.add(k));

		Assertions.assertEquals(4, keys.size());
		Assertions.assertTrue(keys.contains(key2));
		Assertions.assertTrue(keys.contains(key4));
		Assertions.assertTrue(keys.contains(key5));
		Assertions.assertTrue(keys.contains(key6));
	}

	@Test
	void testEntrySet() {
		int key1 = 1;
		int key2 = 2;
		int key4 = 4;
		int key5 = 5;
		int key6 = 6;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Set<Map.Entry<Integer, String>> set = map.entrySet();
		Assertions.assertEquals(4, set.size());

		map.put(key6, "six");
		Assertions.assertEquals(5, set.size());

		Map<Integer, Map.Entry<Integer, String>> entries = new HashMap<>();

		for (Map.Entry<Integer, String> e : set) {
			entries.put(e.getKey(), e);
		}

		Assertions.assertEquals(key1, entries.get(key1).getKey().intValue());
		Assertions.assertEquals("one", entries.get(key1).getValue());
		Assertions.assertEquals("two", entries.get(key2).getValue());
		Assertions.assertEquals("four", entries.get(key4).getValue());
		Assertions.assertEquals("five", entries.get(key5).getValue());

		Assertions.assertTrue(set.remove(entries.get(key1)));
		//noinspection SuspiciousMethodCalls
		Assertions.assertFalse(set.remove(new Object()));

		Assertions.assertFalse(map.containsKey(key1));
		Assertions.assertTrue(map.containsKey(key2));

		Assertions.assertTrue(set.contains(entries.get(key2)));
		Assertions.assertFalse(set.contains(entries.get(key1)));

		// test forEach
		Set<Map.Entry<Integer, String>> es = new HashSet<>();
		set.forEach(k -> es.add(k));

		Assertions.assertEquals(4, es.size());
		Assertions.assertTrue(es.contains(entries.get(key2)));
		Assertions.assertTrue(es.contains(entries.get(key4)));
		Assertions.assertTrue(es.contains(entries.get(key5)));
		Assertions.assertTrue(es.contains(entries.get(key6)));
	}

	@Test
	void testValuesIterator_iterate() {
		int key1 = 1;
		int key2 = 2;
		int key4 = 4;
		int key5 = 5;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<String> iter = map.values().iterator();
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
	void testValuesIterator_remove() {
		int key1 = 1;
		int key2 = 2;
		int key4 = 4;
		int key5 = 5;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<String> iter = map.values().iterator();
		Assertions.assertNotNull(iter);

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("one", iter.next());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("two", iter.next());

		iter.remove();

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals("five", iter.next());
		Assertions.assertEquals("four", iter.next());

		Assertions.assertEquals(3, map.size());
		Assertions.assertTrue(map.containsValue("one"));
		Assertions.assertFalse(map.containsValue("two"));
		Assertions.assertTrue(map.containsValue("four"));
		Assertions.assertTrue(map.containsValue("five"));
	}

	@Test
	void testKeySetIterator_iterate() {
		int key1 = 1;
		int key2 = 2;
		int key4 = 4;
		int key5 = 5;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<Integer> iter = map.keySet().iterator();
		Assertions.assertNotNull(iter);

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(1, iter.next().intValue());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(2, iter.next().intValue());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(4, iter.next().intValue());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(5, iter.next().intValue());

		Assertions.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assertions.fail("Expected exception, got none.");
		} catch (NoSuchElementException ignore) {
		}

		Assertions.assertFalse(iter.hasNext());
	}

	@Test
	void testKeySetIterator_remove() {
		int key1 = 1;
		int key2 = 2;
		int key4 = 4;
		int key5 = 5;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<Integer> iter = map.keySet().iterator();
		Assertions.assertNotNull(iter);

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(1, iter.next().intValue());

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(2, iter.next().intValue());

		iter.remove();

		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(5, iter.next().intValue());
		Assertions.assertEquals(4, iter.next().intValue());

		Assertions.assertEquals(3, map.size());
		Assertions.assertTrue(map.containsKey(1));
		Assertions.assertFalse(map.containsKey(2));
		Assertions.assertTrue(map.containsKey(4));
		Assertions.assertTrue(map.containsKey(5));
	}

	@Test
	void testKeySetToArray() {
		int key1 = 1;
		int key2 = 2;
		int key4 = 4;
		int key5 = 5;

		IntArrayMap<String> map = new IntArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Object[] array = map.keySet().toArray();
		Assertions.assertEquals(key1, array[0]);
		Assertions.assertEquals(key2, array[1]);
		Assertions.assertEquals(key4, array[2]);
		Assertions.assertEquals(key5, array[3]);
	}

}