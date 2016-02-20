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

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * Test for {@link QuadTree}.
 *
 * @author mrieser
 */
public class QuadTreeTest {

	private final static Logger log = Logger.getLogger(QuadTreeTest.class);

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
	@Test
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
	@Test
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

	@Test
	public void testPutOutsideBounds() {
		QuadTree<String> qt = new QuadTree<String>(-50.0, -50.0, 50.0, 50.0);
		try {
			qt.put( -100 , 0 , "-100 0" );
			Assert.fail( "no exception when adding an element on the left" );
		}
		catch (IllegalArgumentException e) {
			log.info( "catched expected exception" , e );
		}

		try {
			qt.put( 100 , 0 , "100 0" );
			Assert.fail( "no exception when adding an element on the right" );
		}
		catch (IllegalArgumentException e) {
			log.info( "catched expected exception" , e );
		}

		try {
			qt.put( 0 , 100 , "0 100" );
			Assert.fail( "no exception when adding an element above" );
		}
		catch (IllegalArgumentException e) {
			log.info( "catched expected exception" , e );
		}

		try {
			qt.put( 0 , -100 , "0 -100" );
			Assert.fail( "no exception when adding an element below" );
		}
		catch (IllegalArgumentException e) {
			log.info( "catched expected exception" , e );
		}
	}

	/**
	 * Test getting values from a QuadTree using {@link QuadTree#getClosest(double, double)}
	 * and {@link QuadTree#getDisk(double, double, double)}.
	 */
	@Test
	public void testGet() {
		QuadTree<String> qt = getTestTree();

		// test single get
		assertEquals("10.0, 10.0", qt.getClosest(0.0, 0.0)); // test nearest
		assertEquals("-15.0, 0.0", qt.getClosest(-5.0, 0.0)); // test nearest
		assertEquals("20.0, 10.0", qt.getClosest(20.0, 10.0)); // test with exact coordinate

		// test single get on point with more than one object
		String object = qt.getClosest(14.9, 14.9);
		assertTrue("15.0, 15.0".equals(object) || "15.0, 15.0 B".equals(object));

		// test "distance" get with exact coordinate
		Collection<String> values = qt.getDisk(15.0, 15.0, 1.0);
		assertEquals(2, values.size());
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with exact coordinate and distance=0
		values = qt.getDisk(15.0, 15.0, 0.0);
		assertEquals(2, values.size());
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with not exact coordinate
		values = qt.getDisk(9.0, 9.0, 10.0);
		assertEquals(3, values.size());
		assertTrue(values.contains("10.0, 10.0"));
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with points in more than one sub-area
		values = qt.getDisk(50.0, 0.0, 51.0);
		assertEquals(5, values.size());
		assertTrue(values.contains("100.0, 0.0"));
		assertTrue(values.contains("20.0, 10.0"));
		assertTrue(values.contains("10.0, 10.0"));
		assertTrue(values.contains("15.0, 15.0"));
		assertTrue(values.contains("15.0, 15.0 B"));

		// test "distance" get with critical distances
		values = qt.getDisk(90.0, 0.0, 9.0);
		assertEquals("test with distance 9.0", 0, values.size());

		values = qt.getDisk(90.0, 0.0, 9.999);
		assertEquals("test with distance 9.999", 0, values.size());

		values = qt.getDisk(90.0, 0.0, 10.0);
		assertEquals("test with distance 10.0", 1, values.size());

		values = qt.getDisk(90.0, 0.0, 10.001);
		assertEquals("test with distance 10.001", 1, values.size());

		values = qt.getDisk(90.0, 0.0, 11.0);
		assertEquals("test with distance 11.0", 1, values.size());

		// test "area"
		values.clear();
		qt.getRectangle(0.0, 0.0, 20.1, 20.1, values); // test with no object on the boundary
		assertEquals(4, values.size());

		values.clear();
		qt.getRectangle(0.0, 0.0, 20.0, 20.0, values); // test with an object exactly on the boundary
		assertEquals(4, values.size());

		values.clear();
		qt.getRectangle(0.0, 0.0, 19.9, 19.9, values); // test with no object on the boundary
		assertEquals(3, values.size());
	}

