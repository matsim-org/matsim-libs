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

package org.matsim.households;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class HouseholdsIoTest extends MatsimTestCase {

	private static final String TESTHOUSEHOLDSINPUT  = "testHouseholds.xml";
	private static final String TESTXMLOUTPUT  = "testHouseholdsOut.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
  private final Id id42 = new IdImpl("42");
  private final Id id43 = new IdImpl("43");
  private final Id id44 = new IdImpl("44");
  private final Id id45 = new IdImpl("45");
  private PersonImpl p23, p42, p43, p44, p45;
  
	public void testBasicReaderWriter() throws FileNotFoundException, IOException {
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(households);
		reader.readFile(this.getPackageInputDirectory() + TESTHOUSEHOLDSINPUT);
		checkContent(households);
		
		HouseholdsWriterV10 writer = new HouseholdsWriterV10(households);
		String outfilename = this.getOutputDirectory() +  TESTXMLOUTPUT;
		writer.writeFile(outfilename);
		
		File outFile = new File(outfilename);
		assertTrue(outFile.exists());
		
		//read it again to check if the same is read as at the very first beginning of test
		households = new HouseholdsImpl();
		reader = new HouseholdsReaderV10(households);
		reader.readFile(outfilename);
		checkContent(households);
	}
	
	private void checkContent(Households households) {
		assertEquals(2, households.getHouseholds().size());
		Household hh = households.getHouseholds().get(id23);
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
