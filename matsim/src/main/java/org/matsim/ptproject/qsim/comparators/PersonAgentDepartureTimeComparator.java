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

package org.matsim.ptproject.qsim.comparators;

import java.io.Serializable;
import java.util.Comparator;

import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;

/**
 * Compares two {@link PersonDriverAgent}s according to their (planned) departure time. If the 
 * departure times are the same, the agent with the higher id is considered smaller.
 *
 * @author mrieser
 * 
 * @see PersonDriverAgent#getActivityEndTime()
 */
public class PersonAgentDepartureTimeComparator implements Comparator<PersonAgent>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(PersonAgent agent1, PersonAgent agent2) {
		int cmp = Double.compare(agent1.getActivityEndTime(), agent2.getActivityEndTime());
		if (cmp == 0) {
			// Both depart at the same time -> let the one with the larger id be first (=smaller)
			return agent2.getPerson().getId().compareTo(agent1.getPerson().getId());
		}
		return cmp;
	}

}
