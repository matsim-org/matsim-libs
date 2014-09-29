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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.wagonSim.WagonSimConstants;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraCountry;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraDirection;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraLink;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraLinkOwner;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraLinkType;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraNode;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraNodeCluster;
import org.matsim.contrib.wagonSim.network.NEMOInfraDataContainer.NEMOInfraTrack;
import org.matsim.utils.objectattributes.ObjectAttributes;

/**
 * @author balmermi
 * @since 2013-07-05
 */
public class NEMOInfraToMATSimNetworkConverter {

	//////////////////////////////////////////////////////////////////////
	// variables
	//////////////////////////////////////////////////////////////////////



	private static final Set<String> defaultModes = new HashSet<String>();
	private final Network network;
	private final ObjectAttributes nodeAttributes;
	private final ObjectAttributes linkAttributes;
	
	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NEMOInfraToMATSimNetworkConverter(Network network, ObjectAttributes nodeAttributes, ObjectAttributes linkAttributes) {
		this.network = network;
		this.nodeAttributes = nodeAttributes;
		this.linkAttributes = linkAttributes;
		defaultModes.add("train");
	}

	//////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////
	
	public static final Set<String> getDefaultModes() {
		defaultModes.add(WagonSimConstants.DEFAULT_LINK_MODE);
		return defaultModes;
	}
	
	//////////////////////////////////////////////////////////////////////

	private final void convertNodes(NEMOInfraDataContainer dataContainer) {
		NetworkFactory factory = network.getFactory();
		
		for (NEMOInfraNode nemoNode : dataContainer.nodes.values()) {
			Node node = factory.createNode(nemoNode.id,nemoNode.coord);
			network.addNode(node);
			
			nodeAttributes.putAttribute(node.getId().toString(), WagonSimConstants.NODE_STATION,nemoNode.isStation);
			nodeAttributes.putAttribute(node.getId().toString(), WagonSimConstants.NODE_VALID,nemoNode.isValid);
			
			NEMOInfraCountry country = dataContainer.countries.get(nemoNode.countryId);
			nodeAttributes.putAttribute(node.getId().toString(), WagonSimConstants.NODE_COUNTRY_ID, country.id);
			nodeAttributes.putAttribute(node.getId().toString(), WagonSimConstants.NODE_NAME, country.name);
			nodeAttributes.putAttribute(node.getId().toString(), WagonSimConstants.NODE_HOME_COUNTRY, country.isHomeCountry);
			
			NEMOInfraNodeCluster nodeCluster = dataContainer.nodeClusters.get(nemoNode.clusterId);
			nodeAttributes.putAttribute(node.getId().toString(), WagonSimConstants.NODE_CLUSTER_ID, nodeCluster.id.toString());
		}
	}

	//////////////////////////////////////////////////////////////////////
	
	private final void convertLinks(NEMOInfraDataContainer dataContainer) {
		NetworkFactory factory = network.getFactory();
		
		Set<Id<Link>> nemoLinkIdsUsed = new HashSet<>();
		Set<String> nemoTrackIdsUsed = new HashSet<>();
		
		for (NEMOInfraDirection nemoDir : dataContainer.directions.values()) {
			NEMOInfraTrack nemoTrack = dataContainer.tracks.get(nemoDir.trackId);
			nemoTrackIdsUsed.add(nemoTrack.id);
			NEMOInfraLink nemoLink = dataContainer.links.get(nemoTrack.linkId);
			nemoLinkIdsUsed.add(nemoLink.id);
			
			Node fromNode = network.getNodes().get(nemoLink.fromNodeId);
			Node toNode = network.getNodes().get(nemoLink.toNodeId);
			boolean dir = nemoDir.direction;
			
			Link link = null;
			if (dir) { link = factory.createLink(nemoDir.id,fromNode,toNode); }
			else { link = factory.createLink(nemoDir.id,toNode,fromNode); }

			network.addLink(link);
			
			link.setLength(nemoLink.length);
			link.setAllowedModes(defaultModes);
			link.setNumberOfLanes(1);
			link.setCapacity(WagonSimConstants.DEFAULT_CAPACITY);
			
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_SIMULATE, nemoLink.isSimuLink);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_GLOBAL, nemoLink.isGlobal);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_CLOSED, nemoLink.isClosed);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_VALID, nemoLink.isValid);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_MAXTRAINLENGTH, nemoLink.maxTrainLength);
			
			NEMOInfraLinkType linkType = dataContainer.linkTypes.get(nemoLink.typeId);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_TYPE, linkType.id);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_VFACTOR, linkType.vFactor);
			link.setFreespeed(linkType.velocity); // m/s

			NEMOInfraLinkOwner linkOwner = dataContainer.linkOwners.get(nemoLink.ownerId);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_OWNERID, linkOwner.id);
			linkAttributes.putAttribute(link.getId().toString(), WagonSimConstants.LINK_OWNERNAME, linkOwner.owner);
		}
		
		if (nemoTrackIdsUsed.size() != dataContainer.tracks.size()) {
			throw new RuntimeException("Inconsistent data: the amount of track refered by the directions (="+nemoTrackIdsUsed.size()+") is not equal to the given number of tracks (="+dataContainer.tracks.size()+"). Bailing out.");
		}
		if (nemoLinkIdsUsed.size() != dataContainer.links.size()) {
			throw new RuntimeException("Inconsistent data: the amount of links refered by the tracks (="+nemoLinkIdsUsed.size()+") is not equal to the given number of links (="+dataContainer.links.size()+"). Bailing out.");
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public final void makeNetworkBiDirectional() {
		NetworkFactory factory = network.getFactory();

		Set<Id<Link>> linkIds = new HashSet<>(network.getLinks().keySet());
		for (Id<Link> linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			boolean hasOtherDirection = false;
			for (Link otherLink : link.getToNode().getOutLinks().values()) {
				if (otherLink.getToNode().getId().equals(link.getFromNode().getId())) {
					hasOtherDirection = true;
					break;
				}
			}
			if (!hasOtherDirection) {
				Link l = factory.createLink(Id.create(link.getId().toString()+"-r", Link.class), link.getToNode(), link.getFromNode());
				l.setLength(link.getLength());
				l.setFreespeed(link.getFreespeed());
				l.setCapacity(link.getCapacity());
				l.setNumberOfLanes(link.getNumberOfLanes());
				l.setAllowedModes(link.getAllowedModes());
				network.addLink(l);
			}
		}
	}
	
	//////////////////////////////////////////////////////////////////////
	
	public final boolean validateNetwork() {
		boolean isValid = true;
		for (Link link : network.getLinks().values()) {
			for (Link otherLink : link.getFromNode().getOutLinks().values()) {
				// check for parallel links
				if (!otherLink.getId().equals(link.getId()) && otherLink.getToNode().getId().equals(link.getToNode().getId())) {
					System.out.println(link.toString()+" <==> "+otherLink.toString());
					isValid = false;
				}
			}
		}
		return isValid;
	}

	//////////////////////////////////////////////////////////////////////

	public final void convert(NEMOInfraDataContainer dataContainer) {
		convertNodes(dataContainer);
		convertLinks(dataContainer);
	}
}
