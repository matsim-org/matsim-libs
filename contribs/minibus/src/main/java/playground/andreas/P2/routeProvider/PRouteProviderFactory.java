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

package playground.andreas.P2.routeProvider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.routeProvider.deprecated.SimpleBackAndForthScheduleProvider;

/**
 * 
 * @author aneumann
 *
 */
public class PRouteProviderFactory {

	private final static Logger log = Logger.getLogger(PRouteProviderFactory.class);

	public static PRouteProvider createRouteProvider(Network network, Population population, PConfigGroup pConfig, TransitSchedule pStopsOnly, String outputDir, EventsManager eventsManager) {

		RandomStopProvider randomStopProvider = new RandomStopProvider(pConfig, population, pStopsOnly, outputDir);

		if(pConfig.getRouteProvider().equalsIgnoreCase(SimpleBackAndForthScheduleProvider.NAME)){
			return new SimpleBackAndForthScheduleProvider(pConfig.getPIdentifier(), pStopsOnly, network, randomStopProvider, 0, pConfig.getVehicleMaximumVelocity(), pConfig.getMode());
		} else if(pConfig.getRouteProvider().equalsIgnoreCase(SimpleCircleScheduleProvider.NAME)){
			return new SimpleCircleScheduleProvider(pConfig.getPIdentifier(), pStopsOnly, network, randomStopProvider, 0, pConfig.getVehicleMaximumVelocity(), pConfig.getMode());
		} else if(pConfig.getRouteProvider().equalsIgnoreCase(ComplexCircleScheduleProvider.NAME)){
			return new ComplexCircleScheduleProvider(pStopsOnly, network, randomStopProvider, 0, pConfig.getVehicleMaximumVelocity(), pConfig.getPlanningSpeedFactor(), pConfig.getMode());
		} else if(pConfig.getRouteProvider().equalsIgnoreCase(TimeAwareComplexCircleScheduleProvider.NAME)){
			return new TimeAwareComplexCircleScheduleProvider(pStopsOnly, network, randomStopProvider, 0, pConfig.getVehicleMaximumVelocity(), pConfig.getPlanningSpeedFactor(), pConfig.getPIdentifier(), eventsManager, pConfig.getMode());
		} else {
			log.error("There is no route provider specified. " + pConfig.getRouteProvider() + " unknown");
			return null;
		}
	}
}
