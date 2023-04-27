package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RailsimSupplyBuilder;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteType;
import ch.sbb.matsim.contrib.railsim.prototype.supply.TransitLineInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleCircuitsPlanner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultVehicleCircuitsPlannerTest {

	private Scenario scenario;

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Before
	public void setUp() {
		Config config = ConfigUtils.createConfig();
		config.global().setCoordinateSystem("CH1903plus_LV95");
		scenario = ScenarioUtils.loadScenario(config);
	}

	@Test
	public void plan() {
		var supply = new RailsimSupplyBuilder(scenario);
		// add transit stops
		supply.addStop("genf", 2499965., 1119074.);
		supply.addStop("nyon", 2507480., 1137714.);
		supply.addStop("morges", 2527501., 1151542.);
		supply.addStop("lausanne", 2507480., 1137714.);
		// add transit line: IR
		double waitingTime = 3 * 60.;
		final var ir = supply.addTransitLine("IR", "IR", "genf", waitingTime);
		ir.addPass("nyon");
		ir.addPass("morges");
		ir.addStop("lausanne", 55 * 60., waitingTime);
		Stream.of(0. * 3600, 1 * 3600., 2 * 3600., 10 * 3600., 11 * 3600.).forEach(departureTime -> ir.addDeparture(RouteDirection.FORWARD, departureTime));
		Stream.of(0.25 * 3600., 1.25 * 3600., 2.25 * 3600., 11.25 * 3600.).forEach(departureTime -> ir.addDeparture(RouteDirection.REVERSE, departureTime));
		// add transit line: S
		final var s = supply.addTransitLine("S", "S", "genf", waitingTime);
		s.addStop("nyon", 25 * 60 * 2., waitingTime);
		s.addStop("morges", 15 * 60 + 2., waitingTime);
		s.addStop("lausanne", 15 * 60 * 2., waitingTime);
		Stream.of(0. * 3600, 0.25 * 3600., 0.5 * 3600., 0.75 * 3600., 1 * 3600.).forEach(departureTime -> s.addDeparture(RouteDirection.FORWARD, departureTime));
		Stream.of(0.10 * 3600., 0.35 * 3600., 0.60 * 3600., 0.85 * 3600.).forEach(departureTime -> s.addDeparture(RouteDirection.REVERSE, departureTime));
		// let supply builder create transit lines
		supply.build();
		// run circuit planer
		var lines = new ArrayList<TransitLineInfo>();
		lines.add(ir);
		lines.add(s);
		VehicleCircuitsPlanner vcp = new DefaultVehicleCircuitsPlanner(scenario);
		var allocations = vcp.plan(lines);
		// check total number of planned allocations
		assertEquals(2, allocations.keySet().size());
		// check IR
		assertEquals(List.of("IR_1", "IR_2"), allocations.get(ir).getVehicleIds(RouteType.DEPOT_TO_DEPOT, RouteDirection.FORWARD));
		assertEquals(List.of("IR_0", "IR_2", "IR_0"), allocations.get(ir).getVehicleIds(RouteType.DEPOT_TO_STATION, RouteDirection.FORWARD));
		assertNull(allocations.get(ir).getVehicleIds(RouteType.STATION_TO_DEPOT, RouteDirection.FORWARD));
		assertNull(allocations.get(ir).getVehicleIds(RouteType.STATION_TO_STATION, RouteDirection.FORWARD));
		assertEquals(List.of("IR_1"), allocations.get(ir).getVehicleIds(RouteType.DEPOT_TO_DEPOT, RouteDirection.REVERSE));
		assertNull(allocations.get(ir).getVehicleIds(RouteType.DEPOT_TO_STATION, RouteDirection.REVERSE));
		assertEquals(List.of("IR_0", "IR_2", "IR_0"), allocations.get(ir).getVehicleIds(RouteType.STATION_TO_DEPOT, RouteDirection.REVERSE));
		assertNull(allocations.get(ir).getVehicleIds(RouteType.STATION_TO_STATION, RouteDirection.REVERSE));
	}
}
