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

import java.util.Map;

import org.xml.sax.Attributes;

import floetteroed.utilities.IdentifiedElementParser;
import floetteroed.utilities.networks.NetworkInverter;
import floetteroed.utilities.networks.construction.NetworkContainer;


/**
 * 
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class SUMONetworkContainerLoader {

	// -------------------- CONSTANTS --------------------

	public static final String SUMO_NETWORK_TYPE = "SUMO";

	public static final String NODE_ELEMENT = "node";

	public static final String NODE_ID_ATTRIBUTE = "id";

	public static final String EDGE_ELEMENT = "edge";

	public static final String EDGE_ID_ATTRIBUTE = "id";

	public static final String EDGE_FROMNODE_ATTRIBUTE = "fromnode";

	public static final String EDGE_TONODE_ATTRIBUTE = "tonode";

	public static final String CONNECTION_ELEMENT = "connection";

	public static final String CONNECTION_FROM_ATTRIBUTE = "from";

	public static final String CONNECTION_TO_ATTRIBUTE = "to";

	// -------------------- CONSTRUCTION --------------------

	public SUMONetworkContainerLoader() {
	}

	// -------------------- IMPLEMENTATION OF NetworkLoader --------------------

	public NetworkContainer loadNetworkContainer(final String nodeFile,
			final String edgeFile, final String connectionFile) {

		/*
		 * (0) check parameters
		 */
		if (nodeFile == null) {
			throw new IllegalArgumentException("node file is null");
		}
		if (edgeFile == null) {
			throw new IllegalArgumentException("edge file is null");
		}

		/*
		 * (1) create network container instance
		 */
		// final BasicNetwork network = new BasicNetwork("SUMO-Network",
		// SUMO_NETWORK_TYPE);
		final NetworkContainer container = new NetworkContainer("SUMO-Network",
				SUMO_NETWORK_TYPE);

		/*
		 * (2) load nodes
		 */
		final Map<String, Attributes> nodeId2Attrs = (new IdentifiedElementParser(
				NODE_ELEMENT, NODE_ID_ATTRIBUTE)).readId2AttrsMap(nodeFile);
		for (Map.Entry<String, Attributes> nodeId2AttrsEntry : nodeId2Attrs
				.entrySet()) {
			// final BasicNode node = new BasicNode(nodeId2AttrsEntry.getKey());
			final String nodeId = nodeId2AttrsEntry.getKey();
			final Attributes attrs = nodeId2AttrsEntry.getValue();
			container.putNode(nodeId);
			for (int i = 0; i < attrs.getLength(); i++) {
				container.putNodeAttribute(nodeId, attrs.getQName(i),
						attrs.getValue(i));
			}
			// node.setAttrs(nodeId2AttrsEntry.getValue());
			// network.addNode(node);
		}

		/*
		 * (3) load links
		 */
		final Map<String, Attributes> edgeId2Attrs = (new IdentifiedElementParser(
				EDGE_ELEMENT, EDGE_ID_ATTRIBUTE)).readId2AttrsMap(edgeFile);
		for (Map.Entry<String, Attributes> edgeId2AttrsEntry : edgeId2Attrs
				.entrySet()) {
			// final BasicLink link = new BasicLink(edgeId2AttrsEntry.getKey());
			final String linkId = edgeId2AttrsEntry.getKey();
			final Attributes attrs = edgeId2AttrsEntry.getValue();
			container.putLink(linkId);
			// final BasicNode fromNode = network.getNode(link
			// .getAttr(EDGE_FROMNODE_ATTRIBUTE));
			// final BasicNode toNode = network.getNode(link
			// .getAttr(EDGE_TONODE_ATTRIBUTE));
			container.setFromNode(linkId,
					attrs.getValue(EDGE_FROMNODE_ATTRIBUTE));
			container.setToNode(linkId, attrs.getValue(EDGE_TONODE_ATTRIBUTE));
			for (int i = 0; i < attrs.getLength(); i++) {
				container.putLinkAttribute(linkId, attrs.getQName(i),
						attrs.getValue(i));
			}
			// BasicNetwork.connect(fromNode, toNode, link);
			// link.setAttrs(edgeId2AttrsEntry.getValue());
			// network.addLink(link);
		}

		/*
		 * (4) load connections (if applicable)
		 */
		if (connectionFile != null) {
			final Map<String, Attributes> connectionId2Attrs = (new IdentifiedElementParser(
					CONNECTION_ELEMENT, null)).readId2AttrsMap(connectionFile);
			for (Attributes attrs : connectionId2Attrs.values()) {
				// final BasicLink fromLink = network.getLink(attrs
				// .getValue(CONNECTION_FROM_ATTRIBUTE));
				final String fromLinkId = attrs
						.getValue(CONNECTION_FROM_ATTRIBUTE);
				// final String oldSuccessors = fromLink
				// .getAttr(NetworkInverter.LINK_SUCCESSORS_ATTRIBUTE);
				final String oldSuccessors = container.getLinkAttribute(
						fromLinkId, NetworkInverter.LINK_SUCCESSORS_ATTRIBUTE);
				// fromLink.setAttr(NetworkInverter.LINK_SUCCESSORS_ATTRIBUTE,
				// (oldSuccessors == null ? "" : oldSuccessors + " ")
				// + attrs.getValue(CONNECTION_TO_ATTRIBUTE));
				container.putLinkAttribute(fromLinkId,
						NetworkInverter.LINK_SUCCESSORS_ATTRIBUTE,
						(oldSuccessors == null ? "" : oldSuccessors + " ")
								+ attrs.getValue(CONNECTION_TO_ATTRIBUTE));
			}
		}
		return container;
	}
}
