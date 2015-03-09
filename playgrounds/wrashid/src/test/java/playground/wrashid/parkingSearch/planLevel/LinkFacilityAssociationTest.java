/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.wrashid.parkingSearch.planLevel;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.init.ParkingRoot;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.linkFacilityMapping.LinkParkingFacilityAssociation;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingAttribute;
import playground.wrashid.parkingSearch.planLevel.parkingType.ParkingFacilityAttributes;
import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;

public class LinkFacilityAssociationTest extends MatsimTestCase {

	public void testGeneralFacility() {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(super.loadConfig(null));

		NetworkImpl net = (NetworkImpl) BaseNonControlerScenario.loadNetwork(sc);

		LinkFacilityAssociation lfa = new LinkFacilityAssociation(sc.getActivityFacilities(), net);

		// parking is possible at facility 19

		assertEquals("19", lfa.getFacilities(Id.create("1", Link.class)).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(Id.create("1", Link.class)).size());

		// only leisure is possible at facility 42 => used in the parking test
		// afterwards for verification
		assertEquals("42", lfa.getFacilities(Id.create("50", Link.class)).get(0).getId().toString());
		assertEquals(1, lfa.getFacilities(Id.create("50", Link.class)).size());
		
		// save state of static variable
		ParkingFacilityAttributes tempParkingFacilityAttributes = ParkingRoot.getParkingFacilityAttributes();
		
		ParkingRoot.setParkingFacilityAttributes(new ParkingFacilityAttributes() {
			@Override
			public LinkedList<ParkingAttribute> getParkingFacilityAttributes(Id<ActivityFacility> facilityId) {
				LinkedList<ParkingAttribute> result=new LinkedList<ParkingAttribute>();
				result.add(ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG);
				return result;
			}
		});
		
		assertEquals(1, lfa.getFacilitiesHavingParkingAttribute(Id.create("50", Link.class), ParkingAttribute.HAS_DEFAULT_ELECTRIC_PLUG).size());
		
		// reset static variable
		ParkingRoot.setParkingFacilityAttributes(tempParkingFacilityAttributes);
	}

	public void testParkingFacility() {
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(super.loadConfig(null));

		NetworkImpl net = (NetworkImpl) BaseNonControlerScenario.loadNetwork(sc);

		LinkParkingFacilityAssociation lpfa = new LinkParkingFacilityAssociation(sc.getActivityFacilities(), net);

		// parking is possible at facility 19

		assertEquals("19", lpfa.getFacilities(Id.create("1", Link.class)).get(0).getId().toString());
		assertEquals(1, lpfa.getFacilities(Id.create("1", Link.class)).size());

		// only shopping is possible at facility 42 => no parking available at
		// link 50
		assertEquals(0, lpfa.getFacilities(Id.create("50", Link.class)).size());
	}

}
