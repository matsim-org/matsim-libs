/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.fare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.Inject;

/**
 * @author jbischoff
 * @author Michal Maciejewski
 * A simple implementation for drt fares.
 * Note that these fares are scored in excess to anything set in the modeparams in the config file.
 */
public class DrtFareHandler implements DrtRequestSubmittedEventHandler, PassengerDroppedOffEventHandler {
	public static PersonMoneyEvent createMoneyEvent(PassengerDroppedOffEvent e, double fee, String reference) {
		return new PersonMoneyEvent(e.getTime(), e.getPersonId(), -fee, PERSON_MONEY_EVENT_PURPOSE_DRT_FARE,
				e.getMode(), reference);
	}

	@Inject
	private EventsManager events;

	public static final String PERSON_MONEY_EVENT_PURPOSE_DRT_FARE = "drtFare";
	public static final String PERSON_MONEY_EVENT_REFERENCE_DRT_FARE_DAILY_FEE = "dailyFee";

	private final double distanceFare_Meter;
	private final double baseFare;
	private final double minFarePerTrip;
	private final double timeFare_sec;
	private final double dailyFee;
	private final Set<Id<Person>> dailyFeeCharged = new HashSet<>();
	private final Map<Id<Request>, DrtRequestSubmittedEvent> requestSubmissions = new HashMap<>();
	private final String mode;

	/**
	 * @params drtFareConfigGroup: DrtFareConfigGroup for the specific mode
	 */
	public DrtFareHandler(String mode, DrtFareParams drtFareParams) {
		this.mode = mode;
		this.distanceFare_Meter = drtFareParams.distanceFare_m;
		this.baseFare = drtFareParams.baseFare;
		this.minFarePerTrip = drtFareParams.minFarePerTrip;
		this.dailyFee = drtFareParams.dailySubscriptionFee;
		this.timeFare_sec = drtFareParams.timeFare_h / 3600.0;
	}

	DrtFareHandler(String mode, DrtFareParams drtFareParams, EventsManager events) {
		this(mode, drtFareParams);
		this.events = events;
	}

	@Override
	public void reset(int iteration) {
		dailyFeeCharged.clear();
		requestSubmissions.clear();
	}

	@Override
	public void handleEvent(PassengerDroppedOffEvent event) {
		if (event.getMode().equals(mode)) {
			if (!dailyFeeCharged.contains(event.getPersonId())) {
				dailyFeeCharged.add(event.getPersonId());
				events.processEvent(createMoneyEvent(event, dailyFee, PERSON_MONEY_EVENT_REFERENCE_DRT_FARE_DAILY_FEE));
			}

			DrtRequestSubmittedEvent submission = requestSubmissions.get(event.getRequestId());
			double fare = distanceFare_Meter * submission.getUnsharedRideDistance()
					+ timeFare_sec * submission.getUnsharedRideTime()
					+ baseFare;
			double actualFare = Math.max(fare, minFarePerTrip);
			events.processEvent(createMoneyEvent(event, actualFare, event.getRequestId() + ""));
		}
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		if (this.mode.equals(event.getMode())) {
			requestSubmissions.put(event.getRequestId(), event);
		}
	}
}
