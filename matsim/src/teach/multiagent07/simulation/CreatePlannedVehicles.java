/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePlannedVehicles.java
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

package teach.multiagent07.simulation;

import teach.multiagent07.net.CALink;
import teach.multiagent07.population.Person;
import teach.multiagent07.population.PersonHandler;

public class CreatePlannedVehicles extends PersonHandler {
	private CAMobSim sim;

	public CreatePlannedVehicles (CAMobSim sim) {
		this.sim = sim;
	}
	
	public void handlePerson(Person person) {
		PlannedDriverVehicle driver = new PlannedDriverVehicle(person, sim);
		CALink startLink = (CALink)driver.getDepartureLink();
		startLink.addParking(driver);
	}
}
