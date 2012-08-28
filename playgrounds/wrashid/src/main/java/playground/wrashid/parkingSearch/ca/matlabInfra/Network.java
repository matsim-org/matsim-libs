package playground.wrashid.parkingSearch.ca.matlabInfra;

import java.util.ArrayList;
import java.util.LinkedList;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.core.network.NetworkImpl;


public class Network {

	public static void main(String[] args) {
		NetworkImpl network = Config.getNetwork();

		LinkedList<Link> selectedLinks = new LinkedList();

		for (Link link : network.getLinks().values()) {
			if (Config.isInsideStudyArea(link.getFromNode().getCoord()) || Config.isInsideStudyArea(link.getToNode().getCoord())) {
				selectedLinks.add(link);
			}
		}

		writeOutLinks(Config.getOutputFolder() + "links.xml", selectedLinks);

		writeOutNodes(Config.getOutputFolder() +  "nodes.xml", getUniqueNodes(selectedLinks));

	}

	private static LinkedList<Node> getUniqueNodes(LinkedList<Link> selectedLinks) {
		LinkedList<Node> selectedNodes = new LinkedList<Node>();

		for (Link link : selectedLinks) {
			if (!selectedNodes.contains(link.getFromNode())) {
				selectedNodes.add(link.getFromNode());
			}
			if (!selectedNodes.contains(link.getToNode())) {
				selectedNodes.add(link.getToNode());
			}
		}

		return selectedNodes;
	}

	private static void writeOutNodes(String fileName, LinkedList<Node> selectedNodes) {
		ArrayList<String> list = new ArrayList<String>();

		list.add("<nodes>");

		for (Node node : selectedNodes) {
			StringBuffer stringBuffer = new StringBuffer();

			stringBuffer.append("\t<node>\n");

			stringBuffer.append("\t\t<id>" + node.getId() + "</id>\n");
			stringBuffer.append("\t\t<x>" + node.getCoord().getX() + "</x>\n");
			stringBuffer.append("\t\t<y>" + node.getCoord().getY() + "</y>\n");

			stringBuffer.append("\t</node>\n");

			list.add(stringBuffer.toString());
		}

		list.add("</nodes>\n");

		GeneralLib.writeList(list, fileName);
	}

	private static void writeOutLinks(String fileName, LinkedList<Link> selectedLinks) {
		ArrayList<String> list = new ArrayList<String>();

		list.add("<links>");

		for (Link link : selectedLinks) {
			StringBuffer stringBuffer = new StringBuffer();

			stringBuffer.append("\t<link>\n");

			stringBuffer.append("\t\t<id>" + link.getId() + "</id>\n");
			stringBuffer.append("\t\t<fromNode>" + link.getFromNode().getId() + "</fromNode>\n");
			stringBuffer.append("\t\t<toNode>" + link.getToNode().getId() + "</toNode>\n");

			stringBuffer.append("\t</link>\n");

			list.add(stringBuffer.toString());
		}

		list.add("</links>\n");

		GeneralLib.writeList(list, fileName);

	}

}
