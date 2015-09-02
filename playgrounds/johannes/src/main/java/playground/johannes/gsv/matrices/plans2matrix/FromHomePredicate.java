/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.matrices.plans2matrix;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 *
 */
public class FromHomePredicate implements Predicate {

	@Override
	public boolean test(PlainPerson person, Attributable leg, Attributable prev, Attributable next) {
		String prevType = prev.getAttribute(CommonKeys.ACTIVITY_TYPE);
		
		if (ActivityType.HOME.equalsIgnoreCase(prevType)) {
			return true;
		} else {
			return false;
		}
	}

}