	@Test
	public void testGetXY_EntryOnDividingBorder() {
		QuadTree<String> qt = new QuadTree<String>(0, 0, 40, 60);
		qt.put(10.0, 10.0, "10.0, 10.0");
		qt.put(20.0, 20.0, "20.0, 20.0"); // on vertical border
		qt.put(20.0, 30.0, "20.0, 30.0"); // exactly on center
		qt.put(30.0, 30.0, "30.0, 30.0"); // on horizontal border

		assertEquals("20.0, 20.0", qt.getClosest(20.0, 20.0));
		assertEquals("20.0, 30.0", qt.getClosest(20.0, 30.0));
		assertEquals("30.0, 30.0", qt.getClosest(30.0, 30.0));
	}

	@Test
	public void testGetXY_EntryOnOutsideBorder() {
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

		assertEquals("SW", qt.getClosest(0.0, 0.0));
		assertEquals("SE", qt.getClosest(40.0, 0.0));
		assertEquals("NW", qt.getClosest(0.0, 60.0));
		assertEquals("NE", qt.getClosest(40.0, 60.0));
		assertEquals("N", qt.getClosest(10.0, 60.0));
		assertEquals("E", qt.getClosest(40.0, 10.0));
		assertEquals("S", qt.getClosest(10.0, 0.0));
		assertEquals("W", qt.getClosest(0.0, 10.0));
	}

	@Test
	public void testGetDistance_fromOutsideExtent() {
		QuadTree<String> qt = getTestTree();
		assertContains(new String[] {"100.0, 0.0"}, qt.getDisk(160.0, 0, 60.1)); // E
		assertContains(new String[] {"15.0, 15.0", "15.0, 15.0 B"}, qt.getDisk(15.0, 160, 145.1)); // N
		assertContains(new String[] {"-15.0, 0.0"}, qt.getDisk(-60.0, 0, 45.1)); // W
		assertContains(new String[] {"100.0, 0.0"}, qt.getDisk(100.0, -60, 60.1)); // S
	}

	@Test
	public void testGetDistance_EntryOnDividingBorder() {
		QuadTree<String> qt = new QuadTree<String>(0, 0, 40, 60);
		qt.put(10.0, 10.0, "10.0, 10.0");
		qt.put(20.0, 20.0, "20.0, 20.0"); // on vertical border
		qt.put(20.0, 30.0, "20.0, 30.0"); // exactly on center
		qt.put(30.0, 30.0, "30.0, 30.0"); // on horizontal border
		qt.put(12.0, 15.0, "12.0, 15.0");
		qt.put(10.0, 25.0, "10.0, 25.0");

		assertContains(new String[] {"10.0, 10.0"}, qt.getDisk(10.0, 7.0, 3.0));
		assertContains(new String[] {"10.0, 10.0"}, qt.getDisk(10.0, 12.0, 2.0));
		assertContains(new String[] {"10.0, 10.0"}, qt.getDisk(7.0, 10.0, 3.0));
		assertContains(new String[] {"10.0, 10.0"}, qt.getDisk(13.0, 10.0, 3.0));

		assertContains(new String[] {"20.0, 20.0"}, qt.getDisk(20.0, 23.0, 3.0));
		assertContains(new String[] {"20.0, 30.0"}, qt.getDisk(20.0, 27.0, 3.0));
		assertContains(new String[] {"30.0, 30.0"}, qt.getDisk(27.0, 30.0, 3.0));
		assertContains(new String[] {"12.0, 15.0"}, qt.getDisk(15.0, 15.0, 3.0));
		assertContains(new String[] {"10.0, 25.0"}, qt.getDisk(10.0, 28.0, 3.0));
	}

	@Test
	public void testGetDistance_EntryOnOutsideBorder() {
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

		assertContains(new String[] {"SW"}, qt.getDisk(3.0, 0.0, 3.0));
		assertContains(new String[] {"SE"}, qt.getDisk(40.0, 3.0, 3.0));
		assertContains(new String[] {"NW"}, qt.getDisk(3.0, 60.0, 3.0));
		assertContains(new String[] {"NE"}, qt.getDisk(40.0, 57.0, 3.0));

		assertContains(new String[] {"N"}, qt.getDisk(7.0, 60.0, 3.0));
		assertContains(new String[] {"E"}, qt.getDisk(40.0, 13.0, 3.0));
		assertContains(new String[] {"S"}, qt.getDisk(13.0, 0.0, 3.0));
		assertContains(new String[] {"W"}, qt.getDisk(3.0, 10.0, 3.0));
	}

