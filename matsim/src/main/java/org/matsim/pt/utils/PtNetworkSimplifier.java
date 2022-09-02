/* *********************************************************************** *
 * project: org.matsim.*
 * CreateVehiclesForSchedule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.pt.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCalcTopoType;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Simplifies a given network, by merging links. For all links with
 * {@link TransportMode.pt}, the transit schedule is considered and adjusted
 * when merging links.
 * 
 * Copied and adapted from
 * {@link playground.vsp.andreas.utils.pt.PTNetworkSimplifier}
 * 
 * @author aneumann
 *
 */
public class PtNetworkSimplifier {

	private static final Logger LOG = LogManager.getLogger(PtNetworkSimplifier.class);

	private final Counter counter = new Counter(
			"[" + PtNetworkSimplifier.class.getSimpleName() + "] processed node # ");

	private boolean mergeLinkStats = false;
	private TransitSchedule transitSchedule;
	private TreeSet<Id<Link>> linksNeededByTransitSchedule = null;
	private Network network;

	private Set<Integer> nodeTypesToMerge = new TreeSet<>(
			Arrays.asList(NetworkCalcTopoType.PASS1WAY, NetworkCalcTopoType.PASS2WAY));

	public static final BiConsumer<Tuple<Link, Link>, Link> DEFAULT_TRANSFER_ATTRIBUTES_CONSUMER = (inOutLinks,
			newLink) -> {
		// do nothing
	};

	/**
	 * Create PTNetworkSimplifier
	 * 
	 * @param network
	 * @param transitSchedule
	 */
	public PtNetworkSimplifier(final Network network, final TransitSchedule transitSchedule) {
		this.network = network;
		this.transitSchedule = transitSchedule;
	}

	/**
	 * Merges all qualifying links.
	 */
	public void run() {
		run(DEFAULT_TRANSFER_ATTRIBUTES_CONSUMER);
	}

