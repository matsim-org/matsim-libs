/* *********************************************************************** *
 * project: org.matsim.*
 * PhysicalEngine.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.coopsim.pysical;

import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

/**
 * @author illenberger
 *
 */
public class PhysicalEngine {

	private final PseudoSim pseudoSim;
	
	private final Network network;
	
	private final TravelTime travelTime;
	
	public PhysicalEngine(Network network) {
		this.network = network;
		this.pseudoSim = new PseudoSim();
		this.travelTime = new TravelTimeCalculator(network, 900, 86400, new TravelTimeCalculatorConfigGroup());
	}
	
	public TravelTime getTravelTime() {
		return travelTime;
	}
	
	public void run(Set<Plan> plans, EventsManager eventsManager) {
		pseudoSim.run(plans, network, travelTime, eventsManager);
	}
}
