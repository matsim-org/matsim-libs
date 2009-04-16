/* *********************************************************************** *
 * project: org.matsim.*
 * DriverAgentDepartureTimeComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.queuesim;

import java.util.Comparator;

/**
 * Compares two {@link DriverAgent}s according to their (planned) departure time. If the 
 * departure times are the same, the agent with the higher id is considered smaller.
 *
 * @author mrieser
 * 
 * @see DriverAgent#getDepartureTime()
 */
/*package*/ class DriverAgentDepartureTimeComparator implements Comparator<QueueVehicle> {

	public int compare(QueueVehicle veh1, QueueVehicle veh2) {
		int cmp = Double.compare(veh1.getDriver().getDepartureTime(), veh2.getDriver().getDepartureTime());
		if (cmp == 0) {
			// Both depart at the same time -> let the one with the larger id be first (=smaller)
			return veh2.getDriver().getPerson().getId().compareTo(veh1.getDriver().getPerson().getId());
		}
		return cmp;
	}

}
