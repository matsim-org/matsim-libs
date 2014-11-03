/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.johannes.gsv.sim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * @author johannes
 *
 */
public class MobsimConnectorFactory implements MobsimFactory {

	private static TravelTime calculator;
	
	/* (non-Javadoc)
	 * @see org.matsim.core.mobsim.framework.MobsimFactory#createMobsim(org.matsim.api.core.v01.Scenario, org.matsim.core.api.experimental.events.EventsManager)
	 */
	@Override
	public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
		return new MobsimConnector(sc, eventsManager);
	}
	
	/*
	 * TODO: redesign!
	 */
	public static TravelTime getTravelTimeCalculator(double factor) {
		if(calculator == null) {
			calculator = new TravelTimeCalculator(factor);
		}
		
		return calculator;
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
