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

package playground.johannes.gsv.synPop.sim;

import java.util.Collection;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;

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
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public double evaluate(ProxyPerson original, ProxyPerson modified) {
		double sum = 0;
		
		for(int i = 0; i < modified.getPlan().getActivities().size(); i++) {
			ProxyObject newAct = modified.getPlan().getActivities().get(i);
			String type = newAct.getAttribute(CommonKeys.ACTIVITY_TYPE);
			
			if(whitelist == null || whitelist.equalsIgnoreCase(type)) {
				ProxyObject oldAct = original.getPlan().getActivities().get(i);
				
				ActivityFacility newFac = (ActivityFacility) newAct.getUserData(MutateActivityLocation.USER_DATA_KEY);
				ActivityFacility oldFac = (ActivityFacility) oldAct.getUserData(MutateActivityLocation.USER_DATA_KEY);
				
				if(newFac == null) {
					newFac = facilities.getFacilities().get(new IdImpl(newAct.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
					newAct.setUserData(MutateActivityLocation.USER_DATA_KEY, newFac);
				}
				
				if(oldFac == null) { // i think this shouldn't ever happen
					oldFac = facilities.getFacilities().get(new IdImpl(oldAct.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
					oldAct.setUserData(MutateActivityLocation.USER_DATA_KEY, oldFac);
				}
				
				ActivityOption newOpt = newFac.getActivityOptions().get(type);
				ActivityOption oldOpt = oldFac.getActivityOptions().get(type);
				
				sum += newOpt.getCapacity() - oldOpt.getCapacity();
			}
		}
		 
		return sum;
	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Hamiltonian#evaluate(java.util.Collection)
	 */
	@Override
	public double evaluate(Collection<ProxyPerson> persons) {
		double sum = 0;
		
		for(ProxyPerson person : persons) {
			for(ProxyObject act : person.getPlan().getActivities()) {
				String type = act.getAttribute(CommonKeys.ACTIVITY_TYPE);
				if(whitelist == null || type.equalsIgnoreCase(whitelist)) {
					ActivityFacility fac = (ActivityFacility) act.getUserData(MutateActivityLocation.USER_DATA_KEY);
					sum += fac.getActivityOptions().get(type).getCapacity();
				}
			}
		}
		
		return sum;
	}

}
