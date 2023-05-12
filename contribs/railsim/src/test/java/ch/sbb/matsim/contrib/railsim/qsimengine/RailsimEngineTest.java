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
	private RailsimTest.EventCollector collector;

	@Before
	public void setUp() throws Exception {
		eventsManager = EventsUtils.createEventsManager();
		collector = new RailsimTest.EventCollector();

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();

	}

	private RailsimTest.Holder getTestEngine(String network) {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), network).toString());

		collector.clear();

		return new RailsimTest.Holder(new RailsimEngine(eventsManager, new RailsimConfigGroup(), net.getLinks()), net);
	}

	@Test
	public void simple() {

		RailsimTest.Holder test = getTestEngine("network0.xml");
		RailsimTest.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l4-5");

		for (int i = 0; i < 400; i++) {
			test.engine().doSimStep(i);
		}

		RailsimTest.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 169, 500, 44)
			.hasTrainState("train", 319.818181481007, 200, 0);

		test = getTestEngine("network0.xml");
		RailsimTest.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l4-5");

		for (int i = 0; i < 400; i++) {
			test.engine().updateAllStates(i);
		}

		RailsimTest.assertThat(collector)
			.hasSizeGreaterThan(5)
			.hasTrainState("train", 169, 500, 44)
			.hasTrainState("train", 319.818181481007, 200, 0);

	}

	@Test
	public void congested() {

		RailsimTest.Holder test = getTestEngine("network0.xml");

		RailsimTest.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l4-5");
		RailsimTest.createDeparture(test, TestVehicle.Sprinter, "train", 120, "l1-2", "l4-5");

	}


	@Test
	public void opposite() {

		RailsimTest.Holder test = getTestEngine("network0.xml");

		RailsimTest.createDeparture(test, TestVehicle.Regio, "train", 0, "l1-2", "l4-5");
		RailsimTest.createDeparture(test, TestVehicle.Regio, "train", 0, "l4-5", "l1-2");


	}
}
