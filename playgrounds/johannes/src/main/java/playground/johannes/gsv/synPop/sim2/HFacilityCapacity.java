/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.sim2;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.sim.MutateActivityLocation;
import playground.johannes.gsv.synPop.sim3.Hamiltonian;

/**
 * @author johannes
 *
 */
public class HFacilityCapacity implements Hamiltonian {

	private final String whitelist;
	
	private final ActivityFacilities facilities;
	
	public HFacilityCapacity(String whitelist, ActivityFacilities facilities) {
		this.whitelist = whitelist;
		this.facilities = facilities;
	}
	
	@Override
	public double evaluate(ProxyPerson person) {
		double sum = 0;
		
		for(int i = 0; i < person.getPlan().getActivities().size(); i++) {
			ProxyObject act = person.getPlan().getActivities().get(i);
			String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
			
			if(whitelist == null || whitelist.equalsIgnoreCase(type)) {
				
				ActivityFacility facility = (ActivityFacility) act.getUserData(MutateActivityLocation.USER_DATA_KEY);
				
				if(facility == null) {
					facility = facilities.getFacilities().get(new IdImpl(act.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
					act.setUserData(MutateActivityLocation.USER_DATA_KEY, facility);
				}
				
				
				ActivityOption option = facility.getActivityOptions().get(type);
				
				sum += option.getCapacity();
			}
		}
		 
		return -sum;
	}

}
