/* *********************************************************************** *
 * project: org.matsim.*
 * MobSimI.java
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

package teach.multiagent07.interfaces;

import org.matsim.interfaces.networks.trafficNet.TrafficNetI;


public interface MobSimI {
	public int getTime();
	public TrafficNetI getNet();
	public EventManagerI getEvents();
	
	public void readNetwork(String filename);
	public void doSim(PopulationManagerI population, EventManagerI events);
	
}
