package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

public class RailsimEngineTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private EventsManager eventsManager;
	private RailsimTestUtils.EventCollector collector;

	@Before
	public void setUp() throws Exception {
		eventsManager = EventsUtils.createEventsManager();
		collector = new RailsimTestUtils.EventCollector();

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();

	}

	private RailsimTestUtils.Holder getTestEngine(String network) {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), network).toString());

		collector.clear();

		return new RailsimTestUtils.Holder(new RailsimEngine(eventsManager, new RailsimConfigGroup(), net.getLinks()), net);
	}

	@Test
	public void simple() {

		RailsimTestUtils.Holder test = getTestEngine("network0.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l5-6");

		test.doSimStepUntil(400);

		RailsimTestUtils.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 148, 0, 20)
			.hasTrainState("train", 188, 200, 0);

		test = getTestEngine("network0.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l5-6");

		test.doStateUpdatesUntil(400, 1);

		RailsimTestUtils.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 148, 0, 20)
			.hasTrainState("train", 188, 200, 0);

	}

	@Test
	public void congested() {

		RailsimTestUtils.Holder test = getTestEngine("network0.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 0, "l1-2", "l5-6");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 60, "l1-2", "l5-6");

		test.doSimStepUntil(600);

	}


	@Test
	public void opposite() {

		RailsimTestUtils.Holder test = getTestEngine("network0.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "l1-2", "l5-6");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "l6-5", "l2-1");

		test.doSimStepUntil(600);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio2", 210.7272722504356, 2000, 0)
			.hasTrainState("regio1", 348.49615180280205, 200, 0);


		test = getTestEngine("network0.xml");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1", 0, "l1-2", "l5-6");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "l6-5", "l2-1");

		test.doStateUpdatesUntil(600, 1);

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio2", 210.7272722504356, 2000, 0)
			.hasTrainState("regio1", 348.49615180280205, 200, 0);

	}
}
