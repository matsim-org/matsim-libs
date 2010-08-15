/* *********************************************************************** *
 * project: org.matsim.*
 * TransitQueueSimulation.java
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

package org.matsim.pt.qsim;


import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.interfaces.QSimI;



public class TransitQSimulation extends QSim implements QSimI {

	
	// Transit has been merged into the QSim. Please just use QSim instead.
	// But remember:
	// scenario.getConfig().scenario().setUseTransit(true);
	// scenario.getConfig().scenario().setUseVehicles(true);
	
	@Deprecated
	public TransitQSimulation(Scenario scenario, EventsManager events) {
		super(scenario, events);	
	}
	
}
