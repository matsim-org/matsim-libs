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

package playground.johannes.gsv.matrices.plans2matrix;

import playground.johannes.synpop.data.ActivityTypes;
import playground.johannes.synpop.data.Attributable;
import playground.johannes.synpop.data.CommonKeys;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 * 
 */
public class ActivityTypePredicate implements Predicate {

	private final String type;

	public ActivityTypePredicate(String type) {
		this.type = type;
	}

	@Override
	public boolean test(PlainPerson person, Attributable leg, Attributable prev, Attributable next) {
		String prevType = prev.getAttribute(CommonKeys.ACTIVITY_TYPE);
		String nextType = next.getAttribute(CommonKeys.ACTIVITY_TYPE);
		if (ActivityTypes.HOME.equalsIgnoreCase(prevType) && type.equalsIgnoreCase(nextType)) {
			return true;
		} else if (ActivityTypes.HOME.equalsIgnoreCase(nextType) && type.equalsIgnoreCase(prevType)) {
			return true;
		} else if (!ActivityTypes.HOME.equalsIgnoreCase(nextType) && !ActivityTypes.HOME.equalsIgnoreCase(prevType)) {
			/*
			 * count only to activity
			 */
			if(type.equalsIgnoreCase(nextType)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

}
