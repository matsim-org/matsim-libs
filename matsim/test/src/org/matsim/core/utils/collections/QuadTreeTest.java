/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import junit.framework.TestCase;

import org.jfree.util.Log;
import org.matsim.core.utils.geometry.CoordImpl;

/**
 * Test for {@link QuadTree}.
 *
 * @author mrieser
 */
public class QuadTreeTest extends TestCase {

	/**
	 * @return A simple QuadTree with 6 entries for tests.
	 */
	private QuadTree<String> getTestTree() {
		QuadTree<String> qt = new QuadTree<String>(-50.0, -50.0, +150.0, +150.0);

		qt.put(10.0, 10.0, "10.0, 10.0");
		qt.put(15.0, 15.0, "15.0, 15.0");
		qt.put(-15.0, 0.0, "-15.0, 0.0");
		qt.put(20.0, 10.0, "20.0, 10.0");
		qt.put(100.0, 0.0, "100.0, 0.0");
		qt.put(15.0, 15.0, "15.0, 15.0 B");

		return qt;
	}

	/**
	 * Test {@link QuadTree#QuadTree(double, double, double, double)}.
	 */
	public void testConstructor() {
		QuadTree<String> qt = new QuadTree<String>(-50.0, -40.0, +30.0, +20.0);
		assertEquals(-50.0, qt.getMinEasting(), 0.0);
		assertEquals(-40.0, qt.getMinNorthing(), 0.0);
		assertEquals(+30.0, qt.getMaxEasting(), 0.0);
		assertEquals(+20.0, qt.getMaxNorthing(), 0.0);
	}

	/**
	 * Test putting values into a QuadTree using {@link QuadTree#put(double, double, Object)}.
	 */
	public void testPut() {
		QuadTree<String> qt = new QuadTree<String>(-50.0, -50.0, +150.0, +150.0);
		assertEquals(0, qt.size());
		qt.put(10.0, 10.0, "10.0, 10.0");
		assertEquals(1, qt.size());
		qt.put(15.0, 15.0, "15.0, 15.0");
		assertEquals(2, qt.size());
		qt.put(-15.0, 0.0, "-15.0, 0.0");
		assertEquals(3, qt.size());
		qt.put(20.0, 10.0, "20.0, 10.0");
		assertEquals(4, qt.size());
		qt.put(100.0, 0.0, "100.0, 0.0");
		assertEquals(5, qt.size());
		qt.put(15.0, 15.0, "15.0, 15.0"); // insert an object a second time, shouldn't be added
		assertEquals(5, qt.size());
		qt.put(15.0, 15.0, "15.0, 15.0 B"); // insert a second object at an existing place
		assertEquals(6, qt.size());
	}

	/**
	 * Test getting values from a QuadTree using {@link QuadTree#get(double, double)}
	 * and {@link QuadTree#get(double, double, double)}.
	 */
	public void testGet() {
		QuadTree<String> qt = getTestTree();

		// test single get
		assertEquals("10.0, 10.0", qt.get(0.0, 0.0)); // test nearest
		assertEquals("-15.0, 0.0", qt.get(-5.0, 0.0)); // test nearest
		assertEquals("20.0, 10.0", qt.get(20.0, 10.0)); // test with exact coordinate

		// test single get on point with more than one object
		String object = qt.get(14.9, 14.9);
		assertTrue("15.0, 15.0".equals(object) || "15.0, 15.0 B".equals(object));

		// test "distance" get with exact coordinate
		Collection<String> values = qt.get(15.0, 15.0, 1.0);
		assertEquals(2, values.size());
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with exact coordinate and distance=0
		values = qt.get(15.0, 15.0, 0.0);
		assertEquals(2, values.size());
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with not exact coordinate
		values = qt.get(9.0, 9.0, 10.0);
		assertEquals(3, values.size());
		assertTrue(values.contains("10.0, 10.0"));
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with points in more than one sub-area
		values = qt.get(50.0, 0.0, 51.0);
		assertEquals(5, values.size());
		assertTrue(values.contains("100.0, 0.0"));
		assertTrue(values.contains("20.0, 10.0"));
		assertTrue(values.contains("10.0, 10.0"));
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with critical distances
		values = qt.get(90.0, 0.0, 9.0);
		assertEquals("test with distance 9.0", 0, values.size());

		values = qt.get(90.0, 0.0, 9.999);
		assertEquals("test with distance 9.999", 0, values.size());

		values = qt.get(90.0, 0.0, 10.0);
		assertEquals("test with distance 10.0", 1, values.size());

		values = qt.get(90.0, 0.0, 10.001);
		assertEquals("test with distance 10.001", 1, values.size());

		values = qt.get(90.0, 0.0, 11.0);
		assertEquals("test with distance 11.0", 1, values.size());

		// test "area"
		values.clear();
		qt.get(0.0, 0.0, 20.1, 20.1, values); // test with no object on the boundary
		assertEquals(4, values.size());

		values.clear();
		qt.get(0.0, 0.0, 20.0, 20.0, values); // test with an object exactly on the boundary
		assertEquals(3, values.size());

		values.clear();
		qt.get(0.0, 0.0, 19.9, 19.9, values); // test with no object on the boundary
		assertEquals(3, values.size());
	}

