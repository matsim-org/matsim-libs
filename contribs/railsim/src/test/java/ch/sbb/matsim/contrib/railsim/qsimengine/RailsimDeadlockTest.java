package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.DeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.NoDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.SimpleDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Set;
import java.util.function.Consumer;

public class RailsimDeadlockTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	private EventsManager eventsManager;
	private RailsimTestUtils.EventCollector collector;

	@Before
	public void setUp() {
		eventsManager = EventsUtils.createEventsManager();
		collector = new RailsimTestUtils.EventCollector();

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
	}

	private RailsimTestUtils.Holder getTestEngine(String network, DeadlockAvoidance dla, @Nullable Consumer<Link> f) {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), network).toString());
		RailsimConfigGroup config = new RailsimConfigGroup();

		collector.clear();

		if (f != null) {
			for (Link link : net.getLinks().values()) {
				f.accept(link);
			}
		}

		RailResourceManager res = new RailResourceManager(eventsManager, config, net, dla);
		TrainRouter router = new TrainRouter(net, res);

		return new RailsimTestUtils.Holder(new RailsimEngine(eventsManager, config, res, new SimpleDisposition(res, router)), net);
	}

	@Test
	public void deadlock() {

		RailsimTestUtils.Holder test = getTestEngine("networkDeadlocksFixedBlocks.xml", new NoDeadlockAvoidance(), null);
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "AB", "EF");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "HG", "DC");

		test.doSimStepUntil(250);
//		test.debugFiles(collector, "deadLock");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 240, "y1y", 0)
			.hasTrainState("regio2", 240, "zy", 0);

	}

	@Test
	public void tooSmall() {

		Set<String> increased = Set.of("y1y", "yy1", "xB", "Bx", "yx", "xy", "AB", "BA");

		// Create an avoidance point, but it is too small for multiple trains
		RailsimTestUtils.Holder test = getTestEngine("networkDeadlocksFixedBlocks.xml", new SimpleDeadlockAvoidance(), l ->{
			String id = l.getId().toString();
			if (increased.contains(id))
				RailsimUtils.setTrainCapacity(l, 2);

			l.setFreespeed(5);
		});

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1a", 0, "AB", "EF");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1b", 0, "AB", "EF");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2a", 0, "HG", "CD");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2b", 0, "HG", "CD");

		test.doSimStepUntil(1500);
		test.debugFiles(collector, "tooSmall");

		// TODO not correct yet

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 139, "EF", 0)
			.hasTrainState("regio2", 203, "CD", 0);

	}

	@Test
	public void oneWay() {

		RailsimTestUtils.Holder test = getTestEngine("networkDeadlocksFixedBlocks.xml", new SimpleDeadlockAvoidance(), null);
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "AB", "EF");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2", 0, "HG", "CD");

		test.doSimStepUntil(800);
//		test.debugFiles(collector, "oneWay");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio", 530, "EF", 0)
			.hasTrainState("regio2", 350, "CD", 0);

	}

	@Test
	public void twoWay() {

		RailsimTestUtils.Holder test = getTestEngine("networkDeadlocksFixedBlocks.xml", new SimpleDeadlockAvoidance(), l -> RailsimUtils.setTrainCapacity(l, 2));
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1a", 0, "AB", "EF");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio1b", 0, "AB", "EF");

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2a", 0, "HG", "CD");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio2b", 0, "HG", "CD");

		test.doSimStepUntil(800);
//		test.debugFiles(collector, "twoWay");

		RailsimTestUtils.assertThat(collector)
			.hasTrainState("regio1a", 350, "EF", 0)
			.hasTrainState("regio1b", 510, "EF", 0)
			.hasTrainState("regio2a", 350, "CD", 0)
			.hasTrainState("regio2b", 510, "CD", 0);

	}

}
