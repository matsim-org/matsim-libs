/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
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
package org.matsim.demandmodeling;

import java.io.IOException;

import org.matsim.basic.v01.IdImpl;
import org.matsim.plans.Act;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Plans;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.World;
import org.matsim.world.ZoneLayer;


/**
 * @author dgrether
 *
 */
public class PopulationAsciiFileReaderTest extends MatsimTestCase {

	private static final String filename = "asciipopulation.txt";
	
	private ZoneLayer zoneLayer;
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		World world = new World();
		this.zoneLayer = (ZoneLayer) world.createLayer(new IdImpl("zones"), "noUseForAName");
	  this.zoneLayer.createZone("1", "3", "3", "1", "1", "6", "6", "25", "name");
	  this.zoneLayer.createZone("2", "3", "3", "1", "1", "6", "6", "25", "name");
	}
	
	public void testReadFile() throws IOException {
		String f = this.getClassInputDirectory() + filename;
		PopulationAsciiFileReader p = new PopulationAsciiFileReader(this.zoneLayer);
		Plans plans = p.readFile(f);
		assertNotNull(plans);
		assertEquals(2, plans.getPersons().size());
		Person p1 = plans.getPerson(new IdImpl("1"));
		Person p2 = plans.getPerson(new IdImpl("2"));
		assertNotNull(p1);
		assertNotNull(p2);
		assertEquals(1, p1.getAge());
		assertEquals(99, p2.getAge());
		assertEquals("m", p1.getSex());
		assertEquals("f", p2.getSex());
		Plan plan1 = p1.getSelectedPlan();
		Plan plan2 = p2.getSelectedPlan();
		assertNotNull(plan1);
		assertNotNull(plan2);
		Act a11 = plan1.getFirstActivity();
		Act a12 = plan1.getNextActivity(plan1.getNextLeg(a11));
		assertNotNull(a11);
		assertNotNull(a12);
		assertEquals("h", a11.getType());
		assertEquals("w", a12.getType());
		Act a21 = plan2.getFirstActivity();
		Act a22 = plan2.getNextActivity(plan2.getNextLeg(a21));
		assertNotNull(a21);
		assertNotNull(a22);
		assertEquals("h", a21.getType());
		assertEquals("work", a22.getType());
	}
	
	
	
	

}
