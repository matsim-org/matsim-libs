/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleDepartureTimeComparator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
 * @author dstrippgen
 *
 * Comparator object, to sort the Vehicle objects in QueueLink.parkingList
 * according to their departure time
 */
public class QueueVehicleEarliestLinkExitTimeComparator implements Comparator<QueueVehicle>,
		Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(final QueueVehicle veh1, final QueueVehicle veh2) {
		if (veh1.getEarliestLinkExitTime() > veh2.getEarliestLinkExitTime()) {
			return 1;
		}
		if (veh1.getEarliestLinkExitTime() < veh2.getEarliestLinkExitTime()) {
			return -1;
		}

		// Both depart at the same time -> let the one with the larger id be first
		return veh2.getId().compareTo(veh1.getId());
	}
}
