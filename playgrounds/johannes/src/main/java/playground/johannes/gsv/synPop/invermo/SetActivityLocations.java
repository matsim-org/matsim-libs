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

package playground.johannes.gsv.synPop.invermo;

import playground.johannes.gsv.synPop.ProxyPlanTask;
import playground.johannes.synpop.data.Element;
import playground.johannes.synpop.data.Episode;

/**
 * @author johannes
 * 
 */
public class SetActivityLocations implements ProxyPlanTask {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.johannes.gsv.synPop.ProxyPersonTask#apply(playground.johannes
	 * .gsv.synPop.ProxyPerson)
	 */
	@Override
	public void apply(Episode plan) {

		for (int i = 0; i < plan.getLegs().size(); i++) {
			Element leg = plan.getLegs().get(i);

			Element prev = plan.getActivities().get(i);
			Element next = plan.getActivities().get(i + 1);

			String startLoc = leg.getAttribute(InvermoKeys.START_LOCATION);
			if (startLoc != null) {
				if (startLoc.equals(InvermoKeys.HOME)) {
					prev.setAttribute(InvermoKeys.LOCATION, InvermoKeys.HOME);
				} else if (startLoc.equals(InvermoKeys.WORK)) {
					prev.setAttribute(InvermoKeys.LOCATION, InvermoKeys.WORK);
				} else if (startLoc.equals(InvermoKeys.PREV)) {

				}
			}

			String destLoc = leg.getAttribute(InvermoKeys.DESTINATION_LOCATION);
			if (destLoc != null) {
				if (destLoc.equals(InvermoKeys.HOME)) {
					next.setAttribute(InvermoKeys.LOCATION, InvermoKeys.HOME);
				} else if (destLoc.equals(InvermoKeys.WORK)) {
					next.setAttribute(InvermoKeys.LOCATION, InvermoKeys.WORK);
				} else {
					next.setAttribute(InvermoKeys.LOCATION, destLoc);
				}
			}
		}

	}

}
