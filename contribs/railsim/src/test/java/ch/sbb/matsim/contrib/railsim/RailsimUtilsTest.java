package ch.sbb.matsim.contrib.railsim;

import ch.sbb.matsim.contrib.railsim.config.RailsimConfigGroup;
import ch.sbb.matsim.contrib.railsim.qsimengine.resources.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RailsimUtilsTest {

	private Network network;
	private Link link;
	private VehicleType vehicleType;
	private RailsimConfigGroup configGroup;
	private Departure departure;

	@BeforeEach
	void setUp() {
		// Create network
		network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();

		// Create nodes and link
		Node fromNode = networkFactory.createNode(Id.createNodeId("from"), new Coord(0, 0));
		Node toNode = networkFactory.createNode(Id.createNodeId("to"), new Coord(100, 0));
		network.addNode(fromNode);
		network.addNode(toNode);

		link = networkFactory.createLink(Id.createLinkId("link"), fromNode, toNode);
		network.addLink(link);

		// Create vehicle type
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();
		vehicleType = vehiclesFactory.createVehicleType(Id.create("train", VehicleType.class));
		vehicles.addVehicleType(vehicleType);

		// Create config group
		configGroup = new RailsimConfigGroup();
		configGroup.setAccelerationDefault(1.0);
		configGroup.setDecelerationDefault(2.0);

		// Create transit schedule and departure
		TransitScheduleFactory scheduleFactory = new TransitScheduleFactoryImpl();
		TransitSchedule schedule = scheduleFactory.createTransitSchedule();
		TransitLine line = scheduleFactory.createTransitLine(Id.create("line", TransitLine.class));
		schedule.addTransitLine(line);

		// Create a simple departure for testing
		departure = scheduleFactory.createDeparture(Id.create("departure", Departure.class), 0.0);
	}

	/**
	 * Utility method to test that attributes persist correctly by writing the network to a file
	 * and reading it back, then retrieving the link and verifying the attributes.
	 */
	private Link writeAndReadNetwork(Link originalLink) throws IOException {
		// Write network to temporary file
		File tempFile = File.createTempFile("test_network", ".xml");
		tempFile.deleteOnExit();

		NetworkUtils.writeNetwork(network, tempFile.getAbsolutePath());
		Network readNetwork = NetworkUtils.readNetwork(tempFile.getAbsolutePath());

		// Get the link from the read network
		return readNetwork.getLinks().get(originalLink.getId());
	}

	@Test
	void testRound() {
		assertThat(RailsimUtils.round(1.2345)).isCloseTo(1.234, within(0.001));
		assertThat(RailsimUtils.round(1.2355)).isCloseTo(1.236, within(0.001));
		assertThat(RailsimUtils.round(1.2344)).isCloseTo(1.234, within(0.001));
		assertThat(RailsimUtils.round(0.0)).isCloseTo(0.0, within(0.001));
		assertThat(RailsimUtils.round(-1.2345)).isCloseTo(-1.234, within(0.001));
	}

	@Test
	void testTrainCapacity() throws IOException {
		// Test default capacity
		assertThat(RailsimUtils.getTrainCapacity(link)).isEqualTo(1);

		// Test setting and getting capacity
		RailsimUtils.setTrainCapacity(link, 5);
		assertThat(RailsimUtils.getTrainCapacity(link)).isEqualTo(5);

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.getTrainCapacity(readLink)).isEqualTo(5);

		// Test with null attribute
		link.getAttributes().removeAttribute(RailsimUtils.LINK_ATTRIBUTE_CAPACITY);
		assertThat(RailsimUtils.getTrainCapacity(link)).isEqualTo(1);
	}

	@Test
	void testMinimumHeadwayTime() throws IOException {
		// Test default minimum time
		assertThat(RailsimUtils.getMinimumHeadwayTime(link)).isCloseTo(0.0, within(0.001));

		// Test setting and getting minimum time
		RailsimUtils.setMinimumHeadwayTime(link, 10.5);
		assertThat(RailsimUtils.getMinimumHeadwayTime(link)).isCloseTo(10.5, within(0.001));

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.getMinimumHeadwayTime(readLink)).isCloseTo(10.5, within(0.001));

		// Test with null attribute
		link.getAttributes().removeAttribute(RailsimUtils.LINK_ATTRIBUTE_MINIMUM_TIME);
		assertThat(RailsimUtils.getMinimumHeadwayTime(link)).isCloseTo(0.0, within(0.001));
	}

	@Test
	void testResourceId() throws IOException {
		// Test default (no resource id)
		assertThat(RailsimUtils.getResourceId(link)).isNull();

		// Test setting and getting resource id
		RailsimUtils.setResourceId(link, "resource123");
		assertThat(RailsimUtils.getResourceId(link)).isEqualTo("resource123");

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.getResourceId(readLink)).isEqualTo("resource123");

		// Test with null
		RailsimUtils.setResourceId(link, null);
		assertThat(RailsimUtils.getResourceId(link)).isNull();
	}

	@Test
	void testLinkNonBlockingArea() throws IOException {
		// Test default (not a non-blocking area)
		assertThat(RailsimUtils.isLinkNonBlockingArea(link)).isFalse();

		// Test setting and getting non-blocking area
		RailsimUtils.setLinkNonBlockingArea(link, true);
		assertThat(RailsimUtils.isLinkNonBlockingArea(link)).isTrue();

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.isLinkNonBlockingArea(readLink)).isTrue();

		RailsimUtils.setLinkNonBlockingArea(link, false);
		assertThat(RailsimUtils.isLinkNonBlockingArea(link)).isFalse();
	}

	@Test
	void testEntryLink() throws IOException {
		// Test default (not an entry link)
		assertThat(RailsimUtils.isEntryLink(link)).isFalse();

		// Test setting and getting entry link
		RailsimUtils.setEntryLink(link, true);
		assertThat(RailsimUtils.isEntryLink(link)).isTrue();

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.isEntryLink(readLink)).isTrue();

		RailsimUtils.setEntryLink(link, false);
		assertThat(RailsimUtils.isEntryLink(link)).isFalse();
	}

	@Test
	void testExitLink() throws IOException {
		// Test default (not an exit link)
		assertThat(RailsimUtils.isExitLink(link)).isFalse();

		// Test setting and getting exit link
		RailsimUtils.setExitLink(link, true);
		assertThat(RailsimUtils.isExitLink(link)).isTrue();

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.isExitLink(readLink)).isTrue();

		RailsimUtils.setExitLink(link, false);
		assertThat(RailsimUtils.isExitLink(link)).isFalse();
	}

	@Test
	void testTrainDeceleration() {
		// Test default deceleration
		assertThat(RailsimUtils.getTrainDeceleration(vehicleType, configGroup)).isCloseTo(2.0, within(0.001));

		// Test setting and getting deceleration
		RailsimUtils.setTrainDeceleration(vehicleType, 3.5);
		assertThat(RailsimUtils.getTrainDeceleration(vehicleType, configGroup)).isCloseTo(3.5, within(0.001));

		// Test with different number types
		vehicleType.getAttributes().putAttribute(RailsimUtils.VEHICLE_ATTRIBUTE_DECELERATION, 4.0f);
		assertThat(RailsimUtils.getTrainDeceleration(vehicleType, configGroup)).isCloseTo(4.0, within(0.001));

		vehicleType.getAttributes().putAttribute(RailsimUtils.VEHICLE_ATTRIBUTE_DECELERATION, 5);
		assertThat(RailsimUtils.getTrainDeceleration(vehicleType, configGroup)).isCloseTo(5.0, within(0.001));
	}

	@Test
	void testTrainAcceleration() {
		// Test default acceleration
		assertThat(RailsimUtils.getTrainAcceleration(vehicleType, configGroup)).isCloseTo(1.0, within(0.001));

		// Test setting and getting acceleration
		RailsimUtils.setTrainAcceleration(vehicleType, 2.5);
		assertThat(RailsimUtils.getTrainAcceleration(vehicleType, configGroup)).isCloseTo(2.5, within(0.001));

		// Test with different number types
		vehicleType.getAttributes().putAttribute(RailsimUtils.VEHICLE_ATTRIBUTE_ACCELERATION, 3.0f);
		assertThat(RailsimUtils.getTrainAcceleration(vehicleType, configGroup)).isCloseTo(3.0, within(0.001));

		vehicleType.getAttributes().putAttribute(RailsimUtils.VEHICLE_ATTRIBUTE_ACCELERATION, 4);
		assertThat(RailsimUtils.getTrainAcceleration(vehicleType, configGroup)).isCloseTo(4.0, within(0.001));
	}

	@Test
	void testTrainReversible() {
		// Test default (not reversible)
		assertThat(RailsimUtils.getTrainReversible(vehicleType)).isEmpty();

		// Test setting and getting reversible
		RailsimUtils.setTrainReversible(vehicleType, 5.0);
		OptionalDouble reversible = RailsimUtils.getTrainReversible(vehicleType);
		assertThat(reversible).isPresent();
		assertThat(reversible.getAsDouble()).isCloseTo(5.0, within(0.001));

		// Test setting to null (not reversible)
		RailsimUtils.setTrainReversible(vehicleType, null);
		assertThat(RailsimUtils.getTrainReversible(vehicleType)).isEmpty();

		// Test with different number types
		vehicleType.getAttributes().putAttribute(RailsimUtils.VEHICLE_ATTRIBUTE_REVERSIBLE, 3.0f);
		reversible = RailsimUtils.getTrainReversible(vehicleType);
		assertThat(reversible).isPresent();
		assertThat(reversible.getAsDouble()).isCloseTo(3.0, within(0.001));
	}

	@Test
	void testLinkVMax() throws IOException {
		// Test default (no vMax set)
		assertThat(RailsimUtils.getLinkVMax(link)).isEmpty();

		// Test setting and getting vMax
		Map<Id<VehicleType>, Double> vMaxMap = new HashMap<>();
		vMaxMap.put(Id.create("train1", VehicleType.class), 50.0);
		vMaxMap.put(Id.create("train2", VehicleType.class), 80.0);

		RailsimUtils.setLinkVMax(link, vMaxMap);
		Map<Id<VehicleType>, Double> retrievedMap = RailsimUtils.getLinkVMax(link);
		assertThat(retrievedMap).isNotNull();
		assertThat(retrievedMap).hasSize(2);
		assertThat(retrievedMap.get(Id.create("train1", VehicleType.class))).isCloseTo(50.0, within(0.001));
		assertThat(retrievedMap.get(Id.create("train2", VehicleType.class))).isCloseTo(80.0, within(0.001));

		Link readLink = writeAndReadNetwork(link);
		retrievedMap = RailsimUtils.getLinkVMax(readLink);
		assertThat(retrievedMap).isNotNull();
		assertThat(retrievedMap).hasSize(2);
		assertThat(retrievedMap.get(Id.create("train1", VehicleType.class))).isCloseTo(50.0, within(0.001));
		assertThat(retrievedMap.get(Id.create("train2", VehicleType.class))).isCloseTo(80.0, within(0.001));

		// Test with empty map
		RailsimUtils.setLinkVMax(link, new HashMap<>());
		retrievedMap = RailsimUtils.getLinkVMax(link);
		assertThat(retrievedMap).isNotNull();
		assertThat(retrievedMap).isEmpty();
	}

	@Test
	void testResourceType() throws IOException {
		// Test default (fixed block)
		assertThat(RailsimUtils.getResourceType(link)).isEqualTo(ResourceType.fixedBlock);

		// Test setting and getting resource type
		RailsimUtils.setResourceType(link, ResourceType.movingBlock);
		assertThat(RailsimUtils.getResourceType(link)).isEqualTo(ResourceType.movingBlock);

		// Test persistence by writing and reading network
		Link readLink = writeAndReadNetwork(link);
		assertThat(RailsimUtils.getResourceType(readLink)).isEqualTo(ResourceType.movingBlock);

		RailsimUtils.setResourceType(link, ResourceType.fixedBlock);
		assertThat(RailsimUtils.getResourceType(link)).isEqualTo(ResourceType.fixedBlock);
	}

	@Test
	void testFormation() {
		// Test default (empty formation)
		assertThat(RailsimUtils.getFormation(departure)).isEmpty();

		// Test setting and getting formation
		List<String> formation = List.of("unit1", "unit2", "unit3");
		RailsimUtils.setFormation(departure, formation);
		List<String> retrievedFormation = RailsimUtils.getFormation(departure);
		assertThat(retrievedFormation).hasSize(3);
		assertThat(retrievedFormation).containsExactly("unit1", "unit2", "unit3");

		// Test with empty formation
		RailsimUtils.setFormation(departure, List.of());
		assertThat(RailsimUtils.getFormation(departure)).isEmpty();

		// Test with single unit
		RailsimUtils.setFormation(departure, List.of("singleUnit"));
		retrievedFormation = RailsimUtils.getFormation(departure);
		assertThat(retrievedFormation).hasSize(1);
		assertThat(retrievedFormation).containsExactly("singleUnit");
	}
}
