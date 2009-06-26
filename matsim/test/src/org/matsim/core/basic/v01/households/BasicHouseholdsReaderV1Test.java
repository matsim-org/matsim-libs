/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.basic.v01.households;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.basic.v01.households.BasicIncome.IncomePeriod;
import org.matsim.core.basic.v01.vehicles.BasicVehicleType;
import org.matsim.core.basic.v01.vehicles.BasicVehicles;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class BasicHouseholdsReaderV1Test extends MatsimTestCase {

	private static final String TESTHOUSEHOLDSINPUT  = "testHouseholds.xml";
	private static final String TESTXMLOUTPUT  = "testHouseholdsOut.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
  private final Id id42 = new IdImpl("42");
  private final Id id43 = new IdImpl("43");
  private final Id id44 = new IdImpl("44");
  private final Id id45 = new IdImpl("45");
  private Person p23, p42, p43, p44, p45;
  
	public void testBasicReaderWriter() throws FileNotFoundException, IOException {
		BasicHouseholds<BasicHousehold> households = new BasicHouseholdsImpl();
		BasicHouseholdsReaderV10 reader = new BasicHouseholdsReaderV10(households);
		reader.readFile(this.getPackageInputDirectory() + TESTHOUSEHOLDSINPUT);
		checkContent(households);
		
		HouseholdsWriterV1 writer = new HouseholdsWriterV1(households);
		String outfilename = this.getOutputDirectory() +  TESTXMLOUTPUT;
		writer.writeFile(outfilename);
		
		File outFile = new File(outfilename);
		assertTrue(outFile.exists());
		
		//read it again to check if the same is read as at the very first beginning of test
		households = new BasicHouseholdsImpl();
		reader = new BasicHouseholdsReaderV10(households);
		reader.readFile(outfilename);
		checkContent(households);
	}
	
	public void testReaderWriter() {
		ScenarioImpl scenario = new ScenarioImpl();
		scenario.getConfig().scenario().setUseHouseholds(true);
		scenario.getConfig().scenario().setUseVehicles(true); 
		createTestPopulation(scenario);
		createTestVehicles(scenario);
		
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(scenario);
		reader.readFile(this.getPackageInputDirectory() + TESTHOUSEHOLDSINPUT);
		checkContent(scenario.getHouseholds());
		checkReferencedContent(scenario.getHouseholds());
	}
	
	private void createTestVehicles(ScenarioImpl scenario) {
		BasicVehicles v = scenario.getVehicles();
		BasicVehicleType defaultType = v.getBuilder().createVehicleType(new IdImpl("default"));
		v.getVehicles().put(id23, v.getBuilder().createVehicle(id23, defaultType));
		v.getVehicles().put(id42, v.getBuilder().createVehicle(id42, defaultType));
	}

	private void createTestPopulation(ScenarioImpl scenario) {
		Population pop = scenario.getPopulation();
		p23 = pop.getPopulationBuilder().createPerson(id23);
		p42 = pop.getPopulationBuilder().createPerson(id42);
		p43 = pop.getPopulationBuilder().createPerson(id43);
		p44 = pop.getPopulationBuilder().createPerson(id44);
		p45 = pop.getPopulationBuilder().createPerson(id45);
		pop.getPersons().put(id23, p23);
		pop.getPersons().put(id42, p42);
		pop.getPersons().put(id43, p43);
		pop.getPersons().put(id44, p44);
		pop.getPersons().put(id45, p45);
	}

	private void checkReferencedContent(Households households) {
		Household hh23 = households.getHouseholds().get(id23);
		assertNotNull(hh23);
		assertEquals(p23, hh23.getMembers().get(id23));
		assertEquals(p23.getHousehold(), hh23);
		assertEquals(p43, hh23.getMembers().get(id43));
		assertEquals(p43.getHousehold(), hh23);
		
		Household hh24 = households.getHouseholds().get(id24);
		assertNotNull(hh24);
		assertEquals(p44, hh24.getMembers().get(id44));
		assertEquals(p44.getHousehold(), hh24);
		assertEquals(p45, hh24.getMembers().get(id45));
		assertEquals(p45.getHousehold(), hh24);
	}

	private void checkContent(BasicHouseholds<? extends BasicHousehold> households) {
		assertEquals(2, households.getHouseholds().size());
		BasicHousehold hh = households.getHouseholds().get(id23);
		assertNotNull(hh);
		assertEquals(id23, hh.getId());
		assertEquals(3, hh.getMemberIds().size());
		List<Id> hhmemberIds = new ArrayList<Id>();
		hhmemberIds.addAll(hh.getMemberIds());
		Collections.sort(hhmemberIds);
		assertEquals(id23, hhmemberIds.get(0));
		assertEquals(id42, hhmemberIds.get(1));
		assertEquals(id43, hhmemberIds.get(2));


		assertNotNull(hh.getVehicleIds());
		List<Id> vehIds = new ArrayList<Id>();
		vehIds.addAll(hh.getVehicleIds());
		Collections.sort(vehIds);
		assertEquals(2, vehIds.size());
		assertEquals(id23, vehIds.get(0));
		assertEquals(id42, vehIds.get(1));
		
		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.month, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(50000.0d, hh.getIncome().getIncome(), EPSILON);
	
		hh = households.getHouseholds().get(id24);
		assertNotNull(hh);
		assertEquals(id24, hh.getId());
		assertEquals(2, hh.getMemberIds().size());
		assertEquals(id44, hh.getMemberIds().get(0));
		assertEquals(id45, hh.getMemberIds().get(1));
		
		assertNotNull(hh.getVehicleIds());
		assertEquals(1, hh.getVehicleIds().size());
		assertEquals(id23, hh.getVehicleIds().get(0));

		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.day, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(1000.0d, hh.getIncome().getIncome(), EPSILON);
	}
}
