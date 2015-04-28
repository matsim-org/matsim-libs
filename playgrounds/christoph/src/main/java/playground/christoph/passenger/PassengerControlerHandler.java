/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerControlerHandler.java
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

package playground.christoph.passenger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.simengine.MultiModalQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.JointDepartureOrganizer;
import org.matsim.core.mobsim.qsim.qnetsimengine.MissedJointDepartureWriter;
import org.matsim.core.mobsim.qsim.qnetsimengine.PassengerQNetsimEngine;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.withinday.controller.WithinDayControlerListener;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;

import javax.inject.Provider;
import java.util.Map;

public class PassengerControlerHandler implements StartupListener {

	private final WithinDayControlerListener withinDayControlerListener;
	private final Provider<Map<String, TravelTime>> multiModalTravelTimes;

	public PassengerControlerHandler(WithinDayControlerListener withinDayControlerListener,
			Provider<Map<String, TravelTime>> multiModalControlerListener) {
		this.withinDayControlerListener = withinDayControlerListener;
		this.multiModalTravelTimes = multiModalControlerListener;
		this.withinDayControlerListener.setModesAnalyzedByTravelTimeCollector(CollectionUtils.stringToSet(TransportMode.car));
	}

	@Override
	public void notifyStartup(final StartupEvent event) {
		
		JointDepartureOrganizer jointDepartureOrganizer = new JointDepartureOrganizer();
		MissedJointDepartureWriter jointDepartureWriter = new MissedJointDepartureWriter(jointDepartureOrganizer);
		event.getControler().addControlerListener(jointDepartureWriter);
		
		RideToRidePassengerContextProvider rideToRidePassengerContextProvider = new RideToRidePassengerContextProvider();

        RideToRidePassengerAgentIdentifierFactory identifierFactory =
				new RideToRidePassengerAgentIdentifierFactory(event.getControler().getScenario().getNetwork(),
						this.withinDayControlerListener.getMobsimDataProvider(), 
						rideToRidePassengerContextProvider, jointDepartureOrganizer);
		InitialIdentifier identifier = identifierFactory.createIdentifier();
		
//		RoutingContext routingContext = new RoutingContextImpl(event.getControler().createTravelDisutilityCalculator(), 
//				event.getControler().getLinkTravelTimes());
//		TravelTime travelTime = new FreeSpeedTravelTime();
//		TravelDisutility travelDisutility = event.getControler().getTravelDisutilityFactory().createTravelDisutility(travelTime, 
//				event.getControler().getConfig().planCalcScore());
//		RoutingContext routingContext = new RoutingContextImpl(travelDisutility, travelTime);
		
		RideToRidePassengerReplannerFactory replannerFactory = new RideToRidePassengerReplannerFactory(
				event.getControler().getScenario(), this.withinDayControlerListener.getWithinDayEngine(), 
				event.getControler().getTripRouterProvider(), rideToRidePassengerContextProvider, jointDepartureOrganizer);
		replannerFactory.addIdentifier(identifier);
		
		this.withinDayControlerListener.getWithinDayEngine().addIntialReplannerFactory(replannerFactory);
		
		final MobsimFactory mobsimFactory = new PassengerQSimFactory(this.multiModalTravelTimes.get(),
				this.withinDayControlerListener.getWithinDayEngine(), jointDepartureOrganizer);
		event.getControler().addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new com.google.inject.Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return mobsimFactory.createMobsim(event.getControler().getScenario(), event.getControler().getEvents());
					}
				});
			}
		});
	}

	private static class PassengerQSimFactory implements MobsimFactory {
		
		private final Map<String, TravelTime> multiModalTravelTimes;
		private final WithinDayEngine withinDayEngine;
		private final JointDepartureOrganizer jointDepartureOrganizer;
		
		public PassengerQSimFactory(Map<String, TravelTime> multiModalTravelTimes, WithinDayEngine withinDayEngine,
				JointDepartureOrganizer jointDepartureOrganizer) {
			this.multiModalTravelTimes = multiModalTravelTimes;
			this.withinDayEngine = withinDayEngine;
			this.jointDepartureOrganizer = jointDepartureOrganizer;
		}
		
		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {

			QSim qSim = new QSim(sc, eventsManager);
			
			ActivityEngine activityEngine = new ActivityEngine(eventsManager, qSim.getAgentCounter());
			qSim.addMobsimEngine(activityEngine);
			qSim.addActivityHandler(activityEngine);
			
			/*
			 * Create a PassengerQNetsimEngine and add its PassengerDepartureHandler
			 * as well as its (super)VehicularDepartureHandler to the QSim.
			 * The later one handles non-joint departures.
			 */
			PassengerQNetsimEngine netsimEngine = new PassengerQNetsimEngine(qSim, MatsimRandom.getLocalInstance(), jointDepartureOrganizer);
			qSim.addMobsimEngine(netsimEngine);
			qSim.addDepartureHandler(netsimEngine.getDepartureHandler());
			qSim.addDepartureHandler(netsimEngine.getVehicularDepartureHandler());


            new MultiModalQSimModule(sc.getConfig(), this.multiModalTravelTimes).configure(qSim);
			
			TeleportationEngine teleportationEngine = new TeleportationEngine(sc, eventsManager);
			qSim.addMobsimEngine(teleportationEngine);
			
	        AgentFactory agentFactory = new DefaultAgentFactory(qSim);
	        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
	        qSim.addAgentSource(agentSource);
	        
	        qSim.addMobsimEngine(this.withinDayEngine);
	        
	        return qSim;
		}
	}
		
}
