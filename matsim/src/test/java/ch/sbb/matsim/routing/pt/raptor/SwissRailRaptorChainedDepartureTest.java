package ch.sbb.matsim.routing.pt.raptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class SwissRailRaptorChainedDepartureTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private Scenario scenario;
	private SwissRailRaptor raptor;
	private RaptorParameters params;

	TransitStopFacility genf;
	TransitStopFacility bern;
	TransitStopFacility luzern;
	TransitStopFacility langenthal;

	@BeforeEach
	void setUp() {

		String input = utils.getPackageInputDirectory();

		Config config = ConfigUtils.createConfig();
		scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(input + "schedule.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input + "network.xml");

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
		raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);

		SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, raptorConfig, scenario.getNetwork(), null);

		raptor = new SwissRailRaptor.Builder(data, config).build();
		params = RaptorUtils.createParameters(config);

		genf = scenario.getTransitSchedule().getFacilities().get(Id.create("f12", TransitStopFacility.class));
		bern = scenario.getTransitSchedule().getFacilities().get(Id.create("f3", TransitStopFacility.class));
		luzern = scenario.getTransitSchedule().getFacilities().get(Id.create("f31", TransitStopFacility.class));
		langenthal = scenario.getTransitSchedule().getFacilities().get(Id.create("f29", TransitStopFacility.class));
	}

	@Test
	void testTree_single() {

		Map<TransitStopFacility, Result> connections = new HashMap<>();
		raptor.calcTreesObservable(genf, 0, 86400, params, null, new SwissRailRaptor.RaptorObserver() {
			@Override
			public void arrivedAtStop(double departureTime, TransitStopFacility stopFacility, double arrivalTime, int transferCount, Supplier<RaptorRoute> route) {

				RaptorRoute r = route.get();

				assertThat(r.getTravelTime())
					.isEqualTo(arrivalTime - departureTime);

//				assertThat(transferCount)
//					.isEqualTo(r.getNumberOfTransfers());

				connections.computeIfAbsent(stopFacility, (k) -> new Result(departureTime, arrivalTime, transferCount));
			}
		});

		assertThat(connections)
			.containsEntry(bern, new Result(20520.0, 26760, 0));
	}

	@Test
	void testTree_split() {

		Map<TransitStopFacility, Result> connections = new HashMap<>();
		raptor.calcTreesObservable(luzern, 0, 86400, params, null, new SwissRailRaptor.RaptorObserver() {
			@Override
			public void arrivedAtStop(double departureTime, TransitStopFacility stopFacility, double arrivalTime, int transferCount, Supplier<RaptorRoute> route) {
				connections.computeIfAbsent(stopFacility, (k) -> new Result(departureTime, arrivalTime, transferCount));
			}
		});

		assertThat(connections)
			.containsEntry(langenthal, new Result(17820.0, 22080.0, 0))
			.containsEntry(bern, new Result(17820.0, 23160.0, 0));

	}

	@Test
	void testTree_ring() {

		TransitStopFacility ziegelb = scenario.getTransitSchedule().getFacilities().get(Id.create("f68", TransitStopFacility.class));

		Map<TransitStopFacility, Result> connections = new HashMap<>();
		raptor.calcTreesObservable(ziegelb, 0, 86400, params, null, new SwissRailRaptor.RaptorObserver() {
			@Override
			public void arrivedAtStop(double departureTime, TransitStopFacility stopFacility, double arrivalTime, int transferCount, Supplier<RaptorRoute> route) {
				if (transferCount == 0) {
					Result r = connections.get(stopFacility);
					Result updated = new Result(departureTime, arrivalTime, transferCount);

					// Store the shortest connections without transfers
					if (r == null || updated.arrivalTime < r.arrivalTime)
						connections.put(stopFacility, updated);
				}
			}
		});

		TransitStopFacility lichtensteig = scenario.getTransitSchedule().getFacilities().get(Id.create("f28", TransitStopFacility.class));

		assertThat(connections)
			.containsEntry(lichtensteig, new Result(19920.0, 21630.0, 0));

	}

	@Test
	void testRoutes() {

		List<RaptorRoute> routes = raptor.calcRoutes(genf, bern, 0, 4 * 3600, 86400, null, null);

		assertThat(routes)
			.hasSize(18)
			.allMatch(r -> r.getNumberOfTransfers() == 0);

		routes = raptor.calcRoutes(luzern, langenthal, 0, 4 * 3600, 86400, null, null);

		assertThat(routes)
			.hasSize(20)
			.allMatch(r -> r.getNumberOfTransfers() == 0);

	}

	@Test
	void testRoute() {

		List<? extends PlanElement> route = raptor.calcRoute(genf, bern, 0, 4 * 3600, 6 * 3600, null, null);

		assertThat(route)
			.hasSize(3);

		PlanElement ptLeg = route.get(1);
		assertThat(ptLeg)
			.isInstanceOf(Leg.class);

		Leg leg = (Leg) ptLeg;

		assertThat(leg.getDepartureTime())
			.isEqualTo(OptionalTime.defined(20520.0));

		DefaultTransitPassengerRoute r = (DefaultTransitPassengerRoute) leg.getRoute();

		assertThat(r.getChainedRoute())
			.isNotNull();


	}

	private record Result(double departureTime, double arrivalTime, int transferCount) {
	}

}
