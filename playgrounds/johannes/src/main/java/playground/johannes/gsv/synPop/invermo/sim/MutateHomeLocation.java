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

import java.util.List;
import java.util.Random;

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.data.FacilityData;

/**
 * @author johannes
 *
 */
public class MutateHomeLocation implements Mutator {

	private List<ActivityFacility> facilities;
	
	private final Random random;
	
	private ActivityFacility old;
	
	public MutateHomeLocation(FacilityData rfacilities, Random random) {
		facilities = rfacilities.getFacilities("home");
		this.random = random;
	}
	
	@Override
	public boolean modify(ProxyPerson person1, ProxyPerson person2) {
		old = (ActivityFacility) person1.getUserData(SwitchHomeLocations.HOME_FACIL_KEY);
		ActivityFacility newFac = facilities.get(random.nextInt(facilities.size()));
		person1.setUserData(SwitchHomeLocations.HOME_FACIL_KEY, newFac);
		return true;
	}

	@Override
	public void revert(ProxyPerson person1, ProxyPerson person2) {
		person1.setUserData(SwitchHomeLocations.HOME_FACIL_KEY, old);
		old = null;
	}

}
