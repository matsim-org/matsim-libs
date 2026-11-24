package org.matsim.core.mobsim.qsim.qnetsimengine;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.snapshotwriters.SnapshotLinkWidthCalculator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * This suite of unit tests is not exhaustive! I wrote these tests, because I wanted to understand what is happening when the QueueWithBuffer is operated
 * with config::qsim.trafficDynamics = 'kinematicWaves'.
 */
class QueueWithBufferTest {

	@Test
	void initLinkQueue() {

		var config = ConfigUtils.createConfig();
		var context = createNetsimeEngineContext(config, new MobsimTimer());
		var link = createQLink(10, 2, 5400, context, config);

		assertEquals(1, link.getOfferingQLanes().size());
		assertEquals(link.getAcceptingQLane(), link.getOfferingQLanes().getFirst());
		QueueWithBuffer qwb = (QueueWithBuffer) link.getAcceptingQLane();
		assertEquals(1.5, qwb.getSimulatedFlowCapacityPerTimeStep());
		assertEquals(2 * 2, qwb.getStorageCapacity());
	}

	@Test
	void initLinkKinematicWaves() {

		var config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var context = createNetsimeEngineContext(config, new MobsimTimer());
		var link = createQLink(10, 2, 5400, context, config);

		assertEquals(1, link.getOfferingQLanes().size());
		assertEquals(link.getAcceptingQLane(), link.getOfferingQLanes().getFirst());
		QueueWithBuffer qwb = (QueueWithBuffer) link.getAcceptingQLane();
		assertEquals(1.5, qwb.getSimulatedFlowCapacityPerTimeStep());

		var vHole = 15 / 3.6;
		// according to https://doi.org/10.1080/23249935.2017.1364802 equation 7
		var expectedStorageCap = link.getSimulatedFlowCapacityPerTimeStep() * link.getLink().getLength() * ( 1 / link.getLink().getFreespeed() + 1 / vHole);
		assertEquals(expectedStorageCap, qwb.getStorageCapacity(), 0.001);
	}

	@Test
	void freeStorageQueue() {
		var config = ConfigUtils.createConfig();
		var timer = new MobsimTimer();
		var context = createNetsimeEngineContext(config, timer);
		var link = createQLink(10, 1, 1800, context, config);
		var driver = mock(MobsimDriverAgent.class);
		var vehicle1 = createVehicle("vehicle-1", driver, 10,1);
		var vehicle2 = createVehicle("vehicle-2", driver, 10,1);

		// the link should accept vehicles until the storage capacity is exhausted
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
		link.getAcceptingQLane().addFromUpstream(vehicle1);
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
		link.getAcceptingQLane().addFromUpstream(vehicle2);
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());

		// this should move one vehicle into the buffer, and free one pcu in the queue immediately
		timer.setTime(1);
		link.doSimStep();

