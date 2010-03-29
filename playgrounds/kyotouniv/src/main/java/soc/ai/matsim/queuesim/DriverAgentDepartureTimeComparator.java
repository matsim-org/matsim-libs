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

package soc.ai.matsim.queuesim;

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares two {@link DriverAgent}s according to their (planned) departure time. If the 
 * departure times are the same, the agent with the higher id is considered smaller.
 *
 * @author mrieser
 * 
 * @see DriverAgent#getDepartureTime()
 */
/*package*/ class DriverAgentDepartureTimeComparator implements Comparator<DriverAgent>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(DriverAgent agent1, DriverAgent agent2) {
		int cmp = Double.compare(agent1.getDepartureTime(), agent2.getDepartureTime());
		if (cmp == 0) {
			// Both depart at the same time -> let the one with the larger id be first (=smaller)
			return agent2.getPerson().getId().compareTo(agent1.getPerson().getId());
		}
		return cmp;
	}

}
