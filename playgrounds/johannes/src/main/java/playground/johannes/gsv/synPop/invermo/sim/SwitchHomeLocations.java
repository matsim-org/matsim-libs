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

import org.matsim.core.api.experimental.facilities.ActivityFacility;

import playground.johannes.gsv.synPop.ProxyPerson;

/**
 * @author johannes
 *
 */
public class SwitchHomeLocations implements Mutator {

	public static final Object HOME_FACIL_KEY = new Object();
	
	@Override
	public boolean modify(ProxyPerson person1, ProxyPerson person2) {
		return doSwitch(person1, person2);
	}

	@Override
	public void revert(ProxyPerson person1, ProxyPerson person2) {
		doSwitch(person1, person2);

	}
	
	private boolean doSwitch(ProxyPerson person1, ProxyPerson person2) {
		ActivityFacility home1 = (ActivityFacility) person1.getUserData(HOME_FACIL_KEY);
		ActivityFacility home2 = (ActivityFacility) person2.getUserData(HOME_FACIL_KEY);
		
		person1.setUserData(HOME_FACIL_KEY, home2);
		person2.setUserData(HOME_FACIL_KEY, home1);
		
		return true;
	}

}
