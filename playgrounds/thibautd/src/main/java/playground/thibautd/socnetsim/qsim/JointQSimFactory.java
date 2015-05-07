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
package playground.thibautd.socnetsim.qsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;

import com.google.inject.Inject;
import com.google.inject.Provider;

import playground.thibautd.pseudoqsim.NetsimWrappingQVehicleProvider;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class JointQSimFactory implements MobsimFactory, Provider<QSim> {
	private static final Logger log =
		Logger.getLogger(JointQSimFactory.class);
	
	private Scenario sc = null;
	private EventsManager events = null;

	@Inject
	public JointQSimFactory( final Scenario sc, final EventsManager events ) {
		this.sc = sc;
		this.events = events;
	}
	
	public JointQSimFactory() {
		this( null , null );
	}
	
	@Override
	public QSim get() {
		return createMobsim( sc , events );
	}

	@Override
	public QSim createMobsim(
			final Scenario sc1,
			final EventsManager eventsManager) {
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
		final QSim qSim = new QSim( sc1 , eventsManager );

		final ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine( activityEngine );
		qSim.addActivityHandler( activityEngine );

        final QNetsimEngine netsimEngine = new QNetsimEngine(qSim);
		qSim.addMobsimEngine( netsimEngine );
		// DO NOT ADD DEPARTURE HANDLER: it is done by the joint departure handler

		final JointModesDepartureHandler jointDepHandler = new JointModesDepartureHandler( netsimEngine );
		qSim.addDepartureHandler( jointDepHandler );
		qSim.addMobsimEngine( jointDepHandler );

		final TeleportationEngine teleportationEngine = new TeleportationEngine(sc1, eventsManager);
		qSim.addMobsimEngine( teleportationEngine );

        if (sc1.getConfig().scenario().isUseTransit()) {
            final TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        }

		final PassengerUnboardingAgentFactory passAgentFactory =
					new PassengerUnboardingAgentFactory(
						sc1.getConfig().scenario().isUseTransit() ?
							new TransitAgentFactory(qSim) :
							new DefaultAgentFactory(qSim) ,
						new NetsimWrappingQVehicleProvider(
							netsimEngine) );
        final AgentSource agentSource =
			new PopulationAgentSourceWithVehicles(
					sc1.getPopulation(),
					passAgentFactory,
					qSim);
		qSim.addMobsimEngine( passAgentFactory );
        qSim.addAgentSource(agentSource);
        return qSim;
	}
}

