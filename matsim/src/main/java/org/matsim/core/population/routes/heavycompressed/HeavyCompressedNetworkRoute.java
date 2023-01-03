package org.matsim.core.population.routes.heavycompressed;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.AbstractNetworkRoute;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Implementation of {@link NetworkRoute} that tries to minimize the amount of
 * data needed to be stored for each route. This will give some memory savings,
 * allowing for larger scenarios (=more agents), especially on detailed
 * networks, but is likely a bit slower due to the more complex access of the
 * route information internally.</p>
 *
 * <p>Description of the compression algorithm:<br />
 * Given a map containing for each link a defined successor (subsequentLinks), this implementation
 * does not store the links in its route-information that are the same as the successor defined in the
 * subsequentLinks-map.<br />
 * Given a startLinkId, endLinkId and a list of linkIds to be stored, this implementation stores
 * first the startLinkId. Next, if the successor of the startLinkId is different from the first linkId
 * in the list, this linkId is stored, otherwise not. Then the successor of that linkId is compared to
 * the next linkId in the list. If the successor is different, the linkId is stored, otherwise not.
 * This procedure is repeated until the complete list of linkIds is processed.
 * </p>

 * @author mrieser / Simunto
 */
public class HeavyCompressedNetworkRoute extends AbstractNetworkRoute {

	private final static byte[] EMPTY_ROUTE = new byte[0];

	private final HeavyCompressedNetworkRouteFactory.CompressionData compressionData;
	private int uncompressedLength = -1;
	private byte[] route = EMPTY_ROUTE;

	public HeavyCompressedNetworkRoute(Id<Link> startLinkId, Id<Link> endLinkId, HeavyCompressedNetworkRouteFactory.CompressionData compressionData) {
		this.compressionData = compressionData;
		this.setStartLinkId(startLinkId);
		this.setEndLinkId(endLinkId);
	}

	@Override
	public HeavyCompressedNetworkRoute clone() {
		return (HeavyCompressedNetworkRoute) super.clone();
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		if (this.uncompressedLength < 0) { // it seems the route never got initialized correctly
			return new ArrayList<>(0);
		}
		ArrayList<Id<Link>> links = new ArrayList<>(this.uncompressedLength);
		Id<Link> previousLinkId = getStartLinkId();
		Id<Link> endLinkId = getEndLinkId();
		if ((previousLinkId == null) || (endLinkId == null)) {
			return links;
		}
		if (previousLinkId.equals(endLinkId) && this.uncompressedLength == 0) {
			return links;
		}
		int[] route = VarIntUtils.decode(this.route);
		for (int linkIndex : route) {
			Id<Link> linkId = Id.get(linkIndex, Link.class);
			getLinksTillLink(links, linkId, previousLinkId);
			links.add(linkId);
			previousLinkId = linkId;
		}
		getLinksTillLink(links, endLinkId, previousLinkId);

		return links;
	}

	private void getLinksTillLink(final List<Id<Link>> links, final Id<Link> nextLinkId, final Id<Link> startLinkId) {
		Link[] allLinks = this.compressionData.links();
		Link[] subsequentLinks = this.compressionData.subsequentLinks();
		Id<Link> linkId = startLinkId;
		Link nextLink = allLinks[nextLinkId.index()];
		while (true) { // loop until we hit "return;"
			Link link = allLinks[linkId.index()];
			if (link.getToNode() == nextLink.getFromNode()) {
				return;
			}
			linkId = subsequentLinks[linkId.index()].getId();
			links.add(linkId);
		}
	}

	@Override
	public void setLinkIds(final Id<Link> startLinkId, final List<Id<Link>> srcRoute, final Id<Link> endLinkId) {
		Link[] subsequentLinks = this.compressionData.subsequentLinks();
		Link[] links = this.compressionData.links();

		Set<Id<Node>> visitedNodes = new HashSet<>();
		Set<Id<Node>> multiplyVisitedNodes = new HashSet<>();
		setStartLinkId(startLinkId);
		setEndLinkId(endLinkId);
		if ((srcRoute == null) || (srcRoute.size() == 0)) {
			this.uncompressedLength = 0;
			return;
		}

		int pos = 0;
		int[] route = new int[srcRoute.size()];
		// compress route
		Id<Link> previousLinkId = startLinkId;
		for (Id<Link> linkId : srcRoute) {
			Link link = links[linkId.index()];
			Id<Node> fromNodeId = link.getFromNode().getId();
			if (!visitedNodes.add(fromNodeId)) {
				// the node was already visited
				multiplyVisitedNodes.add(fromNodeId);
			}
			if (!subsequentLinks[previousLinkId.index()].getId().equals(linkId)) {
				route[pos] = linkId.index();
				pos++;
			}
			previousLinkId = linkId;
		}
		Link endLink = links[endLinkId.index()];
		Id<Node> fromNodeId = endLink.getFromNode().getId();
		if (!visitedNodes.add(fromNodeId)) {
			// the node was already visited
			multiplyVisitedNodes.add(fromNodeId);
		}

		if (!multiplyVisitedNodes.isEmpty()) {
			// the route contains at least one loop, we need to re-encode it and make sure
			// that no loop is left out when reconstructing the uncompressed route
			pos = 0;
			previousLinkId = startLinkId;
			for (Id<Link> linkId : srcRoute) {
				Link link = links[linkId.index()];
				fromNodeId = link.getFromNode().getId();
				if (!subsequentLinks[previousLinkId.index()].getId().equals(linkId) || multiplyVisitedNodes.contains(fromNodeId)) {
					route[pos] = linkId.index();
					pos++;
				}
				previousLinkId = linkId;
			}
		}

		this.route = VarIntUtils.encode(route, 0, pos);
		this.uncompressedLength = srcRoute.size();
	}

}
