/**
 * 
 */
package playground.southafrica.utilities.mapmatching;

import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.MatsimTestUtils;

/**
 * Class to test the matching of the TrackMatching xml records to a MATSim
 * network.
 * 
 * @author jwjoubert
 */
public class TrackMatchingConverterTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void test() {
		setup();
		TrackMatchingXmlReader tmx = new TrackMatchingXmlReader();
		tmx.setValidating(false);
		tmx.parse(utils.getClassInputDirectory() + "test.xml");
		List<Tuple<Id<Node>, Id<Node>>> nodes = tmx.getLargestRoute();
		
		TrackMatchingConverter tmc = new TrackMatchingConverter(utils.getOutputDirectory() + "network.xml");
		Route route = tmc.mapRouteToNetwork(nodes);
		Assert.assertEquals("Wrong first link in route", Id.createLinkId("1"), route.getStartLinkId());
		Assert.assertEquals("Wrong last link in route", Id.createLinkId("2"), route.getEndLinkId());
	}
	
	private void setup(){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = sc.getNetwork();
		NetworkFactory nf = sc.getNetwork().getFactory();
		Node n1 = nf.createNode(Id.createNodeId("1"), new Coord(0.0, 0.0));
		Node n2 = nf.createNode(Id.createNodeId("2"), new Coord(10.0, 0.0));
		Node n3 = nf.createNode(Id.createNodeId("3"), new Coord(20.0, 0.0));
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		
		Link l1 = nf.createLink(Id.createLinkId("1"), n1, n2);
		Link l2 = nf.createLink(Id.createLinkId("2"), n2, n3);
		network.addLink(l1);
		network.addLink(l2);
		
		new NetworkWriter(network).write(utils.getOutputDirectory() + "network.xml");
	}

}
