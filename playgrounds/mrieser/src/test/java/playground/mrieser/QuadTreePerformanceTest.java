/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreePerfTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.mrieser;

import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.QuadTree;

/**
 * Performance tests for QuadTree.
 *
 * @author mrieser
 */
public class QuadTreePerformanceTest extends TestCase {

	private static final Logger log = Logger.getLogger(QuadTreePerformanceTest.class);

	/**
	 * Test the performance of getting the nearest objects to a given coordinate in a QuadTree with a large number of entries.
	 */
	public void testGet() {
		final double minX = 200000.0;
		final double maxX = 400000.0;
		final double minY = 500000.0;
		final double maxY = 700000.0;

		final long nOfEntries = 25000;
		final long nOfQueries = 25000;

		Random r = new Random(4711);
		double deltaX = maxX - minX;
		double deltaY = maxY - minY;

		log.info("build quadtree, adding " + nOfEntries + " entries...");

		QuadTree<Long> qt = new QuadTree<Long>(minX, minY, maxX, maxY);
		for (long i = 0; i < nOfEntries; i++) {
			double x = r.nextDouble() * deltaX + minX;
			double y = r.nextDouble() * deltaY + minY;
			qt.put(x, y, Long.valueOf(i));
		}

		log.info("start get-Queries");
		Gbl.startMeasurement();
		for (long i = 0; i < nOfQueries; i++) {
			double x = r.nextDouble() * deltaX + minX;
			double y = r.nextDouble() * deltaY + minY;
			qt.getClosest(x, y);
		}
		Gbl.printElapsedTime();
		log.info("get-Queries ended.");
	}

	/**
	 * Test the performance of iterating over a QuadTree with a large number of entries.
	 */
	public void testValuesIterator() {
		final double minX = 200000.0;
		final double maxX = 400000.0;
		final double minY = 500000.0;
		final double maxY = 700000.0;

		final long nOfEntries = 25000;

		Random r = new Random(4711);
		double deltaX = maxX - minX;
		double deltaY = maxY - minY;

		log.info("build quadtree, adding " + nOfEntries + " entries...");

		QuadTree<Long> qt = new QuadTree<Long>(minX, minY, maxX, maxY);
		for (long i = 0; i < nOfEntries; i++) {
			double x = r.nextDouble() * deltaX + minX;
			double y = r.nextDouble() * deltaY + minY;
			qt.put(x, y, Long.valueOf(i));
		}

		log.info("start iterator");
		int i = 0;
		Gbl.startMeasurement();
		for (Long l : qt.values()) {
			i++;
		}
		Gbl.printElapsedTime();
		log.info("iterator ended.");
		assertEquals(nOfEntries, i);
	}

}
