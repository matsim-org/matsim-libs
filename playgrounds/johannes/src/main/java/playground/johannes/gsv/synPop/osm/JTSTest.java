/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.osm;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import org.matsim.contrib.common.util.XORShiftRandom;

import java.util.Random;

/**
 * @author johannes
 *
 */
public class JTSTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long time = System.currentTimeMillis();
		
//		PrecisionModel model = new PrecisionModel(0.1);
		GeometryFactory factory = new GeometryFactory();
		
		Coordinate coords[] = new Coordinate[6];
		coords[0] = new Coordinate(3, 1);
		coords[1] = new Coordinate(1, 1);
		coords[2] = new Coordinate(1, 2);
		coords[3] = new Coordinate(2, 3);
		coords[4] = new Coordinate(3, 2);
		coords[5] = new Coordinate(3, 1);
		
		LinearRing ring = factory.createLinearRing(coords);
		Polygon poly = factory.createPolygon(ring, null);
		PreparedGeometry prepGeo = PreparedGeometryFactory.prepare(poly);
		
		Random random = new XORShiftRandom(123);
		
		for(int i = 0; i < 100000000; i++) {
			double x = random.nextDouble() * 4;
			double y = random.nextDouble() * 4;
			Point p = factory.createPoint(new Coordinate(x, y));
			prepGeo.contains(p);
		}
		
		System.out.println(String.format("Geometry test took %s msecs.", System.currentTimeMillis() - time));

	}

}
