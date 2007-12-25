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

package org.matsim.utils.misc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.matsim.testcases.MatsimTestCase;

/**
 * Test for {@link org.matsim.utils.misc.QuadTree}.
 *
 * @author mrieser
 */
public class QuadTreeTest extends MatsimTestCase {

	/**
	 * @return A simple QuadTree with 6 entires for tests.
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
	 * Test putting values into a QuadTree using {@link org.matsim.utils.misc.QuadTree#put(double, double, Object)}.
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
	 * Test getting values from a QuadTree using {@link org.matsim.utils.misc.QuadTree#get(double, double)}
	 * and {@link org.matsim.utils.misc.QuadTree#get(double, double, double)}.
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
	}

	/**
	 * Test removing values from a QuadTree using {@link org.matsim.utils.misc.QuadTree#remove(double, double, Object)}.
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
	 * Test {@link org.matsim.utils.misc.QuadTree#values()}.
	 */
	public void testValues() {
		QuadTree<String> qt = getTestTree();
		int size = qt.size();

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
	 * Tests that by serializing and de-serializing a QuadTree, all important attributes
	 * of the QuadTree are maintained. At the moment, this is essentially the size-attribute.
	 */
	public void testSerialization() {

		QuadTree<String> qt = getTestTree();

		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(getOutputDirectory() + "serializedQuadTree.dat");
			out = new ObjectOutputStream(fos);
			out.writeObject(qt);
			out.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(getOutputDirectory() + "serializedQuadTree.dat");
			in = new ObjectInputStream(fis);
			QuadTree<String> qt2 = (QuadTree<String>)in.readObject();
			in.close();
			assertEquals(qt.size(), qt2.size());
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
	}
}
