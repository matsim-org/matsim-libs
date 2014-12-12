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

package playground.johannes.gsv.matrices;

import playground.johannes.gsv.synPop.ActivityType;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.sim3.ReplaceActTypes;

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
	public boolean test(ProxyObject leg, ProxyObject prev, ProxyObject next) {
		String prevType = prev.getAttribute(ReplaceActTypes.ORIGINAL_TYPE);
		String nextType = next.getAttribute(ReplaceActTypes.ORIGINAL_TYPE);
		if (ActivityType.HOME.equalsIgnoreCase(prevType) && type.equalsIgnoreCase(nextType)) {
			return true;
		} else if (ActivityType.HOME.equalsIgnoreCase(nextType) && type.equalsIgnoreCase(prevType)) {
			return true;
		} else {
			return false;
		}
	}

}
