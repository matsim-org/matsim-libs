/**
 * 
 */
package playground.mzilske.city2000w;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author schroeder
 *
 */
public class GridCreator {

	private static final int GRID_SIZE = 100;

	private static final String NETWORK_FILENAME = "output/grid1000.xml";

	private Scenario scenario;

	private Network network;

	public GridCreator(Scenario scenario) {
		this.scenario = scenario;
		this.network = scenario.getNetwork();
	}

	public void createGrid(int size) {
		for (int i = 0; i <= size; i++) {
			for (int j = 0; j <= size; j++) {
				Node node = network.getFactory().createNode(makeId(i, j, Node.class), makeCoord(i, j));
				network.addNode(node);
				if (i != 0) {
					Link iLink = network.getFactory().createLink(makeLinkId(i, j), network.getNodes().get(makeId(i-1, j, Node.class)), network.getNodes().get(makeId(i, j, Node.class)));
					iLink.setLength(1000);
					iLink.setFreespeed(100);
					iLink.setCapacity(1000);
					Link iLinkR = network.getFactory().createLink(Id.create("i("+i+","+j+")"+"R", Link.class), network.getNodes().get(makeId(i, j, Node.class)), network.getNodes().get(makeId(i-1, j, Node.class)));
					iLinkR.setLength(1000);
					iLinkR.setFreespeed(100);
					iLinkR.setCapacity(1000);
					network.addLink(iLink);
					network.addLink(iLinkR);
				}
				if (j != 0) {
					Link jLink = network.getFactory().createLink(Id.create("j("+i+","+j+")", Link.class), network.getNodes().get(makeId(i, j-1, Node.class)), network.getNodes().get(makeId(i, j, Node.class)));
					jLink.setLength(1000);
					jLink.setFreespeed(100);
					jLink.setCapacity(1000);
					Link jLinkR = network.getFactory().createLink(Id.create("j("+i+","+j+")"+"R", Link.class), network.getNodes().get(makeId(i, j, Node.class)), network.getNodes().get(makeId(i, j-1, Node.class)));
					jLinkR.setLength(1000);
					jLinkR.setFreespeed(100);
					jLinkR.setCapacity(1000);
					network.addLink(jLink);
					network.addLink(jLinkR);
				}
			}
		}
	}

	private Id<Link> makeLinkId(int i, int j) {
		return Id.create("i("+i+","+j+")", Link.class);
	}

	private Coord makeCoord(int i, int j) {
		return new Coord((double) (i * 1000), (double) (j * 1000));
	}

	private <T> Id<T> makeId(int i, int j, Class<T> type) {
		return Id.create("("+i+","+j+")", type);
	}
	
	private void writeNetwork() {
		Network network = (Network) scenario.getNetwork();
		new NetworkWriter(network).write(NETWORK_FILENAME);
	}
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Scenario scenario = ScenarioUtils.createScenario(config);
		GridCreator gridCreator = new GridCreator(scenario);
		gridCreator.createGrid(GRID_SIZE);
		gridCreator.writeNetwork();
	}
	
}
