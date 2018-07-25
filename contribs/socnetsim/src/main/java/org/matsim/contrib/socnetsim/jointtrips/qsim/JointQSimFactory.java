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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.DefaultTeleportationEngine;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentsConfigurator;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;

import com.google.inject.Inject;
import com.google.inject.Provider;

import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.sharedvehicles.qsim.PopulationAgentSourceWithVehicles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author thibautd
 */
public class JointQSimFactory implements MobsimFactory, Provider<QSim> {
	private static final Logger log =
		Logger.getLogger(JointQSimFactory.class);
	
	private final Scenario sc;
	private final EventsManager events;
	private final Config config;

	@Inject
	public JointQSimFactory( final Scenario sc, final EventsManager events, final Config config ) {
		this.sc = sc;
		this.events = events;
		this.config = config;
	}
	
	public JointQSimFactory() {
		this( null , null, ConfigUtils.createConfig() );
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

		/* // default initialisation
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

		final DefaultTeleportationEngine teleportationEngine = new DefaultTeleportationEngine(sc1, eventsManager);
		qSim.addMobsimEngine( teleportationEngine );

        if (sc1.getConfig().transit().isUseTransit()) {
            final TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        }

		final PassengerUnboardingAgentFactory passAgentFactory =
					new PassengerUnboardingAgentFactory(
						sc1.getConfig().transit().isUseTransit() ?
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
        qSim.addAgentSource(agentSource);*/
		
		final Collection<AbstractQSimPlugin> plugins = new ArrayList<>();
		plugins.add(new MessageQueuePlugin(config));
		plugins.add(new ActivityEnginePlugin(config));
		plugins.add(new QNetsimEnginePlugin(config));
		if (config.network().isTimeVariantNetwork()) {
			plugins.add(new NetworkChangeEventsPlugin(config));
		}
		if (config.transit().isUseTransit() && config.transit().isUsingTransitInMobsim() ) {
			plugins.add(new TransitEnginePlugin(config));
		}
		plugins.add(new TeleportationPlugin(config));
		plugins.add(new PopulationPlugin(config));
		plugins.add(new JointQSimPlugin(config));
		
		List<AbstractModule> modules = Collections.singletonList(new AbstractModule() {
			@Override
			public void install() {
				QSimComponents components = new QSimComponents();
				new StandardQSimComponentsConfigurator(config).configure(components);
				
				components.activeDepartureHandlers.clear();
				components.activeDepartureHandlers.add(JointQSimPlugin.JOINT_MODES_DEPARTURE_HANDLER);
				
				components.activeMobsimEngines.add(JointQSimPlugin.JOINT_MODES_DEPARTURE_HANDLER);
				components.activeMobsimEngines.add(JointQSimPlugin.JOINT_PASSENGER_UNBOARDING);
				
				components.activeAgentSources.clear();
				components.activeAgentSources.add(JointQSimPlugin.AGENTS_SOURCE_WITH_VEHICLES);
				
				bind(QSimComponents.class).toInstance(components);
			}
		});
		
		QSim qSim = QSimUtils.createQSim(sc1, eventsManager, modules, plugins);
        return qSim;
	}
}

