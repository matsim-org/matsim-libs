package TransitCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.HashSet;
import java.util.Set;
public class RailLinkCreator {

	public static void main(String[] args) {
		RailLinkCreator creator = new RailLinkCreator();
		Network network = NetworkUtils.readNetwork("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\network22.xml");

		Coord coordStart = new Coord(100, 500);
		double[] distances = {300, 333, 350, 400, 410, 450, 1000, 1100, 1500, 2000};
		int numOfLinks = 10;

		if (distances.length != numOfLinks) {
			System.out.println("The number of distances provided does not match the number of links specified.");
			return;
		}

		Node previousNode = network.getFactory().createNode(Id.createNodeId("" + coordStart.getX()), coordStart);
		network.addNode(previousNode);

		for (int i = 0; i < numOfLinks; i++) {
			Coord coordEnd = new Coord(coordStart.getX() + distances[i], coordStart.getY());
			Node currentNode = network.getFactory().createNode(Id.createNodeId("" + coordEnd.getX()), coordEnd);
			network.addNode(currentNode);

			creator.createRailLink(network, previousNode, currentNode);

			previousNode = currentNode;
			coordStart = coordEnd;
		}

		new NetworkWriter(network).write("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\pathToSave.xml");
	}

	public void createRailLink(Network network, Node fromNode, Node toNode) {
		// Create the original link
		Link link = network.getFactory().createLink(Id.createLinkId("link-id_" + fromNode.getId() + "_" + toNode.getId()), fromNode, toNode);

		Set<String> allowedModes = new HashSet<>();
		allowedModes.add("pt");
		link.setAllowedModes(allowedModes);
		link.setCapacity(100);
		link.setFreespeed(12);

		network.addLink(link);

		// Create the reverse link
		Link reverseLink = network.getFactory().createLink(Id.createLinkId("link-id_" + fromNode.getId() + "_" + toNode.getId() + "_r"), toNode, fromNode);
		reverseLink.setAllowedModes(allowedModes);
		reverseLink.setCapacity(100);
		reverseLink.setFreespeed(12);
		network.addLink(reverseLink);
	}
}

