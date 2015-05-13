/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerUnboardingAgentFactory.java
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
package playground.thibautd.socnetsim.jointtrips.qsim;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import playground.thibautd.pseudoqsim.QVehicleProvider;

public class PassengerUnboardingAgentFactory implements AgentFactory, MobsimEngine {
	private final AgentFactory delegate;
	private final QVehicleProvider vehicleProvider;
	private InternalInterface internalInterface = null;

	public PassengerUnboardingAgentFactory(
			final AgentFactory delegate,
			final QVehicleProvider vehicleProvider) {
		this.delegate = delegate;
		this.vehicleProvider = vehicleProvider;
	}

	@Override
	public MobsimAgent createMobsimAgentFromPerson(final Person p) {
		if ( internalInterface == null ) throw new IllegalStateException( "no internal interface" );
		return new PassengerUnboardingDriverAgent(
				delegate.createMobsimAgentFromPerson( p ),
				vehicleProvider,
				internalInterface);
	}

	@Override
	public void doSimStep(double time) {}

	@Override
	public void onPrepareSim() {}

	@Override
	public void afterSim() {}

	@Override
	public void setInternalInterface(final InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}
}
