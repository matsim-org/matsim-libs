package ch.sbb.matsim.contrib.railsim.qsimengine;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for test cases.
 */
public class RailsimTest {

	/**
	 * Create a departure within the engine. Route will be determined automatically.
	 */
	public static void createDeparture(Holder test, double time, String from, String to) {

		DijkstraFactory f = new DijkstraFactory();
		LeastCostPathCalculator lcp = f.createPathCalculator(test.network(), Mockito.mock(TravelDisutility.class), new FreeSpeedTravelTime());

		Link fromLink = test.network.getLinks().get(Id.createLinkId(from));
		Link toLink = test.network.getLinks().get(Id.createLinkId(to));

		LeastCostPathCalculator.Path path = lcp.calcLeastCostPath(fromLink.getFromNode(), toLink.getToNode(), 0, null, null);
		NetworkRoute route = RouteUtils.createNetworkRoute(path.links.stream().map(Link::getId).toList());

		// Setup mocks for driver and vehicle
		MobsimDriverAgent driver = Mockito.mock(MobsimDriverAgent.class, Answers.RETURNS_MOCKS);
		MobsimVehicle mobVeh = Mockito.mock(MobsimVehicle.class, Answers.RETURNS_MOCKS);

		VehicleType type = VehicleUtils.createVehicleType(Id.create("vehicle type", VehicleType.class));
		Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId("vehicle"), type);

		Mockito.when(mobVeh.getVehicle()).thenReturn(vehicle);
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
			events.add(event);
		}
	}

	public record Holder(RailsimEngine engine, Network network) {
	}
}
