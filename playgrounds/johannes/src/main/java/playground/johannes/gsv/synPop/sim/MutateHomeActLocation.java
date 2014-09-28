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

import java.util.Random;

import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.data.FacilityData;

/**
 * @author johannes
 *
 */
public class MutateHomeActLocation implements Mutator, Initializer {

//	public static final String USER_DATA_KEY = "homefacility";
	
	private final FacilityData facilities;
	
	public MutateHomeActLocation(ActivityFacilities actFacilities, Random random) {
		facilities = new FacilityData(actFacilities, random);
	}
	
	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Initializer#init(playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public void init(ProxyPerson person) {
		mutate(null, person);

	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim.Mutator#mutate(playground.johannes.gsv.synPop.ProxyPerson, playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public boolean mutate(ProxyPerson original, ProxyPerson modified) {
		ActivityFacility newFacility = facilities.randomFacility("home");
		
//		modified.setUserData(USER_DATA_KEY, newFacility);
		modified.setAttribute(CommonKeys.PERSON_HOME_COORD_X, String.valueOf(newFacility.getCoord().getX()));
		modified.setAttribute(CommonKeys.PERSON_HOME_COORD_Y, String.valueOf(newFacility.getCoord().getY()));
		
		for(ProxyObject act : modified.getPlan().getActivities()) {
			if(act.getAttribute(CommonKeys.ACTIVITY_TYPE).equalsIgnoreCase("home")) {
				act.setAttribute(CommonKeys.ACTIVITY_FACILITY, newFacility.getId().toString());
				act.setUserData(MutateActivityLocation.USER_DATA_KEY, newFacility);
			}
		}
		
		return true;
	}

}
