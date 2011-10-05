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

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.sna.util.MultiThreading;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorConfigGroup;

/**
 * @author illenberger
 *
 */
public class PhysicalEngine {

	private final ParallelPseudoSim pseudoSim;
	
	private final Network network;
	
	private final TravelTime travelTime;
	
	private final VisitorTracker tracker;
	
	public PhysicalEngine(Network network) {
		this.network = network;
//		this.pseudoSim = new PseudoSim();
		this.pseudoSim = new ParallelPseudoSim(MultiThreading.getNumAllowedThreads());
		this.travelTime = new TravelTimeCalculator(network, 900, 86400, new TravelTimeCalculatorConfigGroup());
		this.tracker = new VisitorTracker();
	}
	
	public TravelTime getTravelTime() {
		return travelTime;
	}
	
	public VisitorTracker getVisitorTracker() {
		return tracker;
	}
	
	public void run(Collection<Plan> plans, EventsManager eventsManager) {
		eventsManager.addHandler(tracker);
		tracker.reset(0);
		
		pseudoSim.run(plans, network, travelTime, eventsManager);
		
		eventsManager.removeHandler(tracker);
	}
	
	public void finalize() throws Throwable {
		super.finalize();
		pseudoSim.finalize();
	}
}
