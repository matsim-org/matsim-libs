package org.matsim.contrib.wagonSim.schedule.mapping;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser / senozon
 */
public class MergeNodes implements NetworkEdit {

	private final Id nodeId;
	private final Id targetNodeId;
	
	public MergeNodes(final Id nodeId, final Id targetNodeId) {
		this.nodeId = nodeId;
		this.targetNodeId = targetNodeId;
	}
	
	public Id getNodeId() {
		return nodeId;
	}
	
	public Id getTargetNodeId() {
		return targetNodeId;
	}
}
