package ch.sbb.matsim.routing.pt.raptor;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SwissRailRaptorChainedDepartureExtendedTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario scenario;
	private SwissRailRaptor raptor;
	private RaptorParameters params;


	@BeforeEach
	void setUp() {

		String input = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);

		// The test will only run if the files are present. These files are not checked into the repository
		Assumptions.assumeTrue(Files.exists(Path.of(input, "transitSchedule.xml.gz")));
		Assumptions.assumeTrue(Files.exists(Path.of(input, "transitNetwork.xml.gz")));

		new TransitScheduleReader(scenario).readFile(input + "transitSchedule.xml.gz");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input + "transitNetwork.xml.gz");

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
		raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
		// make sure SwissRailRaptor does not add any more transfers than what is specified in minimal transfer times:
		raptorConfig.setBeelineWalkConnectionDistance(10.0);

		SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, raptorConfig, scenario.getNetwork(), null);

		raptor = new SwissRailRaptor.Builder(data, config).build();

		params = RaptorUtils.createParameters(config);
		params.setTransferPenaltyFixCostPerTransfer(0.01);
		params.setTransferPenaltyMinimum(0.01);
		params.setTransferPenaltyMaximum(0.01);

	}

	@Test
	void test_route_structure() {

		TransitStopFacility start = scenario.getTransitSchedule().getFacilities().get(Id.create("1364", TransitStopFacility.class));
		TransitStopFacility end = scenario.getTransitSchedule().getFacilities().get(Id.create("2368", TransitStopFacility.class));

		List<RaptorRoute> routes = new ArrayList<>();

		raptor.calcTreesObservable(start, 20000, 30000, params, null, (departureTime, stopFacility, arrivalTime, transferCount, route) -> {

			RaptorRoute r = route.get();

			if (r.toFacility != end) {
				return;
			}

			routes.add(r);
		});

		// Test if route have increasing arrival times and departure times
		for (RaptorRoute route : routes) {

			RaptorRoute.RoutePart prev = null;
			for (RaptorRoute.RoutePart part : route.parts) {

				// First part is ignored
				if (part.fromStop == null)
					continue;

				assertThat(part.getChainedArrivalTime()).isGreaterThanOrEqualTo(part.depTime);

				if (prev != null) {
					assertThat(part.depTime).isGreaterThanOrEqualTo(prev.getChainedArrivalTime());
					// No two parts should have the same mode
					assertThat(part.mode).isNotEqualTo(prev.mode);
				}

				prev = part;
			}
		}
	}


	@Test
	void test_observable() {

		Map<TransitStopFacility, SwissRailRaptorChainedDepartureTest.Result> connections = new HashMap<>();

		TransitStopFacility f = scenario.getTransitSchedule().getFacilities().get(Id.create("1773", TransitStopFacility.class));
		raptor.calcTreesObservable(f, 0, 86400, params, null, (departureTime, stopFacility, arrivalTime, transferCount, route) -> {

			RaptorRoute r = route.get();

			assertThat(r.getTravelTime())
				.isEqualTo(arrivalTime - departureTime);

			if (transferCount != 0)
				return;

			connections.computeIfAbsent(stopFacility, (k) -> new SwissRailRaptorChainedDepartureTest.Result(departureTime, arrivalTime, transferCount));
		});


	}
}
