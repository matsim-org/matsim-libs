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

import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <N>
 *            the node type
 * @param <L>
 *            the link type
 * @param <NET>
 *            the network type
 */
public abstract class AbstractNetworkFactory<N extends AbstractNode<N, L>, L extends AbstractLink<N, L>, NET extends AbstractNetwork<N, L>> {

	// -------------------- MEMBERS --------------------

	private NetworkPostprocessor<NET> postProcessor;

	// -------------------- INTERFACE DEFINITION --------------------

	protected abstract NET newNetwork(final String id, final String type);

	protected abstract N newNode(final String id);

	protected abstract L newLink(final String id);

	// -------------------- SETTERS AND GETTERS --------------------

	public void setNetworkPostprocessor(
			final NetworkPostprocessor<NET> postProcessor) {
		this.postProcessor = postProcessor;
	}

	public NetworkPostprocessor<NET> getNetworkPostprocessor() {
		return this.postProcessor;
	}

	// -------------------- NETWORK CONSTRUCTION --------------------

	public NET newNetwork(final NetworkContainer container) {

		/*
		 * (1) create the network
		 */
		final NET newNetwork = this.newNetwork(container.networkId,
				container.networkType);
		newNetwork.setAttributes(container.networkAttributes);
		newNetwork.setNodesAttributes(container.nodeSetAttributes);
		newNetwork.setLinksAttributes(container.linkSetAttributes);

		/*
		 * (2) create the nodes
		 */
		for (Map.Entry<String, Map<String, String>> nodeId2Attributes : container.nodeId2Attributes
				.entrySet()) {
			final N newNode = this.newNode(nodeId2Attributes.getKey());
			newNode.setAttributes(nodeId2Attributes.getValue());
			newNetwork.addNode(newNode);
		}

		/*
		 * (3) create the links and establish connectivity
		 */
		for (Map.Entry<String, Map<String, String>> linkId2Attributes : container.linkId2Attributes
				.entrySet()) {
			/*
			 * (3a) create the link
			 */
			final String linkId = linkId2Attributes.getKey();
			final L newLink = this.newLink(linkId);
			newLink.setAttributes(linkId2Attributes.getValue());
			newNetwork.addLink(newLink);
			/*
			 * (3b) establish connectivity
			 */
			final N fromNode = newNetwork.getNode(container.linkId2fromNodeId
					.get(linkId));
			final N toNode = newNetwork.getNode(container.linkId2toNodeId
					.get(linkId));

			// System.out.println("connecting " + fromNode + " and " + toNode +
			// " with link " + newLink);
			// System.err.println("WARNING CHANGED CODE IN AbstractNetworkFactory around line 110");
			// if (fromNode != null && toNode != null) {
			NET.connect(fromNode, toNode, newLink);
			// }
		}

		if (this.postProcessor != null) {
			this.postProcessor.run(newNetwork);
		}

		return newNetwork;
	}
}