	public void testGetXY_EntryOnDividingBorder() {
		QuadTree<String> qt = new QuadTree<String>(0, 0, 40, 60);
		qt.put(10.0, 10.0, "10.0, 10.0");
		qt.put(20.0, 20.0, "20.0, 20.0"); // on vertical border
		qt.put(20.0, 30.0, "20.0, 30.0"); // exactly on center
		qt.put(30.0, 30.0, "30.0, 30.0"); // on horizontal border

		assertEquals("20.0, 20.0", qt.get(20.0, 20.0));
		assertEquals("20.0, 30.0", qt.get(20.0, 30.0));
		assertEquals("30.0, 30.0", qt.get(30.0, 30.0));
	}

	public void testGetXY_EntryOnOutsideBorder() {
		QuadTree<String> qt = new QuadTree<String>(0.0, 0.0, 40.0, 60.0);
		// the 4 corners
		qt.put(0.0, 0.0, "SW");
		qt.put(0.0, 40.0, "SE");
		qt.put(60.0, 0.0, "NW");
		qt.put(60.0, 40.0, "NE");
		// the 4 sides
		qt.put(10.0, 60.0, "N");
		qt.put(40.0, 10.0, "E");
		qt.put(10.0, 0.0, "S");
		qt.put(0.0, 10.0, "W");

		assertEquals("SW", qt.get(0.0, 0.0));
		assertEquals("SE", qt.get(0.0, 40.0));
		assertEquals("NW", qt.get(60.0, 0.0));
		assertEquals("NE", qt.get(60.0, 40.0));
		assertEquals("N", qt.get(10.0, 60.0));
		assertEquals("E", qt.get(40.0, 10.0));
		assertEquals("S", qt.get(10.0, 0.0));
		assertEquals("W", qt.get(0.0, 10.0));
	}

	public void testGetXY_EntryOutsideExtend() {
		QuadTree<String> qt = new QuadTree<String>(5.0, 5.0, 35.0, 55.0);
		// outside the 4 corners
		qt.put(0.0, 0.0, "SW");
		qt.put(40.0, 0.0, "SE");
		qt.put(0.0, 60.0, "NW");
		qt.put(40.0, 60.0, "NE");
		// outside the 4 sides
		qt.put(10.0, 60.0, "N");
		qt.put(40.0, 10.0, "E");
		qt.put(10.0, 0.0, "S");
		qt.put(0.0, 10.0, "W");

		assertEquals("SW", qt.get(0.0, 0.0));
		assertEquals("SE", qt.get(40.0, 0.0));
		assertEquals("NW", qt.get(0.0, 60.0));
		assertEquals("NE", qt.get(400.0, 60.0));
		assertEquals("N", qt.get(10.0, 60.0));
		assertEquals("E", qt.get(40.0, 10.0));
		assertEquals("S", qt.get(10.0, 0.0));
		assertEquals("W", qt.get(0.0, 10.0));
	}

	/**
	 * Test removing values from a QuadTree using {@link QuadTree#remove(double, double, Object)}.
	 */
	public void testRemove() {
		QuadTree<String> qt = getTestTree();
		int size = qt.size();
		// test real removal
		assertTrue(qt.remove(10.0, 10.0, "10.0, 10.0"));
		assertEquals(size-1, qt.size());
		// removed object cannot be removed 2nd time
		assertFalse(qt.remove(10.0, 10.0, "10.0, 10.0"));
		assertEquals(size-1, qt.size());
		// do not remove real object at wrong place
		assertFalse(qt.remove(14.9, 14.9, "15.0, 15.0"));
		assertEquals(size-1, qt.size());
		// remove object at place with more than one object
		assertTrue(qt.remove(15.0, 15.0, "15.0, 15.0"));
		assertEquals(size-2, qt.size());
		assertEquals("15.0, 15.0 B", qt.get(15.0, 15.0)); // the other object should still be there...

		// restart
		qt = getTestTree();
		/* Again, remove object at place with more than one object, but this time remove the other one.
		 * This is to test that no just by chance the right one got removed before. */
		assertTrue(qt.remove(15.0, 15.0, "15.0, 15.0 B"));
		assertEquals(size-1, qt.size());
		assertEquals("15.0, 15.0", qt.get(15.0, 15.0)); // the other object should still be there...

		// test removing non-existent object at real location
		assertFalse(qt.remove(10.0, 10.0, "10.0, 10.0 B"));
		assertEquals(size-1, qt.size());
	}

