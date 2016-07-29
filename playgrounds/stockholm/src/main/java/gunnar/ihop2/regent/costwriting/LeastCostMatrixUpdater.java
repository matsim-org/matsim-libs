package gunnar.ihop2.regent.costwriting;

import static org.matsim.core.network.NetworkUtils.getConnectingLink;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.matrices.Matrix;
import org.matsim.matrices.MatrixUtils;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

import gunnar.ihop2.regent.demandreading.Zone;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class LeastCostMatrixUpdater implements Runnable {

	private final TravelTime linkTTs;

	private final TravelDisutility linkCostForTree;

	private final Network network;

	private final Node fromNode;

	private final String fromZoneID;

	private final Map<Zone, Set<Node>> zone2destinationNodes;

	private final int startTime_s;

	private final Map<String, TravelDisutility> costType2linkCost;

	private final Map<String, Matrix> costType2matrices;

	LeastCostMatrixUpdater(final TravelTime linkTTs,
			final TravelDisutility linkCostForTree, final Network network,
			final Node fromNode, final String fromZoneID,
			final Map<Zone, Set<Node>> zone2sampledNodes,
			final int startTime_s,
			final Map<String, TravelDisutility> costType2linkCost,
			final Map<String, Matrix> costType2matrices) {

		if (!costType2linkCost.keySet().equals(costType2matrices.keySet())) {
			final String msg = "inconsistent cost type sets: "
					+ costType2linkCost.keySet() + " vs. "
					+ costType2matrices.keySet();
			Logger.getLogger(this.getClass().getName()).severe(msg);
			throw new RuntimeException(msg);
		}

		this.linkTTs = linkTTs;
		this.linkCostForTree = linkCostForTree;
		this.network = network;
		this.fromNode = fromNode;
		this.fromZoneID = fromZoneID;
		this.zone2destinationNodes = zone2sampledNodes;
		this.startTime_s = startTime_s;
		this.costType2linkCost = costType2linkCost;
		this.costType2matrices = costType2matrices;
	}

	/**
	 * Returns the least cost path represented by lcpt from that tree's origin
	 * node to destinationNode.
	 */
	private LinkedList<Link> path(final Node destinationNode,
			final LeastCostPathTree lcpt) {
		final LinkedList<Link> result = new LinkedList<>();
		Node currentNode = destinationNode;
		while (!currentNode.getId().equals(lcpt.getOrigin().getId())) {
			final Node prevNode = this.network.getNodes().get(
					lcpt.getTree().get(currentNode.getId()).getPrevNodeId());
			result.addFirst(getConnectingLink(prevNode, currentNode));
			currentNode = prevNode;
		}
		return result;
	}

	/**
	 * Returns the cost of the given path as defined by travelTime and linkCost.
	 */
	private double cost(final List<Link> path, final TravelDisutility linkCost) {
		double result = 0.0;
		double time_s = this.startTime_s;
		for (Link link : path) {
			result += linkCost
					.getLinkTravelDisutility(link, time_s, null, null);
			time_s += this.linkTTs.getLinkTravelTime(link, time_s, null, null);
		}
		return result;
	}

	@Override
	public void run() {

		final LeastCostPathTree lcpt = new LeastCostPathTree(this.linkTTs,
				this.linkCostForTree);
		lcpt.calculate(this.network, this.fromNode, this.startTime_s);

		for (String costType : this.costType2matrices.keySet()) {

			final TravelDisutility linkCost = this.costType2linkCost
					.get(costType);
			final Matrix costMatrix = this.costType2matrices.get(costType);

			for (Map.Entry<Zone, Set<Node>> zoneId2destinationNodesEntry : this.zone2destinationNodes
					.entrySet()) {
				for (Node toNode : zoneId2destinationNodesEntry.getValue()) {
					final List<Link> path = this.path(toNode, lcpt);
					final double cost = this.cost(path, linkCost);
					// >>>>> using globally synchronized method >>>>>
					MatrixUtils.global
							.add(costMatrix, this.fromZoneID,
									zoneId2destinationNodesEntry.getKey()
											.getId(), cost);
					// <<<<< using globally synchronized method <<<<<
				}
			}
		}
	}
}
