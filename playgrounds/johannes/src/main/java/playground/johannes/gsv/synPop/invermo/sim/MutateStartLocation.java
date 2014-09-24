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

package playground.johannes.gsv.synPop.invermo.sim;

import java.util.Random;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.ProxyPlan;
import playground.johannes.gsv.synPop.sim.RandomFacilities;
import playground.johannes.gsv.synPop.sim2.Mutator;

/**
 * @author johannes
 *
 */
public class MutateStartLocation implements Mutator {

	public static final Object START_FACILITY_KEY = new Object();
	
	public static final Object FACILITY_KEY = new Object();
	
	private final RandomFacilities facilities;
	
	private ProxyObject[] acts;
	
	private ActivityFacility[] facils;
	
	private int size;
	
	public MutateStartLocation(RandomFacilities facilities, Random random) {
		this.facilities = facilities;
		
		acts = new ProxyObject[10];
		facils = new ActivityFacility[10];
	}
	
	@Override
	public boolean modify(ProxyPerson person) {
	
		size = 0;
		boolean modified = false;
		
		ProxyPlan plan = person.getPlans().get(0);
		int actCount = plan.getActivities().size();
		if(acts.length < actCount) {
			acts = new ProxyObject[actCount];
			facils = new ActivityFacility[actCount];
		}
		
		ActivityFacility newFacility = null;
		
		for(ProxyObject act : plan.getActivities()) {
			Boolean startFacil = ((Boolean)act.getUserData(MutateStartLocation.START_FACILITY_KEY)); 
			if(startFacil != null && startFacil == true) {
				if(newFacility == null) {
					newFacility = facilities.randomFacility(act.getAttribute(CommonKeys.ACTIVITY_TYPE));
				}
				
				act.setAttribute(CommonKeys.ACTIVITY_FACILITY, newFacility.getId().toString());
				act.setUserData(FACILITY_KEY, newFacility);
				
				acts[size] = act;
				facils[size] = newFacility;
				
				size++;
				modified = true;
			}
		}
		
		return modified;
	}

	@Override
	public void revert(ProxyPerson person) {
		for(int i = 0; i < size; i++) {
			ProxyObject act = acts[i];
			ActivityFacility facility = facils[i];
			
			act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
			act.setUserData(FACILITY_KEY, facility);
		}
	}

}
