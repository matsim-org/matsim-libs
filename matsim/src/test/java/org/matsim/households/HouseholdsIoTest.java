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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Income.IncomePeriod;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicle;

/**
 * @author dgrether
 */
public class HouseholdsIoTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final String TESTHOUSEHOLDSINPUT  = "testHouseholds.xml";
	private static final String TESTXMLOUTPUT  = "testHouseholdsOut.xml";

	private final Id<Person> pid23 = Id.create("23", Person.class);
	private final Id<Person> pid42 = Id.create("42", Person.class);
	private final Id<Person> pid43 = Id.create("43", Person.class);
	private final Id<Person> pid44 = Id.create("44", Person.class);
	private final Id<Person> pid45 = Id.create("45", Person.class);
	private final Id<Vehicle> vid23 = Id.create("23", Vehicle.class);
	private final Id<Vehicle> vid42 = Id.create("42", Vehicle.class);
	private final Id<Household> id23 = Id.create("23", Household.class);
	private final Id<Household> id24 = Id.create("24", Household.class);
	private final Id<Household> id25 = Id.create("25", Household.class);

	@Test
	void testBasicReaderWriter() throws IOException {
		Households households = new HouseholdsImpl();
		HouseholdsReaderV10 reader = new HouseholdsReaderV10(households);
		reader.readFile(utils.getPackageInputDirectory() + TESTHOUSEHOLDSINPUT);
		checkContent(households);

		HouseholdsWriterV10 writer = new HouseholdsWriterV10(households);
		String outfilename = utils.getOutputDirectory() +  TESTXMLOUTPUT;
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
		assertEquals(3, households.getHouseholds().size());
		Household hh = households.getHouseholds().get(id23);
		assertNotNull(hh);
		assertEquals(id23, hh.getId());
		assertEquals(3, hh.getMemberIds().size());
		List<Id<Person>> hhmemberIds = new ArrayList<>();
		hhmemberIds.addAll(hh.getMemberIds());
		Collections.sort(hhmemberIds);
		assertEquals(pid23, hhmemberIds.get(0));
		assertEquals(pid42, hhmemberIds.get(1));
		assertEquals(pid43, hhmemberIds.get(2));

		assertNotNull(hh.getVehicleIds());
		List<Id<Vehicle>> vehIds = new ArrayList<>();
		vehIds.addAll(hh.getVehicleIds());
		Collections.sort(vehIds);
		assertEquals(2, vehIds.size());
		assertEquals(vid23, vehIds.get(0));
		assertEquals(vid42, vehIds.get(1));

		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.month, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(50000.0d, hh.getIncome().getIncome(), MatsimTestUtils.EPSILON);

		Attributes currentAttributes = hh.getAttributes();
		assertNotNull(currentAttributes, "Custom attributes from household with id 23 should not be empty.");
		String customAttributeName = "customAttribute1";
		String customContent = (String)currentAttributes.getAttribute(customAttributeName);
		assertEquals("customValue1", customContent);

		hh = households.getHouseholds().get(id24);
		assertNotNull(hh);
		assertEquals(id24, hh.getId());
		assertEquals(2, hh.getMemberIds().size());
		assertEquals(pid44, hh.getMemberIds().get(0));
		assertEquals(pid45, hh.getMemberIds().get(1));

		assertNotNull(hh.getVehicleIds());
		assertEquals(1, hh.getVehicleIds().size());
		assertEquals(vid23, hh.getVehicleIds().get(0));

		assertNotNull(hh.getIncome());
		assertNotNull(hh.getIncome().getIncomePeriod());
		assertEquals(IncomePeriod.day, hh.getIncome().getIncomePeriod());
		assertEquals("eur", hh.getIncome().getCurrency());
		assertEquals(1000.0d, hh.getIncome().getIncome(), MatsimTestUtils.EPSILON);


		hh = households.getHouseholds().get(id25);
		assertNotNull(hh);
		assertEquals(id25, hh.getId());
		assertEquals(0, hh.getMemberIds().size());

		assertNotNull(hh.getVehicleIds());
		assertEquals(0, hh.getVehicleIds().size());

		assertNull(hh.getIncome());

	}
}
