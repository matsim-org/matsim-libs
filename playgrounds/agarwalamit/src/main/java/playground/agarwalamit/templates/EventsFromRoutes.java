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
package playground.agarwalamit.templates;

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
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.agarwalamit.congestionPricing.testExamples.handlers.CorridorNetworkAndPlans;
import playground.agarwalamit.mixedTraffic.MixedTrafficVehiclesUtils;

/**
 * Tests total delay on link
 * 
 */
public class EventsFromRoutes {
	
	final static String OUTPUT_FOLDER = "./outputPassinRate_LinkEnterDensity/1c8B/";

	public static void main(String[] args) {
		new EventsFromRoutes().writeEvents();
	}
	
	void writeEvents(){
		CorridorNetworkAndPlans pseudoInputs = new CorridorNetworkAndPlans();
		pseudoInputs.createNetwork();
		pseudoInputs.createPopulation(10);
		Scenario net = pseudoInputs.getDesiredScenario();

		VehicleType cars = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
		cars.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(cars.getId().toString()));
		cars.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(cars.getId().toString()));
		net.getVehicles().addVehicleType(cars);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
		bike.setMaximumVelocity(MixedTrafficVehiclesUtils.getSpeed(bike.getId().toString()));
		bike.setPcuEquivalents(MixedTrafficVehiclesUtils.getPCU(bike.getId().toString()));
		net.getVehicles().addVehicleType(bike);

		net.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes = new HashMap<>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new PersonLinkTravelTimeEventHandler(personLinkTravelTimes));
		EventWriterXML eventWriterXML = new EventWriterXML(OUTPUT_FOLDER+"/events.xml");
		manager.addHandler(eventWriterXML);

		PrepareForSimUtils.createDefaultPrepareForSim(net,manager).run();
		QSim qSim = QSimUtils.createDefaultQSim(net,manager);
		qSim.run();
		eventWriterXML.closeFile();
	}

	private static class PersonLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Person>, Map<Id<Link>, Double>> personLinkTravelTimes;
		private final Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

		public PersonLinkTravelTimeEventHandler(Map<Id<Person>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.personLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(personId);
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.personLinkTravelTimes.put(personId, travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Id<Person> personId = this.delegate.getDriverOfVehicle(event.getVehicleId());
			Map<Id<Link>, Double> travelTimes = this.personLinkTravelTimes.get(personId);
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