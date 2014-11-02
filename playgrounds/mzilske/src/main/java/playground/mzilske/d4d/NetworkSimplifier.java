/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.mzilske.d4d;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;

/**
 * Simplifies a given network, by merging links.
 *
 * @author aneumann
 *
 */
class NetworkSimplifier {

	private static final Logger log = Logger.getLogger(NetworkSimplifier.class);
	private boolean mergeLinkStats = false;

	public void run(final Network network) {

		log.info("running " + this.getClass().getName() + " algorithm...");

		log.info("  checking " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links for dead-ends...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		for (Node node : network.getNodes().values()) {
			int nodeTopoType = nodeTopo.getTopoType(node);
			if(nodeTopoType == 4 || nodeTopoType == 5) {

				List<Link> iLinks = new ArrayList<Link> (node.getInLinks().values());

				for (Link iL : iLinks) {
					LinkImpl inLink = (LinkImpl) iL;

					List<Link> oLinks = new ArrayList<Link> (node.getOutLinks().values());

					for (Link oL : oLinks) {
						LinkImpl outLink = (LinkImpl) oL;

						if(inLink != null && outLink != null){
							if(!outLink.getToNode().equals(inLink.getFromNode())){

								if(this.mergeLinkStats){

									// Try to merge both links by guessing the resulting links attributes
									Link link = network.getFactory().createLink(
											Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
											inLink.getFromNode(),
											outLink.getToNode());

									// length can be summed up
									link.setLength(inLink.getLength() + outLink.getLength());

									// freespeed depends on total length and time needed for inLink and outLink
									link.setFreespeed(
											(inLink.getLength() + outLink.getLength()) /
											(inLink.getFreespeedTravelTime() + outLink.getFreespeedTravelTime())
											);

									// the capacity and the new links end is important, thus it will be set to the minimum
									link.setCapacity(Math.min(inLink.getCapacity(), outLink.getCapacity()));

									// number of lanes can be derived from the storage capacity of both links
									link.setNumberOfLanes((inLink.getLength() * inLink.getNumberOfLanes()
													+ outLink.getLength() * outLink.getNumberOfLanes())
													/ (inLink.getLength() + outLink.getLength())
													);

//									inLink.getOrigId() + "-" + outLink.getOrigId(),
									network.addLink(link);
									network.removeLink(inLink.getId());
									(network).removeLink(outLink.getId());

								} else {

									// Only merge links with same attributes
									if(bothLinksHaveSameLinkStats(inLink, outLink)){
										LinkImpl newLink = ((NetworkImpl) network).createAndAddLink(
												Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
												inLink.getFromNode(),
												outLink.getToNode(),
												inLink.getLength() + outLink.getLength(),
												inLink.getFreespeed(),
												inLink.getCapacity(),
												inLink.getNumberOfLanes(),
												inLink.getOrigId() + "-" + outLink.getOrigId(),
												null);

										newLink.setAllowedModes(inLink.getAllowedModes());

										network.removeLink(inLink.getId());
										network.removeLink(outLink.getId());
									}

								}
							}
						}
					}
				}
			}

		}

		org.matsim.core.network.algorithms.NetworkCleaner nc = new org.matsim.core.network.algorithms.NetworkCleaner();
		nc.run(network);

		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(network);

		log.info("  resulting network contains " + network.getNodes().size() + " nodes and " +
				network.getLinks().size() + " links.");
		log.info("done.");
	}



	/**
	 *
	 * @param mergeLinkStats If set true, links will be merged despite their different attributes.
	 *  If set false, only links with the same attributes will be merged, thus preserving as much information as possible.
	 *  Default is set false.
	 */
	public void setMergeLinkStats(boolean mergeLinkStats){
		this.mergeLinkStats = mergeLinkStats;
	}

	// helper

	/**
	 * Compare link attributes. Return whether they are the same or not.
	 */
	private boolean bothLinksHaveSameLinkStats(LinkImpl linkA, LinkImpl linkB){

		boolean bothLinksHaveSameLinkStats = true;

		if(!linkA.getAllowedModes().equals(linkB.getAllowedModes())){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getFreespeed() != linkB.getFreespeed()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getCapacity() != linkB.getCapacity()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getNumberOfLanes() != linkB.getNumberOfLanes()){ bothLinksHaveSameLinkStats = false; }

		return bothLinksHaveSameLinkStats;
	}

}