	/**
	 * Test {@link QuadTree#clear()}.
	 */
	public void testClear() {
		QuadTree<String> qt = getTestTree();
		int size = qt.size();
		assertTrue(size > 0); // it makes no sense to test clear() on an empty tree
		qt.clear();
		assertEquals(0, qt.size());
		valuesTester(0, qt.values());
	}

	/**
	 * Test {@link QuadTree#values()} that it returns the correct content.
	 */
	public void testValues() {
		QuadTree<String> qt = getTestTree();
		int size = qt.size();
		assertEquals(6, size);

		// generic test
		valuesTester(qt.size(), qt.values());

		// test that values() got updated after adding an object to the tree
		qt.put(80.0, 80.0, "80.0, 80.0");
		assertEquals(size + 1, qt.size());
		valuesTester(qt.size(), qt.values());

		// test that values() got updated after removing an object to the tree
		assertTrue(qt.remove(80.0, 80.0, "80.0, 80.0"));
		assertEquals(size, qt.size());
		valuesTester(qt.size(), qt.values());

		// and remove one more...
		assertTrue(qt.remove(10.0, 10.0, "10.0, 10.0"));
		assertEquals(size - 1, qt.size());
		valuesTester(qt.size(), qt.values());

		// test the iterator
		Iterator<String> iter = qt.values().iterator();
		iter.next();
		try {
			iter.remove();
			fail("expected UnsupportedOperationException, got none.");
		} catch (UnsupportedOperationException expected) {}
	}

	public void testValues_EntryOnDividingBorder() {
		QuadTree<String> qt = new QuadTree<String>(0.0, 0.0, 40.0, 60.0);
		qt.put(10.0, 10.0, "10.0, 10.0");
		qt.put(20.0, 20.0, "20.0, 20.0"); // on vertical border
		qt.put(20.0, 30.0, "20.0, 30.0"); // exactly on center
		qt.put(20.0, 30.0, "30.0, 30.0"); // on horizontal border
		qt.put(20.0, 30.0, "30.0, 30.0"); // on horizontal border
		qt.put(10.0, 40.0, "10.0, 40.0"); // on 2nd-level border
		assertEquals(5, qt.size());
		valuesTester(5, qt.values());
		Iterator<String> iter = qt.values().iterator();
		assertEquals("10.0, 10.0", iter.next());
		assertEquals("10.0, 40.0", iter.next());
		assertEquals("20.0, 20.0", iter.next());
		assertEquals("20.0, 30.0", iter.next());
		assertEquals("30.0, 30.0", iter.next());
		assertFalse(iter.hasNext());
	}

	public void testValues_EntryOnOutsideBorder() {
		QuadTree<String> qt = new QuadTree<String>(0.0, 0.0, 40.0, 60.0);
		// the 4 corners
		qt.put(0.0, 0.0, "SW");
		qt.put(40.0, 0.0, "SE");
		qt.put(0.0, 60.0, "NW");
		qt.put(40.0, 60.0, "NE");
		// the 4 sides
		qt.put(10.0, 60.0, "N");
		qt.put(40.0, 10.0, "E");
		qt.put(10.0, 0.0, "S");
		qt.put(0.0, 10.0, "W");

		assertEquals(8, qt.size());
		valuesTester(8, qt.values());
		assertTrue(qt.values().contains("SW"));
		assertTrue(qt.values().contains("SE"));
		assertTrue(qt.values().contains("NW"));
		assertTrue(qt.values().contains("NE"));
		assertTrue(qt.values().contains("N"));
		assertTrue(qt.values().contains("E"));
		assertTrue(qt.values().contains("S"));
		assertTrue(qt.values().contains("W"));
		Iterator<String> iter = qt.values().iterator();
		assertEquals("SW", iter.next());
		assertEquals("W", iter.next());
		assertEquals("S", iter.next());
		assertEquals("NW", iter.next());
		assertEquals("N", iter.next());
		assertEquals("SE", iter.next());
		assertEquals("E", iter.next());
		assertEquals("NE", iter.next());
		assertFalse(iter.hasNext());
	}

