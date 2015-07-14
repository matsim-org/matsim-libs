/* *********************************************************************** *
 * project: org.matsim.*
 * JointPseudoSimFactory.java
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
package playground.thibautd.socnetsimusages.jointpseudosim.qsim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.qsim.JointModesDepartureHandler;
import org.matsim.contrib.socnetsim.jointtrips.qsim.PassengerUnboardingAgentFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;

import playground.thibautd.pseudoqsim.ParkedVehicleProvider;
import playground.thibautd.pseudoqsim.PopulationAgentSourceForTeleportedVehicles;
import playground.thibautd.pseudoqsim.VehicularTeleportationEngine;

/**
 * @author thibautd
 */
public class JointTeleportationSimFactory implements MobsimFactory {
	private static final Logger log =
		Logger.getLogger(JointPseudoSimFactory.class);

	@Override
	public QSim createMobsim(
			final Scenario sc,
			final EventsManager eventsManager) {
        final QSimConfigGroup conf = sc.getConfig().qsim();
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
		final QSim qSim =
			new QSim(
					sc, eventsManager  );
		
		final ParkedVehicleProvider vehicles = new ParkedVehicleProvider();
		final VehicularTeleportationEngine vehicularEngine =
			new VehicularTeleportationEngine(
					conf.getMainModes(),
					vehicles,
					conf.getVehicleBehavior() );

		final ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine( activityEngine );
		qSim.addActivityHandler( activityEngine );

		//final PseudoSimConfigGroup pSimConf = (PseudoSimConfigGroup)
		//	sc.getConfig().getModule( PseudoSimConfigGroup.GROUP_NAME );

		// DO NOT ADD DEPARTURE HANDLER: it is done by the joint departure handler
		qSim.addMobsimEngine( vehicularEngine );

		final JointModesDepartureHandler jointDepHandler =
			new JointModesDepartureHandler(
					vehicles,
					vehicularEngine );
		qSim.addDepartureHandler( jointDepHandler );
		qSim.addMobsimEngine( jointDepHandler );

		final TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine( teleportationEngine );

        if (sc.getConfig().transit().isUseTransit()) {
			log.warn( "pt trips will be teleported!" );
            //final TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            //transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            //qSim.addDepartureHandler(transitEngine);
            //qSim.addAgentSource(transitEngine);
            //qSim.addMobsimEngine(transitEngine);
        }

		final PassengerUnboardingAgentFactory passAgentFactory =
					new PassengerUnboardingAgentFactory(
						sc.getConfig().transit().isUseTransit() ?
							new TransitAgentFactory(qSim) :
							new DefaultAgentFactory(qSim) ,
						vehicles );
        final AgentSource agentSource =
			new PopulationAgentSourceForTeleportedVehicles(
					sc.getPopulation(),
					passAgentFactory,
					qSim,
					vehicles);
		qSim.addMobsimEngine( passAgentFactory );
        qSim.addAgentSource(agentSource);

		// add  at the end: this must just provide the "service network" functionnality
		//qSim.addMobsimEngine( netsimEngine );

        return qSim;
	}
}

