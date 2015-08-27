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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class NetworkContainer {

	// MEMBERS

	protected final String networkId;

	protected final String networkType;

	protected final Map<String, String> networkAttributes = new LinkedHashMap<String, String>();

	protected final Map<String, String> nodeSetAttributes = new LinkedHashMap<String, String>();

	protected final Map<String, String> linkSetAttributes = new LinkedHashMap<String, String>();

	protected final Map<String, Map<String, String>> nodeId2Attributes = new HashMap<String, Map<String, String>>();

	protected final Map<String, Map<String, String>> linkId2Attributes = new HashMap<String, Map<String, String>>();

	protected final Map<String, String> linkId2fromNodeId = new LinkedHashMap<String, String>();

	protected final Map<String, String> linkId2toNodeId = new LinkedHashMap<String, String>();

	// CONSTRUCTION

	public NetworkContainer(final String networkId, final String networkType) {
		this.networkId = networkId;
		this.networkType = networkType;
	}

	// SETTERS

	public void putNetworkAttribute(final String key, final String value) {
		this.networkAttributes.put(key, value);
	}

	public void putNodeSetAttribute(final String key, final String value) {
		this.nodeSetAttributes.put(key, value);
	}

	public void putLinkSetAttribute(final String key, final String value) {
		this.linkSetAttributes.put(key, value);
	}

	public void putNode(final String nodeId) {
		if (!this.nodeId2Attributes.containsKey(nodeId)) {
			this.nodeId2Attributes.put(nodeId,
					new LinkedHashMap<String, String>());
		}
	}

	public void putLink(final String linkId) {
		if (!this.linkId2Attributes.containsKey(linkId)) {
			this.linkId2Attributes.put(linkId,
					new LinkedHashMap<String, String>());
		}
	}

	public void putNodeAttribute(final String nodeId, final String key,
			final String value) {
		Map<String, String> attributes = this.nodeId2Attributes.get(nodeId);
		if (attributes == null) {
			attributes = new LinkedHashMap<String, String>();
			this.nodeId2Attributes.put(nodeId, attributes);
		}
		attributes.put(key, value);
	}

	public void putLinkAttribute(final String linkId, final String key,
			final String value) {
		Map<String, String> attributes = this.linkId2Attributes.get(linkId);
		if (attributes == null) {
			attributes = new LinkedHashMap<String, String>();
			this.linkId2Attributes.put(linkId, attributes);
		}
		attributes.put(key, value);
	}

	public void setFromNode(final String linkId, final String fromNodeId) {
		this.linkId2fromNodeId.put(linkId, fromNodeId);
	}

	public void setToNode(final String linkId, final String toNodeId) {
		this.linkId2toNodeId.put(linkId, toNodeId);
	}

	// GETTERS

	public String getNetworkId() {
		return this.networkId;
	}

	public String getNetworkType() {
		return this.networkType;
	}

	public String getFromNodeId(final String linkId) {
		return this.linkId2fromNodeId.get(linkId);
	}

	public String getToNodeId(final String linkId) {
		return this.linkId2toNodeId.get(linkId);
	}

	public String getNodeAttribute(final String nodeId, final String key) {
		return this.nodeId2Attributes.get(nodeId).get(key);
	}

	public String getLinkAttribute(final String linkId, final String key) {
		return this.linkId2Attributes.get(linkId).get(key);
	}

	// TODO NEW
	public boolean containsLink(final String linkId) {
		return this.linkId2Attributes.containsKey(linkId);
	}

}
