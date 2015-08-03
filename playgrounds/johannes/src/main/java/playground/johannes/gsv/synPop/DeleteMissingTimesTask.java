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

package playground.johannes.gsv.synPop;

import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.PlainPerson;

/**
 * @author johannes
 *
 */
public class DeleteMissingTimesTask implements ProxyPersonTask {

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.ProxyPersonTask#apply(playground.johannes.synpop.data.PlainPerson)
	 */
	@Override
	public void apply(PlainPerson person) {
		for(Element leg : person.getPlan().getLegs()) {
			String start = leg.getAttribute(CommonKeys.LEG_START_TIME);
			String end = leg.getAttribute(CommonKeys.LEG_END_TIME);
			
			if(start == null && end == null) {
				person.setAttribute(CommonKeys.DELETE, "true");
				return;
			}
		}

	}

}
