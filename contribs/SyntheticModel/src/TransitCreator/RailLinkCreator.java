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


	public static void main(String[] args) {
		// Instantiate the RailLinkCreator
		RailLinkCreator creator = new RailLinkCreator();
		creator.generateLinks();
	}

	public void generateLinks() {

		Network network = null;
		try {
			// Attempt to read the network from the XML file
			network = NetworkUtils.readNetwork("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\40kmx1km\\network40x1km.xml");
		} catch (Exception e) {
			System.err.println("Error reading the network file.");
			e.printStackTrace();
			return;  // Exit the program or handle the error accordingly
		}

		// Define the initial coordinate and distances for the rail links
		Coord coordStart = new Coord(100, 500);
		double[] distances = {
			// First half: 20 links from 700 to 1500
			700, 742, 784, 868, 910, 952,
			1036, 1120, 1204, 1246, 1288,
			1372, 1414, 1456,
			// Next quarter: 10 links from 1500 to 3000
			1666, 1832, 1998, 2330,
			2496, 2994, 3000,
			// Last quarter: 10 links from 2500 to 3500
			2700, 2765,
			2895, 2960, 3090, 3155
		};
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
		new NetworkWriter(network).write("C:\\Users\\snasi\\IdeaProjects\\matsim-libs\\examples\\scenarios\\UrbanLine\\40kmx1km\\network_pt.xml");
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

