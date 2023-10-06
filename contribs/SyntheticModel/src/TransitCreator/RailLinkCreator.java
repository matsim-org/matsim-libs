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
		// Instantiate the RailLinkCreator
		RailLinkCreator creator = new RailLinkCreator();

		// Read the network from the XML file
		Network network = NetworkUtils.readNetwork("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\network22.xml");

		// Define the initial coordinate and distances for the rail links
		Coord coordStart = new Coord(100, 500);
		double[] distances = {300, 333, 350, 400, 410, 450, 1000, 1100, 1500, 2000};
		int numOfLinks = 10;

		// Create an initial extra node to the left of the coordStart
		Coord initialExtraCoord = new Coord(coordStart.getX() - 1, coordStart.getY());
		Node InitialStation = network.getFactory().createNode(Id.createNodeId(String.valueOf(initialExtraCoord.getX())), initialExtraCoord);
		network.addNode(InitialStation);  // Add this node to the network

		// Create the starting node to act as a station for turn around
		Node previousNode = network.getFactory().createNode(Id.createNodeId(String.valueOf(coordStart.getX())), coordStart);
		network.addNode(previousNode);
		creator.createRailLink(network, InitialStation, previousNode);

		// Iterate to create the main nodes and links
		for (int i = 0; i < numOfLinks; i++) {
			// Calculate the end coordinate for the current link
			Coord coordEnd = new Coord(coordStart.getX() + distances[i], coordStart.getY());
			Node currentNode = network.getFactory().createNode(Id.createNodeId(String.valueOf(coordEnd.getX())), coordEnd);
			network.addNode(currentNode);  // Add this node to the network

			// Connect the previous node to the current node
			creator.createRailLink(network, previousNode, currentNode);

			// Update the previous node and start coordinate for the next iteration
			previousNode = currentNode;
			coordStart = coordEnd;
		}

		// Create a final extra node to the right of the last coordEnd
		Coord finalExtraCoord = new Coord(coordStart.getX() + 1, coordStart.getY());
		Node finalExtraNode = network.getFactory().createNode(Id.createNodeId("" + finalExtraCoord.getX()), finalExtraCoord);
		network.addNode(finalExtraNode);
		creator.createRailLink(network, previousNode, finalExtraNode);

		// Write the modified network back to the XML file
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

