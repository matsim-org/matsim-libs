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

package org.matsim.mobsim;

import java.io.Serializable;
import java.util.Comparator;

/**
 * @author dstrippgen
 *
 * Comparator object, to sort the Vehicle objects in QueueLink.parkingList according
 * to their departure time
 */
class VehicleDepartureTimeComparator implements Comparator<Vehicle>, Serializable {
	private static final long serialVersionUID = 1L;

	public int compare(final Vehicle veh1, final Vehicle veh2) {
		if (veh1.getDepartureTime_s() > veh2.getDepartureTime_s())
			return 1;
		if (veh1.getDepartureTime_s() < veh2.getDepartureTime_s())
			return -1;

		// Both depart at the same time -> let the one with the larger id be first
		if (veh1.getID() < veh2.getID())
			return 1;
		if (veh1.getID() > veh2.getID())
			return -1;
		return 0;
	}
}
