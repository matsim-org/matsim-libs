/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAlgo_CreateVehicle.java
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

package org.matsim.mobsim;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
@Deprecated
public class PersonAlgo_CreateVehicle extends PersonAlgorithm {

	@Deprecated
	public PersonAlgo_CreateVehicle() {

	}

	//////////////////////////////////////////////////////////////////////
	// run Method, creates a new Vehicle for every person
	//////////////////////////////////////////////////////////////////////
	@Override
	@Deprecated
	public void run(Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan != null) {
			Vehicle veh = new Vehicle();
			veh.setActLegs(plan.getActsLegs());
			veh.setDriver(person);
			veh.initVeh();
		}
	}

}
