package playground.artemc.networkTools;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

public class NetworkSplitter {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		String networkPath = args[0];
		String newNetworkPath = args[1];
		String maxLinkLength = args[2];
		String linkMapPath = args[3];

		List<Node> nodes = new ArrayList<Node>();
		List<Link> newLinks = new ArrayList<Link>();
		List<Id> editedLinks = new ArrayList<Id>();
		HashMap<String, ArrayList<String>> linkMap = new HashMap<String, ArrayList<String>>();

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());

		BufferedWriter linkMapWrtiter = new BufferedWriter(new FileWriter(linkMapPath));

		new NetworkReaderMatsimV1(scenario).parse(networkPath);
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Set<Id> allLinks = new HashSet<Id>();

		for (Id link : network.getLinks().keySet()) {
			allLinks.add(link);
		}

		for (Id link : allLinks) {

			Link linkToEdit = network.getLinks().get(link);
			Node originNode = linkToEdit.getFromNode();
			editedLinks.add(linkToEdit.getId());
			linkMap.put(link.toString(), new ArrayList<String>());

			// Link length on map
			double x_diff = (linkToEdit.getToNode().getCoord().getX() - linkToEdit.getFromNode().getCoord().getX());
			double y_diff = (linkToEdit.getToNode().getCoord().getY() - linkToEdit.getFromNode().getCoord().getY());
			double totalLinkLengthOnMap = Math.sqrt((x_diff * x_diff) + (y_diff * y_diff));

			nodes.add(linkToEdit.getFromNode());

			Integer newNumberOfLinks = (int) Math.ceil(linkToEdit.getLength() / Double.valueOf(maxLinkLength));
			Double newLinkLength = linkToEdit.getLength() / newNumberOfLinks;

			double nodeDistanceOnMap = totalLinkLengthOnMap / newNumberOfLinks;

			Integer count = 0;
			Coord newNodeXY = null;
			Id newLinkId = null;
			Id newNodeId = null;

			System.out.println("Link: " + linkToEdit.getId() + "  New number of links: " + newNumberOfLinks
					+ "  New link length: " + newLinkLength);

			while (count < newNumberOfLinks) {
				// Create new Link
				newLinkId = Id.create(linkToEdit.getId().toString() + "_" + (count + 1), Link.class);
				linkMap.get(link.toString()).add(newLinkId.toString());

				if (newNumberOfLinks.intValue() == (count.intValue() + 1)) {
					nodes.add(linkToEdit.getToNode());
				} else {
					newNodeId = Id.create(originNode.getId().toString() + "_" + linkToEdit.getId().toString()
							+ (count + 1), Node.class);
					newNodeXY = convertDistanceToCoordinates(linkToEdit, nodeDistanceOnMap);
					network.createAndAddNode(newNodeId, newNodeXY);
					nodes.add(network.getNodes().get(newNodeId));
					nodeDistanceOnMap = nodeDistanceOnMap + totalLinkLengthOnMap / newNumberOfLinks;
					System.out.println("   Adding new node: " + newNodeId.toString() + " with coord: "
							+ network.getNodes().get(newNodeId).getCoord());
				}

				network.createAndAddLink(newLinkId, nodes.get(nodes.size() - 2), nodes.get(nodes.size() - 1), newLinkLength,
						linkToEdit.getFreespeed(), linkToEdit.getCapacity(), linkToEdit.getNumberOfLanes());
				network.getLinks().get(newLinkId).setAllowedModes(linkToEdit.getAllowedModes());
				newLinks.add(network.getLinks().get(newLinkId));
				count++;
				System.out.println("   New Link:" + newLinkId.toString() + " From:" + nodes.get(nodes.size() - 2).getId()
						+ " To:" + nodes.get(nodes.size() - 1).getId());
			}

		}

		// Removing edited links
		for (Id linkToRemove : editedLinks) {
			network.removeLink(linkToRemove);
		}

		for (String oldLink : linkMap.keySet()) {
			linkMapWrtiter.newLine();
			linkMapWrtiter.write(oldLink);
			for (int i = 0; i < linkMap.get(oldLink).size(); i++) {
				linkMapWrtiter.write("," + linkMap.get(oldLink).get(i));
			}
		}
		linkMapWrtiter.close();

		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write(newNetworkPath);
	}

	private static Coord convertDistanceToCoordinates(Link link, double distance) {
		double x_diff = (link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX());
		double y_diff = (link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY());
		double thetarad = Math.atan2(y_diff, x_diff);
		// System.out.println("Theta: "+thetarad);
		double x = distance * Math.cos(thetarad);
		double y = distance * Math.sin(thetarad);
		Coord pointXY = new Coord(link.getFromNode().getCoord().getX() + x, link.getFromNode().getCoord().getY() + y);
		// System.out.println("      x: "+(link.getFromNode().getCoord().getX()
		// + x)+" y: "+(link.getFromNode().getCoord().getY() + y));
		return pointXY;
	}

}