	@Test
	public void testGetElliptical() {
		final Collection<Coord> all = new ArrayList<Coord>();
		QuadTree<Coord> qt = new QuadTree<Coord>(0, 0, 40, 60);

		all.add(new Coord(10.0, 10.0));
		all.add(new Coord(20.0, 20.0));
		all.add(new Coord(20.0, 30.0));
		all.add(new Coord(30.0, 30.0));
		all.add(new Coord(12.0, 15.0));
		all.add(new Coord(10.0, 25.0));

		// the 4 corners
		all.add(new Coord(0.0, 0.0));
		all.add(new Coord(40.0, 0.0));
		all.add(new Coord(0.0, 60.0));
		all.add(new Coord(40.0, 60.0));
		// the 4 sides
		all.add(new Coord(10.0, 60.0));
		all.add(new Coord(40.0, 10.0));
		all.add(new Coord(10.0, 0.0));
		all.add(new Coord(0.0, 10.0));
		for ( Coord coord : all ) qt.put( coord.getX() , coord.getY() , coord );

		// put foci in different places, inside and on the limits
		final double[] xPositions = new double[]{  0 , 20 , 30 , 40 };
		final double[] yPositions = new double[]{  0 , 20 , 30 , 60 };
		final double[] distances = new double[]{ 1 , 10 , 70 };
		for ( double x1 : xPositions ) {
			for ( double y1 : yPositions ) {
				final Coord f1 = new Coord(x1, y1);

				for ( double x2 : xPositions ) {
					for ( double y2 : yPositions ) {
						final Coord f2 = new Coord(x2, y2);
						final double interfoci = CoordUtils.calcEuclideanDistance( f1 , f2 );

						for ( double distance : distances ) {
							if ( distance < interfoci ) continue;
							final Collection<Coord> expected = new ArrayList<Coord>();
							for ( Coord coord : all ) {
								if ( CoordUtils.calcEuclideanDistance( coord , f1 ) + CoordUtils.calcEuclideanDistance( coord , f2 ) <= distance ) {
									expected.add( coord );
								}
							}

							final Collection<Coord> actual =
								qt.getElliptical(
										x1, y1,
										x2, y2,
										distance );

							//log.info( "testing foci "+f1+" and "+f2+", distance="+distance+", expected="+expected );
							Assert.assertEquals(
									"unexpected number of elements returned for foci "+f1+" and "+f2+", distance="+distance,
									expected.size(),
									actual.size() );

							Assert.assertTrue(
									"unexpected elements returned for foci "+f1+" and "+f2+", distance="+distance,
									expected.containsAll( actual ) );
						}
					}
				}
			}
		}
	}

	@Test
	public void testGetRect() {
		QuadTree<String> qt = new QuadTree<String>(0, 0, 1000, 1000);
		qt.put(100, 200, "node1");
		qt.put(400, 900, "node2");
		qt.put(700, 300, "node3");
		qt.put(900, 400, "node4");
		
		Collection<String> values = new ArrayList<String>();
		qt.getRectangle(new Rect(400, 300, 700, 900), values);
		Assert.assertEquals(2, values.size());
		Assert.assertTrue(values.contains("node2"));
		Assert.assertTrue(values.contains("node3"));
	}

	@Test
	public void testGetRect_flatNetwork() {
		QuadTree<String> qt = new QuadTree<String>(0, 0, 1000, 0);
		qt.put(0, 0, "node1");
		qt.put(100, 0, "node2");
		qt.put(500, 0, "node3");
		qt.put(900, 0, "node4");

		Collection<String> values = new ArrayList<String>();
		qt.getRectangle(new Rect(90, -10, 600, +10), values);
		Assert.assertEquals(2, values.size());
		Assert.assertTrue(values.contains("node2"));
		Assert.assertTrue(values.contains("node3"));

		Collection<String> values2 = new ArrayList<String>();
		qt.getRectangle(new Rect(90, 0, 600, 0), values2);
		Assert.assertEquals(2, values2.size());
		Assert.assertTrue(values2.contains("node2"));
		Assert.assertTrue(values2.contains("node3"));
	}

