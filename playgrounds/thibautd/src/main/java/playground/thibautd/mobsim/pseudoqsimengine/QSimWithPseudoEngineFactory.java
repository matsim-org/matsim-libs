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
package playground.thibautd.mobsim.pseudoqsimengine;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.router.util.TravelTime;

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

		final QSim qSim = new QSim(sc, eventsManager);

		final ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);

		final PseudoQsimEngine netsimEngine =
			new PseudoQsimEngine(
					sc.getConfig().qsim().getMainModes(),
					travelTime,
					sc.getNetwork() );
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine);

		qSim.addMobsimEngine( new DefaultQSimEngineFactory().createQSimEngine( qSim ) );

		final TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		//AgentFactory agentFactory;
		if (sc.getConfig().scenario().isUseTransit()) {
			throw new RuntimeException();
			//agentFactory = new TransitAgentFactory(qSim);
			//TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
			//transitEngine.setUseUmlaeufe(true);
			//transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
			//qSim.addDepartureHandler(transitEngine);
			//qSim.addAgentSource(transitEngine);
			//qSim.addMobsimEngine(transitEngine);
		}
		final AgentFactory agentFactory = new DefaultAgentFactory(qSim);

		if (sc.getConfig().network().isTimeVariantNetwork()) {
			qSim.addMobsimEngine(new NetworkChangeEventsEngine());		
		}

		final AgentSource agentSource =
			new AgentSource() {
				@Override
				public void insertAgentsIntoMobsim() {
					for (Person p : sc.getPopulation().getPersons().values()) {
						final MobsimAgent agent = agentFactory.createMobsimAgentFromPerson(p);
						qSim.insertAgentIntoMobsim(agent);
					}
				}
			};
		qSim.addAgentSource(agentSource);

		return qSim;
	}
}

