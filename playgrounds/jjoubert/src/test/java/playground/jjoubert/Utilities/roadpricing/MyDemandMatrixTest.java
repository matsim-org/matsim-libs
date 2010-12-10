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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.matrices.Matrix;
import org.matsim.testcases.MatsimTestCase;

public class MyDemandMatrixTest extends MatsimTestCase{
	
	public void testParseMatrix(){
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.parseMatrix(getClassInputDirectory() + "matrix.txt", "dummy", "dummy");
		Matrix m = mdm.getDemandMatrix(); 
		assertEquals("Matrix must have 4 rows", 4, m.getFromLocations().size());
		assertEquals("Matrix must have 4 columns.", 4, m.getToLocations().size());
		assertEquals("Wrong entry from 1 to 1.", 5.0, m.getEntry(new IdImpl("1"), new IdImpl("1")).getValue());
		assertEquals("Wrong entry from 1 to 2.", 12.0, m.getEntry(new IdImpl("1"), new IdImpl("2")).getValue());
		assertEquals("Wrong entry from 1 to 3.", 8.0, m.getEntry(new IdImpl("1"), new IdImpl("3")).getValue());
		assertEquals("Wrong entry from 1 to 4.", 3.0, m.getEntry(new IdImpl("1"), new IdImpl("4")).getValue());
		assertEquals("Wrong entry from 2 to 1.", 16.0, m.getEntry(new IdImpl("2"), new IdImpl("1")).getValue());
		assertEquals("Wrong entry from 2 to 2.", 13.0, m.getEntry(new IdImpl("2"), new IdImpl("2")).getValue());
		assertEquals("Wrong entry from 2 to 3.", 5.0, m.getEntry(new IdImpl("2"), new IdImpl("3")).getValue());
		assertEquals("Wrong entry from 2 to 4.", 29.0, m.getEntry(new IdImpl("2"), new IdImpl("4")).getValue());
		assertEquals("Wrong entry from 3 to 1.", 3.0, m.getEntry(new IdImpl("3"), new IdImpl("1")).getValue());
		assertEquals("Wrong entry from 3 to 2.", 7.0, m.getEntry(new IdImpl("3"), new IdImpl("2")).getValue());
		assertEquals("Wrong entry from 3 to 3.", 7.0, m.getEntry(new IdImpl("3"), new IdImpl("3")).getValue());
		assertEquals("Wrong entry from 3 to 4.", 5.0, m.getEntry(new IdImpl("3"), new IdImpl("4")).getValue());
		assertEquals("Wrong entry from 4 to 1.", 13.0, m.getEntry(new IdImpl("4"), new IdImpl("1")).getValue());
		assertEquals("Wrong entry from 4 to 2.", 21.0, m.getEntry(new IdImpl("4"), new IdImpl("2")).getValue());
		assertEquals("Wrong entry from 4 to 3.", 4.0, m.getEntry(new IdImpl("4"), new IdImpl("3")).getValue());
		assertEquals("Wrong entry from 4 to 4.", 8.0, m.getEntry(new IdImpl("4"), new IdImpl("4")).getValue());
	}
	
	public void testReadLocationCoordinates(){
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.readLocationCoordinates(getClassInputDirectory() + "centroids.txt");
		Map<Id, Coord> lm = mdm.getLocationCoordinates();
		assertEquals("Wrong number of entries.", 4, lm.size());
		assertEquals("Location 1 has the wrong coordinate.", true, lm.get(new IdImpl("1")).getX() == 0.0 && lm.get(new IdImpl("1")).getY() == 1.0);
		assertEquals("Location 2 has the wrong coordinate.", true, lm.get(new IdImpl("2")).getX() == 1.0 && lm.get(new IdImpl("2")).getY() == 1.0);
		assertEquals("Location 3 has the wrong coordinate.", true, lm.get(new IdImpl("3")).getX() == 0.0 && lm.get(new IdImpl("3")).getY() == 0.0);
		assertEquals("Location 4 has the wrong coordinate.", true, lm.get(new IdImpl("4")).getX() == 1.0 && lm.get(new IdImpl("4")).getY() == 0.0);
	}
	
	public void testGenerateExternalDemand(){
		MyDemandMatrix mdm = new MyDemandMatrix();
		mdm.readLocationCoordinates(getClassInputDirectory() + "centroids.txt");
		mdm.parseMatrix(getClassInputDirectory() + "matrix.txt", "dummy", "dummy");
		
		List<Id> list = new ArrayList<Id>(2);
		list.add(new IdImpl("4"));
		Random r = new Random(1234);
		Scenario sc = mdm.generateDemand(list, r, 1.0);
		
		assertEquals("Wrong population size.", 91, sc.getPopulation().getPersons().size());
	}
}
