package org.matsim.contrib.wagonSim.schedule.mapping;

import org.matsim.api.core.v01.Id;

/**
 * @author mrieser / senozon
 */
public class AddLink implements NetworkEdit {

	private final Id linkId;
	private final Id fromNodeId;
	private final Id toNodeId;
	
	public AddLink(final Id linkId, final Id fromNodeId, final Id toNodeId) {
		this.linkId = linkId;
		this.fromNodeId = fromNodeId;
		this.toNodeId = toNodeId;
	}
	
	public Id getLinkId() {
		return linkId;
	}
	
	public Id getFromNodeId() {
		return fromNodeId;
	}
	
	public Id getToNodeId() {
		return toNodeId;
	}

}
