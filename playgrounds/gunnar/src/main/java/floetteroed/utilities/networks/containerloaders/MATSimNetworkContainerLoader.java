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
package floetteroed.utilities.networks.containerloaders;

import org.xml.sax.Attributes;

import floetteroed.utilities.networks.construction.NetworkContainer;


/**
 * 
 * TODO This implements only a subset of the respective DTD.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class MATSimNetworkContainerLoader extends NetworkContainerLoaderXML {

	// -------------------- CONSTANTS --------------------

	public static final String MATSIM_NETWORK_TYPE = "MATSim";

	public static final String NETWORK = "network";

	public static final String NETWORK_NAME = "name";

	public static final String NODES = "nodes";

	public static final String LINKS = "links";

	public static final String LINKS_CAPPERIOD = "capperiod";

	public static final String NODE = "node";

	public static final String NODE_ID = "id";

	public static final String LINK = "link";

	public static final String LINK_ID = "id";

	public static final String LINK_FROM = "from";

	public static final String LINK_TO = "to";

	public static final String LINK_LENGTH = "length";

	public static final String LINK_CAPACITY = "capacity";

	public static final String LINK_PERMLANES = "permlanes";

	public static final String LINK_FREESPEED = "freespeed";
	
	// -------------------- CONSTRUCTION --------------------

	public MATSimNetworkContainerLoader() {
		super();
	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	@Override
	public void startElement(final String namespaceURI, final String sName,
			final String qName, final Attributes attrs) {
		if (NETWORK.equals(qName)) {
			startNetwork(attrs);
		} else if (NODES.equals(qName)) {
			startNodes(attrs);
		} else if (LINKS.equals(qName)) {
			startLinks(attrs);
		} else if (NODE.equals(qName)) {
			startNode(attrs);
		} else if (LINK.equals(qName)) {
			startLink(attrs);
		}
	}

	private void startNetwork(final Attributes attrs) {
		final String networkId = attrs.getValue(NETWORK_NAME);
		this.container = new NetworkContainer(networkId == null ? ""
				: networkId, MATSIM_NETWORK_TYPE);
		// if (networkId == null) {
		// this.net = new BasicNetwork("MATSim network", MATSIM_NETWORK_TYPE);
		// } else {
		// this.net = new BasicNetwork(networkId, MATSIM_NETWORK_TYPE);
		// }
		for (int i = 0; i < attrs.getLength(); i++) {
			final String name = attrs.getQName(i);
			if (!NETWORK_NAME.equals(name)) {
				this.container.putNetworkAttribute(name, attrs.getValue(i));
				// this.net.setAttr(name, attrs.getValue(i));
			}
		}
	}

	private void startNodes(final Attributes attrs) {
		for (int i = 0; i < attrs.getLength(); i++) {
			this.container.putNodeSetAttribute(attrs.getQName(i),
					attrs.getQName(i));
			// this.net.setNodesAttr(attrs.getQName(i), attrs.getValue(i));
		}
	}

	private void startLinks(final Attributes attrs) {
		for (int i = 0; i < attrs.getLength(); i++) {
			this.container.putLinkSetAttribute(attrs.getQName(i),
					attrs.getValue(i));
			// this.net.setLinksAttr(attrs.getQName(i), attrs.getValue(i));
		}
	}

	private void startNode(final Attributes attrs) {
		final String nodeId = attrs.getValue(NODE_ID);
		this.container.putNode(nodeId);
		// final BasicNode node = new BasicNode(attrs.getValue(NODE_ID));
		for (int i = 0; i < attrs.getLength(); i++) {
			final String name = attrs.getQName(i);
			if (!NODE_ID.equals(name)) {
				this.container
						.putNodeAttribute(nodeId, name, attrs.getValue(i));
				// node.setAttr(name, attrs.getValue(i));
			}
		}
		// this.net.addNode(node);
	}

	private void startLink(final Attributes attrs) {
		// final BasicLink link = new BasicLink(attrs.getValue(LINK_ID));
		final String linkId = attrs.getValue(LINK_ID);
		this.container.putLink(linkId);

		// final BasicNode fromNode =
		// this.net.getNode(attrs.getValue(LINK_FROM));
		// if (fromNode == null) {
		// throw new RuntimeException("unknown from-node for link "
		// + link.getId());
		// }
		this.container.setFromNode(linkId, attrs.getValue(LINK_FROM));

		// final BasicNode toNode = this.net.getNode(attrs.getValue(LINK_TO));
		// if (toNode == null) {
		// throw new RuntimeException("unknown to-node for link "
		// + link.getId());
		// }
		this.container.setToNode(linkId, attrs.getValue(LINK_TO));

		// link.setFromNode(fromNode);
		// link.setToNode(toNode);
		// fromNode.addOutLink(link);
		// toNode.addInLink(link);

		for (int i = 0; i < attrs.getLength(); i++) {
			final String name = attrs.getQName(i);
			if (!LINK_ID.equals(name) && !LINK_FROM.equals(name)
					&& !LINK_TO.equals(name)) {
				// link.setAttr(name, attrs.getValue(i));
				this.container
						.putLinkAttribute(linkId, name, attrs.getValue(i));
			}
		}
		// this.net.addLink(link);
	}
}
