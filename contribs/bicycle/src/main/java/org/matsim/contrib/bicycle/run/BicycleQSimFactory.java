/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;

@Deprecated
public class BicycleQSimFactory implements Provider<Mobsim> {
	
	@Inject Map<String, TravelTime> multiModalTravelTimes;
	@Inject Scenario scenario;
	@Inject EventsManager eventsManager;
	@Inject AgentCounter agentCounter;
	@Inject MobsimTimer mobsimTimer;
	
	@Override
	public Mobsim get() {

		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		// construct the QSim:
		QSim qSim = new QSim(scenario, eventsManager, agentCounter, mobsimTimer);

		// add the activity engine:
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, mobsimTimer, agentCounter);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		// add the netsim engine:

		// possible variant without Dobler approach, where link speed calculator is used to influence bicycle speeds:
//		ConfigurableQNetworkFactory qNetworkFactory = new ConfigurableQNetworkFactory(eventsManager, scenario) ;
//		qNetworkFactory.setLinkSpeedCalculator(new LinkSpeedCalculator(){
//			LinkSpeedCalculator delegate = new DefaultLinkSpeedCalculator() ;
//			@Override public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
//				if ( vehicle.getVehicle().getType().equals( "bicycle" ) ) {
//					return MixedTrafficVehiclesUtils.getSpeed("bike"); // compute bicycle speed instead
//				} else {
//					return delegate.getMaximumVelocity(vehicle, link, time) ;
//				}
//			}
//		});
//		QNetsimEngine netsimEngine = new QNetsimEngine(qSim, qNetworkFactory ) ;

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim ) ;
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		AgentFactory agentFactory = new DefaultAgentFactory(qSim);

		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, qSim);
		qSim.addAgentSource(agentSource);

		return qSim ;
	}
}