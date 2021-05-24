package org.matsim.core.utils.collections;

import org.junit.Assert;
import org.junit.Test;

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
public class ArrayMapTest {

	@Test
	public void testPutGetRemoveSize() {
		ArrayMap<String, String> map = new ArrayMap<>();

		Assert.assertEquals(0, map.size());
		Assert.assertTrue(map.isEmpty());

		Assert.assertNull(map.put("1", "one"));

		Assert.assertEquals(1, map.size());
		Assert.assertFalse(map.isEmpty());

		Assert.assertNull(map.put("2", "two"));

		Assert.assertEquals(2, map.size());
		Assert.assertFalse(map.isEmpty());

		Assert.assertEquals("one", map.put("1", "also-one"));
		Assert.assertEquals(2, map.size());

		Assert.assertNull(map.put("3", "three"));
		Assert.assertEquals(3, map.size());

		Assert.assertNull(map.put("4", null));
		Assert.assertEquals(4, map.size());

		Assert.assertEquals("also-one", map.get("1"));
		Assert.assertEquals("two", map.get("2"));
		Assert.assertEquals("three", map.get("3"));
		Assert.assertNull(map.get("4"));

		Assert.assertEquals("two", map.remove("2"));
		Assert.assertEquals(3, map.size());
		Assert.assertNull(map.get("2"));
	}

