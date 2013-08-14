/* *********************************************************************** *
 * project: org.matsim.*
 * RideToRidePassengerAgentIdentifierFactory.java
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifierFactory;

public class RideToRidePassengerAgentIdentifierFactory extends InitialIdentifierFactory {

	private final Network network;
	private final MobsimDataProvider mobsimDataProvider;
	private final RideToRidePassengerContextProvider rideToRidePassengerContextProvider;
	private final JointDepartureOrganizer jointDepartureOrganizer;
	
	public RideToRidePassengerAgentIdentifierFactory(Network network, MobsimDataProvider mobsimDataProvider,
			RideToRidePassengerContextProvider rideToRidePassengerContextProvider, JointDepartureOrganizer jointDepartureOrganizer) {
		this.network = network;
		this.mobsimDataProvider = mobsimDataProvider;
		this.rideToRidePassengerContextProvider = rideToRidePassengerContextProvider;
		this.jointDepartureOrganizer = jointDepartureOrganizer;
	}

	@Override
	public InitialIdentifier createIdentifier() {
		return new RideToRidePassengerAgentIdentifier(this.network , this.mobsimDataProvider, 
				this.rideToRidePassengerContextProvider, this.jointDepartureOrganizer);
	}

}
