/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.queuesim;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author dgrether
 */
public class PersonAgentDepartureTimeComparator implements Comparator<PersonAgent>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(PersonAgent pa1, PersonAgent pa2) {
		if (pa1.getDepartureTime() > pa2.getDepartureTime())
			return 1;
		if (pa1.getDepartureTime() < pa2.getDepartureTime())
			return -1;

		// Both depart at the same time -> let the one with the larger id be first
		//TODO this is only due to backwards compatibility: normally the id of the PersonAgent
		//should be used for comparison, however this will need a full change of
		//all checksum reference files
		int veh1id = Integer.parseInt(pa1.getVehicle().getId().toString());
		int veh2id = Integer.parseInt(pa2.getVehicle().getId().toString());

		if (veh1id < veh2id)
			return 1;
		if (veh1id > veh2id)
			return -1;
		return 0;
	}

}
