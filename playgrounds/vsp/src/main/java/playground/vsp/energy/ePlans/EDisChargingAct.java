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
package playground.vsp.energy.ePlans;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import playground.vsp.energy.energy.DisChargingProfile;


/**
 * @author droeder
 *
 */
public class EDisChargingAct implements EVehiclePlanElement{

	private Id<Person> personId;
	private Id<DisChargingProfile> profileId;
	
	public EDisChargingAct(Id<DisChargingProfile> profileId, Id<Person> personId){
		this.personId =  personId;
		this.profileId = profileId;
	}

	@Override
	public Id<Person> getPersonId() {
		return this.personId;
	}

	@Override
	public Id<DisChargingProfile> getProfileId() {
		return this.profileId;
	}

}
