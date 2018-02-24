/* *********************************************************************** *
 * project: org.matsim.*
 * JointQSimFactory.java
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
package org.matsim.contrib.socnetsim.jointtrips.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class JointQSimFactory implements Provider<QSim> {
	private static final Logger log =
		Logger.getLogger(JointQSimFactory.class);
	
	private final Scenario sc;
	private final EventsManager events;
	private final MobsimTimer mobsimTimer;
	private final AgentCounter agentCounter;
	private final ActiveQSimBridge activeQSimBridge;

	@Inject
	public JointQSimFactory( final Scenario sc, final EventsManager events, final MobsimTimer mobsimTimer, final AgentCounter agentCounter, ActiveQSimBridge activeQSimBridge) {
		this.sc = sc;
		this.events = events;
		this.mobsimTimer = mobsimTimer;
		this.agentCounter = agentCounter;
		this.activeQSimBridge = activeQSimBridge;
	}
	
	public JointQSimFactory() {
		this( null , null, null, null, null );
	}
	
	@Override
	public QSim get() {
		return createMobsim( sc , events, mobsimTimer, agentCounter, activeQSimBridge );
	}

	public QSim createMobsim(
			final Scenario sc1,
			final EventsManager eventsManager, MobsimTimer mobsimTimer, AgentCounter agentCounter, ActiveQSimBridge activeQSimBridge) {
        final QSimConfigGroup conf = sc1.getConfig().qsim();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		if ( !conf.getMainModes().contains( JointActingTypes.DRIVER ) ) {
			log.warn( "adding the driver mode as a main mode in the config at "+getClass()+" initialisation!" );
			final List<String> ms = new ArrayList<String>( conf.getMainModes() );
			ms.add( JointActingTypes.DRIVER );
			conf.setMainModes( ms );
		}

		// default initialisation
		final QSim qSim = new QSim( sc1 , eventsManager, agentCounter, mobsimTimer, activeQSimBridge );

		final ActivityEngine activityEngine = new ActivityEngine(eventsManager, agentCounter, mobsimTimer);
		qSim.addMobsimEngine( activityEngine );
		qSim.addActivityHandler( activityEngine );

        final QNetsimEngine netsimEngine = new QNetsimEngine(sc1.getConfig(), sc1, eventsManager, mobsimTimer, agentCounter);
		qSim.addMobsimEngine( netsimEngine );
		// DO NOT ADD DEPARTURE HANDLER: it is done by the joint departure handler

		final JointModesDepartureHandler jointDepHandler = new JointModesDepartureHandler( netsimEngine );
		qSim.addDepartureHandler( jointDepHandler );
		qSim.addMobsimEngine( jointDepHandler );

		final DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(sc1, eventsManager, mobsimTimer);
		qSim.addMobsimEngine( teleportationEngine );

        if (sc1.getConfig().transit().isUseTransit()) {
            final TransitQSimEngine transitEngine = new TransitQSimEngine(qSim, sc1.getConfig(), sc1, eventsManager, mobsimTimer, agentCounter);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        }

		final PassengerUnboardingAgentFactory passAgentFactory =
					new PassengerUnboardingAgentFactory(
						sc1.getConfig().transit().isUseTransit() ?
							new TransitAgentFactory(sc1, eventsManager, mobsimTimer) :
							new DefaultAgentFactory(sc1, eventsManager, mobsimTimer) ,
						new NetsimWrappingQVehicleProvider(
							netsimEngine), eventsManager, mobsimTimer );
        final AgentSource agentSource =
			new PopulationAgentSourceWithVehicles(
					sc1.getPopulation(),
					passAgentFactory,
					qSim, sc1);
		qSim.addMobsimEngine( passAgentFactory );
        qSim.addAgentSource(agentSource);
        return qSim;
	}
}

