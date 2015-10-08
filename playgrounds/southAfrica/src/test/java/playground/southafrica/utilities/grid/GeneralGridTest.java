/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,     *
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

package playground.southafrica.utilities.grid;

import java.io.File;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.utilities.grid.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeneralGridTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testConstructor(){
		GeneralGrid g1 = new GeneralGrid(10, GridType.SQUARE);
		Assert.assertTrue("Wrong grid type.", g1.getGridType() == GridType.SQUARE);
		GeneralGrid g2 = new GeneralGrid(10, GridType.HEX);
		Assert.assertTrue("Wrong grid type.", g2.getGridType() == GridType.HEX);
		GeneralGrid g3 = new GeneralGrid(10, GridType.UNKNOWN);
		Assert.assertTrue("Wrong grid type.", g3.getGridType() == GridType.UNKNOWN);
	}
	
	@Test
	public void testGenerateGrid_Square(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, GridType.SQUARE);
		g1.generateGrid(p);
		
		QuadTree<Point> qt1 = g1.getGrid();
		Assert.assertEquals("Wrong number of cells.", 121, qt1.size());
		
		/* Cell at (0,0) */
		Point p1 = p.getFactory().createPoint(new Coordinate(0.0, 0.0));
		Assert.assertEquals("Points should be at the same location.", 0.0, p1.distance(qt1.getClosest(0.0, 0.0)), MatsimTestUtils.EPSILON);
		/* Cell at (10,10) */
		Point p2 = p.getFactory().createPoint(new Coordinate(10.0, 10.0));
		Assert.assertEquals("Points should be at the same location.", 0.0, p2.distance(qt1.getClosest(10.0, 10.0)), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testGenerateGrid_Hex(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, GridType.HEX);
		g1.generateGrid(p);
		
		QuadTree<Point> qt1 = g1.getGrid();
		Assert.assertEquals("Wrong number of cells.", 168, qt1.size());
		
		/* Cell at (0,0) */
		Point p1 = p.getFactory().createPoint(new Coordinate(0.0, 0.0));
		Assert.assertEquals("Points should be at the same location.", 0.0, p1.distance(qt1.getClosest(0.0, 0.0)), MatsimTestUtils.EPSILON);
		
		/* Cell at (10,0) */
		Point p2 = p.getFactory().createPoint(new Coordinate(7.5, ( Math.sqrt(3.0) / 2 ) * 5.0));
		Assert.assertEquals("Points should be at the same location.", 0.0, p2.distance(qt1.getClosest(7.5, (Math.sqrt(3.0) / 2) * 5.0)), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testWriteGrid(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, GridType.SQUARE);
		g1.generateGrid(p);
		g1.writeGrid(utils.getOutputDirectory(), null);
		Assert.assertTrue("File does not exist.", new File(utils.getOutputDirectory() + "/SQUARE_10.csv").exists());

		GeneralGrid g2 = new GeneralGrid(10.0, GridType.HEX);
		g2.generateGrid(p);
		g2.writeGrid(utils.getOutputDirectory(), null);
		Assert.assertTrue("File does not exist.", new File(utils.getOutputDirectory() + "/HEX_10.csv").exists());
	}
	
	@Test
	public void testGetGeometrySquare(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, GridType.SQUARE);
		g1.generateGrid(p);
		GeometryFactory gf = p.getFactory();
		
		/* Try lower left corner. */
		Coordinate c1 = new Coordinate(-5.0, -5.0);
		Coordinate c2 = new Coordinate(-5.0, 5.0);
		Coordinate c3 = new Coordinate(5.0, 5.0);
		Coordinate c4 = new Coordinate(5.0, -5.0);
		Coordinate[] ca = {c1, c2, c3, c4, c1};
		Polygon pTest = gf.createPolygon(ca);
		Geometry g = g1.getCellGeometry(gf.createPoint(new Coordinate(0.0, 0.0)));
		Assert.assertEquals("Wrong polygon object.", pTest, g);
	}
	
	@Test
	public void testGetGeometryHex(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, GridType.HEX);
		g1.generateGrid(p);
		GeometryFactory gf = p.getFactory();
		
		/* Try lower left corner. */
		double w = 0.5*10.0;
		double h = Math.sqrt(3.0)/2.0 * w;
		
		Coordinate c1 = new Coordinate(-w, 0.0);
		Coordinate c2 = new Coordinate(-0.5*w, h);
		Coordinate c3 = new Coordinate(0.5*w, h);
		Coordinate c4 = new Coordinate(w, 0.0);
		Coordinate c5 = new Coordinate(0.5*w, -h);
		Coordinate c6 = new Coordinate(-0.5*w, -h);
		
		Coordinate[] ca = {c1, c2, c3, c4, c5, c6, c1};
		Polygon pTest = gf.createPolygon(ca);
		Geometry g = g1.getCellGeometry(gf.createPoint(new Coordinate(0.0, 0.0)));
		Assert.assertEquals("Wrong polygon object.", pTest, g);
	}
	
	
	private Polygon buildDummyPolygon(){
		GeometryFactory gf = new GeometryFactory();
		Coordinate c1 = new Coordinate(0.0, 0.0);
		Coordinate c2 = new Coordinate(0.0, 100);
		Coordinate c3 = new Coordinate(100, 100);
		Coordinate c4 = new Coordinate(100, 0.0);
		Coordinate[] ca = {c1,c2,c3,c4,c1};
		return gf.createPolygon(ca);
	}


}
