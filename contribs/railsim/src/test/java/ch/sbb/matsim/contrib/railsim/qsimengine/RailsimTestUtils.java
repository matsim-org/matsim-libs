package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.analysis.RailsimCsvWriter;
import ch.sbb.matsim.contrib.railsim.events.RailsimLinkStateChangeEvent;
import ch.sbb.matsim.contrib.railsim.events.RailsimTrainStateEvent;
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
import org.matsim.vehicles.*;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.*;

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
	public static void createDeparture(Holder test, TestVehicle type,  String veh, double time, String from, String to) {

		DijkstraFactory f = new DijkstraFactory();
		LeastCostPathCalculator lcp = f.createPathCalculator(test.network(), new OnlyTimeDependentTravelDisutility(new FreeSpeedTravelTime()), new FreeSpeedTravelTime());

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


		test.engine.handleDeparture(time, driver, route.getStartLinkId(), route);
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

		public void debug(EventCollector collector, String out) {
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
	public static EventsAssert assertThat(EventCollector events) {
		return new EventsAssert(events.events, EventsAssert.class);
	}

}
