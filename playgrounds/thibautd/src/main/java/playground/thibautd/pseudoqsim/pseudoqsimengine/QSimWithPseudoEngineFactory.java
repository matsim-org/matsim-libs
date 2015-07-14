/* *********************************************************************** *
 * project: org.matsim.*
 * QSimWithPseudoEngineFactory.java
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
package playground.thibautd.pseudoqsim.pseudoqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
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
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.router.util.TravelTime;
import org.matsim.contrib.socnetsim.qsim.NetsimWrappingQVehicleProvider;
import playground.thibautd.pseudoqsim.PseudoSimConfigGroup;

/**
 * @author thibautd
 */
public class QSimWithPseudoEngineFactory implements MobsimFactory {
	private final TravelTime travelTime;

	public QSimWithPseudoEngineFactory(
			final TravelTime travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public QSim createMobsim(
			final Scenario sc,
			final EventsManager eventsManager) {

		final QSimConfigGroup conf = sc.getConfig().qsim();
		if (conf == null) {
			throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
		}

		final QSim qSim =
			new QSim(
					sc, eventsManager );

		final ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

        final QNetsimEngine netsim = new QNetsimEngine(qSim);

		final PseudoSimConfigGroup pSimConf = (PseudoSimConfigGroup)
			sc.getConfig().getModule( PseudoSimConfigGroup.GROUP_NAME );
		final PseudoQsimEngine pseudoEngine =
			new PseudoQsimEngine(
					pSimConf != null ?
						pSimConf.getNThreads() :
						1,
					sc.getConfig().qsim().getMainModes(),
					travelTime,
					sc.getNetwork(),
					new NetsimWrappingQVehicleProvider(
						netsim ) );
		qSim.addMobsimEngine(pseudoEngine);
		qSim.addDepartureHandler(pseudoEngine);

		qSim.addMobsimEngine( netsim );

		final TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
		qSim.addMobsimEngine(teleportationEngine);

		final AgentFactory agentFactory = 
				sc.getConfig().transit().isUseTransit() ?
					new TransitAgentFactory( qSim ) :
					new DefaultAgentFactory( qSim );

		if (sc.getConfig().transit().isUseTransit()) {
			final TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			qSim.addDepartureHandler(transitEngine);
			qSim.addAgentSource(transitEngine);
			qSim.addMobsimEngine(transitEngine);
		}

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}

		final AgentSource agentSource =
			new PopulationAgentSource(
					sc.getPopulation(),
					agentFactory,
					qSim);
		qSim.addAgentSource(agentSource);

		return qSim;
	}
}

