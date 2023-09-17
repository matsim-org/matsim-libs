package NetworkCreator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

class LinksCalculator {
	private static final Pattern NODE_PATTERN = Pattern.compile("<node id=\"(\\d{2})(\\d{2})(\\d{2})\" x=\"([0-9\\.]+)\" y=\"([0-9\\.]+)\" />");

	public List<String> getLinks(MultiSquareCoordinateCalculator calculator) {
		List<String> intersections = calculator.getIntersections();
		List<String> links = new ArrayList<>();

		for (int i = 0; i < intersections.size(); i++) {
			Node fromNode = extractNode(intersections.get(i));
			Node closestRight = null;
			Node closestBelow = null;

			for (int j = 0; j < intersections.size(); j++) {
				Node potentialNode = extractNode(intersections.get(j));

				if (fromNode.x == potentialNode.x && potentialNode.y > fromNode.y) {
					if (closestBelow == null || potentialNode.y - fromNode.y < closestBelow.y - fromNode.y) {
						closestBelow = potentialNode;
					}
				}

				if (fromNode.y == potentialNode.y && potentialNode.x > fromNode.x) {
					if (closestRight == null || potentialNode.x - fromNode.x < closestRight.x - fromNode.x) {
						closestRight = potentialNode;
					}
				}
			}

			if (closestRight != null) links.addAll(createLink(fromNode, closestRight));
			if (closestBelow != null) links.addAll(createLink(fromNode, closestBelow));
		}

		return links;
	}

	private Node extractNode(String nodeXml) {
		Matcher matcher = NODE_PATTERN.matcher(nodeXml);
		if (matcher.find()) {
			String nodeId = matcher.group(1) + matcher.group(2) + matcher.group(3);
			double x = Double.parseDouble(matcher.group(4));
			double y = Double.parseDouble(matcher.group(5));
			return new Node(nodeId, x, y);
		} else {
			throw new RuntimeException("Unable to extract node data from XML: " + nodeXml);
		}
	}

	private List<String> createLink(Node from, Node to) {
		double distance = Math.sqrt(Math.pow(from.x - to.x, 2) + Math.pow(from.y - to.y, 2));

		String forwardLink = String.format("\t<link id=\"%s%s\" from=\"%s\" to=\"%s\" length=\"%.2f\" capacity=\"2000\" freespeed=\"12\" modes=\"car\" permlanes=\"1\" />",
			from.id, to.id, from.id, to.id, distance);

		String reverseLink = String.format("\t<link id=\"%s%s-r\" from=\"%s\" to=\"%s\" length=\"%.2f\" capacity=\"2000\" freespeed=\"12\" modes=\"car\" permlanes=\"1\" />",
			to.id, from.id, to.id, from.id, distance);

		return Arrays.asList(forwardLink, reverseLink);
	}

	static class Node {
		String id;
		double x, y;

		Node(String id, double x, double y) {
			this.id = id;
			this.x = x;
			this.y = y;
		}
	}
}
