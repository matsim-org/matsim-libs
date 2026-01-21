package ch.sbb.matsim.contrib.railsim.qsimengine.disposition;

import ch.sbb.matsim.contrib.railsim.qsimengine.RailsimTransitDriverAgent;
import ch.sbb.matsim.contrib.railsim.qsimengine.TrainPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test mostly focused on the {@link SpeedProfile#getNextArrival(double, TrainPosition)} implementation.
 */
public class SpeedProfileArrivalTest {

	private Scenario scenario;
	private TransitSchedule transitSchedule;
	private SpeedProfile speedProfile;
	private TrainPosition mockPosition;
	private RailsimTransitDriverAgent mockPtAgent;

	@BeforeEach
	void setUp() {
		// Load real transit schedule from test input
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		TransitScheduleReader reader = new TransitScheduleReader(scenario);
		reader.readFile("test/input/ch/sbb/matsim/contrib/railsim/integration/shuttle/transitSchedule.xml");
		transitSchedule = scenario.getTransitSchedule();

		// Create a simple implementation of SpeedProfile for testing
		speedProfile = (time, position, nextArrival) -> {
			return Double.POSITIVE_INFINITY; // Default implementation
		};

		// Setup mocks
		mockPosition = mock(TrainPosition.class);
		mockPtAgent = mock(RailsimTransitDriverAgent.class);
	}

	@Test
	void testGetNextArrival_WithValidTransitData() {
		// Get real transit data from the loaded schedule
		TransitRoute route = transitSchedule.getTransitLines().get(Id.create("shuttle_line_forward", org.matsim.pt.transitSchedule.api.TransitLine.class))
			.getRoutes().get(Id.create("shuttle_route_forward", TransitRoute.class));
		Departure departure = route.getDepartures().get(Id.create("dep_forward_1", Departure.class));
		TransitRouteStop firstStop = route.getStops().get(0);

		// Setup mocks with real data
		setupMockPosition(mockPosition, mockPtAgent, firstStop.getStopFacility(), route, 0, departure);

		// Test the method
		PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

		assertThat(result).isNotEqualTo(PlannedArrival.UNDEFINED);
		assertThat(result.time()).isEqualTo(departure.getDepartureTime() + firstStop.getArrivalOffset().seconds());
	}

	@Test
	void testGetNextArrival_WithSecondStop() {
		// Test with the second stop in the route
		TransitRoute route = transitSchedule.getTransitLines().get(Id.create("shuttle_line_forward", org.matsim.pt.transitSchedule.api.TransitLine.class))
			.getRoutes().get(Id.create("shuttle_route_forward", TransitRoute.class));
		Departure departure = route.getDepartures().get(Id.create("dep_forward_1", Departure.class));
		TransitRouteStop secondStop = route.getStops().get(1);

		setupMockPosition(mockPosition, mockPtAgent, secondStop.getStopFacility(), route, 1, departure);

		PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

		assertThat(result).isNotNull();
		assertThat(result).isNotEqualTo(PlannedArrival.UNDEFINED);
		assertThat(result.time()).isEqualTo(departure.getDepartureTime() + secondStop.getArrivalOffset().seconds());
	}

	@Test
	void testGetNextArrival_WithUndefinedArrivalOffset() {
		// Create a mock route stop with undefined arrival offset
		TransitRouteStop mockRouteStop = mock(TransitRouteStop.class);
		when(mockRouteStop.getArrivalOffset()).thenReturn(OptionalTime.undefined());

		TransitRoute mockRoute = mock(TransitRoute.class);
		List<TransitRouteStop> stops = new ArrayList<>();
		stops.add(mockRouteStop);
		when(mockRoute.getStops()).thenReturn(stops);

		setupMockPosition(mockPosition, mockPtAgent, mock(TransitStopFacility.class), mockRoute, 0, null);

		PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

		assertThat(result).isEqualTo(PlannedArrival.UNDEFINED);
	}

	@Test
	void testGetNextArrival_WithNullPtAgent() {
		// Test when PT agent is null
		when(mockPosition.getPt()).thenReturn(null);

		PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

		assertThat(result).isEqualTo(PlannedArrival.UNDEFINED);
	}

	@Test
	void testGetNextArrival_WithNullNextStop() {
		// Test when next stop is null
		when(mockPosition.getPt()).thenReturn(mockPtAgent);
		when(mockPosition.getNextStop()).thenReturn(null);

		PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

		assertThat(result).isEqualTo(PlannedArrival.UNDEFINED);
	}

	@Test
	void testGetNextArrival_WithBackwardRoute() {
		// Test with the backward route from the schedule
		TransitRoute route = transitSchedule.getTransitLines().get(Id.create("shuttle_line_backward", org.matsim.pt.transitSchedule.api.TransitLine.class))
			.getRoutes().get(Id.create("shuttle_route_backward", TransitRoute.class));
		Departure departure = route.getDepartures().get(Id.create("dep_backward_1", Departure.class));
		TransitRouteStop firstStop = route.getStops().get(0);

		setupMockPosition(mockPosition, mockPtAgent, firstStop.getStopFacility(), route, 0, departure);

		PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

		assertThat(result).isNotNull();
		assertThat(result).isNotEqualTo(PlannedArrival.UNDEFINED);
		assertThat(result.time()).isEqualTo(departure.getDepartureTime() + firstStop.getArrivalOffset().seconds());
	}

	@Test
	void testGetNextArrival_WithDifferentDepartureTimes() {
		// Test with different departure times
		TransitRoute route = transitSchedule.getTransitLines().get(Id.create("shuttle_line_forward", org.matsim.pt.transitSchedule.api.TransitLine.class))
			.getRoutes().get(Id.create("shuttle_route_forward", TransitRoute.class));
		TransitRouteStop firstStop = route.getStops().get(0);

		// Test with first departure (00:00:00 = 0 seconds)
		Departure departure1 = route.getDepartures().get(Id.create("dep_forward_1", Departure.class));
		setupMockPosition(mockPosition, mockPtAgent, firstStop.getStopFacility(), route, 0, departure1);

		PlannedArrival result1 = speedProfile.getNextArrival(0.0, mockPosition);
		assertThat(result1.time()).isEqualTo(departure1.getDepartureTime() + firstStop.getArrivalOffset().seconds());

		// Test with second departure (00:05:00 = 300 seconds)
		Departure departure2 = route.getDepartures().get(Id.create("dep_forward_2", Departure.class));
		setupMockPosition(mockPosition, mockPtAgent, firstStop.getStopFacility(), route, 0, departure2);

		PlannedArrival result2 = speedProfile.getNextArrival(0.0, mockPosition);
		assertThat(result2.time()).isEqualTo(departure2.getDepartureTime() + firstStop.getArrivalOffset().seconds());
	}

	@Test
	void testGetNextArrival_WithMultipleStops() {
		// Test with multiple stops in the route
		TransitRoute route = transitSchedule.getTransitLines().get(Id.create("shuttle_line_forward", org.matsim.pt.transitSchedule.api.TransitLine.class))
			.getRoutes().get(Id.create("shuttle_route_forward", TransitRoute.class));
		Departure departure = route.getDepartures().get(Id.create("dep_forward_1", Departure.class));

		// Test each stop in the route
		for (int i = 0; i < route.getStops().size(); i++) {
			TransitRouteStop stop = route.getStops().get(i);

			setupMockPosition(mockPosition, mockPtAgent, stop.getStopFacility(), route, i, departure);

			PlannedArrival result = speedProfile.getNextArrival(0.0, mockPosition);

			if (stop.getArrivalOffset().isDefined()) {
				assertThat(result).isNotNull();
				assertThat(result).isNotEqualTo(PlannedArrival.UNDEFINED);
				assertThat(result.time()).isEqualTo(departure.getDepartureTime() + stop.getArrivalOffset().seconds());
			} else {
				assertThat(result).isEqualTo(PlannedArrival.UNDEFINED);
			}
		}
	}

	/**
	 * Helper method to setup mock position with common configuration.
	 */
	private void setupMockPosition(TrainPosition position, RailsimTransitDriverAgent ptAgent,
								   TransitStopFacility nextStop, TransitRoute route,
								   int currentStopIndex, Departure departure) {
		when(position.getPt()).thenReturn(ptAgent);
		when(position.getNextStop()).thenReturn(nextStop);
		when(ptAgent.getTransitRoute()).thenReturn(route);
		when(ptAgent.getCurrentStopIndex()).thenReturn(currentStopIndex);
		when(ptAgent.getDeparture()).thenReturn(departure);
	}
}
