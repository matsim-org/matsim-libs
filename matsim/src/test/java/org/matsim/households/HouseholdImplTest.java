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

import org.matsim.api.core.v01.Id;
import org.matsim.testcases.MatsimTestCase;

public class HouseholdImplTest extends MatsimTestCase {
	
	/**
	 * Test that households with the same {@link Id} are not accepted.
	 */
	public void testAddHousehold_DuplicateId(){
		HouseholdsImpl hhs = new HouseholdsImpl();
		Household hh1 = new HouseholdImpl(Id.create("1", Household.class));
		Household hh2 = new HouseholdImpl(Id.create("1", Household.class));
		
		assertEquals("Shouldn't have a household.", 0, hhs.getHouseholds().size());
		hhs.addHousehold(hh1);
		assertEquals("Didn't add the household.", 1, hhs.getHouseholds().size());
		assertEquals("Should have added the household.", hh1, hhs.getHouseholds().get(hh1.getId()));
		try{
			hhs.addHousehold(hh2);
			fail("Should not have accepted household with similar Id.");
		} catch (IllegalArgumentException e){
		}
	}
	

	/**
	 * Test that households are accumulated if streaming is off.
	 */
	public void testAddHousehold_NoStreaming(){
		HouseholdsImpl hhs = new HouseholdsImpl();
		Household hh1 = new HouseholdImpl(Id.create("1", Household.class));
		Household hh2 = new HouseholdImpl(Id.create("2", Household.class));
		
		hhs.addHousehold(hh1);
		assertEquals("Should have the first household added.", 1, hhs.getHouseholds().size());
		assertTrue("First household not present.", hhs.getHouseholds().containsValue(hh1));
		hhs.addHousehold(hh2);
		assertEquals("Should have the first AND second household added.", 2, hhs.getHouseholds().size());
		assertTrue("First household not present.", hhs.getHouseholds().containsValue(hh1));
		assertTrue("Second household not present.", hhs.getHouseholds().containsValue(hh2));
	}

}

