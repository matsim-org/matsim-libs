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

		return new RailsimTest.Holder(new RailsimEngine(eventsManager, new RailsimConfigGroup(), net.getLinks()), net);
	}

	@Test
	public void simple() {

		RailsimTest.Holder test = getTestEngine("network0.xml");

		RailsimTest.createDeparture(test, 0, "t1_OUT-t2_IN", "t3_IN-t3_OUT");

		test.engine().doSimStep(0);

		System.out.println(collector.events);

		// TODO: Add assertions when more logic is implemented

	}

}
