/* *********************************************************************** *
 * project: org.matsim.*
 * WithindayCreateVehiclePersonAlgorithm.java
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

package org.matsim.withinday;

import org.matsim.mobsim.PersonAlgo_CreateVehicle;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.withinday.mobsim.OccupiedVehicle;


/**
 * This is used to create the vehicles if withinday replanning should be used.
 * The algorithm is the same as in PersonAlgo_CreateVehicle, however the created vehicles are
 * referenced by this class to be read by the WithindayAgentControler.
 * @author dgrether
 *
 */
public class WithindayCreateVehiclePersonAlgorithm extends PersonAlgo_CreateVehicle {

	private WithindayControler controler;

	/**
	 * For performance reasons the constructor needs the size of the population as only argument.
	 * @param controler
	 */
	public WithindayCreateVehiclePersonAlgorithm(final WithindayControler controler) {
		this.controler = controler;
	}
	/**
	 * @see org.matsim.plans.algorithms.PersonAlgorithm#run(org.matsim.plans.Person)
	 */
	@Override
	public void run(final Person person) {
		Plan plan = person.getSelectedPlan();
		if (plan != null) {
			OccupiedVehicle veh = new OccupiedVehicle();
			veh.setActLegs(plan.getActsLegs());
			veh.setDriverID(person.getId().toString());
			veh.setDriver(person);
			veh.initVeh();
			this.controler.createAgent(person, veh);
		}
	}

}
