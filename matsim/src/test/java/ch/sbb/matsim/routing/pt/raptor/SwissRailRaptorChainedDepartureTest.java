package ch.sbb.matsim.routing.pt.raptor;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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

import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

public class SwissRailRaptorChainedDepartureTest {

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

		new TransitScheduleReader(scenario).readFile(input + "schedule.xml");
		new MatsimNetworkReader(scenario.getNetwork()).readFile(input + "network.xml");

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);

		SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, raptorConfig, scenario.getNetwork(), null);

		raptor = new SwissRailRaptor.Builder(data, config).build();
		params = RaptorUtils.createParameters(config);
	}

	@Test
	void testTree_single() {

		TransitStopFacility genf = scenario.getTransitSchedule().getFacilities().get(Id.create("f12", TransitStopFacility.class));

		Object2IntMap<TransitStopFacility> connections = new Object2IntOpenHashMap<>();

		raptor.calcTreesObservable(genf, 0, 86400, params, null, new SwissRailRaptor.RaptorObserver() {
			@Override
			public void arrivedAtStop(double departureTime, TransitStopFacility stopFacility, double arrivalTime, int transferCount, Supplier<RaptorRoute> route) {
				connections.putIfAbsent(stopFacility, transferCount);
			}
		});

		TransitStopFacility bern = scenario.getTransitSchedule().getFacilities().get(Id.create("f3", TransitStopFacility.class));

//		assertThat(connections)
//			.containsEntry(bern, 0);
	}

	@Test
	void testTree_split() {

		TransitStopFacility luzern = scenario.getTransitSchedule().getFacilities().get(Id.create("f31", TransitStopFacility.class));

		Object2IntMap<TransitStopFacility> connections = new Object2IntOpenHashMap<>();

		raptor.calcTreesObservable(luzern, 0, 86400, params, null, new SwissRailRaptor.RaptorObserver() {
			@Override
			public void arrivedAtStop(double departureTime, TransitStopFacility stopFacility, double arrivalTime, int transferCount, Supplier<RaptorRoute> route) {
				connections.putIfAbsent(stopFacility, transferCount);
			}
		});

		TransitStopFacility langental = scenario.getTransitSchedule().getFacilities().get(Id.create("f29", TransitStopFacility.class));
		TransitStopFacility bern = scenario.getTransitSchedule().getFacilities().get(Id.create("f3", TransitStopFacility.class));

//		assertThat(connections)
//			.containsEntry(langental, 0)
//			.containsEntry(bern, 0);

	}

	void testConnection_single() {

		TransitStopFacility genf = scenario.getTransitSchedule().getFacilities().get(Id.create("f12", TransitStopFacility.class));
		TransitStopFacility bern = scenario.getTransitSchedule().getFacilities().get(Id.create("f3", TransitStopFacility.class));

		List<RaptorRoute> routes = raptor.calcRoutes(genf, bern, 0, 4 * 3600, 86400, null, null);

	}

}
