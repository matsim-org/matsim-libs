/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.preprocess.plans.planmodificationsTRB09;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.misc.Counter;

public class FacilitiesV3Modifier extends Modifier {

	private final static Logger log = Logger.getLogger(FacilitiesV3Modifier.class);

	public FacilitiesV3Modifier(Population plans, NetworkImpl network, ActivityFacilitiesImpl  facilities){
		super(plans, network, facilities);
	}


	@Override
	public void modify() {
		this.removeInfos();
		this.setFacilitiesV3();
	}

	private void removeInfos() {
		// done in advance with grep at the moment.
		// xml route attribute should be removed completely not
		// just set to null:
		// <route> null </route>
	}

	private void setFacilitiesV3() {
		PersonXY2Facilitychanged personXY2Facility=new PersonXY2Facilitychanged(this.facilities);

		log.info("running FacilitiesV3Modifier:");
		Iterator<? extends Person> person_iter = this.plans.getPersons().values().iterator();
		Counter counter = new Counter(" person # ");
		while (person_iter.hasNext()) {
			Person person = person_iter.next();
				counter.incCounter();

				Iterator<? extends Plan> plan_iter = person.getPlans().iterator();
				while (plan_iter.hasNext()) {
					Plan plan = plan_iter.next();
					personXY2Facility.run(plan);
				}
		}
		log.info("FacilitiesV3Modifier done.");
	}
}
