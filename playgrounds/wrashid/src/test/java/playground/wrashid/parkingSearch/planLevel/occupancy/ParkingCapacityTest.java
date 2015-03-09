/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.planLevel.occupancy;

import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingSearch.planLevel.scenario.BaseNonControlerScenario;

public class ParkingCapacityTest extends MatsimTestCase {

	public void testBasic() {
		Config config = super.loadConfig(null);
		ScenarioImpl sc = (ScenarioImpl) ScenarioUtils.createScenario(config);
		BaseNonControlerScenario.loadNetwork(sc);

		ParkingCapacity pc = new ParkingCapacity(sc.getActivityFacilities());

		assertEquals(1, pc.getParkingCapacity(Id.create(8, ActivityFacility.class)));
		assertEquals(5, pc.getParkingCapacity(Id.create(9, ActivityFacility.class)));

	}

}
