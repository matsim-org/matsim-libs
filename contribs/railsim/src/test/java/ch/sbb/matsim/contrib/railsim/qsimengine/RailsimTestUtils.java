/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.analysis.RailsimCsvWriter;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailLink;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for test cases.
 */
public class RailsimTestUtils {

	static Map<TestVehicle, VehicleType> vehicles = new EnumMap<>(TestVehicle.class);

	static {

		Vehicles veh = VehicleUtils.createVehiclesContainer();

		MatsimVehicleReader reader = new MatsimVehicleReader(veh);
		reader.readURL(RailsimTestUtils.class.getResource("/trainVehicleTypes.xml"));

		vehicles.put(TestVehicle.Sprinter, veh.getVehicleTypes().get(Id.create("Sprinter", VehicleType.class)));
		vehicles.put(TestVehicle.Express, veh.getVehicleTypes().get(Id.create("Express", VehicleType.class)));
		vehicles.put(TestVehicle.Regio, veh.getVehicleTypes().get(Id.create("Regio", VehicleType.class)));
		vehicles.put(TestVehicle.Cargo, veh.getVehicleTypes().get(Id.create("Cargo", VehicleType.class)));
	}

	/**
	 * Create a departure within the engine. Route will be determined automatically.
	 */
	public static void createDeparture(Holder test, TestVehicle type, String veh, double time, String from, String to) {

		DijkstraFactory f = new DijkstraFactory();
		LeastCostPathCalculator lcp = f.createPathCalculator(test.network(), new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()),
			new FreeSpeedTravelTime());

		Link fromLink = test.network.getLinks().get(Id.createLinkId(from));
		Link toLink = test.network.getLinks().get(Id.createLinkId(to));

		LeastCostPathCalculator.Path path = lcp.calcLeastCostPath(fromLink.getFromNode(), toLink.getToNode(), 0, null, null);
		NetworkRoute route = RouteUtils.createNetworkRoute(path.links.stream().map(Link::getId).toList());

		System.out.println("Creating departure with route" + route);

		// Setup mocks for driver and vehicle
		Id<Vehicle> vehicleId = Id.createVehicleId(veh);

		MobsimDriverAgent driver = Mockito.mock(MobsimDriverAgent.class, Answers.RETURNS_MOCKS);
		MobsimVehicle mobVeh = Mockito.mock(MobsimVehicle.class, Answers.RETURNS_MOCKS);

		Vehicle vehicle = VehicleUtils.createVehicle(vehicleId, vehicles.get(type));

		Mockito.when(mobVeh.getVehicle()).thenReturn(vehicle);
		Mockito.when(mobVeh.getId()).thenReturn(vehicleId);
		Mockito.when(driver.getVehicle()).thenReturn(mobVeh);
		Mockito.when(driver.getId()).thenReturn(Id.createPersonId("driver_" + veh));

		test.engine.handleDeparture(time, driver, route.getStartLinkId(), route);
	}

	/**
	 * Create a RailLink for testing.
	 */
	public static RailLink createLink(double length, int trainCapacity) {

		Link link = Mockito.mock(Link.class, Answers.RETURNS_MOCKS);

		AttributesImpl attr = new AttributesImpl();
		Mockito.when(link.getAttributes()).thenReturn(attr);
		Mockito.when(link.getLength()).thenReturn(length);

		RailsimUtils.setTrainCapacity(link, trainCapacity);

		return new RailLink(link);
	}

	/**
	 * Collect events during testing
	 */
	public static class EventCollector implements BasicEventHandler {

		List<Event> events = new ArrayList<>();

		@Override
		public void handleEvent(Event event) {
			System.out.println(event);
			events.add(event);
		}

		public void clear() {
			events.clear();
		}

		public void dump(String out) {
			EventWriterXML writer = new EventWriterXML(out);
			events.forEach(writer::handleEvent);
			writer.closeFile();
		}

	}

	public record Holder(RailsimEngine engine, Network network) {

		/**
		 * Step at one second until time is reached.
		 */
		public void doSimStepUntil(double time) {
			for (double t = 0; t < time; t++) {
				engine().doSimStep(t);
			}
		}

		/**
		 * Call state updates until time is reached with fixed interval.
		 */
		public void doStateUpdatesUntil(double time, double interval) {

			for (double t = 0; t < time; t += interval) {
				engine().updateAllStates(t);
			}
		}

		public void debugFiles(EventCollector collector, String out) {
			RailsimCsvWriter.writeTrainStatesCsv(
				collector.events.stream().filter(ev -> ev instanceof RailsimTrainStateEvent)
					.map(ev -> (RailsimTrainStateEvent) ev)
					.toList(),
				network,
				out + "_trainStates.csv"
			);

			RailsimCsvWriter.writeLinkStatesCsv(
				collector.events.stream().filter(ev -> ev instanceof RailsimLinkStateChangeEvent)
					.map(ev -> (RailsimLinkStateChangeEvent) ev)
					.toList(),
				out + "_linkStates.csv"
			);
		}
	}

	/**
	 * Helper method for event assertions.
	 */
	static EventsAssert assertThat(EventCollector events) {
		return new EventsAssert(events.events, EventsAssert.class);
	}

}
