/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */ 
package floetteroed.utilities.networks.construction;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class AbstractNetwork<N extends AbstractNode<N, L>, L extends AbstractLink<N, L>>
		extends AttributeContainer {

	// // -------------------- CONSTANTS --------------------
	//
	// public static final String TYPE_ATTRIBUTE = "type";

	// -------------------- MEMBER VARIABLES --------------------

	private final String type;

	private Map<String, String> nodesAttr = new LinkedHashMap<String, String>();

	private Map<String, String> linksAttr = new LinkedHashMap<String, String>();

	private final Map<String, N> nodes = new LinkedHashMap<String, N>();

	private final Map<String, L> links = new LinkedHashMap<String, L>();

	// -------------------- CONSTRUCTION --------------------

	public AbstractNetwork(final String id, final String type) {
		super(id);
		this.type = type;
	}

	// public BasicNetwork(final BasicNetwork parent) {
	// /*
	// * (1) create network clone without nodes and links
	// */
	// super(parent);
	// this.nodesAttr.putAll(parent.nodesAttr);
	// this.linksAttr.putAll(parent.linksAttr);
	// /*
	// * (2) fill with unconnected node and link clones
	// */
	// for (BasicNode node : parent.nodes.values()) {
	// this.nodes.put(node.getId(), new BasicNode(node));
	// }
	// for (BasicLink link : parent.links.values()) {
	// this.links.put(link.getId(), new BasicLink(link));
	// }
	// /*
	// * (3) establish connectivity of nodes and links
	// */
	// for (BasicLink link : parent.links.values()) {
	// final BasicLink newLink = this.getLink(link.getId());
	// final BasicNode newFromNode = this.getNode(link.getFromNode().getId());
	// final BasicNode newToNode = this.getNode(link.getToNode().getId());
	// newLink.setFromNode(newFromNode);
	// newLink.setToNode(newToNode);
	// newFromNode.addOutLink(newLink);
	// newToNode.addInLink(newLink);
	// }
	// }

	// -------------------- CONTENT WRITING --------------------

	// TODO NEW
	protected void setNodesAttributes(final Map<String, String> nodesAttributes) {
		this.nodesAttr = nodesAttributes;
	}

	// TODO NEW
	protected void setLinksAttributes(final Map<String, String> linksAttributes) {
		this.linksAttr = linksAttributes;
	}

	public static <N extends AbstractNode<N, L>, L extends AbstractLink<N, L>> void connect(
			final N fromNode, final N toNode, final L link) {
		fromNode.getOutLinks().add(link);
		toNode.getInLinks().add(link);
		link.setFromNode(fromNode);
		link.setToNode(toNode);
	}

	// public void remove(final Node node, final boolean removeAdjacentLinks) {
	// for (Link link : node.getInLinks()) {
	// this.remove(link);
	// }
	// for (Link link : node.getOutLinks()) {
	// this.remove(link);
	// }
	// }
	//
	// public void remove(final Link link) {
	// this.links.remove(link.getId());
	// }

	public void setNodesAttr(final String key, final String value) {
		this.nodesAttr.put(key, value);
	}

	public String getNodesAttr(final String key) {
		return this.nodesAttr.get(key);
	}

	public void setLinksAttr(final String key, final String value) {
		this.linksAttr.put(key, value);
	}

	public String getLinksAttr(final String key) {
		return this.linksAttr.get(key);
	}

	// public BasicNode add(final BasicNode node) {
	public N addNode(final N node) {
		return this.nodes.put(node.getId(), node);
	}

	// public BasicLink add(final BasicLink link) {
	public L addLink(final L link) {
		return this.links.put(link.getId(), link);
	}

	public boolean isConsistent() {
		/*
		 * check if all nodes known by the links are existing
		 */
		for (L link : this.links.values()) {
			if (!this.nodes.containsValue(link.getFromNode())) {
				return false;
			}
			if (!this.nodes.containsValue(link.getToNode())) {
				return false;
			}
		}
		/*
		 * check if all links known by the nodes are existing
		 */
		for (N node : this.nodes.values()) {
			for (L link : node.getInLinks()) {
				if (!this.links.containsValue(link)) {
					return false;
				}
			}
			for (L link : node.getOutLinks()) {
				if (!this.links.containsValue(link)) {
					return false;
				}
			}
		}
		return true;
	}

	// -------------------- CONTENT READING --------------------

	public String getType() {
		// return this.getAttr(TYPE_ATTRIBUTE);
		return this.type;
	}

	// public boolean containsNodeId(final Object nodeId) {
	// return this.nodes.containsKey(nodeId);
	// }
	//
	// public boolean containsLinkId(final Object linkId) {
	// return this.links.containsKey(linkId);
	// }

	public N getNode(final String nodeId) {
		return this.nodes.get(nodeId);
	}

	public L getLink(final String linkId) {
		return this.links.get(linkId);
	}

	public L getLink(final String fromNodeId, final String toNodeId) {
		final N fromNode = this.getNode(fromNodeId);
		if (fromNode == null) {
			return null;
		}
		for (L link : fromNode.getOutLinks()) {
			if (link.getToNode().getId().equals(toNodeId)) {
				return link;
			}
		}
		return null;
	}

	public Collection<N> getNodes() {
		return nodes.values();
	}

	public Collection<L> getLinks() {
		return links.values();
	}

	// // TODO new
	public Map<String, String> getKey2NodesAttrView() {
		return Collections.unmodifiableMap(this.nodesAttr);
	}

	// // TODO new
	public Map<String, String> getKey2LinksAttrView() {
		return Collections.unmodifiableMap(this.linksAttr);
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer(this.getClass()
				.getSimpleName());
		result.append("(id = ");
		result.append(this.getId());
		result.append(", #nodes = ");
		result.append(this.getNodes().size());
		result.append(", #links = ");
		result.append(this.getLinks().size());
		result.append(", super = ");
		result.append(super.toString());
		result.append(")");
		return result.toString();
	}
}
