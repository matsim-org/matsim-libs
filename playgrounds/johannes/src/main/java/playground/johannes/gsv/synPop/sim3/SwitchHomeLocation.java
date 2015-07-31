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

package playground.johannes.gsv.synPop.sim3;

import org.matsim.facilities.ActivityFacility;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 *
 */
public class SwitchHomeLocation implements SwitchMutator {

	public static final Object USER_FACILITY_KEY = new Object();
	
	@Override
	public boolean mutate(PlainPerson person1, PlainPerson person2) {
		switchFacilities(person1, person2);
		return true;
	}

	@Override
	public void revert(PlainPerson person1, PlainPerson person2) {
		switchFacilities(person1, person2);
	}
	
	private void switchFacilities(PlainPerson person1, PlainPerson person2) {
		ActivityFacility f1 = (ActivityFacility) person1.getUserData(USER_FACILITY_KEY);
		ActivityFacility f2 = (ActivityFacility) person2.getUserData(USER_FACILITY_KEY);
		
		person1.setUserData(USER_FACILITY_KEY, f2);
		person2.setUserData(USER_FACILITY_KEY, f1);
	}

}
