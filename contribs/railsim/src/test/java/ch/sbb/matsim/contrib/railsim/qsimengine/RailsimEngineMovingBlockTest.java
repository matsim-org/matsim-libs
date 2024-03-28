package ch.sbb.matsim.contrib.railsim.qsimengine;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.deadlocks.NoDeadlockAvoidance;
import ch.sbb.matsim.contrib.railsim.qsimengine.disposition.SimpleDisposition;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.RailResourceManager;
import ch.sbb.matsim.contrib.railsim.qsimengine.router.TrainRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.util.function.Consumer;

/**
 * Tests for moving block logic.
 */
public class RailsimEngineMovingBlockTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	private EventsManager eventsManager;
	private RailsimTestUtils.EventCollector collector;

	@BeforeEach
	public void setUp() {
		eventsManager = EventsUtils.createEventsManager();
		collector = new RailsimTestUtils.EventCollector();

		eventsManager.addHandler(collector);
		eventsManager.initProcessing();
	}

	private RailsimTestUtils.Holder getTestEngine(String network, @Nullable Consumer<Link> f) {
		Network net = NetworkUtils.readNetwork(new File(utils.getPackageInputDirectory(), network).toString());
		RailsimConfigGroup config = new RailsimConfigGroup();

		collector.clear();

		if (f != null) {
			for (Link link : net.getLinks().values()) {
				f.accept(link);
			}
		}
		RailResourceManager res = new RailResourceManager(eventsManager, config, net, new NoDeadlockAvoidance());
		TrainRouter router = new TrainRouter(net, res);

		return new RailsimTestUtils.Holder(new RailsimEngine(eventsManager, config, res, new SimpleDisposition(res, router)), net);
	}

	private RailsimTestUtils.Holder getTestEngine(String network) {
		return getTestEngine(network, null);
	}

	@Test
	public void multipleTrains() {

		RailsimTestUtils.Holder test = getTestEngine("networkMovingBlocks.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 60, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 120, "l1-2", "l6-7");

		test.doSimStepUntil(5_000);

		test.debugFiles(collector, utils.getOutputDirectory() + "/movingBlock");

		RailsimTestUtils.assertThat(collector).hasTrainState("regio", 2370, 200, 0).hasTrainState("cargo", 3268, 200, 0)
			.hasTrainState("sprinter", 3345, 200, 0);

		test = getTestEngine("networkMovingBlocks.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 60, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 120, "l1-2", "l6-7");

		test.doStateUpdatesUntil(5_000, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/movingBlock_detailed");

		RailsimTestUtils.assertThat(collector).hasTrainState("regio", 2370, 200, 0).hasTrainState("cargo", 3268, 200, 0)
			.hasTrainState("sprinter", 3345, 200, 0);

	}

	@Test
	public void opposite() {

		RailsimTestUtils.Holder test = getTestEngine("networkMovingBlocks.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 0, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter2", 400, "l6-5", "l2-1");

		test.doSimStepUntil(2_000);
		test.debugFiles(collector, utils.getOutputDirectory() + "/opposite");

		RailsimTestUtils.assertThat(collector).hasTrainState("sprinter2", 1368, 200, 0).hasTrainState("sprinter", 1559, 200, 0);

		test = getTestEngine("networkMovingBlocks.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 0, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter2", 400, "l6-5", "l2-1");

		test.doStateUpdatesUntil(2_000, 5);
		test.debugFiles(collector, utils.getOutputDirectory() + "/opposite_detailed");

		RailsimTestUtils.assertThat(collector).hasTrainState("sprinter2", 1368, 200, 0).hasTrainState("sprinter", 1559, 200, 0);

	}

	@Test
	public void multiTrack() {

		// This test increased capacity
		RailsimTestUtils.Holder test = getTestEngine("networkMovingBlocks.xml", l -> RailsimUtils.setTrainCapacity(l, 3));

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 60, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 120, "l1-2", "l6-7");

		test.doSimStepUntil(5_000);
		test.debugFiles(collector, utils.getOutputDirectory() + "/multiTrack");

		RailsimTestUtils.assertThat(collector).hasTrainState("regio", 2370, 200, 0).hasTrainState("cargo", 3268, 200, 0)
			.hasTrainState("sprinter", 1984, 200, 0);

		test = getTestEngine("networkMovingBlocks.xml", l -> RailsimUtils.setTrainCapacity(l, 3));

		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 60, "l1-2", "l6-7");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 120, "l1-2", "l6-7");

		test.doStateUpdatesUntil(5_000, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/multiTrack_detailed");

		RailsimTestUtils.assertThat(collector).hasTrainState("regio", 2370, 200, 0).hasTrainState("cargo", 3268, 200, 0)
			.hasTrainState("sprinter", 1984, 200, 0);

	}


	@Test
	public void mixed() {

		RailsimTestUtils.Holder test = getTestEngine("networkMixedTypes.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 0, "1-2", "20-21");

		test.doSimStepUntil(2_000);
		test.debugFiles(collector, utils.getOutputDirectory() + "/mixed");

		RailsimTestUtils.assertThat(collector).hasTrainState("regio", 1418, 1000, 0).hasTrainState("cargo", 1241, 1000, 0)
			.hasTrainState("sprinter", 1324, 1000, 0);

		test = getTestEngine("networkMixedTypes.xml");
		RailsimTestUtils.createDeparture(test, TestVehicle.Regio, "regio", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Cargo, "cargo", 0, "1-2", "20-21");
		RailsimTestUtils.createDeparture(test, TestVehicle.Sprinter, "sprinter", 0, "1-2", "20-21");

		test.doStateUpdatesUntil(2_000, 1);
		test.debugFiles(collector, utils.getOutputDirectory() + "/mixed_detailed");

		RailsimTestUtils.assertThat(collector).hasTrainState("regio", 1418, 1000, 0).hasTrainState("cargo", 1241, 1000, 0)
			.hasTrainState("sprinter", 1324, 1000, 0);

	}


}
