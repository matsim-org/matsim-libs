/* *********************************************************************** *
 * project: org.matsim.*
 * DigicoreNetworkRouter.java
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

package playground.southafrica.population.analysis.vkt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class DigicoreNetworkRouterFactory {
	private final Logger LOG = Logger.getLogger(DigicoreNetworkRouterFactory.class);
	private Scenario sc;
	private TravelTime travelTime;
	private PreProcessLandmarks preprocessor;
	
	public DigicoreNetworkRouterFactory(Scenario scenario) {
		LOG.info("Setting up the router...");
		this.sc = scenario;
		
		LOG.info("Processing the network file for travel time calculation");
		TravelDisutility travelCost = new TravelDisutility() {
			@Override
			public double getLinkTravelDisutility(Link link, double time,
					Person person, Vehicle vehicle) {
				return getLinkMinimumTravelDisutility(link);
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		};
		
		LOG.info("Preprocessing the network for travel time calculation.");
		this.preprocessor = new PreProcessLandmarks(travelCost);
		this.preprocessor.run(this.sc.getNetwork());
		
		this.travelTime = new TravelTime() {
			
			@Override
			public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength() / link.getFreespeed();
			}
		};
		
		LOG.info("Router factory prepared.");
	}
	
	/**
	 * Returns the {@link AStarLandmarks} router.
	 * @return
	 */
	public AStarLandmarks createRouter(){
		return new AStarLandmarks(sc.getNetwork(), preprocessor, travelTime);
	}
	
	
	public NetworkImpl getNetwork(){
		return (NetworkImpl) this.sc.getNetwork();
	}

}

