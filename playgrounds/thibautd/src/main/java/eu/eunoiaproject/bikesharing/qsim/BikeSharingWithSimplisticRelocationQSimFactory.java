/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingWithSimplisticRelocationQSimFactory.java
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
package eu.eunoiaproject.bikesharing.qsim;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacilities;

/**
 * @author thibautd
 */
public class BikeSharingWithSimplisticRelocationQSimFactory implements MobsimFactory {
	private final int nVehicles;

	public  BikeSharingWithSimplisticRelocationQSimFactory(
			final int nVehicles ) {
		this.nVehicles = nVehicles;
	}

	@Override
	public QSim createMobsim(
			final Scenario sc,
			final EventsManager eventsManager) {
		final QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		final QNetsimEngineFactory netsimEngFactory = new DefaultQSimEngineFactory();
	
		final QSim qSim = new QSim( sc, eventsManager );

		final QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim);
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());

		// ---------------------------------------------------------------------
		// Here is the only modified part.
		// The rest is just re-organized according to my obsessions.
		final BikeSharingManager bikeSharingManager =
			new BikeSharingManager(
					(BikeSharingFacilities) sc.getScenarioElement(
						BikeSharingFacilities.ELEMENT_NAME ) );
		final BikeSharingEngine bikeSharingEngine =
			new BikeSharingEngine(
					bikeSharingManager );
		qSim.addDepartureHandler( bikeSharingEngine );
		qSim.addMobsimEngine( bikeSharingEngine );

		bikeSharingManager.addListener( new FacilityStateChangeRepeater( qSim ) );

		final SimplisticRelocatorManagerEngine relocatorManager =
			new SimplisticRelocatorManagerEngine(
					nVehicles,
					sc.getNetwork(),
					bikeSharingManager );
		qSim.addMobsimEngine( relocatorManager );
		qSim.addActivityHandler( relocatorManager );
		// here ends the modified part.
		//
		// A relocation strategy could be added here,
		// for instance as a BikeSharingManagerListener,
		// or even as agents driving around to relocate bikes.
		// ---------------------------------------------------------------------

		// this needs to be added AFTER the relocator manager
		final ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);



		final TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		final AgentFactory agentFactory = createAgentFactory( qSim , sc );

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}

		final PopulationAgentSource agentSource =
			new PopulationAgentSource(
					sc.getPopulation(),
					agentFactory,
					qSim);
		qSim.addAgentSource(agentSource);
		return qSim;
	}

	private static AgentFactory createAgentFactory(
			final QSim qSim,
			final Scenario sc) {
		if (sc.getConfig().scenario().isUseTransit()) {
			final AgentFactory agentFactory = new TransitAgentFactory(qSim);
			TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setUseUmlaeufe(true);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
			return agentFactory;
		}

		return new DefaultAgentFactory(qSim);
	}

}

