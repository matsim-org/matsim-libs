package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.turnRestrictions.ColoredLink;
import org.matsim.core.router.turnRestrictions.TurnRestrictionsContext;

import java.util.Arrays;

/**
 * Creates a {@link SpeedyGraph} for a provided {@link Network}.
 *
 * @author mrieser / Simunto
 */
public class SpeedyGraphBuilder {

	private int nodeCount;
	private int linkCount;
	private int[] nodeData;
	private int[] linkData;
	private Link[] links;
	private Node[] nodes;

	public static SpeedyGraph build(Network network) {
		if (hasTurnRestrictions(network)) {
			return new SpeedyGraphBuilder().buildWithTurnRestrictions(network);
		}
		return new SpeedyGraphBuilder().buildWithoutTurnRestrictions(network);
	}

	private static boolean hasTurnRestrictions(Network network) {
		for (Link link : network.getLinks().values()) {
			if (NetworkUtils.getDisallowedNextLinks(link) != null) {
				return true;
			}
		}
		return false;
	}

	private SpeedyGraph buildWithTurnRestrictions(Network network) {

		TurnRestrictionsContext context = TurnRestrictionsContext.buildContext(network);

		// create routing graph from context
		this.nodeCount = context.getNodeCount();
		this.linkCount = context.getLinkCount();

		this.nodeData = new int[this.nodeCount * SpeedyGraph.NODE_SIZE];
		this.linkData = new int[this.linkCount * SpeedyGraph.LINK_SIZE];
		this.links = new Link[this.linkCount];
		this.nodes = new Node[this.nodeCount];

		Arrays.fill(this.nodeData, -1);
		Arrays.fill(this.linkData, -1);

		for (Node node : network.getNodes().values()) {
			this.nodes[node.getId().index()] = node;
		}
		for (Link link : network.getLinks().values()) {
			if (context.getReplacedLinks().get(link.getId()) == null) {
				addLink(link);
			}
		}
		for (TurnRestrictionsContext.ColoredNode node : context.getColoredNodes()) {
			this.nodes[node.index()] = node.node();
		}
		for (ColoredLink link : context.getColoredLinks()) {
			addLink(link);
		}

		return new SpeedyGraph(this.nodeData, this.linkData, this.nodes, this.links, true);
	}



	private SpeedyGraph buildWithoutTurnRestrictions(Network network) {
		this.nodeCount = Id.getNumberOfIds(Node.class);
		this.linkCount = Id.getNumberOfIds(Link.class);

		this.nodeData = new int[this.nodeCount * SpeedyGraph.NODE_SIZE];
		this.linkData = new int[this.linkCount * SpeedyGraph.LINK_SIZE];
		this.links = new Link[this.linkCount];
		this.nodes = new Node[this.nodeCount];

		Arrays.fill(this.nodeData, -1);
		Arrays.fill(this.linkData, -1);

		for (Node node : network.getNodes().values()) {
			this.nodes[node.getId().index()] = node;
		}
		for (Link link : network.getLinks().values()) {
			addLink(link);
		}

		return new SpeedyGraph(this.nodeData, this.linkData, this.nodes, this.links, false);
	}

	private void addLink(Link link) {
		int fromNodeIdx = link.getFromNode().getId().index();
		int toNodeIdx = link.getToNode().getId().index();
		int linkIdx = link.getId().index();

		int base = linkIdx * SpeedyGraph.LINK_SIZE;
		this.linkData[base + 2] = fromNodeIdx;
		this.linkData[base + 3] = toNodeIdx;
		this.linkData[base + 4] = (int) Math.round(link.getLength() * 100.0);
		this.linkData[base + 5] = (int) Math.round(link.getLength() / link.getFreespeed() * 100.0);

		setOutLink(fromNodeIdx, linkIdx);
		setInLink(toNodeIdx, linkIdx);

		this.links[linkIdx] = link;
	}

	private void addLink(ColoredLink link) {
		int fromNodeIdx = -1;
		int toNodeIdx = -1;
		int linkIdx = link.getIndex();

		if (link.getFromColoredNode() != null) {
			fromNodeIdx = link.getFromColoredNode().index();
		}
		if (link.getFromNode() != null) {
			fromNodeIdx = link.getFromNode().getId().index();
		}
		if (link.getToColoredNode() != null) {
			toNodeIdx = link.getToColoredNode().index();
		}
		if (link.getToNode() != null) {
			toNodeIdx = link.getToNode().getId().index();
		}

		int base = linkIdx * SpeedyGraph.LINK_SIZE;
		this.linkData[base + 2] = fromNodeIdx;
		this.linkData[base + 3] = toNodeIdx;
		this.linkData[base + 4] = (int) Math.round(link.getLink().getLength() * 100.0);
		this.linkData[base + 5] = (int) Math.round(link.getLink().getLength() / link.getLink().getFreespeed() * 100.0);

		setOutLink(fromNodeIdx, linkIdx);
		setInLink(toNodeIdx, linkIdx);

		this.links[linkIdx] = link.getLink();
	}

	private void setOutLink(int fromNodeIdx, int linkIdx) {
		final int nodeI = fromNodeIdx * SpeedyGraph.NODE_SIZE;
		int outLinkIdx = this.nodeData[nodeI];
		if (outLinkIdx < 0) {
			this.nodeData[nodeI] = linkIdx;
			return;
		}
		int lastLinkIdx;
		do {
			lastLinkIdx = outLinkIdx;
			outLinkIdx = this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE];
		} while (outLinkIdx >= 0);
		this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE] = linkIdx;
	}

	private void setInLink(int toNodeIdx, int linkIdx) {
		final int nodeI = toNodeIdx * SpeedyGraph.NODE_SIZE + 1;
		int inLinkIdx = this.nodeData[nodeI];
		if (inLinkIdx < 0) {
			this.nodeData[nodeI] = linkIdx;
			return;
		}
		int lastLinkIdx;
		do {
			lastLinkIdx = inLinkIdx;
			inLinkIdx = this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE + 1];
		} while (inLinkIdx >= 0);
		this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE + 1] = linkIdx;
	}

}
