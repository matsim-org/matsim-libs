/* *********************************************************************** *
 * project: org.matsim.*
 * MyDemandMatrixTest.java
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

package playground.jjoubert.Utilities.roadpricing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.matrices.Matrix;
import org.matsim.testcases.MatsimTestUtils;

public class MyDemandMatrixTest{
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testParseMatrix(){
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.parseMatrix(utils.getClassInputDirectory() + "matrix.txt", "dummy", "dummy");
		Matrix m = mdm.getDemandMatrix(); 
		Assert.assertEquals("Matrix must have 4 rows", 4, m.getFromLocations().size(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Matrix must have 4 columns.", 4, m.getToLocations().size());
		Assert.assertEquals("Wrong entry from 1 to 1.",  5.0, m.getEntry("1", "1").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 1 to 2.", 12.0, m.getEntry("1", "2").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 1 to 3.",  8.0, m.getEntry("1", "3").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 1 to 4.",  3.0, m.getEntry("1", "4").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 2 to 1.", 16.0, m.getEntry("2", "1").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 2 to 2.", 13.0, m.getEntry("2", "2").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 2 to 3.",  5.0, m.getEntry("2", "3").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 2 to 4.", 29.0, m.getEntry("2", "4").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 3 to 1.",  3.0, m.getEntry("3", "1").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 3 to 2.",  7.0, m.getEntry("3", "2").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 3 to 3.",  7.0, m.getEntry("3", "3").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 3 to 4.",  5.0, m.getEntry("3", "4").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 4 to 1.", 13.0, m.getEntry("4", "1").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 4 to 2.", 21.0, m.getEntry("4", "2").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 4 to 3.",  4.0, m.getEntry("4", "3").getValue(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong entry from 4 to 4.",  8.0, m.getEntry("4", "4").getValue(), MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testReadLocationCoordinates(){
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.readLocationCoordinates(utils.getClassInputDirectory() + "centroids.txt", 2, 0, 1);
		Map<String, Coord> lm = mdm.getLocationCoordinates();
		Assert.assertEquals("Wrong number of entries.", 4, lm.size());
		Assert.assertEquals("Location 1 has the wrong coordinate.", true, lm.get("1").getX() == 0.0 && lm.get("1").getY() == 1.0);
		Assert.assertEquals("Location 2 has the wrong coordinate.", true, lm.get("2").getX() == 1.0 && lm.get("2").getY() == 1.0);
		Assert.assertEquals("Location 3 has the wrong coordinate.", true, lm.get("3").getX() == 0.0 && lm.get("3").getY() == 0.0);
		Assert.assertEquals("Location 4 has the wrong coordinate.", true, lm.get("4").getX() == 1.0 && lm.get("4").getY() == 0.0);
	}
	
	@Test
	public void testGenerateExternalDemand(){
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.readLocationCoordinates(utils.getClassInputDirectory() + "centroids.txt", 2, 0, 1);
		mdm.parseMatrix(utils.getClassInputDirectory() + "matrix.txt", "dummy", "dummy");
		
		List<String> list = new ArrayList<>(2);
		list.add("4");
		Random r = new Random(1234);
		Scenario sc = mdm.generateDemand(list, r, 1.0, "car");
		
		Assert.assertEquals("Wrong population size.", 91, sc.getPopulation().getPersons().size());
	}
}
