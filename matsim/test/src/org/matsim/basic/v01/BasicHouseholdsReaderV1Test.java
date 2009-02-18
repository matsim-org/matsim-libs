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

package org.matsim.basic.v01;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.BasicIncome.IncomePeriod;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 */
public class BasicHouseholdsReaderV1Test extends MatsimTestCase {

  private static final String TESTXML  = "testHouseholds.xml";

  private final Id id23 = new IdImpl("23");
  private final Id id24 = new IdImpl("24");
  private final Id id42 = new IdImpl("42");
  private final Id id43 = new IdImpl("43");
  private final Id id44 = new IdImpl("44");
  private final Id id45 = new IdImpl("45");
  private final Id id666 = new IdImpl("666");
  
	public void testBasicParser() {
		Map<Id, BasicHousehold> households = new HashMap<Id, BasicHousehold>();
		BasicHouseholdsReaderV1 reader = new BasicHouseholdsReaderV1(households);
		reader.readFile(this.getPackageInputDirectory() + TESTXML);
		checkContent(households);
	}
	
	
	public void checkContent(Map<Id, BasicHousehold> households) {
		assertEquals(2, households.size());
		BasicHousehold hh = households.get(id23);
		assertNotNull(hh);
		assertEquals(id23, hh.getId());
		assertEquals(3, hh.getMemberIds().size());
		List<Id> hhmemberIds = hh.getMemberIds();
		Collections.sort(hhmemberIds);
		assertEquals(id23, hhmemberIds.get(0));
		assertEquals(id42, hhmemberIds.get(1));
		assertEquals(id43, hhmemberIds.get(2));

		assertNotNull(hh.getBasicLocation());
		assertNotNull(hh.getBasicLocation().getId()); 
		assertEquals(LocationType.FACILITY, hh.getBasicLocation().getLocationType());
		assertEquals(id666, hh.getBasicLocation().getId());

		assertNotNull(hh.getVehicleIds());
		List<Id> vehIds = hh.getVehicleIds();
		Collections.sort(vehIds);
		assertEquals(2, vehIds.size());
		assertEquals(id23, vehIds.get(0));
		assertEquals(id42, vehIds.get(1));
		
		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.month, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(50000.0d, hh.getIncome().getIncome(), EPSILON);
		
		assertNotNull(hh.getLanguage());
		assertEquals("german", hh.getLanguage());
		
	
		hh = households.get(id24);
		assertNotNull(hh);
		assertEquals(id24, hh.getId());
		assertEquals(2, hh.getMemberIds().size());
		assertEquals(id44, hh.getMemberIds().get(0));
		assertEquals(id45, hh.getMemberIds().get(1));
		
		assertNotNull(hh.getBasicLocation());
		assertNotNull(hh.getBasicLocation().getCenter());
		assertNull(hh.getBasicLocation().getId());
		assertEquals(48.28d, hh.getBasicLocation().getCenter().getX(), EPSILON);
		assertEquals(7.56d, hh.getBasicLocation().getCenter().getY(), EPSILON);

		assertNotNull(hh.getVehicleIds());
		assertEquals(1, hh.getVehicleIds().size());
		assertEquals(id23, hh.getVehicleIds().get(0));

		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.day, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(1000.0d, hh.getIncome().getIncome(), EPSILON);

		assertNull(hh.getLanguage());
	}
}
