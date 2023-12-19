/**
 *
 */
package org.matsim.analysis;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author Aravind
 *
 */
public class VolumesAnalyzerTest {

	@RegisterExtension
	private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void performTest() {

		final Id<Link> link1 = Id.create(10723, Link.class);
		final Id<Link> link2 = Id.create(123160, Link.class);
		final Id<Link> link3 = Id.create(130181, Link.class);

		Id<Person> person1 = Id.create("1", Person.class);
		Id<Person> person2 = Id.create("2", Person.class);
		Id<Person> person3 = Id.create("3", Person.class);
		Id<Person> person4 = Id.create("4", Person.class);
		Id<Person> person5 = Id.create("5", Person.class);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		NetworkFactory factory = network.getFactory();

		Node n0, n1, n2, n3;
		network.addNode(n0 = factory.createNode(Id.createNodeId(0), new Coord(30.0, 50.0)));
		network.addNode(n1 = factory.createNode(Id.createNodeId(1), new Coord(1800.0, 2500.0)));
		network.addNode(n2 = factory.createNode(Id.createNodeId(2), new Coord(3000, 5200)));
		network.addNode(n3 = factory.createNode(Id.createNodeId(3), new Coord(1800, 3500)));
		Link LinkOne = factory.createLink(link1, n0, n1);
		Link LinkTwo = factory.createLink(link2, n1, n2);
		Link LinkThree = factory.createLink(link3, n2, n3);

		network.addLink(LinkOne);
		network.addLink(LinkTwo);
		network.addLink(LinkThree);

		VolumesAnalyzer analyzer = new VolumesAnalyzer(3600, 86400, network);
		Id<Vehicle> veh1 = Id.create("1001", Vehicle.class);
		Id<Vehicle> veh2 = Id.create("1002", Vehicle.class);
		Id<Vehicle> veh3 = Id.create("1003", Vehicle.class);
		Id<Vehicle> veh4 = Id.create("1004", Vehicle.class);
		Id<Vehicle> veh5 = Id.create("1005", Vehicle.class);

		analyzer.handleEvent(new VehicleEntersTrafficEvent(3600.0, person4, link1, veh4, TransportMode.car, 1.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(3610.0, person1, link1, veh1, TransportMode.car, 2.0));

		analyzer.handleEvent(new LinkLeaveEvent(5100, veh4, link1));
		analyzer.handleEvent(new LinkLeaveEvent(5410, veh1, link1));

		analyzer.handleEvent(new VehicleEntersTrafficEvent(7200.0, person2, link1, veh2, TransportMode.car, 1.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(7210.0, person5, link1, veh5, TransportMode.car, 2.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(7215.0, person4, link1, veh4, TransportMode.car, 3.0));

		analyzer.handleEvent(new LinkLeaveEvent(9000, veh2, link1));
		analyzer.handleEvent(new LinkLeaveEvent(8710, veh5, link1));
		analyzer.handleEvent(new LinkLeaveEvent(8895, veh4, link1));

		analyzer.handleEvent(new VehicleEntersTrafficEvent(10800.0, person1, link1, veh1, TransportMode.car, 1.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(10810.0, person3, link1, veh3, TransportMode.car, 2.0));

		analyzer.handleEvent(new LinkLeaveEvent(12600, veh1, link1));
		analyzer.handleEvent(new LinkLeaveEvent(12370, veh3, link1));

		analyzer.handleEvent(new VehicleEntersTrafficEvent(21600.0, person1, link1, veh1, TransportMode.car, 1.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(21650.0, person2, link1, veh2, TransportMode.car, 2.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(21700.0, person3, link1, veh3, TransportMode.car, 3.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(21750.0, person4, link1, veh4, TransportMode.car, 4.0));
		analyzer.handleEvent(new VehicleEntersTrafficEvent(21800.0, person5, link1, veh5, TransportMode.car, 5.0));

		analyzer.handleEvent(new LinkLeaveEvent(22800, veh1, link1));
		analyzer.handleEvent(new LinkLeaveEvent(23450, veh2, link1));
		analyzer.handleEvent(new LinkLeaveEvent(22800, veh3, link1));
		analyzer.handleEvent(new LinkLeaveEvent(22800, veh4, link1));
		analyzer.handleEvent(new LinkLeaveEvent(22800, veh5, link1));

		double[] volume = analyzer.getVolumesPerHourForLink(link1);
		int[] volumeForLink = analyzer.getVolumesForLink(link1);

		Assertions.assertEquals(volume[1], 2.0, 0);
		Assertions.assertEquals(volume[2], 3.0, 0);
		Assertions.assertEquals(volume[3], 2.0, 0);
		Assertions.assertEquals(volume[6], 5.0, 0);
		Assertions.assertEquals(volumeForLink[1], 2, 0);
		Assertions.assertEquals(volumeForLink[2], 3, 0);
		Assertions.assertEquals(volumeForLink[3], 2, 0);
		Assertions.assertEquals(volumeForLink[6], 5, 0);

		VolumesAnalyzer analyzerBike = new VolumesAnalyzer(3600, 86400, network, true);

		analyzerBike.handleEvent(new VehicleEntersTrafficEvent(21600.0, person1, link2, veh1, TransportMode.bike, 1.0));
		analyzerBike.handleEvent(new VehicleEntersTrafficEvent(21650.0, person2, link2, veh2, TransportMode.bike, 2.0));
		analyzerBike.handleEvent(new VehicleEntersTrafficEvent(21700.0, person3, link2, veh3, TransportMode.bike, 3.0));
		analyzerBike.handleEvent(new VehicleEntersTrafficEvent(21750.0, person4, link2, veh4, TransportMode.car, 4.0));
		analyzerBike.handleEvent(new VehicleEntersTrafficEvent(21800.0, person5, link2, veh5, TransportMode.car, 5.0));

		analyzerBike.handleEvent(new LinkLeaveEvent(22800, veh1, link2));
		analyzerBike.handleEvent(new LinkLeaveEvent(23450, veh2, link2));
		analyzerBike.handleEvent(new LinkLeaveEvent(22800, veh3, link2));
		analyzerBike.handleEvent(new LinkLeaveEvent(22800, veh4, link2));
		analyzerBike.handleEvent(new LinkLeaveEvent(22800, veh5, link2));

		double[] volumeBike = analyzerBike.getVolumesPerHourForLink(link2, TransportMode.bike);
		int[] volumeForLinkBike = analyzerBike.getVolumesForLink(link2, TransportMode.bike);
		Assertions.assertEquals(volumeBike[6], 3.0, 0);
		Assertions.assertEquals(volumeForLinkBike[6], 3, 0);

	}
}
