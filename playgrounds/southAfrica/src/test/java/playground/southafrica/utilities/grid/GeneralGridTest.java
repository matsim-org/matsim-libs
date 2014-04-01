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
import org.matsim.core.utils.collections.Tuple;
import org.matsim.testcases.MatsimTestUtils;

import playground.southafrica.utilities.grid.GeneralGrid.GridType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeneralGridTest {
	private final static double delta = 0.0001;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testConstructor(){
		GeneralGrid g1 = new GeneralGrid(10, 1);
		Assert.assertTrue("Wrong grid type.", g1.getGridType() == GridType.SQUARE);
		GeneralGrid g2 = new GeneralGrid(10, 2);
		Assert.assertTrue("Wrong grid type.", g2.getGridType() == GridType.HEX);
		GeneralGrid g3 = new GeneralGrid(10, 3);
		Assert.assertTrue("Wrong grid type.", g3.getGridType() == GridType.UNKNOWN);
	}
	
	@Test
	public void testGenerateGrid_Square(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, 1);
		g1.generateGrid(p);
		
		QuadTree<Tuple<String,Point>> qt1 = g1.getGrid();
		Assert.assertEquals("Wrong number of cells.", 100, qt1.size());
		
		/* Cell (0,0) */
		Tuple<String, Point> t00 = qt1.get(0.5, 0.5);
		Assert.assertTrue("Wrong cell (0,0).", ((String)t00.getFirst()).equalsIgnoreCase("0_0"));
		Assert.assertEquals("Wrong x-coordinate for (0,0)", 5, t00.getSecond().getX(), delta);
		Assert.assertEquals("Wrong y-coordinate for (0,0)", 5, t00.getSecond().getY(), delta);
		
		/* Cell (9,9) */
		Tuple<String, Point> t1010 = qt1.get(95, 95);
		Assert.assertTrue("Wrong cell (9,9).", ((String)t1010.getFirst()).equalsIgnoreCase("9_9"));
		Assert.assertEquals("Wrong x-coordinate for (9,9)", 95, t1010.getSecond().getX(), delta);
		Assert.assertEquals("Wrong y-coordinate for (9,9)", 95, t1010.getSecond().getY(), delta);
	}

	@Test
	public void testGenerateGrid_Hex(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, 2);
		g1.generateGrid(p);
		
		QuadTree<Tuple<String,Point>> qt1 = g1.getGrid();
		Assert.assertEquals("Wrong number of cells.", 143, qt1.size());
		
		/* Cell (0,0) */
		Tuple<String, Point> t00 = qt1.get(0.5, 0.5);
		Assert.assertTrue("Wrong cell (0,0).", ((String)t00.getFirst()).equalsIgnoreCase("0_0"));
		Assert.assertEquals("Wrong x-coordinate for (0,0)", 5, t00.getSecond().getX(), delta);
		Assert.assertEquals("Wrong y-coordinate for (0,0)", 5, t00.getSecond().getY(), delta);
		
		/* Cell (10,12) */
		Tuple<String, Point> t1010 = qt1.get(95, 95);
		Assert.assertTrue("Wrong cell (10,12).", ((String)t1010.getFirst()).equalsIgnoreCase("10_12"));
		double dx = 0.5*10 + 12*((3.0/4.0)*10);
		Assert.assertEquals("Wrong x-coordinate for (10,12)", dx, t1010.getSecond().getX(), delta);
		double dy = 0.5*10 + 10*(Math.sqrt(3.0)/2*10);
		Assert.assertEquals("Wrong y-coordinate for (10,12)", dy, t1010.getSecond().getY(), delta);
	}
	
	@Test
	public void testWriteGrid(){
		Polygon p = buildDummyPolygon();
		GeneralGrid g1 = new GeneralGrid(10.0, 1);
		g1.generateGrid(p);
		g1.writeGrid(utils.getOutputDirectory());
		Assert.assertTrue("File does not exist.", new File(utils.getOutputDirectory() + "/SQUARE.csv").exists());

		GeneralGrid g2 = new GeneralGrid(10.0, 2);
		g2.generateGrid(p);
		g2.writeGrid(utils.getOutputDirectory());
		Assert.assertTrue("File does not exist.", new File(utils.getOutputDirectory() + "/HEX.csv").exists());
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
