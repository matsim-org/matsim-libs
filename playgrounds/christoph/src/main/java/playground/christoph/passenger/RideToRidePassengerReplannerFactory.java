/* *********************************************************************** *
 * project: org.matsim.*
 * RideToRidePassengerReplannerFactory.java
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

package playground.christoph.passenger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

import javax.inject.Provider;

public class RideToRidePassengerReplannerFactory extends WithinDayInitialReplannerFactory {

	private final Scenario scenario;
	private final Provider<TripRouter> tripRouterFactory;
	private final RideToRidePassengerContextProvider rideToRidePassengerContextProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	public RideToRidePassengerReplannerFactory(Scenario scenario, WithinDayEngine withinDayEngine,
			Provider<TripRouter> tripRouterFactory, RideToRidePassengerContextProvider rideToRidePassengerContextProvider,
			JointDepartureOrganizer jointDepartureOrganizer) {
		super(withinDayEngine);
		this.scenario = scenario;
		this.tripRouterFactory = tripRouterFactory;
		this.rideToRidePassengerContextProvider = rideToRidePassengerContextProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}

	@Override
	public WithinDayInitialReplanner createReplanner() {
		WithinDayInitialReplanner replanner = new RideToRidePassengerReplanner(super.getId(), scenario, 
				this.getWithinDayEngine().getInternalInterface(),
				this.tripRouterFactory.get(),
				this.rideToRidePassengerContextProvider, this.jointDepartureOrganizer);
		return replanner;
	}

}
