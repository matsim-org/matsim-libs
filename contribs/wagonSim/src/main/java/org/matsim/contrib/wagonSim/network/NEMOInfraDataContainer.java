/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.contrib.wagonSim.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

/**
 * @author balmermi @ Seonzon AG
 * @since 3013-07-05
 */
public class NEMOInfraDataContainer {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////
	
	final Map<String, NEMOInfraNodeCluster> nodeClusters = new HashMap<>();
	final Map<Id<Node>, NEMOInfraNode> nodes = new HashMap<>();
	final Map<String, NEMOInfraCountry> countries = new HashMap<>();
	final Map<String, NEMOInfraLinkType> linkTypes = new HashMap<>();
	final Map<String, NEMOInfraLinkOwner> linkOwners = new HashMap<>();
	final Map<Id<Link>,NEMOInfraLink> links = new HashMap<>();
	final Map<String, NEMOInfraTrack> tracks = new HashMap<>();
	final Map<Id<Link>, NEMOInfraDirection> directions = new HashMap<>();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////
	
	public NEMOInfraDataContainer() {
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	final boolean validateCountries() {
		boolean isValid = true;
		for (NEMOInfraCountry country : countries.values()) {
			if (country.name == null) { isValid = false; }
			if (country.isHomeCountry == null) { isValid = false; }
		}
		return isValid;
	}
	
	//////////////////////////////////////////////////////////////////////

	final boolean validateLinkTypes() {
		boolean isValid = true;
		for (NEMOInfraLinkType linkType : linkTypes.values()) {
			if (linkType.velocity == null) { isValid = false; }
			if (linkType.vFactor == null) { isValid = false; }
		}
		return isValid;
	}
	
	//////////////////////////////////////////////////////////////////////

	final boolean validateLinkOwners() {
		boolean isValid = true;
		for (NEMOInfraLinkOwner linkOwner : linkOwners.values()) {
			if (linkOwner.owner == null) { isValid = false; }
		}
		return isValid;
	}
	
	//////////////////////////////////////////////////////////////////////

	final boolean validateNodeClusters() {
		boolean isValid = true;
		for (NEMOInfraNodeCluster nodeCluster : nodeClusters.values()) {
			if (nodeCluster.nodeIds.isEmpty()) { isValid = false; }
		}
		return isValid;
	}
	
	//////////////////////////////////////////////////////////////////////

	final boolean validateNodes() {
		boolean isValid = true;
		for (NEMOInfraNode node : nodes.values()) {
			if (node.name == null) { isValid = false; }
			if (node.coord == null) { isValid = false; }
			if (node.countryId == null) { isValid = false; }
			if (!countries.containsKey(node.countryId)) { isValid = false; }
			if (node.isStation == null) { isValid = false; }
			if (node.isValid == null) { isValid = false; }
			if (node.clusterId == null) { isValid = false; }
			if (!nodeClusters.containsKey(node.clusterId)) { isValid = false; }
		}
		return isValid;
	}

	//////////////////////////////////////////////////////////////////////

	final boolean validateLinks() {
		boolean isValid = true;
		for (NEMOInfraLink link : links.values()) {
			if (link.fromNodeId == null) { isValid = false; }
			if (!nodes.containsKey(link.fromNodeId)) { isValid = false; }
			if (link.toNodeId == null) { isValid = false; }
			if (!nodes.containsKey(link.toNodeId)) { isValid = false; }
			if (link.isSimuLink == null) { isValid = false; }
			if (link.length == null) { isValid = false; }
			if (link.hasTwoTracks == null) { isValid = false; }
			if (link.isGlobal == null) { isValid = false; }
			if (link.typeId == null) { isValid = false; }
			if (!linkTypes.containsKey(link.typeId)) { isValid = false; }
			if (link.ownerId == null) { isValid = false; }
			if (!linkOwners.containsKey(link.ownerId)) { isValid = false; }
			if (link.isClosed == null) { isValid = false; }
			if (link.isValid == null) { isValid = false; }
			if (link.maxTrainLength == null) { isValid = false; }
		}
		return isValid;
	}

	//////////////////////////////////////////////////////////////////////

	final boolean validateTracks() {
		boolean isValid = true;
		for (NEMOInfraTrack track : tracks.values()) {
			if (!links.containsKey(track.linkId)) { isValid = false; }
		}
		return isValid;
	}

	//////////////////////////////////////////////////////////////////////

	final boolean validateDirections() {
		boolean isValid = true;
		for (NEMOInfraDirection direction : directions.values()) {
			if (!tracks.containsKey(direction.trackId)) { isValid = false; }
		}
		return isValid;
	}

	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////

	static class NEMOInfraNodeCluster {
		final String id;
		Set<Id<Node>> nodeIds = new HashSet<>();
		
		NEMOInfraNodeCluster(String cluster) { id = cluster; }
	}
	
	//////////////////////////////////////////////////////////////////////
	
	static class NEMOInfraNode {
		final Id<Node> id;
		String name = null;
		Coord coord = null;
		String countryId = null;
		Boolean isStation = null;
		Boolean isValid = null;
		String clusterId = null;
		
		NEMOInfraNode(String bscode) { id = Id.create(bscode, Node.class); }
	}

	//////////////////////////////////////////////////////////////////////
	
	static class NEMOInfraCountry {
		final String id;
		String name = null;
		Boolean isHomeCountry = null;
		
		NEMOInfraCountry(int id_Land) { id = Integer.toString(id_Land);  }
	}

	//////////////////////////////////////////////////////////////////////
	
	static class NEMOInfraLinkType {
		final String id;
		Double velocity = null;
		Double vFactor = null;
		
		NEMOInfraLinkType(int id_Streckenkategorie) { id = Integer.toString(id_Streckenkategorie);  }
	}

	//////////////////////////////////////////////////////////////////////
	
	static class NEMOInfraLinkOwner {
		final String id;
		String owner = null;
		
		NEMOInfraLinkOwner(int id_StreckenKST) { id = Integer.toString(id_StreckenKST);  }
	}

	//////////////////////////////////////////////////////////////////////

	static class NEMOInfraLink {
		final Id<Link> id;
		Id<Node> fromNodeId = null;
		Id<Node> toNodeId = null;
		Boolean isSimuLink = null;
		Double length = null;
		Boolean hasTwoTracks = null;
		Boolean isGlobal = null;
		String typeId = null;
		String ownerId = null;
		Boolean isClosed = null;
		Boolean isValid = null;
		Double maxTrainLength = null;
		
		NEMOInfraLink(int id_Kante) { id = Id.create(id_Kante, Link.class);  }
	}

	//////////////////////////////////////////////////////////////////////
	
	static class NEMOInfraTrack {
		final String id; // format: [linkId]-[trackNr]
		final Id<Link> linkId;
		final Boolean trackNr;
		
		NEMOInfraTrack(int kante, boolean gleisnr) {
			linkId = Id.create(kante, Link.class);
			trackNr = new Boolean(gleisnr);
			id = linkId.toString()+"-"+trackNr.toString();
		}
	}

	//////////////////////////////////////////////////////////////////////
	
	static class NEMOInfraDirection {
		final Id<Link> id; // format: [linkId]-[trackNr]-[trafficDirection]
		final String trackId; // format: [linkId]-[trackNr]
		final Id<Link> linkId;
		final Boolean trackNr;
		final Boolean direction;
		
		NEMOInfraDirection(int kante, boolean gleisnr, boolean richtung) {
			linkId = Id.create(kante, Link.class);
			trackNr = new Boolean(gleisnr);
			direction = new Boolean(richtung);
			trackId = linkId.toString()+"-"+trackNr.toString();
			id = Id.create(linkId.toString()+"-"+trackNr.toString()+"-"+direction.toString(), Link.class);
		}
	}
}
