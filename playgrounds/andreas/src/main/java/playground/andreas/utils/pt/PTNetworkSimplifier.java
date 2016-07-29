/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkCleaner.java
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

package playground.andreas.utils.pt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.TransitScheduleReaderV1;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.andreas.utils.net.NetworkRemoveUnusedNodes;

/**
 * Simplifies a given network, by merging links.
 *
 * @author aneumann
 *
 */
public class PTNetworkSimplifier {

	private static final Logger log = Logger.getLogger(PTNetworkSimplifier.class);
	private boolean mergeLinkStats = false;
	private TransitSchedule transitSchedule;
	private TreeSet<String> linksNeededByTransitSchedule = null;
	private Network network;

	private String netInFile;
	private String scheduleInFile;
	private String netOutFile;
	private String scheduleOutFile;
	private Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();

	public PTNetworkSimplifier(String netInFile, String scheduleInFile, String netOutFile, String scheduleOutFile){
		this.netInFile = netInFile;
		this.scheduleInFile = scheduleInFile;
		this.netOutFile = netOutFile;
		this.scheduleOutFile = scheduleOutFile;
	}

	public void run(final Network net, final TransitSchedule tranSched) {

		this.network = net;
		this.transitSchedule = tranSched;

		TransitScheduleCleaner.removeEmptyLines(this.transitSchedule);
		TransitScheduleCleaner.removeStopsNotUsed(this.transitSchedule);
		this.network = TransitScheduleCleaner.tagTransitLinksInNetwork(this.transitSchedule, this.network);

		if(this.nodeTypesToMerge.size() == 0){
			throw new RuntimeException("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		log.info("running " + this.getClass().getName() + " algorithm...");

		log.info("  checking " + this.network.getNodes().size() + " nodes and " +
				this.network.getLinks().size() + " links for dead-ends...");

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(this.network);

		TreeSet<String> nodesConnectedToTransitStop = new TreeSet<String>();

		int nodesProcessed = 0;
		int nextMessageAt = 2;


		for (Node node : this.network.getNodes().values()) {

			nodesProcessed++;
			if(nextMessageAt == nodesProcessed){
				log.info(nodesProcessed + " nodes processed so far");
				nextMessageAt = 2 * nodesProcessed;
			}

			if(nodesConnectedToTransitStop.contains(node.getId().toString())){
				continue;
			}

			if(this.nodeTypesToMerge.contains(Integer.valueOf(nodeTopo.getTopoType(node)))){

				List<Link> iLinks = new ArrayList<Link> (node.getInLinks().values());

				for (Link iL : iLinks) {
					Link inLink = (Link) iL;

					List<Link> oLinks = new ArrayList<Link> (node.getOutLinks().values());

					for (Link oL : oLinks) {
						Link outLink = (Link) oL;

						if(inLink != null && outLink != null){

							if(!outLink.getToNode().equals(inLink.getFromNode())){

								if(!linkNeededByTransitStop(inLink, outLink)){

									Link link = null;

									if(this.mergeLinkStats){

										// Try to merge both links by guessing the resulting links attributes
										link = this.network.getFactory().createLink(
												Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
												inLink.getFromNode(),
												outLink.getToNode());

										// length can be summed up
										link.setLength(inLink.getLength() + outLink.getLength());

										// freespeed depends on total length and time needed for inLink and outLink
										link.setFreespeed(
												(inLink.getLength() + outLink.getLength()) /
												(NetworkUtils.getFreespeedTravelTime(inLink) + NetworkUtils.getFreespeedTravelTime(outLink))
										);

										// the capacity and the new links end is important, thus it will be set to the minimum
										link.setCapacity(Math.min(inLink.getCapacity(), outLink.getCapacity()));

										// number of lanes can be derived from the storage capacity of both links
										link.setNumberOfLanes((inLink.getLength() * inLink.getNumberOfLanes()
												+ outLink.getLength() * outLink.getNumberOfLanes())
												/ (inLink.getLength() + outLink.getLength())
										);

										//									inLink.getOrigId() + "-" + outLink.getOrigId(),


									} else {

										// Only merge links with same attributes
										if(bothLinksHaveSameLinkStats(inLink, outLink)){
											link = this.network.getFactory().createLink(
													Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
													inLink.getFromNode(),
													outLink.getToNode());

											link.setLength(inLink.getLength() + outLink.getLength());

											link.setFreespeed(inLink.getFreespeed());


											link.setCapacity(inLink.getCapacity());

											link.setNumberOfLanes(inLink.getNumberOfLanes());

											link.setAllowedModes(inLink.getAllowedModes());
										}

									}

									if(link != null){
										if(!nodesConnectedToTransitStop.contains(node.getId().toString())){
											if(!nodesConnectedToTransitStop.contains(link.getFromNode().getId().toString())){
												if(!nodesConnectedToTransitStop.contains(link.getToNode().getId().toString())){

													if(inLink.getAllowedModes().contains(TransportMode.pt) || outLink.getAllowedModes().contains(TransportMode.pt)){
														if(removeLinksFromTransitSchedule(link, inLink, outLink)){
															this.network.addLink(link);
															this.network.removeLink(inLink.getId());
															this.network.removeLink(outLink.getId());
														}
													} else {
														this.network.addLink(link);
														this.network.removeLink(inLink.getId());
														this.network.removeLink(outLink.getId());
													}

												}
											}
										}

									}

								} else {
									nodesConnectedToTransitStop.add(node.getId().toString());
								}
							}
						}
					}
				}
			}

		}

		NetworkRemoveUnusedNodes nc = new NetworkRemoveUnusedNodes();
		nc.run(this.network);

		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(this.network);

		log.info("  resulting network contains " + this.network.getNodes().size() + " nodes and " +
				this.network.getLinks().size() + " links.");
		log.info("done.");
		// no functionality, because TransitScheduleCleaner creates a deep copy /dr
		TransitScheduleCleaner.removeAllRoutesWithMissingLinksFromSchedule(this.transitSchedule, this.network);
		TransitScheduleCleaner.removeEmptyLines(this.transitSchedule);
		TransitScheduleCleaner.removeStopsNotUsed(this.transitSchedule);
	}

	private boolean removeLinksFromTransitSchedule(Link link, Link inLink, Link outLink) {
		// first test - links must not be changed if, only one link is part of a route, but the other one not
		for (TransitLine transitLine : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {

				if(transitRoute.getRoute().getLinkIds().contains(inLink.getId()) || transitRoute.getRoute().getLinkIds().contains(inLink.getId())){

					LinkedList<Id<Link>> routeLinkIds = new LinkedList<Id<Link>>();
					routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
					for (Id<Link> id : transitRoute.getRoute().getLinkIds()) {
						routeLinkIds.add(id);
					}
					routeLinkIds.add(transitRoute.getRoute().getEndLinkId());

					for (Iterator<Id<Link>> iterator = routeLinkIds.iterator(); iterator.hasNext();) {
						Id<Link> id = iterator.next();
						if(id.toString().equalsIgnoreCase(inLink.getId().toString())){
							Id<Link> nextId = iterator.next();
							if(nextId.toString().equalsIgnoreCase(outLink.getId().toString())){
								// everything okay
								break;
							} else {
								// inLink and outLink ar not followers, thus they should not be touched
								return false;
							}
						}

					}
				}
			}
		}

		// second perform

		for (TransitLine transitLine : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {

				if(transitRoute.getRoute().getLinkIds().contains(inLink.getId()) || transitRoute.getRoute().getLinkIds().contains(inLink.getId())){

					LinkedList<Id<Link>> routeLinkIds = new LinkedList<Id<Link>>();
					routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
					for (Id<Link> id : transitRoute.getRoute().getLinkIds()) {
						routeLinkIds.add(id);
					}
					routeLinkIds.add(transitRoute.getRoute().getEndLinkId());

					if(routeLinkIds.contains(inLink.getId()) && routeLinkIds.contains(outLink.getId())){
						routeLinkIds.add(routeLinkIds.indexOf(inLink.getId()), link.getId());
						routeLinkIds.remove(inLink.getId());
						routeLinkIds.remove(outLink.getId());
					}

					NetworkRoute newRoute = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(routeLinkIds.getFirst(), routeLinkIds.getLast());
					Id<Link> startLink = routeLinkIds.pollFirst();
					Id<Link> endLink = routeLinkIds.pollLast();
					newRoute.setLinkIds(startLink, routeLinkIds, endLink);
					transitRoute.setRoute(newRoute);
				}
			}
		}
		return true;
	}

	private boolean linkNeededByTransitStop(Link inLink, Link outLink) {

		if(this.linksNeededByTransitSchedule == null){
			this.linksNeededByTransitSchedule = new TreeSet<String>();
			for (TransitStopFacility transitStopFacility : this.transitSchedule.getFacilities().values()) {
				this.linksNeededByTransitSchedule.add(transitStopFacility.getLinkId().toString());
			}
		}

		if(this.linksNeededByTransitSchedule.contains(inLink.getId().toString())){
			return true;
		}
		if(this.linksNeededByTransitSchedule.contains(outLink.getId().toString())){
			return true;
		}

		return false;
	}

	/**
	 * Specify the types of node which should be merged.
	 *
	 * @param nodeTypesToMerge A Set of integer indicating the node types as specified by {@link NetworkCalcTopoType}
	 * @see NetworkCalcTopoType NetworkCalcTopoType for a list of available classifications.
	 */
	public void setNodesToMerge(Set<Integer> nodeTypesToMerge){
		this.nodeTypesToMerge.addAll(nodeTypesToMerge);
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
	private boolean bothLinksHaveSameLinkStats(Link linkA, Link linkB){

		boolean bothLinksHaveSameLinkStats = true;

		if(!linkA.getAllowedModes().equals(linkB.getAllowedModes())){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getFreespeed() != linkB.getFreespeed()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getCapacity() != linkB.getCapacity()){ bothLinksHaveSameLinkStats = false; }

		if(linkA.getNumberOfLanes() != linkB.getNumberOfLanes()){ bothLinksHaveSameLinkStats = false; }

		return bothLinksHaveSameLinkStats;
	}

	public static void main(String[] args) {
		PTNetworkSimplifier simplifier = new PTNetworkSimplifier("e:/_out/osm/transit-network_bb_subway.xml", "e:/_out/osm/osm_transitSchedule_subway.xml", "e:/_out/osm/transit-network_bb_subway_simplified_merged.xml", "e:/_out/osm/osm_transitSchedule_subway_merged.xml");
		Set<Integer> nodeTypesToMerge = new TreeSet<Integer>();
		nodeTypesToMerge.add(new Integer(4));
		nodeTypesToMerge.add(new Integer(5));
		simplifier.setNodesToMerge(nodeTypesToMerge);
		simplifier.setMergeLinkStats(false);
		simplifier.simplifyPTNetwork();
	}

	public void simplifyPTNetwork(){

		log.info("Start...");
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		this.network = scenario.getNetwork();
		log.info("Reading " + this.netInFile);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(this.netInFile);

		MutableScenario osmScenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config osmConfig = osmScenario.getConfig();
		osmConfig.transit().setUseTransit(true);
		osmConfig.network().setInputFile(this.netInFile);
		ScenarioUtils.loadScenario(osmScenario);

		log.info("Reading " + this.scheduleInFile);
		new TransitScheduleReaderV1(osmScenario).readFile(this.scheduleInFile);

		log.info("Running simplifier...");
		run(this.network, osmScenario.getTransitSchedule());
		TransitScheduleCleaner.removeAllRoutesWithMissingLinksFromSchedule(osmScenario.getTransitSchedule(), this.network);

		log.info("Writing network to " + this.netOutFile);
		new NetworkWriter(this.network).write(this.netOutFile);
		log.info("Writing transit schedule to " + this.scheduleOutFile);
		new TransitScheduleWriter(osmScenario.getTransitSchedule()).writeFile(this.scheduleOutFile);

	}
}