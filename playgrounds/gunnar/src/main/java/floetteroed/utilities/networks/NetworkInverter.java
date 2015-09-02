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
package floetteroed.utilities.networks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import floetteroed.utilities.networks.basic.BasicLink;
import floetteroed.utilities.networks.basic.BasicNetwork;
import floetteroed.utilities.networks.basic.BasicNetworkElement;
import floetteroed.utilities.networks.basic.BasicNode;


/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class NetworkInverter {

	// -------------------- CONSTANTS --------------------

	// either "true" or "false" string representation of a boolean value
	public static final String NETWORK_INVERTED_ATTRIBUTE = "inverted";

	public static final String NODE_ORIGINALFROMNODE_ID = "originalfromnode";

	public static final String NODE_ORIGINALTONODE_ID = "originaltonode";

	public static final String LINK_SUCCESSORS_ATTRIBUTE = "successors";

	// public static final String LINK_ORIGINALNODEID_ATTRIBUTE =
	// "originalnodeid";

	// -------------------- PRIVATE CONSTRUCTION --------------------

	private NetworkInverter() {
	}

	// -------------------- STATIC IMPLEMENTATION --------------------

	public static String newConnectionId(final String from, final String to) {
		return from + "__-->__" + to;
	}

	public static String newConnectionId(final BasicNetworkElement from,
			final BasicNetworkElement to) {
		return newConnectionId(from.getId(), to.getId());
	}

	private static List<String> successorIds(final BasicLink link) {
		List<String> result = new ArrayList<String>(link.getToNode()
				.getOutLinks().size());
		if (link.getAttr(LINK_SUCCESSORS_ATTRIBUTE) == null) {
			for (BasicLink succ : link.getToNode().getOutLinks()) {
				result.add(succ.getId());
			}
		} else {
			for (String succId : link.getAttr(LINK_SUCCESSORS_ATTRIBUTE).split(
					"\\s")) {
				result.add(succId.trim());
			}
		}
		return result;
	}

	/**
	 * Creates an inverted version of <code>originalNetwork</code>. The inverted
	 * network consists of the following elements:
	 * 
	 * (1) The new network inherits the id, the <code>NetworkElement</code>
	 * attributes and the node<u>s</u> and link<u>s</u> attributes of
	 * <code>originalNetwork</code>. Note that node<u>s</u> attributes stay
	 * node<u>s</u> attributes and link<u>s</u> attributes stay link<u>s</u>
	 * attributes.
	 * 
	 * (2) The new network contains a node for every link in
	 * <code>originalNetwork</code>. The node inherits only the id of the
	 * original link. It also receives two new attributes, originalfromnode and
	 * originaltonode, which contain the IDs of the original upstream and
	 * downstream node of the original link.
	 * 
	 * (3) The new network contains a link for every turning move in
	 * <code>originalNetwork</code>. The link inherits the attributes of the
	 * upstream link of the original turning move. Its id is composed of the
	 * respective upstream and downstream link in <code>originalNetwork</code>.
	 * 
	 * 
	 * @param originalNetwork
	 * @return
	 */
	public static BasicNetwork newInvertedNetwork(
			final BasicNetwork originalNetwork) {
		/*
		 * (0) checks and preliminaries
		 */
		if (originalNetwork.getAttr(NETWORK_INVERTED_ATTRIBUTE) != null
				&& Boolean.parseBoolean(originalNetwork
						.getAttr(NETWORK_INVERTED_ATTRIBUTE))) {
			System.err.println("The network is already inverted. "
					+ "Returning the same instance.");
			return originalNetwork;
		}
		final BasicNetwork newNetwork = new BasicNetwork(
				originalNetwork.getId(), originalNetwork.getType());
		newNetwork.setAttr(NETWORK_INVERTED_ATTRIBUTE, Boolean.toString(true));
		for (Map.Entry<String, String> entry : originalNetwork
				.getKey2NodesAttrView().entrySet()) {
			newNetwork.setNodesAttr(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<String, String> entry : originalNetwork
				.getKey2LinksAttrView().entrySet()) {
			newNetwork.setLinksAttr(entry.getKey(), entry.getValue());
		}
		/*
		 * (1) create one new node per original link
		 */
		for (BasicLink originalLink : originalNetwork.getLinks()) {
			final BasicNode newNode = new BasicNode(originalLink.getId());
			for (Map.Entry<String, String> entry : originalLink
					.getKey2AttributeView().entrySet()) {
				newNode.setAttr(entry.getKey(), entry.getValue());
			}
			newNode.setAttr(NODE_ORIGINALFROMNODE_ID, originalLink
					.getFromNode().getId());
			newNode.setAttr(NODE_ORIGINALTONODE_ID, originalLink.getToNode()
					.getId());
			newNetwork.addNode(newNode);
		}
		/*
		 * (2) create one link per turning move
		 */
		for (BasicNode originalNode : originalNetwork.getNodes()) {
			for (BasicLink originalInLink : originalNode.getInLinks()) {
				for (String originalOutLinkId : successorIds(originalInLink)) {
					final BasicLink originalOutLink = originalNetwork
							.getLink(originalOutLinkId);
					
					// >>>>> TODO No idea why this compiled before. >>>>>
					// final BasicLink newLink = new BasicLink(newConnectionId(
					// originalInLink, originalOutLink));
					final BasicLink newLink = new BasicLink(newConnectionId(
							originalInLink.getId(), originalOutLinkId));
					// <<<<< TODO No idea why this compiled before. <<<<<
					
					for (Map.Entry<String, String> entry : originalInLink
							.getKey2AttributeView().entrySet()) {
						newLink.setAttr(entry.getKey(), entry.getValue());
					}
					// newLink.setAttr(LINK_ORIGINALNODEID_ATTRIBUTE,
					// originalNode
					// .getId());
					final BasicNode newFromNode = newNetwork
							.getNode(originalInLink.getId());
					final BasicNode newToNode = newNetwork
							.getNode(originalOutLink.getId());
					BasicNetwork.connect(newFromNode, newToNode, newLink);
					newNetwork.addLink(newLink);
				}
			}
		}
		return newNetwork;
	}
}