	/**
	 * Test removing values from a QuadTree using {@link QuadTree#remove(double, double, Object)}.
	 */
	@Test
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
		assertEquals("15.0, 15.0 B", qt.getClosest(15.0, 15.0)); // the other object should still be there...

		// restart
		qt = getTestTree();
		/* Again, remove object at place with more than one object, but this time remove the other one.
		 * This is to test that no just by chance the right one got removed before. */
		assertTrue(qt.remove(15.0, 15.0, "15.0, 15.0 B"));
		assertEquals(size-1, qt.size());
		assertEquals("15.0, 15.0", qt.getClosest(15.0, 15.0)); // the other object should still be there...

		// test removing non-existent object at real location
		assertFalse(qt.remove(10.0, 10.0, "10.0, 10.0 B"));
		assertEquals(size-1, qt.size());
	}

	/**
	 * Test {@link QuadTree#clear()}.
	 */
	@Test
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
	@Test
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

	@Test
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

	@Test
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

	/**
	 * Tests that a once obtained values-collection is indeed a live view
	 * on the QuadTree, so when the QuadTree changes, the view is updated
	 * as well.
	 */
	@Test
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

	@Test
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
			log.info("catched expected exception: ", e);
		}
	}

	/**
	 * Test {@link QuadTree#execute(double, double, double, double, QuadTree.Executor)}.
	 */
	@Test
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
	@Test
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
	 * Checks that the given collection contains exactly all of the exptectedEntries, but none more.
	 *
	 * @param <T>
	 * @param expectedEntries
	 * @param collection
	 */
	private <T> void assertContains(final T[] expectedEntries, final Collection<T> collection) {
		assertEquals(expectedEntries.length, collection.size());
		for (T t : expectedEntries) {
			assertTrue(collection.contains(t));
		}
	}

	/**
	 * An internal class to test the execute()-methods
	 *
	 */
	static class TestExecutor implements QuadTree.Executor<String> {

		public final Collection<Tuple<Coord, String>> objects = new ArrayList<Tuple<Coord, String>>();

		@Override
		public void execute(final double x, final double y, final String object) {
			this.objects.add(new Tuple<Coord, String>(new Coord(x, y), object));
		}

	}

	/**
	 * Test read access on {@link QuadTree#getRing(double, double, double, double)}.
	 */
	@Test
	public void testGetRing() {
		QuadTree qt = new QuadTree(0, 0, 3, 3);

		for(int x = 0; x < 4; x++) {
			for(int y = 0; y < 4; y++) {
				qt.put(x, y, String.format("%s,%s", x, y));
			}
		}

		Collection<String> result = qt.getRing(1, 1, 0, 1);
		assertEquals(result.contains("1,1"), true);
		assertEquals(result.contains("0,1"), true);
		assertEquals(result.contains("1,0"), true);
		assertEquals(result.contains("2,1"), true);
		assertEquals(result.contains("1,2"), true);

		result = qt.getRing(1, 1, 0.5, 1);
		assertEquals(result.contains("1,1"), false);
		assertEquals(result.contains("0,1"), true);
		assertEquals(result.contains("1,0"), true);
		assertEquals(result.contains("2,1"), true);
		assertEquals(result.contains("1,2"), true);

		result = qt.getRing(1, 1, 1.1, 1);
		assertEquals(result.size(), 0);

		result = qt.getRing(1, 1, 0, 0);
		assertEquals(result.size(), 1);
		assertEquals(result.contains("1,1"), true);

		result = qt.getRing(-1, 1, 1, 1.4);
		assertEquals(result.size(), 1);
		assertEquals(result.contains("0,1"), true);

		result = qt.getRing(-1, 1, 1, 1.5);
		assertEquals(result.size(), 3);
		assertEquals(result.contains("0,1"), true);
		assertEquals(result.contains("0,0"), true);
		assertEquals(result.contains("0,2"), true);

		result = qt.getRing(0, 0, Math.sqrt(18), Math.sqrt(18));
		assertEquals(result.size(), 1);
		assertEquals(result.contains("3,3"), true);
	}

}