	/**
	 * Merges all qualifying links
	 * 
	 * @param transferAttributes consumer(Tuple.of(inLink, outLink), newLink) to
	 *                           customize merging of non-standard attributes
	 */
	public void run(final BiConsumer<Tuple<Link, Link>, Link> transferAttributes) {

		if (this.nodeTypesToMerge.isEmpty()) {
			throw new RuntimeException("No types of node specified. Please use setNodesToMerge to specify which nodes should be merged");
		}

		LOG.info("running {} algorithm...", this.getClass().getName());

		NetworkCalcTopoType nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(this.network);

		TreeSet<Id<Node>> nodesConnectedToTransitStop = new TreeSet<>();
		for (Node node : this.network.getNodes().values()) {
			this.counter.incCounter();

			if (nodesConnectedToTransitStop.contains(node.getId())) {
				continue;
			}

			if (this.nodeTypesToMerge.contains(Integer.valueOf(nodeTopo.getTopoType(node)))) {
				List<Link> iLinks = new ArrayList<>(node.getInLinks().values());

				for (Link inLink : iLinks) {
					List<Link> otLinks = new ArrayList<>(node.getOutLinks().values());

					for (Link outLink : otLinks) {

						if (inLink != null && outLink != null && !outLink.getToNode().getId().equals(inLink.getFromNode().getId())) {

							if (!linkNeededByTransitStop(inLink, outLink)) {
								Link link = null;

								if (this.mergeLinkStats) {

									// Try to merge both links by guessing the resulting links attributes
									link = this.network.getFactory().createLink(
											Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
											inLink.getFromNode(), outLink.getToNode());

									// length can be summed up
									link.setLength(inLink.getLength() + outLink.getLength());

									// freespeed depends on total length and time needed for inLink and outLink
									link.setFreespeed((inLink.getLength() + outLink.getLength())
											/ (NetworkUtils.getFreespeedTravelTime(inLink)
													+ NetworkUtils.getFreespeedTravelTime(outLink)));

									// the capacity and the new links end is important, thus it will be set to the
									// minimum
									link.setCapacity(Math.min(inLink.getCapacity(), outLink.getCapacity()));

									// number of lanes can be derived from the storage capacity of both links
									link.setNumberOfLanes((inLink.getLength() * inLink.getNumberOfLanes()
											+ outLink.getLength() * outLink.getNumberOfLanes())
											/ (inLink.getLength() + outLink.getLength()));
									if (NetworkUtils.getOrigId(inLink) != null || NetworkUtils.getOrigId(outLink) != null) {
										NetworkUtils.setOrigId(link, NetworkUtils.getOrigId(inLink) + "-" + NetworkUtils.getOrigId(outLink));
									}

								} else {
									// Only merge links with same attributes
									if (NetworkSimplifier.bothLinksHaveSameLinkStats(inLink, outLink)) {
										link = this.network.getFactory().createLink(
												Id.create(inLink.getId() + "-" + outLink.getId(), Link.class),
												inLink.getFromNode(), outLink.getToNode());
										if (NetworkUtils.getOrigId(inLink) != null || NetworkUtils.getOrigId(outLink) != null) { 
											NetworkUtils.setOrigId(link, NetworkUtils.getOrigId(inLink) + "-" + NetworkUtils.getOrigId(outLink));
										}
										link.setAllowedModes(inLink.getAllowedModes());
										link.setLength(inLink.getLength() + outLink.getLength());
										link.setFreespeed(inLink.getFreespeed());
										link.setCapacity(inLink.getCapacity());
										link.setNumberOfLanes(inLink.getNumberOfLanes());
									}
								}

								if (link != null && !nodesConnectedToTransitStop.contains(node.getId())
										&& !nodesConnectedToTransitStop.contains(link.getFromNode().getId())
										&& !nodesConnectedToTransitStop.contains(link.getToNode().getId())) {

									transferAttributes.accept(Tuple.of(inLink, outLink), link);
									if (inLink.getAllowedModes().contains(TransportMode.pt) || outLink.getAllowedModes().contains(TransportMode.pt)) {
										if (removeLinksFromTransitSchedule(link, inLink, outLink)) {
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

							} else {
								nodesConnectedToTransitStop.add(node.getId());
							}
						}
					}
				}
			}
		}

		LOG.info("  resulting network contains {} nodes and {} links.", this.network.getNodes().size(),
				this.network.getLinks().size());
		LOG.info("done.");

		// writes stats as a side effect
		nodeTopo = new NetworkCalcTopoType();
		nodeTopo.run(this.network);
	}

	private boolean areLinksMergeable(final Link inLink, final Link outLink) {
		return this.transitSchedule.getTransitLines().values().parallelStream()
				.allMatch(transitLine -> areLinksMergeable(transitLine, inLink, outLink));
	}

	private static boolean areLinksMergeable(final TransitLine transitLine, final Link inLink, final Link outLink) {
		for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
			if (transitRoute.getRoute().getLinkIds().contains(inLink.getId())
					|| transitRoute.getRoute().getLinkIds().contains(outLink.getId())) {

				LinkedList<Id<Link>> routeLinkIds = new LinkedList<>();
				routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
				for (Id<Link> id : transitRoute.getRoute().getLinkIds()) {
					routeLinkIds.add(id);
				}
				routeLinkIds.add(transitRoute.getRoute().getEndLinkId());

				for (Iterator<Id<Link>> iterator = routeLinkIds.iterator(); iterator.hasNext();) {
					Id<Link> id = iterator.next();
					if (id.equals(inLink.getId())) {
						Id<Link> nextId = iterator.next();
						if (nextId.equals(outLink.getId())) {
							// everything okay
							break;
						} else {
							// inLink and outLink ar not followers, thus they should not be touched
							return false;
						}
					} else if (id.equals(outLink.getId())) {
						// if we find the outLink before/without the inLink, we should not merge
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean removeLinksFromTransitSchedule(Link link, Link inLink, Link outLink) {
		// check if links can be merged considering all transit routes
		if (!areLinksMergeable(inLink, outLink)) {
			return false;
		}

		// merge links
		for (TransitLine transitLine : this.transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {

				if (transitRoute.getRoute().getLinkIds().contains(inLink.getId())
						&& transitRoute.getRoute().getLinkIds().contains(outLink.getId())) {

					LinkedList<Id<Link>> routeLinkIds = new LinkedList<>();
					routeLinkIds.add(transitRoute.getRoute().getStartLinkId());
					for (Id<Link> id : transitRoute.getRoute().getLinkIds()) {
						routeLinkIds.add(id);
					}
					routeLinkIds.add(transitRoute.getRoute().getEndLinkId());

					if (routeLinkIds.contains(inLink.getId()) && routeLinkIds.contains(outLink.getId())) {
						routeLinkIds.add(routeLinkIds.indexOf(inLink.getId()), link.getId());
						routeLinkIds.remove(inLink.getId());
						routeLinkIds.remove(outLink.getId());
					}

					NetworkRoute newRoute = (NetworkRoute) new LinkNetworkRouteFactory()
							.createRoute(routeLinkIds.getFirst(), routeLinkIds.getLast());
					Id<Link> startLink = routeLinkIds.pollFirst();
					Id<Link> endLink = routeLinkIds.pollLast();
					newRoute.setLinkIds(startLink, routeLinkIds, endLink);
					transitRoute.setRoute(newRoute);
				}
			}
		}
		return true;
	}

	private boolean linkNeededByTransitStop(final Link inLink, final Link outLink) {
		if (this.linksNeededByTransitSchedule == null) {
			this.linksNeededByTransitSchedule = new TreeSet<>();
			for (TransitStopFacility transitStopFacility : this.transitSchedule.getFacilities().values()) {
				this.linksNeededByTransitSchedule.add(transitStopFacility.getLinkId());
			}
		}
		return this.linksNeededByTransitSchedule.contains(inLink.getId()) || this.linksNeededByTransitSchedule.contains(outLink.getId());
	}

	/**
	 * Specify the types of node which should be merged.
	 * 
	 * See {@link NetworkCalcTopoType} for a list of available classifications.
	 *
	 * @param nodeTypesToMerge A set of integers indicating the node types as
	 *                         specified by {@link NetworkCalcTopoType}
	 */
	public void setNodesToMerge(Set<Integer> nodeTypesToMerge) {
		this.nodeTypesToMerge.addAll(nodeTypesToMerge);
	}

	/**
	 * Set, whether links with different standard attributes should be merged.
	 *
	 * @param mergeLinkStats If set true, links will be merged despite their
	 *                       different attributes.
	 *                       If set false (default), only links with the same
	 *                       attributes will be merged, thus preserving as much
	 *                       information as possible.
	 */
	public void setMergeLinkStats(boolean mergeLinkStats) {
		this.mergeLinkStats = mergeLinkStats;
	}

	public static void main(String[] args) {
		if (args.length < 4) {
			LOG.error("Required arguments: inNetworkFile inScheduleFile outNetworkFile outScheduleFile");
			return;
		}
		final String inNetworkFile = args[0];
		final String inScheduleFile = args[1];
		final String outNetworkFile = args[2];
		final String outScheduleFile = args[3];

		Set<Integer> nodeTypesToMerge = new TreeSet<>();
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS1WAY);
		nodeTypesToMerge.add(NetworkCalcTopoType.PASS2WAY);

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(inNetworkFile);
		new TransitScheduleReader(scenario).readFile(inScheduleFile);

		PtNetworkSimplifier ptSimplifier = new PtNetworkSimplifier(scenario.getNetwork(),
				scenario.getTransitSchedule());
		ptSimplifier.setNodesToMerge(nodeTypesToMerge);
		LOG.info("Simplifying network and merging node types: {}", nodeTypesToMerge);
		ptSimplifier.run();

		new NetworkWriter(scenario.getNetwork()).write(outNetworkFile);
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(outScheduleFile);
	}

}