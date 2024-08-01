package org.matsim.api.core.v01;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdSetTest {

	@Test
	void testAddContainsRemoveSize() {
		IdSet<Person> set = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);

		Assertions.assertEquals(0, set.size());
		Assertions.assertTrue(set.isEmpty());

		Assertions.assertTrue(set.add(id1));
		Assertions.assertEquals(1, set.size());
		Assertions.assertFalse(set.isEmpty());
		Assertions.assertTrue(set.contains(id1));
		Assertions.assertFalse(set.contains(id2));
		Assertions.assertTrue(set.contains(id1.index()));
		Assertions.assertFalse(set.contains(id2.index()));

		Assertions.assertFalse(set.add(id1));
		Assertions.assertEquals(1, set.size());

		Assertions.assertTrue(set.add(id3));
		Assertions.assertEquals(2, set.size());
		Assertions.assertTrue(set.contains(id1));
		Assertions.assertFalse(set.contains(id2));
		Assertions.assertTrue(set.contains(id3));
		Assertions.assertTrue(set.contains(id1.index()));
		Assertions.assertFalse(set.contains(id2.index()));
		Assertions.assertTrue(set.contains(id3.index()));

		Assertions.assertFalse(set.remove(id4));
		Assertions.assertEquals(2, set.size());

		Assertions.assertTrue(set.remove(id1));
		Assertions.assertEquals(1, set.size());
		Assertions.assertFalse(set.remove(id1));
		Assertions.assertEquals(1, set.size());
	}

	@Test
	void testIterator() {
		IdSet<Person> set = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3_unused = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);

		set.add(id2);
		set.add(id4);
		set.add(id1);

		Iterator<Id<Person>> iter = set.iterator();
		Assertions.assertNotNull(iter);
		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(id1, iter.next());
		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(id2, iter.next());
		Assertions.assertTrue(iter.hasNext());
		Assertions.assertEquals(id4, iter.next());
		Assertions.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assertions.fail("expected NoSuchElementException, got none.");
		} catch (NoSuchElementException ignore) {
		}
	}

	@Test
	void testClear() {
		IdSet<Person> set = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		set.add(id3);
		set.add(id1);
		set.add(id4);

		Assertions.assertEquals(3, set.size());

		set.clear();

		Assertions.assertEquals(0, set.size());
		Assertions.assertTrue(set.isEmpty());

		Assertions.assertFalse(set.iterator().hasNext());
	}

	@Test
	void testAddAll() {
		IdSet<Person> set1 = new IdSet<>(Person.class);
		IdSet<Person> set2 = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		set1.add(id3);
		set1.add(id1);
		set1.add(id4);

		set2.add(id6);
		set2.add(id4);
		set2.add(id5);

		Assertions.assertTrue(set1.addAll(set2));

		Assertions.assertTrue(set1.contains(id1));
		Assertions.assertTrue(set1.contains(id3));
		Assertions.assertTrue(set1.contains(id4));
		Assertions.assertTrue(set1.contains(id5));
		Assertions.assertTrue(set1.contains(id6));
		Assertions.assertEquals(5, set1.size());

		Assertions.assertFalse(set1.addAll(set2));
	}

	@Test
	void testRemoveAll() {
		IdSet<Person> set1 = new IdSet<>(Person.class);
		IdSet<Person> set2 = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		set1.add(id3);
		set1.add(id1);
		set1.add(id4);

		set2.add(id6);
		set2.add(id4);
		set2.add(id5);

		Assertions.assertTrue(set1.removeAll(set2));

		Assertions.assertTrue(set1.contains(id1));
		Assertions.assertFalse(set1.contains(id2));
		Assertions.assertTrue(set1.contains(id3));
		Assertions.assertFalse(set1.contains(id4));
		Assertions.assertFalse(set1.contains(id5));
		Assertions.assertFalse(set1.contains(id6));
		Assertions.assertEquals(2, set1.size());
	}

	@Test
	void testRetainAll() {
		IdSet<Person> set1 = new IdSet<>(Person.class);
		IdSet<Person> set2 = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		set1.add(id3);
		set1.add(id1);
		set1.add(id4);

		set2.add(id6);
		set2.add(id4);
		set2.add(id5);

		Assertions.assertTrue(set1.retainAll(set2));

		Assertions.assertFalse(set1.contains(id1));
		Assertions.assertFalse(set1.contains(id2));
		Assertions.assertFalse(set1.contains(id3));
		Assertions.assertTrue(set1.contains(id4));
		Assertions.assertFalse(set1.contains(id5));
		Assertions.assertFalse(set1.contains(id6));

		Assertions.assertEquals(1, set1.size());
	}

	@Test
	void testContainsAll() {
		IdSet<Person> set1 = new IdSet<>(Person.class);
		IdSet<Person> set2 = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		set1.add(id3);
		set1.add(id1);
		set1.add(id4);

		set2.add(id6);
		set2.add(id4);
		set2.add(id5);

		Assertions.assertFalse(set1.containsAll(set2));

		set1.add(id5);
		set1.add(id6);

		Assertions.assertTrue(set1.containsAll(set2));
	}

	@Test
	void testToArray() {
		IdSet<Person> set = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		set.add(id3);
		set.add(id1);
		set.add(id4);

		Id<Person>[] array1 = set.toArray();

		Assertions.assertEquals(3, array1.length);
		Id<Person> tmp = array1[0];
		for (int i = 1; i < array1.length; i++) {
			if (tmp.index() > array1[i].index()) {
				Assertions.fail();
			} else {
				tmp = array1[i];
			}
		}

		Id<Person>[] array2 = set.toArray((Id<Person>[]) new Id[3]);
		Assertions.assertEquals(3, array2.length);
		tmp = array2[0];
		for (int i = 1; i < array2.length; i++) {
			if (tmp.index() > array2[i].index()) {
				Assertions.fail();
			} else {
				tmp = array2[i];
			}
		}

		Id<Person>[] tmpArray = (Id<Person>[]) new Id[5]; // too big
		tmpArray[0] = id5;
		tmpArray[1] = id4;
		tmpArray[2] = id3;
		tmpArray[3] = id2;
		tmpArray[4] = id1;
		Id<Person>[] array3 = set.toArray(tmpArray);
		Assertions.assertEquals(5, array3.length);
		tmp = array3[0];
		for (int i = 1; i < array1.length; i++) {
			if (tmp.index() > array3[i].index()) {
				Assertions.fail();
			} else {
				tmp = array3[i];
			}
		}
		Assertions.assertNull(array3[3]);
		Assertions.assertNull(array3[4]);

		Id<Person>[] array4 = set.toArray((Id<Person>[]) new Id[1]); // too small
		Assertions.assertEquals(3, array4.length);
		tmp = array4[0];
		for (int i = 1; i < array4.length; i++) {
			if (tmp.index() > array4[i].index()) {
				Assertions.fail();
			} else {
				tmp = array4[i];
			}
		}
	}

	@Test
	void testEqualsAndHashCode() {
		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);
		Id<Person> id5 = Id.create("5", Person.class);
		Id<Person> id6 = Id.create("6", Person.class);

		IdSet<Person> setA = new IdSet<>(Person.class, 4);
		IdSet<Person> setB = new IdSet<>(Person.class, 4);
		IdSet<Link> setWrongType = new IdSet<>(Link.class, 4);

		Assertions.assertEquals(setA, setA);
		Assertions.assertEquals(setA, setB);
		Assertions.assertNotEquals(setA, setWrongType);

		setA.add(id1);

		Assertions.assertEquals(setA, setA);
		Assertions.assertNotEquals(setA, setB);
		Assertions.assertEquals(setA.hashCode(), setA.hashCode());
		Assertions.assertNotEquals(setA.hashCode(), setB.hashCode());

		setB.add(id1);

		Assertions.assertEquals(setA, setB);
		Assertions.assertEquals(setA.hashCode(), setB.hashCode());

		setA.add(id2);
		setA.add(id3);

		Assertions.assertNotEquals(setA, setB);
		Assertions.assertNotEquals(setA.hashCode(), setB.hashCode());

		setB.add(id3);
		setB.add(id2);

		Assertions.assertEquals(setA, setB);
		Assertions.assertEquals(setA.hashCode(), setB.hashCode());

		setA.add(id4);
		setA.add(id5);
		setA.add(id6);
		setA.remove(id4);
		setA.remove(id5);
		setA.remove(id6);

		Assertions.assertEquals(setA, setB);
		Assertions.assertEquals(setA.hashCode(), setB.hashCode());

		setA.add(id4);
		HashSet<Id<Person>> hSetA = new HashSet<Id<Person>>(setA);

		Assertions.assertEquals(hSetA, setA);
		Assertions.assertNotEquals(hSetA, setB);
//		Assert.assertEquals(hSetA.hashCode(), setA.hashCode()); // this does not work yet because the hashCode() of IdImpl still uses id instead of index
		Assertions.assertNotEquals(hSetA.hashCode(), setB.hashCode());
	}

}
