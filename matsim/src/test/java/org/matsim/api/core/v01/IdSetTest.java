package org.matsim.api.core.v01;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.population.Person;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author mrieser / Simunto GmbH
 */
public class IdSetTest {

	@Test
	public void testAddContainsRemoveSize() {
		IdSet<Person> set = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3 = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);

		Assert.assertEquals(0, set.size());
		Assert.assertTrue(set.isEmpty());

		Assert.assertTrue(set.add(id1));
		Assert.assertEquals(1, set.size());
		Assert.assertFalse(set.isEmpty());
		Assert.assertTrue(set.contains(id1));
		Assert.assertFalse(set.contains(id2));

		Assert.assertFalse(set.add(id1));
		Assert.assertEquals(1, set.size());

		Assert.assertTrue(set.add(id3));
		Assert.assertEquals(2, set.size());
		Assert.assertTrue(set.contains(id1));
		Assert.assertFalse(set.contains(id2));
		Assert.assertTrue(set.contains(id3));

		Assert.assertFalse(set.remove(id4));
		Assert.assertEquals(2, set.size());

		Assert.assertTrue(set.remove(id1));
		Assert.assertEquals(1, set.size());
		Assert.assertFalse(set.remove(id1));
		Assert.assertEquals(1, set.size());
	}

	@Test
	public void testIterator() {
		IdSet<Person> set = new IdSet<>(Person.class);

		Id<Person> id1 = Id.create("1", Person.class);
		Id<Person> id2 = Id.create("2", Person.class);
		Id<Person> id3_unused = Id.create("3", Person.class);
		Id<Person> id4 = Id.create("4", Person.class);

		set.add(id2);
		set.add(id4);
		set.add(id1);

		Iterator<Id<Person>> iter = set.iterator();
		Assert.assertNotNull(iter);
		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals(id1, iter.next());
		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals(id2, iter.next());
		Assert.assertTrue(iter.hasNext());
		Assert.assertEquals(id4, iter.next());
		Assert.assertFalse(iter.hasNext());
		try {
			iter.next();
			Assert.fail("expected NoSuchElementException, got none.");
		} catch (NoSuchElementException ignore) {
		}
	}

	@Test
	public void testClear() {
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

		Assert.assertEquals(3, set.size());

		set.clear();

		Assert.assertEquals(0, set.size());
		Assert.assertTrue(set.isEmpty());

		Assert.assertFalse(set.iterator().hasNext());
	}

	@Test
	public void testAddAll() {
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

		Assert.assertTrue(set1.addAll(set2));

		Assert.assertTrue(set1.contains(id1));
		Assert.assertTrue(set1.contains(id3));
		Assert.assertTrue(set1.contains(id4));
		Assert.assertTrue(set1.contains(id5));
		Assert.assertTrue(set1.contains(id6));
		Assert.assertEquals(5, set1.size());

		Assert.assertFalse(set1.addAll(set2));
	}

	@Test
	public void testRemoveAll() {
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

		Assert.assertTrue(set1.removeAll(set2));

		Assert.assertTrue(set1.contains(id1));
		Assert.assertFalse(set1.contains(id2));
		Assert.assertTrue(set1.contains(id3));
		Assert.assertFalse(set1.contains(id4));
		Assert.assertFalse(set1.contains(id5));
		Assert.assertFalse(set1.contains(id6));
		Assert.assertEquals(2, set1.size());
	}

	@Test
	public void testRetainAll() {
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

		Assert.assertTrue(set1.retainAll(set2));

		Assert.assertFalse(set1.contains(id1));
		Assert.assertFalse(set1.contains(id2));
		Assert.assertFalse(set1.contains(id3));
		Assert.assertTrue(set1.contains(id4));
		Assert.assertFalse(set1.contains(id5));
		Assert.assertFalse(set1.contains(id6));

		Assert.assertEquals(1, set1.size());
	}

	@Test
	public void testContainsAll() {
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

		Assert.assertFalse(set1.containsAll(set2));

		set1.add(id5);
		set1.add(id6);

		Assert.assertTrue(set1.containsAll(set2));
	}

	@Test
	public void testToArray() {
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
		Assert.assertEquals(3, array1.length);
		Assert.assertEquals(id1, array1[0]);
		Assert.assertEquals(id3, array1[1]);
		Assert.assertEquals(id4, array1[2]);

		Id<Person>[] array2 = set.toArray((Id<Person>[]) new Id[3]);
		Assert.assertEquals(3, array2.length);
		Assert.assertEquals(id1, array2[0]);
		Assert.assertEquals(id3, array2[1]);
		Assert.assertEquals(id4, array2[2]);

		Id<Person>[] tmpArray = (Id<Person>[]) new Id[5]; // too big
		tmpArray[0] = id5;
		tmpArray[1] = id4;
		tmpArray[2] = id3;
		tmpArray[3] = id2;
		tmpArray[4] = id1;
		Id<Person>[] array3 = set.toArray(tmpArray);
		Assert.assertEquals(5, array3.length);
		Assert.assertEquals(id1, array3[0]);
		Assert.assertEquals(id3, array3[1]);
		Assert.assertEquals(id4, array3[2]);
		Assert.assertNull(array3[3]);
		Assert.assertNull(array3[4]);

		Id<Person>[] array4 = set.toArray((Id<Person>[]) new Id[1]); // too small
		Assert.assertEquals(3, array4.length);
		Assert.assertEquals(id1, array4[0]);
		Assert.assertEquals(id3, array4[1]);
		Assert.assertEquals(id4, array4[2]);
	}

}