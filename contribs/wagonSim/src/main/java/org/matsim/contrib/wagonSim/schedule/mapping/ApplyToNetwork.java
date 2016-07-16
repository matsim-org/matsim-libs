package org.matsim.contrib.wagonSim.schedule.mapping;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * @author mrieser / senozon
 */
public class ApplyToNetwork {

	private final Network network;
	
	public ApplyToNetwork(final Network network) {
		this.network = network;
	}
	
	public void applyEdits(final List<NetworkEdit> edits) {
		for (NetworkEdit edit : edits) {
			if (edit instanceof MergeNodes) {
				doMergeNodes((MergeNodes) edit);
			} else if (edit instanceof AddLink) {
				doAddLink((AddLink) edit);
			} else if (edit instanceof ReplaceLink) {
				doReplaceLink((ReplaceLink) edit);
			}
		}
	}
	
	private void doMergeNodes(final MergeNodes edit) {
		Node node = this.network.getNodes().get(edit.getNodeId());
		Node targetNode = this.network.getNodes().get(edit.getTargetNodeId());
		for (Link inLink : node.getInLinks().values()) {
			inLink.setToNode(targetNode);
		}
		node.getInLinks().clear();
		for (Link outLink : node.getOutLinks().values()) {
			outLink.setFromNode(targetNode);
		}
		node.getOutLinks().clear();
		this.network.removeNode(node.getId());
	}
	
	private void doAddLink(final AddLink edit) {
		Node fromNode = this.network.getNodes().get(edit.getFromNodeId());
		Node toNode = this.network.getNodes().get(edit.getToNodeId());
		Link link = this.network.getFactory().createLink(edit.getLinkId(), fromNode, toNode);
		this.network.addLink(link);
	}
	
	private void doReplaceLink(final ReplaceLink edit) {
		if (edit.getReplacementLinkIds().size() != 1 || !edit.getLinkId().equals(edit.getReplacementLinkIds().get(0))) {
			this.network.removeLink(edit.getLinkId());
		}
	}
}
