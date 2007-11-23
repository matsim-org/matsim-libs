/* *********************************************************************** *
 * project: org.matsim.*
 * SimWorld.java
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

package teach.multiagent07.simulation;

import org.matsim.interfaces.networks.trafficNet.TrafficNetI;

public class SimWorld {
	private static int time = 0;
	private static TrafficNetI net;

	public static int getCurrentTime() {
		return time;
	}
	public static TrafficNetI getNet() {
		return net;
	}

}