	@Test
	public void testValuesIterable() {
		ArrayMap<String, String> map = new ArrayMap<>();

		map.put("1", "one");
		map.put("2", "two");
		map.put("4", "four");
		map.put("5", "five");

		int i = 0;
		for (String data : map.values()) {
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
		ArrayMap<String, String> map = new ArrayMap<>();

		map.put("1", "one");
		map.put("2", "two");
		map.put("4", "four");
		map.put("5", "five");

		List<Tuple<String, String>> data = new ArrayList<>();

		map.forEach((k, v) -> data.add(new Tuple<>(k, v)));

		Assert.assertEquals("1", data.get(0).getFirst());
		Assert.assertEquals("one", data.get(0).getSecond());

		Assert.assertEquals("2", data.get(1).getFirst());
		Assert.assertEquals("two", data.get(1).getSecond());

		Assert.assertEquals("4", data.get(2).getFirst());
		Assert.assertEquals("four", data.get(2).getSecond());

		Assert.assertEquals("5", data.get(3).getFirst());
		Assert.assertEquals("five", data.get(3).getSecond());
	}

	@Test
	public void testContainsKey() {
		ArrayMap<String, String> map = new ArrayMap<>();

		map.put("1", "one");
		map.put("2", "two");
		map.put("4", "four");
		map.put("5", "five");

		Assert.assertTrue(map.containsKey("1"));
		Assert.assertTrue(map.containsKey("2"));
		Assert.assertFalse(map.containsKey("3"));
		Assert.assertTrue(map.containsKey("4"));
		Assert.assertTrue(map.containsKey("5"));
		Assert.assertFalse(map.containsKey("6"));

		Assert.assertTrue(map.containsKey("1"));
		Assert.assertTrue(map.containsKey("2"));
		Assert.assertFalse(map.containsKey("3"));
		Assert.assertTrue(map.containsKey("4"));
		Assert.assertTrue(map.containsKey("5"));
		Assert.assertFalse(map.containsKey("6"));
	}

	@Test
	public void testContainsValue() {
		ArrayMap<String, String> map = new ArrayMap<>();

		map.put("1", "one");
		map.put("2", "two");
		map.put("4", "four");
		map.put("5", "five");

		Assert.assertTrue(map.containsValue("one"));
		Assert.assertTrue(map.containsValue("two"));
		Assert.assertFalse(map.containsValue("three"));
		Assert.assertTrue(map.containsValue("four"));
		Assert.assertTrue(map.containsValue("five"));
		Assert.assertFalse(map.containsValue("six"));
	}

	@Test
	public void testPutAll_ArrayMap() {
		ArrayMap<String, String> map = new ArrayMap<>();

		ArrayMap<String, String> map2 = new ArrayMap<>();
		map2.put("1", "one");
		map2.put("2", "two");
		map2.put("4", "four");
		map2.put("5", "five");

		map.putAll(map2);

		Assert.assertEquals(4, map.size());

		Assert.assertEquals("one", map.get("1"));
		Assert.assertEquals("two", map.get("2"));
		Assert.assertNull(map.get("3"));
		Assert.assertEquals("four", map.get("4"));
		Assert.assertEquals("five", map.get("5"));
	}

	@Test
	public void testPutAll_GenericMap() {
		ArrayMap<String, String> map = new ArrayMap<>();

		Map<String, String> map2 = new HashMap<>();
		map2.put("1", "one");
		map2.put("2", "two");
		map2.put("4", "four");
		map2.put("5", "five");

		map.putAll(map2);

		Assert.assertEquals(4, map.size());

		Assert.assertEquals("one", map.get("1"));
		Assert.assertEquals("two", map.get("2"));
		Assert.assertNull(map.get("3"));
		Assert.assertEquals("four", map.get("4"));
		Assert.assertEquals("five", map.get("5"));
	}

	@Test
	public void testClear() {
		ArrayMap<String, String> map = new ArrayMap<>();

		map.put("1", "one");
		map.put("2", "two");
		map.put("4", "four");
		map.put("5", "five");

		Assert.assertEquals(4, map.size());

		map.clear();

		Assert.assertEquals(0, map.size());

		Assert.assertNull(map.get("1"));
		Assert.assertNull(map.get("2"));
		Assert.assertNull(map.get("3"));

		Assert.assertFalse(map.containsKey("1"));
	}

	@Test
	public void testValues() {
		ArrayMap<String, String> map = new ArrayMap<>();

		map.put("1", "one");
		map.put("2", "two");
		map.put("4", "four");
		map.put("5", "five");

		Collection<String> coll = map.values();
		Assert.assertEquals(4, coll.size());

		map.put("6", "six");
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
		String key1 = "1";
		String key2 = "2";
		String key3 = "3";
		String key4 = "4";
		String key5 = "5";
		String key6 = "6";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Set<String> set = map.keySet();
		Assert.assertEquals(4, set.size());

		map.put(key6, "six");
		Assert.assertEquals(5, set.size());

		Assert.assertTrue(set.remove(key1));
		Assert.assertFalse(set.remove(key3));

		Assert.assertFalse(map.containsKey(key1));
		Assert.assertTrue(map.containsKey(key2));

		Assert.assertTrue(set.contains(key2));
		Assert.assertFalse(set.contains(key1));

		Set<String> keys = new HashSet<>();
		set.forEach(k -> keys.add(k));

		Assert.assertEquals(4, keys.size());
		Assert.assertTrue(keys.contains(key2));
		Assert.assertTrue(keys.contains(key4));
		Assert.assertTrue(keys.contains(key5));
		Assert.assertTrue(keys.contains(key6));
	}

	@Test
	public void testEntrySet() {
		String key1 = "1";
		String key2 = "2";
		String key4 = "4";
		String key5 = "5";
		String key6 = "6";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Set<Map.Entry<String, String>> set = map.entrySet();
		Assert.assertEquals(4, set.size());

		map.put(key6, "six");
		Assert.assertEquals(5, set.size());

		Map<String, Map.Entry<String, String>> entries = new HashMap<>();

		for (Map.Entry<String, String> e : set) {
			entries.put(e.getKey(), e);
		}

		Assert.assertEquals(key1, entries.get(key1).getKey());
		Assert.assertEquals("one", entries.get(key1).getValue());
		Assert.assertEquals("two", entries.get(key2).getValue());
		Assert.assertEquals("four", entries.get(key4).getValue());
		Assert.assertEquals("five", entries.get(key5).getValue());

		Assert.assertTrue(set.remove(entries.get(key1)));
		//noinspection SuspiciousMethodCalls
		Assert.assertFalse(set.remove(new Object()));

		Assert.assertFalse(map.containsKey(key1));
		Assert.assertTrue(map.containsKey(key2));

		Assert.assertTrue(set.contains(entries.get(key2)));
		Assert.assertFalse(set.contains(entries.get(key1)));

		// test forEach
		Set<Map.Entry<String, String>> es = new HashSet<>();
		set.forEach(k -> es.add(k));

		Assert.assertEquals(4, es.size());
		Assert.assertTrue(es.contains(entries.get(key2)));
		Assert.assertTrue(es.contains(entries.get(key4)));
		Assert.assertTrue(es.contains(entries.get(key5)));
		Assert.assertTrue(es.contains(entries.get(key6)));
	}

	@Test
	public void testValuesIterator_iterate() {
		String key1 = "1";
		String key2 = "2";
		String key4 = "4";
		String key5 = "5";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<String> iter = map.values().iterator();
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
	public void testValuesIterator_remove() {
		String key1 = "1";
		String key2 = "2";
		String key4 = "4";
		String key5 = "5";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<String> iter = map.values().iterator();
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
	public void testKeySetIterator_iterate() {
		String key1 = "1";
		String key2 = "2";
		String key4 = "4";
		String key5 = "5";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<String> iter = map.keySet().iterator();
		Assert.assertNotNull(iter);

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("1", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("2", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("4", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("5", iter.next());

		Assert.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assert.fail("Expected exception, got none.");
		} catch (NoSuchElementException ignore) {
		}

		Assert.assertFalse(iter.hasNext());
	}

	@Test
	public void testKeySetIterator_remove() {
		String key1 = "1";
		String key2 = "2";
		String key4 = "4";
		String key5 = "5";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Iterator<String> iter = map.keySet().iterator();
		Assert.assertNotNull(iter);

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("1", iter.next());

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("2", iter.next());

		iter.remove();

		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals("4", iter.next());

		Assert.assertEquals(3, map.size());
		Assert.assertTrue(map.containsKey("1"));
		Assert.assertFalse(map.containsKey("2"));
		Assert.assertTrue(map.containsKey("4"));
		Assert.assertTrue(map.containsKey("5"));
	}

	@Test
	public void testKeySetToArray() {
		String key1 = "1";
		String key2 = "2";
		String key4 = "4";
		String key5 = "5";

		ArrayMap<String, String> map = new ArrayMap<>();

		map.put(key1, "one");
		map.put(key2, "two");
		map.put(key4, "four");
		map.put(key5, "five");

		Object[] array = map.keySet().toArray();
		Assert.assertEquals(key1, array[0]);
		Assert.assertEquals(key2, array[1]);
		Assert.assertEquals(key4, array[2]);
		Assert.assertEquals(key5, array[3]);

	}

	@Test
	public void testCopyConstructor() {
		Map<String, String> map0 = new HashMap<>();
		map0.put("1", "one");
		map0.put("2", "two");
		map0.put("3", "three");
		map0.put("4", "four");

		ArrayMap<String, String> map = new ArrayMap<>(map0);

		Assert.assertEquals(4, map.size());
		Assert.assertFalse(map.isEmpty());

		Assert.assertTrue(map.containsKey("2"));
		Assert.assertTrue(map.containsKey("1"));
		Assert.assertTrue(map.containsKey("3"));
		Assert.assertTrue(map.containsKey("4"));
		Assert.assertFalse(map.containsKey("5"));

		Assert.assertTrue(map.containsValue("one"));
		Assert.assertTrue(map.containsValue("two"));
		Assert.assertTrue(map.containsValue("three"));
		Assert.assertTrue(map.containsValue("four"));
		Assert.assertFalse(map.containsValue("five"));

		Assert.assertEquals("one", map.get("1"));
		Assert.assertEquals("two", map.get("2"));
		Assert.assertEquals("three", map.get("3"));
		Assert.assertEquals("four", map.get("4"));
		Assert.assertNull(map.get("5"));
	}

}