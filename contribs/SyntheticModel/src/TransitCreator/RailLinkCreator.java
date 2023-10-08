package TransitCreator;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class RailLinkCreator {
// This has been superseded by RailScheduleCreator

	public static void main(String[] args) {
		// Instantiate the RailLinkCreator
		RailLinkCreator creator = new RailLinkCreator();
		String scenarioPath = "examples/scenarios/UrbanLine/40kmx1km";
		Coord coordStart = new Coord(100, 500);
		double[] distances = {
			// First half: links from 700 to 1500
			700.0, 742.0, 868.0, 910.0, 952.0,
			1036.0, 1246.0,
			1414.0, 1456.0,
			// Next quarter: links from 1500 to 3000
			1666.0, 1998.0, 2330.0, 2994.0,
			//  last bit: 10 links from 2500 to 3500
			2765.0, 2895.0
		};
		creator.generateLinks(scenarioPath,coordStart,distances);
	}

	public void generateLinks(String scenarioPath, Coord coordStart, double[] distances) {

		Network network = null;
		try {
			// Attempt to read the network from the XML file
			network = NetworkUtils.readNetwork(scenarioPath + "/network.xml");
		} catch (Exception e) {
			System.err.println("Error reading the network file.");
			e.printStackTrace();
			return;  // Exit the program or handle the error accordingly
		}


		int numOfLinks = distances.length;

		// Create an initial extra node to the left of the coordStart
		Coord initialExtraCoord = new Coord(coordStart.getX() - 1, coordStart.getY());
		Node InitialStation = network.getFactory().createNode(Id.createNodeId("station" + initialExtraCoord.getX()), initialExtraCoord);
		network.addNode(InitialStation);  // Add this node to the network

		// Create the starting node to act as a station for turn around
		Node previousNode = network.getFactory().createNode(Id.createNodeId("station" + coordStart.getX()), coordStart);
		network.addNode(previousNode);
		createRailLink(network, InitialStation, previousNode);

		// Iterate to create the main nodes and links
		for (int i = 0; i < numOfLinks; i++) {
			// Calculate the end coordinate for the current link
			Coord coordEnd = new Coord(coordStart.getX() + distances[i], coordStart.getY());

			String nodeId = "station" + coordEnd.getX();
			System.out.println("Creating node with ID: " + nodeId + " and Coordinates: (" + coordEnd.getX() + ", " + coordEnd.getY() + ")");

			Node currentNode = network.getFactory().createNode(Id.createNodeId(nodeId), coordEnd);
			network.addNode(currentNode);  // Add this node to the network

			// Connect the previous node to the current node
			createRailLink(network, previousNode, currentNode);

			// Update the previous node and start coordinate for the next iteration
			previousNode = currentNode;
			coordStart = coordEnd;
		}


		// Create a final extra node to the right of the last coordEnd
		Coord finalExtraCoord = new Coord(coordStart.getX() + 1, coordStart.getY());
		Node finalExtraNode = network.getFactory().createNode(Id.createNodeId("station" + finalExtraCoord.getX()), finalExtraCoord);
		network.addNode(finalExtraNode);
		createRailLink(network, previousNode, finalExtraNode);

		// Write the modified network back to the XML file
		new NetworkWriter(network).write(scenarioPath + "/network.xml");
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

		generatedLinkIds.add(link.getId()); // Store the link ID when you create a link

	}
	private final List<Id<Link>> generatedLinkIds = new ArrayList<>(); // Add this field to store generated link IDs

	public List<Id<Link>> getGeneratedLinkIds() {
		return generatedLinkIds;
	}

}

