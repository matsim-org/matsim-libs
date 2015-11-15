/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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
package playground.agarwalamit.utils.templates;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngine;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import playground.agarwalamit.congestionPricing.testExamples.handlers.CorridorNetworkAndPlans;
import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * Tests total delay on link
 * 
 */
public class EventsFromRoutes {
	
	final static String outputFolder = "./outputPassinRate_LinkEnterDensity/1c8B/";

	public static void main(String[] args) {
		new EventsFromRoutes().writeEvents();
	}
	
	void writeEvents(){
		CorridorNetworkAndPlans pseudoInputs = new CorridorNetworkAndPlans();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(10);
		 Scenario net = pseudoInputs.getDesiredScenario();
		
		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = new HashMap<>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));
		EventWriterXML eventWriterXML = new EventWriterXML(outputFolder+"/events.xml");
		manager.addHandler(eventWriterXML);
		
		QSim qSim = createQSim(net,manager);
		qSim.run();
		eventWriterXML.closeFile();
	}

	QSim createQSim (Scenario sc, EventsManager manager){
		QSim qSim1 = new QSim(sc, manager);
		ActivityEngine activityEngine = new ActivityEngine(manager, qSim1.getAgentCounter());
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);

		QNetsimEngine netsimEngine = new QNetsimEngine(qSim1);
		qSim1.addMobsimEngine(netsimEngine);
		qSim1.addDepartureHandler(netsimEngine.getDepartureHandler());
		TeleportationEngine teleportationEngine = new TeleportationEngine(sc, manager);
		qSim1.addMobsimEngine(teleportationEngine);
		QSim qSim = qSim1;
		AgentFactory agentFactory = new DefaultAgentFactory(qSim);
		PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), agentFactory, qSim);
		Map<String, VehicleType> modeVehicleTypes = new HashMap<String, VehicleType>();

		VehicleType cars = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		cars.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(cars.getId().toString()));
		cars.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(cars.getId().toString()));
		modeVehicleTypes.put("car", cars);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(bike.getId().toString()));
		bike.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(bike.getId().toString()));
		modeVehicleTypes.put("bike", bike);
		
		agentSource.setModeVehicleTypes(modeVehicleTypes);
		qSim.addAgentSource(agentSource);
		return qSim;
	}


	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes;

		public PersonLinkTravelTimeEventHandler(Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.personLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			System.out.println(event.toString());
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(event.getDriverId());
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.personLinkTravelTimes.put(event.getDriverId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			System.out.println(event.toString());
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(event.getDriverId());
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}