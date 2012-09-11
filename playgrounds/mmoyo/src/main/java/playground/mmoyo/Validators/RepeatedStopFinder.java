/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.mmoyo.Validators;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

import playground.mmoyo.utils.DataLoader;

/**
 * Looks for repeated stops in a transit route
 */
public class RepeatedStopFinder {
	private TransitSchedule transitSchedule;
	private static final Logger log = Logger.getLogger(RepeatedStopFinder.class);
	
	public RepeatedStopFinder(TransitSchedule transitSchedule){
		this.transitSchedule = transitSchedule;
	}

	public void run(){
		final String ERROR_LOG = " the stop already exist in transit route ";		
		for (TransitLine line : this.transitSchedule.getTransitLines().values()){
			for (TransitRoute route :line.getRoutes().values()){
				List<Id> stopIdList = new ArrayList<Id>();
				log.info(route.getId());
				for (TransitRouteStop stop:  route.getStops()){
					if (stopIdList.contains(stop.getStopFacility().getId())){
						log.error(stop.getStopFacility().getId() + ERROR_LOG +  route.getId() );
					}
					stopIdList.add(stop.getStopFacility().getId());
				}
			}
		}
	}
		
	/**
	 * Returns the minimal distance between two PTLines. This can help the decision of joining them with a Detached Transfer
	 */
	public double getMinimalDistance (final TransitRoute transitRoute1, final TransitRoute transitRoute2){
		double minDistance=0;
		// ->compare distances from first ptline with ever node of secondptline, store the minimal distance
		return minDistance;
	}

	class PseudoTimeCost implements TravelDisutility, TravelTime {

		public PseudoTimeCost() {
		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
			return 1.0;
		}

		@Override
		public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
			return 1.0;
		}
		
		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 1.0;
		}
	}

	public static void main(String[] args) {
		String config = null;

		if (args.length==1){
			config = args[0];
		}else{
			config= "../playgrounds/mmoyo/output/trRoutVis/config.xml";
		}
		ScenarioImpl scenarioImpl = new DataLoader().loadScenario(config);
		new RepeatedStopFinder(scenarioImpl.getTransitSchedule()).run();
	}
	
}