/* *********************************************************************** *
 * project: org.matsim.*
 * WorldUtilsTest.java
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

package org.matsim.utils;

import org.matsim.basic.v01.IdImpl;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.World;
import org.matsim.world.Zone;
import org.matsim.world.ZoneLayer;

public class WorldUtilsTest extends MatsimTestCase {

	/**
	 * Tests org.matsim.util.WorldUtils.distancePointLinesegment()
	 *
	 * @author mrieser
	 */
	public void testDistancePointLinesegment() {
		CoordImpl p1 = new CoordImpl(10, 20);
		CoordImpl p2 = new CoordImpl(10, 30);
		CoordImpl p3 = new CoordImpl(10, 40);
		CoordImpl p4 = new CoordImpl(20, 30);

		assertEquals(10.0, WorldUtils.distancePointLinesegment(p1, p3, p4), 1e-10);

		assertEquals("special case where lineFrom == lineTo",
				10.0, WorldUtils.distancePointLinesegment(p1, p1, p2), 1e-10);

		assertEquals("special case where point before lineFrom",
				10.0, WorldUtils.distancePointLinesegment(p2, p3, p1), 1e-10);

		assertEquals("special case where point after lineTo",
				10.0, WorldUtils.distancePointLinesegment(p1, p2, p3), 1e-10);

		assertEquals("special case where point on line segment",
				0.0, WorldUtils.distancePointLinesegment(p1, p3, p2), 1e-10);

	}

	/**
	 * Tests that the returned random coordinates are indeed somehow random
	 * within the area when the zones have a bounding box, and that the
	 * coordinates cover the whole area more or less equally.
	 *
	 * @author mrieser
	 */
	public void testGetRandomCoordInZoneBoundingBox() {
		final double minX = 0.0;
		final double minY = 0.0;
		final double maxX = 9.0;
		final double maxY = 18.0;
		final int[] areaCounters = new int[9];
		final World world = new World();
		ZoneLayer layer = (ZoneLayer) world.createLayer(new IdImpl("zones"), "zones for test");
		Zone zone = layer.createZone("1", "4.5", "9", "0", "0", "9", "18", null, "center zone");
		layer.createZone("2", "30", "15", "9", "0", "51", "30", null, "another zone");

		for (int i = 0; i < 900; i++) {
			Coord c = WorldUtils.getRandomCoordInZone(zone, layer);
			assertTrue("Coordinate is out of bounds: x = " + c.getX(), c.getX() >= minX);
			assertTrue("Coordinate is out of bounds: x = " + c.getX(), c.getX() <= maxX);
			assertTrue("Coordinate is out of bounds: y = " + c.getY(), c.getY() >= minY);
			assertTrue("Coordinate is out of bounds: y = " + c.getY(), c.getY() <= maxY);
			int areaIndex = ((int) c.getX()) / ((int) ((maxX - minX) / 3.0))*3 + ((int) c.getY()) / ((int) ((maxY - minY) / 3.0));
			areaCounters[areaIndex]++;
		}
		for (int i = 0; i < areaCounters.length; i++) {
				int count = areaCounters[i];
				assertTrue("random coordinates seem not to be equally distributed, as area " + i + " has only " + count + " points in it.", count > 90);
		}
	}

	/**
	 * Tests that the returned random coordinates are indeed somehow random
	 * within the area when the zones have <em>no</em> bounding box, and that
	 * the coordinates cover the center area stronger than the outer areas.
	 *
	 * @author mrieser
	 */
	public void testGetRandomCoordInZoneCenterOnly() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		final int[] areaCounters = new int[11]; // radius from center in %: 0-10, 10-20, 20- ... -90, 90-100, 100+.
		final World world = new World();
		ZoneLayer layer = (ZoneLayer) world.createLayer(new IdImpl("zones"), "zones for test");
		Zone zone = layer.createZone("1", "4.5", "9", null, null, null, null, null, "center zone");
		Zone zone2 = layer.createZone("2", "30", "15", "9", null, null, null, null, "another zone");
		Coord center = zone.getCenter();
		final double distance = center.calcDistance(zone2.getCenter());

		for (int i = 0; i < 700; i++) {
			Coord c = WorldUtils.getRandomCoordInZone(zone, layer);
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
			int areaIndex = (int) (c.calcDistance(center) / distance * 10);
			areaCounters[areaIndex]++;
		}
		assertTrue("random coordinates are not spread enough. minX = " + minX, minX < (4.5 - distance/2.0));
		assertTrue("random coordinates are not spread enough. maxX = " + maxX, maxX > (4.5 + distance/2.0));
		assertTrue("random coordinates are not spread enough. minY = " + minY, minY < (9.0 - distance/2.0));
		assertTrue("random coordinates are not spread enough. maxY = " + maxY, maxY > (9.0 + distance/2.0));
		for (int i = 0; i < 7; i++) { // the actual algorithm uses 0.7*distance, so we expect points only in that area
			int count = areaCounters[i]; // in an ideal case, every area should have 100 points, but with randomness...
			assertTrue("random coordinates seem not to be equally distributed, as area " + i + " has only " + count + " points in it.", count > 88);
		}
		for (int i = 8; i < areaCounters.length; i++) {
			assertTrue("there should not be any point in area " + i, areaCounters[i] == 0);
		}
	}

}
