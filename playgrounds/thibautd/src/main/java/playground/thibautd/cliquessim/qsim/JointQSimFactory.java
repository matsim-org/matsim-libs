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
package playground.thibautd.cliquessim.qsim;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.agents.TransitAgentFactory;
import org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandlerFactory;
import org.matsim.core.mobsim.qsim.pt.TransitQSimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.DefaultQSimEngineFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.thibautd.cliquessim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class JointQSimFactory implements MobsimFactory {

	@Override
	public Mobsim createMobsim(
			final Scenario sc,
			final EventsManager eventsManager) {
        QSimConfigGroup conf = sc.getConfig().getQSimConfigGroup();
        if (conf == null) {
            throw new NullPointerException("There is no configuration set for the QSim. Please add the module 'qsim' to your config file.");
        }

		// never use multi-thread, as the current implementation makes strong
		// assumptions of the order in which function calls are made
		QNetsimEngineFactory netsimEngFactory;
		netsimEngFactory = new DefaultQSimEngineFactory();

		// default initialisation
		QSim qSim = new QSim(sc, new DriverArtifactsSwallower( eventsManager ));
		ActivityEngine activityEngine = new ActivityEngine();
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngine netsimEngine = netsimEngFactory.createQSimEngine(qSim, MatsimRandom.getRandom());
		qSim.addMobsimEngine(netsimEngine);
		qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim.addMobsimEngine(teleportationEngine);

		// set specific engine
		JointTripsEngine jointEngine = new JointTripsEngine( qSim );
		qSim.addDepartureHandler( jointEngine );
		qSim.addMobsimEngine( jointEngine );

		// create agent factory
        AgentFactory agentFactory;
        if (sc.getConfig().scenario().isUseTransit()) {
            agentFactory = new TransitAgentFactory(qSim);
            TransitQSimEngine transitEngine = new TransitQSimEngine(qSim);
            transitEngine.setUseUmlaeufe(true);
            transitEngine.setTransitStopHandlerFactory(new ComplexTransitStopHandlerFactory());
            qSim.addDepartureHandler(transitEngine);
            qSim.addAgentSource(transitEngine);
            qSim.addMobsimEngine(transitEngine);
        }
		else {
            agentFactory = new DefaultAgentFactory(qSim);
        }
        
        AgentSource agentSource =
			new DriverAwarePopulationAgentSource(
					sc.getPopulation(),
					new JointTravelerAgentFactory( agentFactory ),
					qSim);
        qSim.addAgentSource(agentSource);
        return qSim;
	}

	private static class DriverAwarePopulationAgentSource implements AgentSource {
		private final AgentSource source;
		private final Population population;
		private final Collection<String> mainModes;
		private final QSim qsim;

		public DriverAwarePopulationAgentSource(
				final Population pop,
				final AgentFactory fact,
				final QSim qsim) {
			this.source = new PopulationAgentSource(
					pop,
					fact,
					qsim);
			this.population = pop;
			this.mainModes = qsim.getScenario().getConfig().getQSimConfigGroup().getMainMode();
			this.qsim = qsim;
		}

		@Override
		public void insertAgentsIntoMobsim() {
			source.insertAgentsIntoMobsim();

			VehicleType type = VehicleUtils.getDefaultVehicleType();
			popLoop:
			for (Person p : population.getPersons().values()) {
				Id homeLink = null;
				// now, add a vehicle at each location where a driver leg starts,
				// if it is not after another vehicular move (otherwise,
				// problems appear if driver as no individual vehicular leg)
				for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
					if (homeLink == null) {
						homeLink = ((Activity) pe).getLinkId();
					}
					if (pe instanceof Leg) {
						String mode = ((Leg) pe).getMode();

						if (mainModes.contains( mode )) {
							// if the plan is valid, the vehicle will get moved
							// to the pick ups
							continue popLoop;
						}
						else if ( mode.equals( JointActingTypes.DRIVER ) ) {
							qsim.createAndParkVehicleOnLink(
									VehicleUtils.getFactory().createVehicle(
										p.getId(),
										type),
									homeLink);
						}
					}
				}
			}
		}
	}

	private static class DriverArtifactsSwallower implements EventsManager {
		private final EventsManager delegate;

		public DriverArtifactsSwallower(final EventsManager m) {
			delegate = m;
		}

		@Override
		public EventsFactory getFactory() {
			return delegate.getFactory();
		}

		@Override
		public void processEvent(final Event event) {
			if (event instanceof AgentDepartureEvent &&
					((AgentDepartureEvent) event).getLegMode().equals( JointTripsEngine.DRIVER_SIM_MODE )) {
				// gloup! Swallow the event, as otherwise the events stream is inconsistent.
				return;
			}
			delegate.processEvent(event);
		}

		@Override
		public void addHandler(final EventHandler handler) {
			delegate.addHandler(handler);
		}

		@Override
		public void removeHandler(final EventHandler handler) {
			delegate.removeHandler(handler);
		}

		@Override
		public void resetCounter() {
			delegate.resetCounter();
		}

		@Override
		public void resetHandlers(int iteration) {
			delegate.resetHandlers(iteration);
		}

		@Override
		public void clearHandlers() {
			delegate.clearHandlers();
		}

		@Override
		public void printEventsCount() {
			delegate.printEventsCount();
		}

		@Override
		public void printEventHandlers() {
			delegate.printEventHandlers();
		}

		@Override
		public void initProcessing() {
			delegate.initProcessing();
		}

		@Override
		public void afterSimStep(double time) {
			delegate.afterSimStep(time);
		}

		@Override
		public void finishProcessing() {
			delegate.finishProcessing();
		}
	}
}

