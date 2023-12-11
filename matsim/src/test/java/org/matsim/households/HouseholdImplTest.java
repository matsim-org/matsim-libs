/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestUtils;

public class HouseholdImplTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * Test that households with the same {@link Id} are not accepted.
	 */
	@Test
	void testAddHousehold_DuplicateId(){
		HouseholdsImpl hhs = new HouseholdsImpl();
		Household hh1 = new HouseholdImpl(Id.create("1", Household.class));
		Household hh2 = new HouseholdImpl(Id.create("1", Household.class));

		assertEquals(0, hhs.getHouseholds().size(), "Shouldn't have a household.");
		hhs.addHousehold(hh1);
		assertEquals(1, hhs.getHouseholds().size(), "Didn't add the household.");
		assertEquals(hh1, hhs.getHouseholds().get(hh1.getId()), "Should have added the household.");
		try{
			hhs.addHousehold(hh2);
			fail("Should not have accepted household with similar Id.");
		} catch (IllegalArgumentException e){
		}
	}


	/**
	 * Test that households are accumulated if streaming is off.
	 */
	@Test
	void testAddHousehold_NoStreaming(){
		HouseholdsImpl hhs = new HouseholdsImpl();
		Household hh1 = new HouseholdImpl(Id.create("1", Household.class));
		Household hh2 = new HouseholdImpl(Id.create("2", Household.class));

		hhs.addHousehold(hh1);
		assertEquals(1, hhs.getHouseholds().size(), "Should have the first household added.");
		assertTrue(hhs.getHouseholds().containsValue(hh1), "First household not present.");
		hhs.addHousehold(hh2);
		assertEquals(2, hhs.getHouseholds().size(), "Should have the first AND second household added.");
		assertTrue(hhs.getHouseholds().containsValue(hh1), "First household not present.");
		assertTrue(hhs.getHouseholds().containsValue(hh2), "Second household not present.");
	}

}

