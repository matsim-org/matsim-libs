package org.matsim.utils.geometry.geotools;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordI;
import org.matsim.utils.geometry.geotools.MGC;
import org.matsim.utils.geometry.shared.Coord;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author laemmel
 *
 */
public class MGCTest extends MatsimTestCase {

	public void testCoord2CoordinateAndViceVersa(){
		double x = 123.456789;
		double y = 987.654321;
		double delta = 0.0000001;
		CoordI coord1 = new Coord(x,y);
		Coordinate coord2 = MGC.coord2Coordinate(coord1);
		CoordI coord3 = MGC.Coordinate2Coord(coord2);
		double x1 = coord3.getX();
		double y1 = coord3.getY();
		junit.framework.Assert.assertEquals(x,x1,delta);
		junit.framework.Assert.assertEquals(y,y1,delta);
	}
	
	public void testCoord2PointAndViceVersa(){
		double x = 123.456789;
		double y = 987.654321;
		double delta = 0.0000001;
		CoordI coord1 = new Coord(x,y);
		Point p = MGC.coord2Point(coord1);
		CoordI coord2 = MGC.point2Coord(p);
		double x1 = coord2.getX();
		double y1 = coord2.getY();
		junit.framework.Assert.assertEquals(x,x1,delta);
		junit.framework.Assert.assertEquals(y,y1,delta);

	}
}
