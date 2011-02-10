/**
 * 
 */
package playground.mzilske.city2000w;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;

/**
 * @author schroeder
 *
 */
public class GridWithSpikeCreator {

	private static final int GRID_SIZE = 8;

	private static final String NETWORK_FILENAME = "output/gridWithSpike.xml";

	private Scenario scenario;

	private void createGrid(int size) {
		Config config = new Config();
		config.addCoreModules();
		scenario = new ScenarioImpl(config);
		Network network = scenario.getNetwork();
		for (int i = 0; i <= size; i++) {
			for (int j = 0; j <= size; j++) {
				Node node = network.getFactory().createNode(makeId(i, j), makeCoord(i, j));
				network.addNode(node);
				if (i != 0) {
					Link iLink = network.getFactory().createLink(makeLinkId(i, j), makeId(i-1, j), makeId(i, j));
					iLink.setLength(100);
					iLink.setFreespeed(100);
					iLink.setCapacity(1000);
					Link iLinkR = network.getFactory().createLink(scenario.createId("i("+i+","+j+")"+"R"), makeId(i, j),makeId(i-1, j));
					iLinkR.setLength(100);
					iLinkR.setFreespeed(100);
					iLinkR.setCapacity(1000);
					network.addLink(iLink);
					network.addLink(iLinkR);
				}
				if (j != 0) {
					Link jLink = network.getFactory().createLink(scenario.createId("j("+i+","+j+")"), makeId(i, j-1), makeId(i, j));
					jLink.setLength(100);
					jLink.setFreespeed(100);
					jLink.setCapacity(1000);
					Link jLinkR = network.getFactory().createLink(scenario.createId("j("+i+","+j+")"+"R"), makeId(i, j), makeId(i, j-1));
					jLinkR.setLength(100);
					jLinkR.setFreespeed(100);
					jLinkR.setCapacity(1000);
					network.addLink(jLink);
					network.addLink(jLinkR);
				}
			}
		}
		createSpike(network);
	}

	private void createSpike(Network network) {
		Node spikeNode = network.getFactory().createNode(scenario.createId("sNodeID"), makeCoord(16,4));
		network.addNode(spikeNode);
		
		Link spike = network.getFactory().createLink(scenario.createId("spike"),spikeNode.getId(), makeId(8,4));
		spike.setLength(800);
		spike.setFreespeed(100);
		spike.setCapacity(1000);
		Link spikeR = network.getFactory().createLink(scenario.createId("spikeR"), makeId(8,4), spikeNode.getId());
		spikeR.setLength(800);
		spikeR.setFreespeed(100);
		spikeR.setCapacity(1000);
		network.addLink(spike);
		network.addLink(spikeR);
	}

	private Id makeLinkId(int i, int j) {
		return scenario.createId("i("+i+","+j+")");
	}

	private Coord makeCoord(int i, int j) {
		return scenario.createCoord(i * 100, j * 100);
	}

	private Id makeId(int i, int j) {
		return scenario.createId("("+i+","+j+")");
	}
	
	private void writeNetwork() {
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		new NetworkWriter(network).write(NETWORK_FILENAME);
	}
	
	void run() {
		createGrid(8);
		writeNetwork();
	}
	
	public static void main(String[] args) {
		GridWithSpikeCreator gridCreator = new GridWithSpikeCreator();
		gridCreator.run();
	}
	
}
