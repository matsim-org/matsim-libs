/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package org.matsim.contrib.wagonSim.mobsim.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.wagonSim.mobsim.qsim.agents.WagonSimAgentFactory;
import org.matsim.contrib.wagonSim.mobsim.qsim.framework.listeners.WagonSimVehicleLoadListener;
import org.matsim.contrib.wagonSim.mobsim.qsim.pt.WagonSimTransitStopHandlerFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.AgentCounterImpl;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.utils.objectattributes.ObjectAttributes;


/**
 * @author droeder
 *
 */
public class WagonSimQSimFactory implements MobsimFactory {

	private static final Logger log = Logger
			.getLogger(WagonSimQSimFactory.class);
	private ObjectAttributes vehicleLinkSpeedAttributes;
	private WagonSimVehicleLoadListener vehicleLoadListener;

	/**
	 * Instead of {@link TransitAgentFactory}
	 * a custom factory is used ({@link WagonSimAgentFactory}. 
	 */
	public WagonSimQSimFactory(ObjectAttributes vehicleLinkSpeedAttributes, WagonSimVehicleLoadListener vehicleLoadListener){
		this.vehicleLinkSpeedAttributes = vehicleLinkSpeedAttributes; 
		this.vehicleLoadListener = vehicleLoadListener;
	}
	
	// actually this method is more or less c&p from QSimFactory but I can not see another way
	// to plug an own AgentFactory to the simulation.
	// Although it is possible to plug another AgentSource to the QSim it is not possible to remove
	// those that are inserted here. Hence, QSim will try to create TransitAgents AND 
	//	WagonSimAgents. 
	@Override
	public Mobsim createMobsim(Scenario scenario, EventsManager eventsManager) {

		QSimConfigGroup conf = scenario.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}
		
		MobsimTimer mobsimTimer = new MobsimTimer(scenario.getConfig());
		AgentCounter agentCounter = new AgentCounterImpl();

		QSim qSim = new QSim(scenario, eventsManager, agentCounter, mobsimTimer);
		ActivityEngine activityEngine = new ActivityEngine(eventsManager, agentCounter, mobsimTimer);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		//
		ConfigurableQNetworkFactory factory = new ConfigurableQNetworkFactory( eventsManager, scenario ) ;
		// add the vehicleLinkSpeedCalculator
		factory.setLinkSpeedCalculator(new LocomotiveLinkSpeedCalculator(vehicleLinkSpeedAttributes));
		QNetsimEngine netsimEngine = new QNetsimEngine(factory, scenario.getConfig(), scenario, eventsManager, mobsimTimer, agentCounter);
		//
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(scenario, eventsManager, mobsimTimer);
		qSim.addMobsimEngine(teleportationEngine);

		// use an own TransitStopHandlerFactory here
		AgentFactory agentFactory = new TransitAgentFactory(scenario, eventsManager, mobsimTimer);
		TransitQSimEngine transitEngine = new TransitQSimEngine(qSim, scenario.getConfig(), scenario, eventsManager, mobsimTimer, agentCounter);
		// use an own transitStopHandler.
		transitEngine.setTransitStopHandlerFactory(new WagonSimTransitStopHandlerFactory(vehicleLoadListener,
				scenario.getPopulation().getPersonAttributes(),
				vehicleLinkSpeedAttributes));
		qSim.addDepartureHandler(transitEngine);
		qSim.addAgentSource(transitEngine);
		qSim.addMobsimEngine(transitEngine);
		if (scenario.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine(scenario.getNetwork(), netsimEngine.getNetsimNetwork()));		
		}
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, scenario.getConfig(), scenario, qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}
	
	static class LocomotiveLinkSpeedCalculator implements LinkSpeedCalculator {

		//////////////////////////////////////////////////////////////////////

		private final ObjectAttributes vehicleLinkSpeedAttributes;
		
		//////////////////////////////////////////////////////////////////////

		LocomotiveLinkSpeedCalculator(final ObjectAttributes vehicleLinkSpeedAttributes) {
			this.vehicleLinkSpeedAttributes = vehicleLinkSpeedAttributes;
		}

		//////////////////////////////////////////////////////////////////////

		@Override
		public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
			Object obj = (Double)vehicleLinkSpeedAttributes.getAttribute(vehicle.getId().toString(),link.getId().toString());
			if (obj == null) { throw new RuntimeException("time="+time+",vId="+vehicle.getId()+",lId="+link.getId()+": no speed defined in vehicleLinkSpeedAttributes. Bailing out."); }
			return (Double)obj;
		}
	}
}

