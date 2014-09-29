package org.matsim.contrib.wagonSim.schedule.mapping;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/**
 * @author mrieser / senozon
 */
public class MergeNodes implements NetworkEdit {

	private final Id<Node> nodeId;
	private final Id<Node> targetNodeId;

	public MergeNodes(final Id<Node> nodeId, final Id<Node> targetNodeId) {
		this.nodeId = nodeId;
		this.targetNodeId = targetNodeId;
	}

	public Id<Node> getNodeId() {
		return nodeId;
	}

	public Id<Node> getTargetNodeId() {
		return targetNodeId;
	}
}
