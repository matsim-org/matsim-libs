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

package playground.kai.usecases.ownmobsim;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public class DriverVehicleUnit {

	private Person originalPerson;
	private Id currentLinkId;
	private int idx=0 ;

	DriverVehicleUnit(Person person) {
		this.originalPerson = person ;
		Plan plan = person.getSelectedPlan() ;
		Activity act = (Activity) plan.getPlanElements().get(this.idx) ;
		this.currentLinkId = act.getLinkId() ;
		Assert.assertNotNull(this.currentLinkId) ;
	}
	
	Id getCurrentLinkId() {
		return this.currentLinkId ;
	}
	
	double getCurrentActivityEndTime() {
		Activity act = (Activity) originalPerson.getSelectedPlan().getPlanElements().get(this.idx) ;
		return act.getEndTime() ;
	}

	 Id getNextLinkId() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
