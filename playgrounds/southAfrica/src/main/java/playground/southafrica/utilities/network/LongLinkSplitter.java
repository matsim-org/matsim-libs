/* *********************************************************************** *
 * project: org.matsim.*
 * LongLinkSplitter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.southafrica.utilities.network;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;

import playground.southafrica.utilities.Header;

/**
 * Class to read in a MATSim {@link Network}, typically a cleaned network, and
 * split all links exceeding a given threshold in equal lengths.
 * 
 * @author jwjoubert
 */
public class LongLinkSplitter {
	final private static Logger LOG = Logger.getLogger(LongLinkSplitter.class);

	/**
	 * Executing the class.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(LongLinkSplitter.class.toString(), args);

		Header.printFooter();
	}

	/**
	 * Hide from outside.
	 */
	private LongLinkSplitter() {
	}
	
	public static Network  splitNetwork(Network network, double longestLink){
		return splitNetwork(network, longestLink, true);
	}
	
	public static Network splitNetwork(Network network, double longestLink, boolean verbose){
		Network newNetwork = null;
		newNetwork = NetworkUtils.createNetwork();
		NetworkFactory nf = new NetworkFactoryImpl(newNetwork);
		
		Map<Integer, Integer> splitCount = new TreeMap<Integer, Integer>();
		splitCount.put(0, 0);
		
		/* 'Copy' all the current nodes. But do NOT just add the complete node 
		 * from the original network, otherwise all the in- and out-links will
		 * also be copied, causing the code to crash due to duplicate links. */
		for(Node node : network.getNodes().values()){
			newNetwork.addNode(nf.createNode(node.getId(), node.getCoord()));
		}
		
		/* Work through all the links. */
		for(Link link : network.getLinks().values()){
			if(link.getLength() <= longestLink){
				/* Add the link as it is right now. */
				splitCount.put(0, splitCount.get(0) + 1);
				newNetwork.addLink(link);
			} else{
				/* Splitting it up. */
				int units = (int) Math.ceil(link.getLength() / longestLink);
				double segmentLength = link.getLength() / ((double)units);
				double dX = (link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX()) / ((double)units);
				double dY = (link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY()) / ((double)units);
				Node fromNode = link.getFromNode();
				for(int i = 1; i < units; i++){
					/* Create the dummy node. */
					Coord toCoord = new Coord(
							fromNode.getCoord().getX() + dX,
							fromNode.getCoord().getY() + dY);
					Node toNode = nf.createNode(
							Id.createNodeId(String.format("%s_n%04d", link.getId().toString(), i)), 
							toCoord);
					newNetwork.addNode(toNode);
					
					/* Create the dummy link. */
					Link dummyLink = nf.createLink(
							Id.createLinkId(String.format("%s_l%04d", link.getId().toString(), i)), 
							fromNode, toNode);
					dummyLink.setLength(segmentLength);
					dummyLink.setAllowedModes(link.getAllowedModes());
					dummyLink.setCapacity(link.getCapacity());
					dummyLink.setFreespeed(link.getFreespeed());
					dummyLink.setNumberOfLanes(link.getNumberOfLanes());
					if(!newNetwork.getLinks().containsKey(dummyLink.getId())){
						try{
							newNetwork.addLink(dummyLink);
						} catch(IllegalArgumentException e){
							LOG.debug("Why is a duplicate link added?!");
						}
						fromNode = toNode;
					}
				}
				/* Now add the last portion. */
				Link lastLink = nf.createLink(
						Id.createLinkId(String.format("%s_l%04d", link.getId().toString(), units)), 
						fromNode, link.getToNode());
				lastLink.setLength(segmentLength);
				lastLink.setAllowedModes(link.getAllowedModes());
				lastLink.setCapacity(link.getCapacity());
				lastLink.setFreespeed(link.getFreespeed());
				lastLink.setNumberOfLanes(link.getNumberOfLanes());
				newNetwork.addLink(lastLink);

				/* Update the split count. */
				if(!splitCount.containsKey(units-1)){
					splitCount.put(units-1, 1);
				} else{
					splitCount.put(units-1, splitCount.get(units-1) + 1);
				}
			}
		}
		
		/* Report the statistics. */
		LOG.info("=======================================================");
		LOG.info("Network splitting statistics:");
		LOG.info("   Old network:");
		LOG.info("      # nodes: " + network.getNodes().size());
		LOG.info("      # links: " + network.getLinks().size());
		LOG.info("   New network:");
		LOG.info("      # nodes: " + newNetwork.getNodes().size());
		LOG.info("      # links: " + newNetwork.getLinks().size());
		if(verbose){
			LOG.info("   Number of splits:");
			for(Integer i : splitCount.keySet()){
				LOG.info("      " + i + ": " + splitCount.get(i));
			}
		}
		LOG.info("=======================================================");
		return newNetwork;
	}
	
	
	
}
