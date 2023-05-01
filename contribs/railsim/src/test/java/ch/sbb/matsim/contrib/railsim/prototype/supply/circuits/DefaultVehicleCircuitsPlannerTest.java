package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RailsimSupplyBuilder;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RailsimSupplyConfigGroup;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteType;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

/**
 * Testing default vehicle circuits planner approach
 *
 * @author Merlin Unterfinger
 */
public class DefaultVehicleCircuitsPlannerTest {
	private RailsimSupplyBuilder supply;

	@Before
	public void setUp() {
		// configure
		var config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("CH1903plus_LV95");
		var railsimConfigGroup = ConfigUtils.addOrGetModule(config, RailsimSupplyConfigGroup.class);
		railsimConfigGroup.setCircuitPlanningApproach(RailsimSupplyConfigGroup.CircuitPlanningApproach.DEFAULT);
		var scenario = ScenarioUtils.loadScenario(config);
		// setup supply builder
		supply = new RailsimSupplyBuilder(scenario);
		// add transit stops
		supply.addStop("genf", 2499965., 1119074.);
		supply.addStop("nyon", 2507480., 1137714.);
		supply.addStop("morges", 2527501., 1151542.);
		supply.addStop("lausanne", 2507480., 1137714.);
		// add transit line: IR
		double waitingTime = 3 * 60.;
		var ir = supply.addTransitLine("IR", "IR", "genf", waitingTime);
		ir.addPass("nyon");
		ir.addPass("morges");
		ir.addStop("lausanne", 55 * 60., waitingTime);
		Stream.of(0. * 3600, 1 * 3600., 2 * 3600., 10 * 3600., 11 * 3600.).forEach(departureTime -> ir.addDeparture(RouteDirection.FORWARD, departureTime));
		Stream.of(0.25 * 3600., 1.25 * 3600., 2.25 * 3600., 11.25 * 3600.).forEach(departureTime -> ir.addDeparture(RouteDirection.REVERSE, departureTime));
		// add transit line: S
		var s = supply.addTransitLine("S", "S", "genf", waitingTime);
		s.addStop("nyon", 25 * 60 * 2., waitingTime);
		s.addStop("morges", 15 * 60 + 2., waitingTime);
		s.addStop("lausanne", 15 * 60 * 2., waitingTime);
		Stream.of(0. * 3600, 0.25 * 3600., 0.5 * 3600., 0.75 * 3600., 1 * 3600.).forEach(departureTime -> s.addDeparture(RouteDirection.FORWARD, departureTime));
		Stream.of(0.10 * 3600., 0.35 * 3600., 0.60 * 3600., 0.85 * 3600.).forEach(departureTime -> s.addDeparture(RouteDirection.REVERSE, departureTime));
	}

	@Test
	public void plan() {
		// let supply builder create transit lines and plan circuits
		supply.build();
		// check
		var schedule = supply.getScenario().getTransitSchedule();
		// IR
		String lineId = "IR";
		assertTrue(checkVehicleIds(List.of("IR_1", "IR_2"), lineId, RouteDirection.FORWARD, RouteType.DEPOT_TO_DEPOT));
		assertTrue(checkVehicleIds(List.of("IR_0", "IR_2", "IR_0"), lineId, RouteDirection.FORWARD, RouteType.DEPOT_TO_STATION));
		assertTrue(checkVehicleIds(null, lineId, RouteDirection.FORWARD, RouteType.STATION_TO_DEPOT));
		assertTrue(checkVehicleIds(null, lineId, RouteDirection.FORWARD, RouteType.STATION_TO_STATION));
		assertTrue(checkVehicleIds(List.of("IR_1"), lineId, RouteDirection.REVERSE, RouteType.DEPOT_TO_DEPOT));
		assertTrue(checkVehicleIds(null, lineId, RouteDirection.REVERSE, RouteType.DEPOT_TO_STATION));
		assertTrue(checkVehicleIds(List.of("IR_0", "IR_2", "IR_0"), lineId, RouteDirection.REVERSE, RouteType.STATION_TO_DEPOT));
		assertTrue(checkVehicleIds(null, lineId, RouteDirection.REVERSE, RouteType.STATION_TO_STATION));
	}

	private boolean checkVehicleIds(List<String> vehicleIds, String lineId, RouteDirection routeDirection, RouteType routeType) {
		var line = supply.getScenario().getTransitSchedule().getTransitLines().get(Id.create(lineId, TransitLine.class));
		var route = line.getRoutes().get(createRouteId(lineId, routeDirection, routeType));
		if (route != null) {
			var allocatedVehicleIds = route.getDepartures().values().stream().map(d -> d.getVehicleId().toString()).collect(Collectors.toList());
			if (vehicleIds.equals(allocatedVehicleIds)) {
				return true;
			}
			System.out.printf("Got %s instead of %s\n", allocatedVehicleIds, vehicleIds);
		}
		return route == null && vehicleIds == null;
	}

	private Id<TransitRoute> createRouteId(String lineId, RouteDirection routeDirection, RouteType routeType) {
		return Id.create(String.format("%s_%s_%s", lineId, routeDirection.getAbbreviation(), routeType.name()), TransitRoute.class);
	}

}
