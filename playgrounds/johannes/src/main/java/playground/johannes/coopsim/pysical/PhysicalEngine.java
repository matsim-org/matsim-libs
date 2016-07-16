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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetgen.sna.util.MultiThreading;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Collection;

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
		this(network, 1.0);
	}
	
	public PhysicalEngine(Network network, double ttFactor) {
		this.network = network;
		this.pseudoSim = new ParallelPseudoSim(MultiThreading.getNumAllowedThreads());
		this.tracker = new VisitorTracker();
		
		this.travelTime = new TravelTimeCalculator(ttFactor);
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
	
	private static class TravelTimeCalculator implements TravelTime {

		private final double factor;
		
		public TravelTimeCalculator(double factor) {
			this.factor = factor;
		}

		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return factor * link.getLength() / link.getFreespeed();
		}
		
	}
}