	public void testValues_EntryOutsideExtend() {
		QuadTree<String> qt = new QuadTree<String>(5.0, 5.0, 35.0, 55.0);
		// outside the 4 corners
		qt.put(0.0, 0.0, "SW");
		qt.put(40.0, 0.0, "SE");
		qt.put(0.0, 60.0, "NW");
		qt.put(40.0, 60.0, "NE");
		// outside the 4 sides
		qt.put(10.0, 60.0, "N");
		qt.put(40.0, 10.0, "E");
		qt.put(10.0, 0.0, "S");
		qt.put(0.0, 10.0, "W");

		assertEquals(8, qt.size());
		valuesTester(8, qt.values());
		assertTrue(qt.values().contains("SW"));
		assertTrue(qt.values().contains("SE"));
		assertTrue(qt.values().contains("NW"));
		assertTrue(qt.values().contains("NE"));
		assertTrue(qt.values().contains("N"));
		assertTrue(qt.values().contains("E"));
		assertTrue(qt.values().contains("S"));
		assertTrue(qt.values().contains("W"));
	}

	/**
	 * Tests that a once obtained values-collection is indeed a live view
	 * on the QuadTree, so when the QuadTree changes, the view is updated
	 * as well.
	 */
	public void testValues_isView() {
		QuadTree<String> qt = getTestTree();
		int size = qt.size();
		Collection<String> values = qt.values();
		valuesTester(qt.size(), values);

		qt.put(80.0, 80.0, "80.0, 80.0");
		assertEquals(size + 1, qt.size());
		valuesTester(size + 1, values);

		qt.put(75.0, 75.0, "75.0, 75.0");
		assertEquals(size + 2, qt.size());
		valuesTester(size + 2, values);

		assertTrue(qt.remove(80.0, 80.0, "80.0, 80.0"));
		assertEquals(size + 1, qt.size());
		valuesTester(size + 1, values);
	}

	public void testValuesIterator_ConcurrentModification() {
		QuadTree<String> qt = getTestTree();
		Iterator<String> iter = qt.values().iterator();
		assertTrue(iter.hasNext());
		assertNotNull(iter.next());
		qt.put(39.0, 52.1, "39.0 52.1");
		assertTrue(iter.hasNext()); // hasNext() should not yet provoke exception
		try {
			iter.next();
			fail("missing exception.");
		}
		catch (ConcurrentModificationException e) {
			Log.info("catched expected exception: ", e);
		}
	}

	/**
	 * Test {@link QuadTree#execute(double, double, double, double, QuadTree.Executor)}.
	 */
	public void testExecute() {
		QuadTree<String> qt = getTestTree();
		TestExecutor executor = new TestExecutor();
		int count = qt.execute(0.0, 0.0, 20.1, 20.1, executor);
		assertEquals(4, count);
		assertEquals(4, executor.objects.size());

		executor = new TestExecutor();
		count = qt.execute(0.0, 0.0, 20.0, 20.0, executor);
		assertEquals(3, count);
		assertEquals(3, executor.objects.size());

		executor = new TestExecutor();
		count = qt.execute(0.0, 0.0, 19.9, 19.9, executor);
		assertEquals(3, count);
		assertEquals(3, executor.objects.size());

		executor = new TestExecutor();
		count = qt.execute(null, executor);
		assertEquals(qt.size(), count);
		assertEquals(qt.size(), executor.objects.size());
	}

	/**
	 * Tests that by serializing and de-serializing a QuadTree, all important attributes
	 * of the QuadTree are maintained. At the moment, this is essentially the size-attribute.
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void testSerialization() throws IOException, ClassNotFoundException {
		QuadTree<String> qt = getTestTree();

		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(outStream);
		out.writeObject(qt);
		out.close();

		ByteArrayInputStream inStream = new ByteArrayInputStream(outStream.toByteArray());
		ObjectInputStream in = new ObjectInputStream(inStream);
		QuadTree<String> qt2 = (QuadTree<String>)in.readObject();
		in.close();
		assertEquals(qt.size(), qt2.size());
		valuesTester(qt2.size(), qt2.values());
	}

	/**
	 * An internal helper method to do the real testing.
	 *
	 * @param treeSize
	 * @param values
	 */
	private void valuesTester(final int treeSize, final Collection<String> values) {
		int counter = 0;
		for (String value : values) {
			counter++;
		}
		assertEquals(treeSize, counter);
		assertEquals(treeSize, values.size());
	}

	/**
	 * An internal class to test the execute()-methods
	 *
	 */
	static class TestExecutor implements QuadTree.Executor<String> {

		public final Collection<Tuple<CoordImpl, String>> objects = new ArrayList<Tuple<CoordImpl, String>>();

		public void execute(final double x, final double y, final String object) {
			this.objects.add(new Tuple<CoordImpl, String>(new CoordImpl(x, y), object));
		}

	}

}
