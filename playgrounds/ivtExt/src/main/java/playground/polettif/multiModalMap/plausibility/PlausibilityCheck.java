/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.polettif.multiModalMap.plausibility;

import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import playground.polettif.multiModalMap.tools.NetworkTools;
import playground.polettif.multiModalMap.tools.ScheduleTools;

/**
 * Performs a plausibility check on the given schedule
 * and network.
 *
 * @author polettif
 */
public class PlausibilityCheck {
	
	public static void main(final String[] args) {
		TransitSchedule schedule = ScheduleTools.loadTransitSchedule(args[0]);
		Network network = NetworkTools.loadNetwork(args[1]);
	}
	
}