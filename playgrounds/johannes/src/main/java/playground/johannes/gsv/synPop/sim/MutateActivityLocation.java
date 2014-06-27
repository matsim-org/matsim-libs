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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class MutateActivityLocation implements Mutator, Initializer {

//	private final ActivityFacilities facilities;
	public static final String USER_DATA_KEY = MutateActivityLocation.class.getCanonicalName();
	
	private final String activityType;
	
	private final Random random;
	
	private final List<ActivityFacility> facilities;
	
	public MutateActivityLocation(ActivityFacilities facilities, Random random, String type) {
		this.random = random;
		this.activityType = type;
		
		this.facilities = new ArrayList<ActivityFacility>(facilities.getFacilities().size());
		for(ActivityFacility fac : facilities.getFacilities().values()) {
			for(ActivityOption opt : fac.getActivityOptions().values()) {
				if(opt.getType().equalsIgnoreCase(type)) {
					this.facilities.add(fac);
					break;
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Mutator#mutate(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public boolean mutate(ProxyPerson original, ProxyPerson modified) {
		List<ProxyObject> activities = modified.getPlan().getActivities();
		
		ProxyObject act = activities.get(random.nextInt(activities.size()));
		String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
		
		if(activityType.equalsIgnoreCase(type)) {
			ActivityFacility facility = facilities.get(random.nextInt(facilities.size()));
			act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
			act.setUserData(USER_DATA_KEY, facility);
			return true;
		} else {	
			return false;
		}

	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Initializer#init(playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public void init(ProxyPerson person) {
		for(ProxyObject act : person.getPlan().getActivities()) {
			String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
		
			if(activityType.equalsIgnoreCase(type)) {
				ActivityFacility facility = facilities.get(random.nextInt(facilities.size()));
				act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
				act.setUserData(USER_DATA_KEY, facility);
			}
		}
	}

}
