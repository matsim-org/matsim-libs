package org.matsim.api.core.v01;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdMapTest {

	@Test
	public void testPutGetSize() {
		IdMap<Person, String> map = new IdMap<>(Person.class, String.class, 10);

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
		IdMap<Person, String> map = new IdMap<>(Person.class, String.class, 10);

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
	public void forEach() {
		IdMap<Person, String> map = new IdMap<>(Person.class, String.class, 10);

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
}
