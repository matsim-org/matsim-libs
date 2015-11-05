package gunnar.ihop2.regent.costwriting;

import static org.matsim.matrices.MatrixUtils.add;
import static org.matsim.matrices.MatrixUtils.inc;
import floetteroed.utilities.Units;
import gunnar.ihop2.regent.demandreading.Zone;

import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.matrices.Matrix;
import org.matsim.utils.leastcostpathtree.LeastCostPathTree;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class LeastCostMatrixUpdater implements Runnable {

	private final TravelTime linkTTs;

	private final Network network;

	private final Node fromNode;

	private final int time_s;

	final Map<Zone, Set<Node>> zone2sampledNodes;

	final Matrix ttMatrix_min;

	final Matrix cntMatrix;

	final String fromZoneID;

	LeastCostMatrixUpdater(final TravelTime linkTTs, final Network network,
			final Node fromNode, final int time_s,
			final Map<Zone, Set<Node>> zone2sampledNodes,
			final Matrix ttMatrix_min, final Matrix cntMatrix,
			final String fromZoneID) {
		this.linkTTs = linkTTs;
		this.network = network;
		this.fromNode = fromNode;
		this.time_s = time_s;
		this.zone2sampledNodes = zone2sampledNodes;
		this.ttMatrix_min = ttMatrix_min;
		this.cntMatrix = cntMatrix;
		this.fromZoneID = fromZoneID;
	}

	@Override
	public void run() {
		final LeastCostPathTree lcpt = new LeastCostPathTree(this.linkTTs,
				new OnlyTimeDependentTravelDisutility(this.linkTTs));
		lcpt.calculate(this.network, this.fromNode, this.time_s);
		for (Map.Entry<Zone, Set<Node>> zoneId2sampledNodesEntry : this.zone2sampledNodes
				.entrySet()) {
			final String toZoneID = zoneId2sampledNodesEntry.getKey().getId();
			for (Node toNode : zoneId2sampledNodesEntry.getValue()) {
				// >>> these static methods are synchronized >>>
				add(this.ttMatrix_min, this.fromZoneID, toZoneID, lcpt
						.getTree().get(toNode.getId()).getCost()
						* Units.MIN_PER_S);
				inc(this.cntMatrix, this.fromZoneID, toZoneID);
				// <<< these static methods are synchronized <<<
			}
		}
	}
}