		// test that capacity is available at the upstream end of the queue
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());

		// test that only one vehicle was moved into the buffer. Remove the first vehicle, and then the link should not offer more vehicles
		assertEquals(vehicle1.getId(), link.getOfferingQLanes().getFirst().popFirstVehicle().getId());
		// this double negation is so terrible!
		assertTrue(link.getOfferingQLanes().getFirst().isNotOfferingVehicle());
	}

	@Test
	void freeStorageKinematicWaves() {
		var config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var timer = new MobsimTimer();
		var context = createNetsimeEngineContext(config, timer);
		var link = createQLink(15, 1, 1800, context, config);
		var driver = mock(MobsimDriverAgent.class);
		// vehicle 1 needs 15m / 5m/s = 3s for the link, so that it is still on the link while the inflow capacity is refilled
		var vehicle1 = createVehicle("vehicle-1", driver, 5, 2);
		var vehicle2 = createVehicle("vehicle-2", driver, 1, 1);
		link.doSimStep();

		// the link should accept vehicles according to its max inflow capacity
		// vehicle consumes 2pcu. Max inflow should be: 1/cellSize / (1/vHole + 1/vMax) = 0.588...
		// the inflow capacity is refilled by the max inflow per time step, also for the time steps in which the link is not processed. We need 3
		// time steps to increase the accumulated inflow to a value over 0.
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
		link.getAcceptingQLane().addFromUpstream(vehicle1);
		// acc inflow is -1.412
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());
		timer.setTime(1);
		link.doSimStep();
		// acc inflow is -0.824
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());
		timer.setTime(2);
		link.doSimStep();
		// acc inflow is -0.236
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());

		// this should move vehicle 1 into the buffer, and start a backwards travelling hole
		timer.setTime(3);
		link.doSimStep();
		// acc inflow is +0.352 > 0, and the hole storage capacity left after vehicle 1 suffices for vehicle 2
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
		link.getAcceptingQLane().addFromUpstream(vehicle2);
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());

		// now, one vehicle should be in the buffer and one vehicle should be in the queue.
		// The link has free storage capacity, but it is not freed yet, because the leaving vehicle has sent a backwards travelling hole on its way.
		// the earliest exit time of that hole should be: now + length / vHole -> 3 + 15 * 3.6 / 15km/h = 6.6s
		// remove the first vehicle from the buffer and asser that it was the only vehicle in the buffer
		assertEquals(vehicle1.getId(), link.getOfferingQLanes().getFirst().popFirstVehicle().getId());
		assertTrue(link.getOfferingQLanes().getFirst().isNotOfferingVehicle());

		// acc inflow is -0.06
		timer.setTime(4);
		link.doSimStep();
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());
		assertTrue(link.getOfferingQLanes().getFirst().isNotOfferingVehicle());
		// from here on, the inflow capacity would accept a vehicle, but the storage capacity is not freed before the hole arrives
		timer.setTime(5);
		link.doSimStep();
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());
		assertTrue(link.getOfferingQLanes().getFirst().isNotOfferingVehicle());
		timer.setTime(6);
		link.doSimStep();
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());
		assertTrue(link.getOfferingQLanes().getFirst().isNotOfferingVehicle());
		timer.setTime(7); // 7 > 6.6: 6.6 is the arrival time of the backwards travelling hole.
		link.doSimStep();
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
		assertTrue(link.getOfferingQLanes().getFirst().isNotOfferingVehicle());
	}

	/**
	 * <a href="https://github.com/matsim-org/matsim-libs/issues/5239">#5239</a>: the inflow capacity is refilled also for the time steps in
	 * which the link is not processed, i.e. not active.
	 */
	@Test
	void inflowCapacityRefilledWhileInactiveKinematicWaves() {
		var config = ConfigUtils.createConfig();
		config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);
		var timer = new MobsimTimer();
		var context = createNetsimeEngineContext(config, timer);
		var link = createQLink(10, 1, 1800, context, config);
		var driver = mock(MobsimDriverAgent.class);
		var truck = createVehicle("truck", driver, 10, 3.5);
		link.doSimStep();

		// acc inflow is 0.588 - 3.5 = -2.912
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
		link.getAcceptingQLane().addFromUpstream(truck);
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());

		// the truck moves into the buffer at 1s, and its hole arrives at 1 + 10 * 3.6 / 15km/h = 3.4s. After that, the link is not active any
		// more, with acc inflow -2.912 + 4 * 0.588 = -0.56
		for (int now = 1; now <= 4; now++) {
			timer.setTime(now);
			link.doSimStep();
			if (now == 1) {
				assertEquals(truck.getId(), link.getOfferingQLanes().getFirst().popFirstVehicle().getId());
			}
		}
		assertFalse(link.isActive());

		// without further calls to doSimStep, the refill of 5s brings acc inflow to +0.028 > 0, which a vehicle from upstream sees at 6s
		timer.setTime(5);
		assertFalse(link.getAcceptingQLane().isAcceptingFromUpstream());
		timer.setTime(6);
		assertTrue(link.getAcceptingQLane().isAcceptingFromUpstream());
	}

	private static NetsimEngineContext createNetsimeEngineContext(Config config, MobsimTimer timer) {
		return new NetsimEngineContext(
			mock(EventsManager.class),
			5,
			mock(AgentCounter.class),
			mock(AbstractAgentSnapshotInfoBuilder.class),
			config.qsim(),
			timer,
			mock(SnapshotLinkWidthCalculator.class)
		);
	}

	QLinkImpl createQLink(double length, double lanes, double cap, NetsimEngineContext context, Config config) {
		var net = NetworkUtils.createNetwork();
		var n1 = net.getFactory().createNode(Id.createNodeId("n1"), new Coord(0, 0));
		var n2 = net.getFactory().createNode(Id.createNodeId("n2"), new Coord(0, 100));
		net.addNode(n1);
		net.addNode(n2);
		var link = net.getFactory().createLink(Id.createLinkId("test"), n1, n2);
		link.setCapacity(cap);
		link.setFreespeed(10);
		link.setLength(length);
		link.setNumberOfLanes(lanes);

		var internalInterface = mock(QNetsimEngineI.NetsimInternalInterface.class);
		QNodeImpl qNode = new QNodeImpl.Builder(internalInterface, context, config.qsim()).build(n2);
		qNode.setNetElementActivationRegistry(mock(NetElementActivationRegistry.class));
		var b = new QLinkImpl.Builder(context, internalInterface);
		b.setLinkSpeedCalculator(new DefaultLinkSpeedCalculator());
		b.setVehicleHandler(new DefaultVehicleHandler());
		var l =  b.build(link, qNode);
		l.setNetElementActivationRegistry(mock(NetElementActivationRegistry.class));
		return l;
	}

	QVehicle createVehicle(String id, MobsimDriverAgent driver, double maxV, double pcu) {

		var type = VehicleUtils.createVehicleType(Id.create("type", VehicleType.class));
		type.setMaximumVelocity(10);
		type.setPcuEquivalents(pcu);
		type.setMaximumVelocity(maxV);
		var vehicle = VehicleUtils.createVehicle(Id.createVehicleId(id), type);
		var result = new QVehicleImpl(vehicle);
		result.setDriver(driver);
		return result;
	}
 }
