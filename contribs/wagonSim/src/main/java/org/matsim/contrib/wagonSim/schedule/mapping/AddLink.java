package org.matsim.contrib.wagonSim.schedule.mapping;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * @author mrieser / senozon
 */
public class AddLink implements NetworkEdit {

	private final Id<Link> linkId;
	private final Id<Node> fromNodeId;
	private final Id<Node> toNodeId;
	
	public AddLink(final Id<Link> linkId, final Id<Node> fromNodeId, final Id<Node> toNodeId) {
		this.linkId = linkId;
		this.fromNodeId = fromNodeId;
		this.toNodeId = toNodeId;
	}
	
	public Id<Link> getLinkId() {
		return linkId;
	}
	
	public Id<Node> getFromNodeId() {
		return fromNodeId;
	}
	
	public Id<Node> getToNodeId() {
		return toNodeId;
	}

}
