/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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

package org.matsim.core.network.algorithms.intersectionSimplifier.containers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.matsim.testcases.MatsimTestUtils;

public class ConcaveHullTest {
	@RegisterExtension
	MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testMain(){
		String[] args = new String[]{
			utils.getClassInputDirectory() + "testCoordinates.csv",
			"2",
			utils.getOutputDirectory()
		};
		try{
			ConcaveHull.main(args);
		} catch(Exception e){
			Assertions.fail(e);
		}
	}


	/** Test whether duplicate input points are removed. **/
	@Test
	void testConstructor(){
		GeometryCollection gcIncorrect = setupWithDuplicates();
		ConcaveHull ch1 = new ConcaveHull(gcIncorrect, 2);
		Assertions.assertEquals(8, ch1.getInputPoints(), "Duplicates not removed.");

		GeometryCollection gcCorrect = setup();
		ConcaveHull ch2 = new ConcaveHull(gcCorrect, 2);
		Assertions.assertEquals(8, ch2.getInputPoints(), "Wrong number of input points.");
	}


	@Test
	public void testGetConcaveHull(){
		GeometryCollection gc = setup();
		ConcaveHull ch = new ConcaveHull(gc, 1.0);
		Geometry g = ch.getConcaveHull();
        Assertions.assertInstanceOf(Polygon.class, g, "Wrong geometry created.");
	}


	/**
	 * Set up a small test case:
	 *   ^
	 *   |
	 *   3         4
	 *   |    7
     *   | 8     6
	 *   |    5
	 *   1_________2___>
	 *
	 */
	private GeometryCollection setup(){
		GeometryFactory gf = new GeometryFactory();
		Geometry[] ga = new Geometry[8];
		ga[0] = gf.createPoint(new Coordinate(0, 0));
		ga[1] = gf.createPoint(new Coordinate(4, 0));
		ga[2] = gf.createPoint(new Coordinate(0, 4));
		ga[3] = gf.createPoint(new Coordinate(4, 4));
		ga[4] = gf.createPoint(new Coordinate(2, 1));
		ga[5] = gf.createPoint(new Coordinate(3, 2));
		ga[6] = gf.createPoint(new Coordinate(2, 3));
		ga[7] = gf.createPoint(new Coordinate(1, 2));
		return new GeometryCollection(ga, gf);
	}

	/**
	 * Creates a similar {@link GeometryCollection} as in setup() but have
	 * triplicates of points 7 & 8.
	 */
	private GeometryCollection setupWithDuplicates(){
		GeometryFactory gf = new GeometryFactory();
		Geometry[] ga = new Geometry[12];
		ga[0] = gf.createPoint(new Coordinate(0, 0)); // 1
		ga[1] = gf.createPoint(new Coordinate(4, 0)); // 2
		ga[2] = gf.createPoint(new Coordinate(0, 4)); // 3
		ga[3] = gf.createPoint(new Coordinate(4, 4)); // 4
		ga[4] = gf.createPoint(new Coordinate(2, 1)); // 5
		ga[5] = gf.createPoint(new Coordinate(3, 2)); // 6
		ga[6] = gf.createPoint(new Coordinate(2, 3)); // 7
		ga[7] = gf.createPoint(new Coordinate(1, 2)); // 8

		ga[8] = gf.createPoint(new Coordinate(2, 3)); // 7
		ga[9] = gf.createPoint(new Coordinate(2, 3)); // 7

		ga[10] = gf.createPoint(new Coordinate(1, 2)); // 8
		ga[11] = gf.createPoint(new Coordinate(1, 2)); // 8
		return new GeometryCollection(ga, gf);
	}

}
