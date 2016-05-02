/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.ivt.analysis.tripstats;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;

import javax.inject.Inject;

/**
 * @author thibautd
 */
public class TripStatisticsModule extends AbstractModule {
	@Override
	public void install() {
		bind(TripStatisticsCollectingEventHandler.class);
		addControlerListenerBinding().to(TripStatisticsCollectingEventHandler.class);

		addEventHandlerBinding()
				.toProvider(
					new Provider<EventHandler>() {
						@Inject Scenario scenario;
						@Inject TripStatisticsCollectingEventHandler legHandler;

						@Override
						public EventHandler get() {
							final EventsToLegs eventsToLegs = new EventsToLegs( scenario );
							eventsToLegs.addLegHandler(legHandler);
							return eventsToLegs;
						}
					} );

		addEventHandlerBinding()
				.toProvider(
					new Provider<EventHandler>() {
						@Inject TripStatisticsCollectingEventHandler activityHandler;

						@Override
						public EventHandler get() {
							final EventsToActivities eventsToActivities = new EventsToActivities( );
							// Awful, just waiting for better design fo eventsToActivities
							eventsToActivities.addActivityHandler(activityHandler);
							activityHandler.setEventsToActivities( eventsToActivities );
							return eventsToActivities;
						}
					} );
	}
}
