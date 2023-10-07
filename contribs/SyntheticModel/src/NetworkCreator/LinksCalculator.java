package NetworkCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.util.List;
import java.util.ArrayList;

class LinksCalculator {

	public List<Link> getLinks(MultiSquareCoordinateCalculator calculator) {
		List<Node> nodes = calculator.getIntersections();
		List<Link> links = new ArrayList<>();

		for (Node fromNode : nodes) {
			Node closestRight = null;
			Node closestBelow = null;

			for (Node potentialNode : nodes) {
				if (fromNode.getCoord().getX() == potentialNode.getCoord().getX() && potentialNode.getCoord().getY() > fromNode.getCoord().getY()) {
					if (closestBelow == null || potentialNode.getCoord().getY() - fromNode.getCoord().getY() < closestBelow.getCoord().getY() - fromNode.getCoord().getY()) {
						closestBelow = potentialNode;
					}
				}

				if (fromNode.getCoord().getY() == potentialNode.getCoord().getY() && potentialNode.getCoord().getX() > fromNode.getCoord().getX()) {
					if (closestRight == null || potentialNode.getCoord().getX() - fromNode.getCoord().getX() < closestRight.getCoord().getX() - fromNode.getCoord().getX()) {
						closestRight = potentialNode;
					}
				}
			}

			if (closestRight != null) {
				links.add(createLink(fromNode, closestRight));
				links.add(createReverseLink(fromNode, closestRight));
			}
			if (closestBelow != null) {
				links.add(createLink(fromNode, closestBelow));
				links.add(createReverseLink(fromNode, closestBelow));
			}
		}

		return links;
	}

	private Link createLink(Node from, Node to) {
		double distance = NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord());
		Id<Link> linkId = Id.createLinkId(from.getId().toString() + "_" + to.getId());
		return NetworkUtils.createLink(linkId, from, to, null, distance, 12.0, 2000.0, 1.0);
	}

	private Link createReverseLink(Node from, Node to) {
		double distance = NetworkUtils.getEuclideanDistance(from.getCoord(), to.getCoord());
		Id<Link> linkId = Id.createLinkId(to.getId().toString() + "_" + from.getId() + "_r");
		return NetworkUtils.createLink(linkId, to, from, null, distance, 12.0, 2000.0, 1.0);
	}
}
