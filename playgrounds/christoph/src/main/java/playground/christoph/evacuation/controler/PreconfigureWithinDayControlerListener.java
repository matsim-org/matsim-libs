/* *********************************************************************** *
 * project: org.matsim.*
 * PreconfigureWithinDayControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.controler;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.router.util.PersonalizedTravelTime;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.withinday.controller.WithinDayControlerListener;

import javax.inject.Provider;
import java.util.Map;

/**
 * Configures the WithinDayControlerListener with objects from the MultiModalControlerListener.
 * this is done after the MultiModalControlerListener has processed the StartupEvent but before
 * the event is processed by the WithinDayControlerListener.
 * 
 * @author cdobler
 */
public class PreconfigureWithinDayControlerListener implements StartupListener {

	private final WithinDayControlerListener withinDayControlerListener;
	private final Provider<Map<String, TravelTime>> multiModalTravelTimes;
	
	public PreconfigureWithinDayControlerListener(WithinDayControlerListener withinDayControlerListener, 
			Provider<Map<String, TravelTime>> multiModalTravelTimes) {
		this.withinDayControlerListener = withinDayControlerListener;
		this.multiModalTravelTimes = multiModalTravelTimes;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		
		/*
		 * Set the TravelTime objects which are used to calculate agents' earliest link
		 * exit times. For car trip, a free speed travel time calculator is used.
		 */
		Map<String, TravelTime> multiModalTravelTimes = this.multiModalTravelTimes.get();
		this.withinDayControlerListener.addMultiModalTravelTimes(multiModalTravelTimes);
		this.withinDayControlerListener.addMultiModalTravelTime(TransportMode.car, new FreeSpeedTravelTime());
		
		// workaround until PT is fully implemented
		TravelTime ptTravelTime = multiModalTravelTimes.get(TransportMode.pt);
		if (ptTravelTime instanceof PersonalizedTravelTime) {
            for (Id personId : event.getControler().getScenario().getPopulation().getPersons().keySet()) {
				((PersonalizedTravelTime) ptTravelTime).setPersonSpeed(personId, 15.0);
			}
		}
	}
}