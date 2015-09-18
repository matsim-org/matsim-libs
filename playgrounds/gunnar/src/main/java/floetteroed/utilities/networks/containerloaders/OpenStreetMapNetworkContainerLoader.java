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
 * Implements a subset of the OpenStreetMap XML file format.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class OpenStreetMapNetworkContainerLoader extends
		NetworkContainerLoaderXML {

	// -------------------- CONSTANTS --------------------

	public static final String OPENSTREETMAP_NETWORK_TYPE = "OpenStreetMap";

	public static final String NODE = "node";

	public static final String NODE_ID = "id";

	public static final String WAY = "way";

	public static final String WAY_ID = "id";

	public static final String WAY_ND = "nd";

	public static final String WAY_ND_ID = "ref";

	// -------------------- MEMBER VARIABLES --------------------

	// private BasicLink currentLink = null;
	private String currentLinkId = null;

	// -------------------- CONSTRUCTION --------------------

	public OpenStreetMapNetworkContainerLoader() {
		super();
	}

	// -------------------- OVERRIDING OF DefaultHandler --------------------

	@Override
	public void startDocument() {
		// this.net = new BasicNetwork("OSM network",
		// OPENSTREETMAP_NETWORK_TYPE);
		this.container = new NetworkContainer("", OPENSTREETMAP_NETWORK_TYPE);
		// this.currentLink = null;
		this.currentLinkId = null;
	}

	@Override
	public void startElement(final String namespaceURI, final String sName,
			final String qName, final Attributes attrs) {
		if (NODE.equals(qName)) {
			this.startNode(attrs);
		} else if (WAY.equals(qName)) {
			this.startWay(attrs);
		} else if (WAY_ND.equals(qName)) {
			this.startWayNd(attrs);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) {
		if (WAY.equals(qName)) {
			this.endWay();
		}
	}

	private void startNode(final Attributes attrs) {
		// final BasicNode node = new BasicNode(attrs.getValue(NODE_ID));
		// this.net.addNode(node);
		final String nodeId = attrs.getValue(NODE_ID);
		this.container.putNode(nodeId);

		for (int i = 0; i < attrs.getLength(); i++) {
			final String name = attrs.getQName(i);
			if (!NODE_ID.equals(name)) {
				// node.setAttr(name, attrs.getValue(i));
				this.container
						.putNodeAttribute(nodeId, name, attrs.getValue(i));
			}
		}
	}

	private void startWay(final Attributes attrs) {
		// this.currentLink = new BasicLink(attrs.getValue(WAY_ID));
		this.currentLinkId = attrs.getValue(WAY_ID);
		this.container.putLink(this.currentLinkId);

		for (int i = 0; i < attrs.getLength(); i++) {
			final String name = attrs.getQName(i);
			if (!WAY_ID.equals(name)) {
				// this.currentLink.setAttr(name, attrs.getValue(i));
				this.container.putLinkAttribute(this.currentLinkId, name,
						attrs.getValue(i));
			}
		}
	}

	private void startWayNd(final Attributes attrs) {
		// final BasicNode node = this.net.getNode(attrs.getValue(WAY_ND_ID));
		final String nodeId = attrs.getValue(WAY_ND_ID);
		// if (this.currentLink.getFromNode() == null) {
		// this.currentLink.setFromNode(node);
		// node.addOutLink(this.currentLink);
		// } else {
		// this.currentLink.setToNode(node);
		// node.addInLink(this.currentLink);
		// }
		if (this.container.getFromNodeId(this.currentLinkId) == null) {
			// this.currentLink.setFromNode(node);
			// node.addOutLink(this.currentLink);
			this.container.setFromNode(this.currentLinkId, nodeId);
		} else {
			// this.currentLink.setToNode(node);
			// node.addInLink(this.currentLink);
			this.container.setToNode(this.currentLinkId, nodeId);
		}
	}

	private void endWay() {
		// this.net.addLink(this.currentLink);
		// this.currentLink = null;
		this.currentLinkId = null;
	}
}
