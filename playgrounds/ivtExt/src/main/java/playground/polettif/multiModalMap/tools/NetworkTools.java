/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.multiModalMap.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.multiModalMap.config.PublicTransportMapConfigGroup;

import java.util.*;

/**
 * Provides Tools for analysing and manipulating networks.
 *
 * @author polettif
 */
public class NetworkTools {

	protected static Logger log = Logger.getLogger(NetworkTools.class);

	public static Network loadNetwork(String filePath) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(filePath);
		return network;
	}

	public static void writeNetwork(Network network, String filePath) {
		new NetworkWriter(network).write(filePath);
	}

	public static Network createNetwork() {
		return NetworkUtils.createNetwork();
	}

	public static void transformNetwork(Network network, String fromCoordinateSystem, String toCoordinateSystem) {
		new NetworkTransform(TransformationFactory.getCoordinateTransformation(fromCoordinateSystem, toCoordinateSystem)).run(network);
	}

	public static void transformNetworkFile(String networkFile, String fromCoordinateSystem, String toCoordinateSystem) {
		log.info("... Transformig network from "+fromCoordinateSystem+" to "+toCoordinateSystem);
		Network network = loadNetwork(networkFile);
		transformNetwork(network, fromCoordinateSystem, toCoordinateSystem);
		writeNetwork(network, networkFile);
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes(Coord, double)},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns maxNLinks or
	 * all links within maxLinkDistance (whichever is reached earlier).
	 *
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl A network implementation
	 * @param coord the coordinate from which the closest links are
	 *              to be searched
	 * @param nodeSearchRadius Only links from and to nodes within this
	 *                         radius are considered
	 * @param maxNLinks How many links should be returned. Note: Method
	 *                  an return more than n links if two links have the
	 *                  same distance from the facility.
	 * @param maxLinkDistance Only returns links which are closer than
	 *                        this distance to the coordinate.
	 * @return the list of closest links
	 */
	public static List<Link> findNClosestLinks(NetworkImpl networkImpl, Coord coord, double nodeSearchRadius, int maxNLinks, double maxLinkDistance) {
		List<Link> closestLinks = new ArrayList<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.0001; double tol=0.001;

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			// check every in- and outlink of each node
			for (Node node : nearestNodes) {
				Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
				Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
				double lineSegmentDistance;

				for (Link outLink : outLinks.values()) {
					// check if link is already in the closestLinks set
					if(!closestLinksMap.containsValue(outLink)) {
						// only use links with a viable network transport mode
						lineSegmentDistance = CoordUtils.distancePointLinesegment(outLink.getFromNode().getCoord(), outLink.getToNode().getCoord(), coord);

						// since distance is used as key, we need to ensure the exact distance is not used already
						while(closestLinksMap.containsKey(lineSegmentDistance))
							lineSegmentDistance += incr;

						closestLinksMap.put(lineSegmentDistance, outLink);
					}
				}
				for (Link inLink : inLinks.values()) {
					if (!closestLinksMap.containsValue(inLink)) {
						lineSegmentDistance = CoordUtils.distancePointLinesegment(inLink.getFromNode().getCoord(), inLink.getToNode().getCoord(), coord);
						while(closestLinksMap.containsKey(lineSegmentDistance)) {
							lineSegmentDistance += incr;
						}
						closestLinksMap.put(lineSegmentDistance, inLink);
					}
				}
			}

			int i = 1; double previousDistance = 2*tol;
			for(Map.Entry<Double, Link> entry : closestLinksMap.entrySet()) {
				// if the distance difference to the previous link is less than tol, add the link as well
				if(i > maxNLinks && Math.abs(entry.getKey() - previousDistance) >= tol) {
					break;
				}
				if(entry.getKey() > maxLinkDistance) {
					break;
				}

				previousDistance = entry.getKey();
				closestLinks.add(entry.getValue());
				i++;
			}

			return closestLinks;
		}
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns maxNLinks or
	 * all links within maxLinkDistance (whichever is reached earlier).
	 *<p/>
	 * If N links are reached, additional links are added to the set
	 * if their distance is less than toleranceFactor * distance to the
	 * farthest link
	 *
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl A network implementation
	 * @param coord the coordinate from which the closest links are
	 *              to be searched
	 * @param nodeSearchRadius Only links from and to nodes within this
	 *                         radius are considered
	 * @param maxNLinks How many links should be returned. Note: Method
	 *                  an return more than n links if two links have the
	 *                  same distance from the facility.
	 * @param maxLinkDistance Only returns links which are closer than
	 *                        this distance to the coordinate.
	 * @param toleranceFactor [> 1]
	 * @return the list of closest links
	 */
	@Deprecated
	public static List<Link> findClosestLinksSoftConstraints(NetworkImpl networkImpl, Coord coord, double nodeSearchRadius, int maxNLinks, double maxLinkDistance, double toleranceFactor) {
		List<Link> closestLinks = new ArrayList<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.001;
		if(toleranceFactor < 1)
			toleranceFactor = 1;

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			// check every in- and outlink of each node
			for (Node node : nearestNodes) {
				Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
				Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
				double lineSegmentDistance;

				for (Link outLink : outLinks.values()) {
					// check if link is already in the closestLinks set
					if(!closestLinksMap.containsValue(outLink)) {
						// only use links with a viable network transport mode
						lineSegmentDistance = CoordUtils.distancePointLinesegment(outLink.getFromNode().getCoord(), outLink.getToNode().getCoord(), coord);

						// since distance is used as key, we need to ensure the exact distance is not used already
						while(closestLinksMap.containsKey(lineSegmentDistance))
							lineSegmentDistance += incr;

						closestLinksMap.put(lineSegmentDistance, outLink);
					}
				}
				for (Link inLink : inLinks.values()) {
					if (!closestLinksMap.containsValue(inLink)) {
						lineSegmentDistance = CoordUtils.distancePointLinesegment(inLink.getFromNode().getCoord(), inLink.getToNode().getCoord(), coord);
						while(closestLinksMap.containsKey(lineSegmentDistance)) {
							lineSegmentDistance += incr;
						}
						closestLinksMap.put(lineSegmentDistance, inLink);
					}
				}
			}

			int i = 1; double maxSoftDistance = 0;
			for(Map.Entry<Double, Link> entry : closestLinksMap.entrySet()) {
				if(i == maxNLinks) {
					maxSoftDistance = (entry.getKey()+2*incr)*toleranceFactor;
				}

				// if the distance difference to the previous link is less than tol, add the link as well
				if(i > maxNLinks && Math.abs(entry.getKey()) > maxSoftDistance) {
					break;
				}
				if(entry.getKey() > maxLinkDistance) {
					break;
				}
				closestLinks.add(entry.getValue());
				i++;
			}

			return closestLinks;
		}
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes(Coord, double)},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns links with the allowed
	 * networkTransportMode for the input scheduleTransportMode (defined in
	 * config). Returns maxNLinks or all links within maxLinkDistance (whichever
	 * is reached earlier).
	 *
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl A network implementation (needed
	 *                    for {@link NetworkImpl#getNearestNodes(Coord, double)}
	 * @param coord the coordinate from which the closest links
	 *              are searched
	 * @param scheduleTransportMode the transport mode of the "current" transitRoute.
	 *                              The config should define which networkTransportModes
	 *                              are allowed for this scheduleMode
	 * @param config	The config defining maxNnodes, search radius etc.
	 * @return a Set of the closest links
	 */
	public static Set<Link> findClosestLinksByMode(NetworkImpl networkImpl, Coord coord, String scheduleTransportMode, PublicTransportMapConfigGroup config) {
		Set<Link> closestLinks = new HashSet<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, config.getNodeSearchRadius());
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.001;
		double toleranceFactor = (config.getLinkDistanceTolerance() < 1 ? 1 : config.getLinkDistanceTolerance());
		int maxNLinks = config.getMaxNClosestLinks();

		Set<String> networkTransportModes = config.getModeRoutingAssignment().get(scheduleTransportMode);

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			// check every in- and outlink of each node
			for (Node node : nearestNodes) {
				Set<Link> links = new HashSet<>(node.getInLinks().values());
				links.addAll(node.getOutLinks().values());

				double lineSegmentDistance;

				for (Link link : links) {
					// check if link is already in the closestLinks set
					if(!closestLinksMap.containsValue(link)) {
						// only use links with a viable network transport mode
						if(MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), networkTransportModes)) {
							lineSegmentDistance = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);

							// since distance is used as key, we need to ensure the exact distance is not used already
							while(closestLinksMap.containsKey(lineSegmentDistance))
								lineSegmentDistance += incr;

							closestLinksMap.put(lineSegmentDistance, link);
						}
					}
				}
			}

			int i = 1; double maxSoftDistance = Double.MAX_VALUE;
			for(Map.Entry<Double, Link> entry : closestLinksMap.entrySet()) {
				if(i == maxNLinks) {
					maxSoftDistance = (entry.getKey()+2*incr)*toleranceFactor;
				}

				// if the distance difference to the previous link is less than tol, add the link as well
				if(i > maxNLinks && entry.getKey() > maxSoftDistance) {
					break;
				}
				if(entry.getKey() > config.getMaxStopFacilityDistance()) {
					break;
				}
				closestLinks.add(entry.getValue());
				i++;
			}

			return closestLinks;
		}
	}

	/**
	 * Adds a node on the position of coord and connects it with two links to the neareast node of the network.
	 * @param coord where the new node should be created
	 * @param network that should be modified
	 * @param idPrefix the prefix for the new node and links
	 * @param idCounter is simply appended to the idPrefix and incremented
	 * @return a list with the two newly created links
	 */
	@Deprecated
	public static List<Link> createLinkToNearestNode(Coord coord, Network network, String idPrefix, int idCounter) {
		NetworkImpl networkImpl = (NetworkImpl) network;
		NetworkFactory networkFactory = network.getFactory();

		Node newNode = networkFactory.createNode(Id.create(idPrefix + "node_" + idCounter, Node.class), coord);
		Node nearestNode = networkImpl.getNearestNode(coord);
		Link newLink = networkFactory.createLink(Id.createLinkId(idPrefix + idCounter + ":1"), newNode, nearestNode);
		Link newLink2 = networkFactory.createLink(Id.createLinkId(idPrefix + idCounter + ":2"), nearestNode, newNode);

		network.addNode(newNode);
		network.addLink(newLink);
		network.addLink(newLink2);

		List<Link> newLinks = new ArrayList<>();
		newLinks.add(newLink);
		newLinks.add(newLink2);

		return newLinks;
	}

	/**
	 * Creates a node and dummy/loop link on the coordinate of the stop facility and
	 * adds both to the network. The stop facility is NOT referenced.
	 * @return the new Link.
	 */
	public static Link createArtificialStopFacilityLink(TransitStopFacility stopFacility, Network network, String prefix) {
		NetworkFactory networkFactory = network.getFactory();

		Coord coord = stopFacility.getCoord();

		Node dummyNode = networkFactory.createNode(Id.createNodeId(prefix+stopFacility.getId()+"_node"), coord);
		Link dummyLink = networkFactory.createLink(Id.createLinkId(prefix+stopFacility.getId()+"_link"), dummyNode, dummyNode);

		dummyLink.setAllowedModes(Collections.singleton(PublicTransportMapConfigGroup.ARTIFICIAL_LINK_MODE));
		dummyLink.setLength(1.0);

		if(!network.getNodes().containsKey(dummyNode.getId())) {
			network.addNode(dummyNode);
			network.addLink(dummyLink);
		}

		return dummyLink;
	}

	/**
	 * @return the azimuth in [rad] of a line defined by two points.
	 */
	public static double getAzimuth(Coord from, Coord to) {
		double deltaE = to.getX()-from.getX();
		double deltaN = to.getY()-from.getY();

		double az2 = Math.atan2(deltaE, deltaN);

		if(az2 < 0)
			az2 = az2+2*Math.PI;

		if(az2 >= 2*Math.PI)
			az2 = az2-2*Math.PI;

		return az2;
	}

	/**
	 * @return Returns the point on the line between lineStart and lineEnd which
	 * is closest to refPoint.
	 */
	public static Coord getClosestPointOnLine(Coord lineStart, Coord lineEnd, Coord refPoint) {
		double azLine = getAzimuth(lineStart, lineEnd);
		double azPoint = getAzimuth(lineStart, refPoint);
		double azDiff = (azLine > azPoint ? azLine-azPoint : azPoint-azLine);

		double distanceToNewPoint = Math.cos(azDiff) * CoordUtils.calcEuclideanDistance(lineStart, refPoint);

		// assuming precision < 1 mm is not needed
		double newN = lineStart.getY() + Math.round(Math.cos(azLine) * distanceToNewPoint * 1000) / 1000.;
		double newE = lineStart.getX() + Math.round(Math.sin(azLine) * distanceToNewPoint * 1000) / 1000.;

		return new Coord(newE, newN);
	}

	public static Coord getClosestPointOnLine(Link link, Coord refPoint) {	return getClosestPointOnLine(link.getFromNode().getCoord(), link.getToNode().getCoord(), refPoint);}


	/**
	 * @return the opposite direction link
	 */
	public static Link getOppositeLink(Link link) {
		if (link == null) {
			return null;
		}

		Link oppositeDirectionLink = null;
		Map<Id<Link>, ? extends Link> inLinks = link.getFromNode().getInLinks();
		if(inLinks != null) {
			for (Link inLink : inLinks.values()) {
				if (inLink.getFromNode().equals(link.getToNode())) {
					oppositeDirectionLink = inLink;
				}
			}
		}

		return oppositeDirectionLink;
	}

	/**
	 * Checks if a link sequence has loops (i.e. the same link is passed twice).
	 *
	 * @param links
	 */
	public static boolean linkSequenceHasLoops(List<Link> links) {
		Set tmpSet = new HashSet<>(links);
		return tmpSet.size() < links.size();
	}


	/**
	 * Checks if a link sequence has u-turns (i.e. the opposite direction link is
	 * passed immediately after a link).
	 */
	public static boolean linkSequenceHasUTurns(List<Link> links) {
		for(int i = 1; i < links.size(); i++) {
			if(links.get(i).getToNode().equals(links.get(i - 1).getFromNode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A debug method to assign weights to network links as number of lanes.
	 * The network is changed permanently, so this should really only be used for
	 * debugging.
	 */
	public static void visualizeWeightsAsLanes(Network network, Map<Id<Link>, Double> weightMap) {
		for(Map.Entry<Id<Link>, Double> w : weightMap.entrySet()) {
			network.getLinks().get(w.getKey()).setNumberOfLanes(w.getValue());
		}
	}

	/**
	 * Calculates the extent of the given network.
	 * @return Array of Coords with the minimal South-West and the
	 * 		   maximal North-East Coordinates
	 */
	public static Coord[] getExtent(Network network) {
		double maxE = 0;
		double maxN = 0;
		double minS = Double.MAX_VALUE;
		double minW = Double.MAX_VALUE;

		for(Node node : network.getNodes().values()) {
			if(node.getCoord().getX() > maxE) {
				maxE = node.getCoord().getX();
			}
			if(node.getCoord().getY() > maxN) {
				maxN = node.getCoord().getY();
			}
			if(node.getCoord().getX() < minW) {
				minW = node.getCoord().getX();
			}
			if(node.getCoord().getY() < minS) {
				minS = node.getCoord().getY();
			}
		}

		return new Coord[]{new Coord(minW, minS), new Coord(maxE, maxN)};
	}

	/**
	 * Checks if a coordinate is in the area given by SE and NE.
	 * @param coord the coordinate to check
	 * @param SW the south-west corner of the area
	 * @param NE the north-east corner of the area
	 */
	public static boolean isInArea(Coord coord, Coord SW, Coord NE) {
		return (getCompassQuarter(SW, coord) == 1 && getCompassQuarter(NE, coord) == 3);
	}

	/**
	 * Checks if a coordinate is in the area
	 * @param coord the coordinate to check
	 * @param area [0] the south-west corner of the area, [1] the north-east corner of the area
	 */
	public static boolean isInArea(Coord coord, Coord[] area) {
		return (getCompassQuarter(area[0], coord) == 1 && getCompassQuarter(area[1], coord) == 3);
	}

	/**
	 * @return whether Coord2 lies<br/>
	 * [1] North-East<br/>
	 * [2] South-East<br/>
	 * [3] South-West<br/>
	 * [4] North-West<br/>
	 * of Coord1
	 */
	public static int getCompassQuarter(Coord baseCoord, Coord toCoord) {
		double az = getAzimuth(baseCoord, toCoord);


		if(az < Math.PI/2) {
			return 1;
		} else if(az >= Math.PI/2 && az < Math.PI) {
			return 2;
		} else if(az > Math.PI && az < 1.5*Math.PI) {
			return 3;
		} else {
			return 4;
		}
	}

	/**
	 * @return a Map with a boolean for each stop facility denoting whether
	 * the facility is within the area or not.
	 */
	public static Map<TransitStopFacility, Boolean> getStopsInAreaBool(TransitSchedule schedule, Coord[] area) {
		HashMap<TransitStopFacility, Boolean> stopsInArea = new HashMap<>();
		for(TransitStopFacility stopFacility : schedule.getFacilities().values()) {
			stopsInArea.put(stopFacility, NetworkTools.isInArea(stopFacility.getCoord(), area));
		}
		return stopsInArea;
	}

	/**
	 * Merges all network into baseNetworks. If a link id already
	 * exists in the base network, the link is not added to it.
	 *
	 * @param baseNetwork the network in which all other networks are integrated
	 * @param networks collection of networks to merge into the base network
	 */
	public static void mergeNetworks(Network baseNetwork, Collection<Network> networks) {
		log.info("Merging networks...");

		int numberOfLinksBefore = baseNetwork.getLinks().size();
		int numberOfNodesBefore = baseNetwork.getNodes().size();

		for(Network currentNetwork : networks) {
			integrateNetwork(baseNetwork, currentNetwork);
		}

		log.info("... Total number of links added to network: " + (baseNetwork.getLinks().size()-numberOfLinksBefore));
		log.info("... Total number of nodes added to network: " + (baseNetwork.getNodes().size()-numberOfNodesBefore));
		log.info("Merging networks... done.");
	}

	/**
	 * Integrates network B into network A. Network
	 * A contains all links and nodes of both networks
	 * after integration.
	 *
	 * @param networkA
	 * @param networkB
	 */
	public static void integrateNetwork(final Network networkA, final Network networkB) {
		final NetworkFactory factory = networkA.getFactory();

		// Nodes
		for (Node node : networkB.getNodes().values()) {
			Id<Node> nodeId = Id.create(node.getId().toString(), Node.class);
			if(!networkA.getNodes().containsKey(nodeId)) {
				Node newNode = factory.createNode(nodeId, node.getCoord());
				networkA.addNode(newNode);
			}
		}

		// Links
		double capacityFactor = networkA.getCapacityPeriod() / networkB.getCapacityPeriod();
		for (Link link : networkB.getLinks().values()) {
			Id<Link> linkId = Id.create(link.getId().toString(), Link.class);
			if (!networkA.getLinks().containsKey(linkId)) {
				Id<Node> fromNodeId = Id.create(link.getFromNode().getId().toString(), Node.class);
				Id<Node> toNodeId = Id.create(link.getToNode().getId().toString(), Node.class);
				Link newLink = factory.createLink(linkId, networkA.getNodes().get(fromNodeId), networkA.getNodes().get(toNodeId));
				newLink.setAllowedModes(link.getAllowedModes());
				newLink.setCapacity(link.getCapacity() * capacityFactor);
				newLink.setFreespeed(link.getFreespeed());
				newLink.setLength(link.getLength());
				newLink.setNumberOfLanes(link.getNumberOfLanes());
				networkA.addLink(newLink);
			}
		}
	}


	/**
	 * @return which border of a rectangular area of interest a line fromCoord-toCoord crosses. One coord has to be
	 * inside area of interest<br/>
	 * [10] north->inside<br/>
	 * [17] inside->north<br/>
	 * [20] east->inside<br/>
	 * [27] inside->east<br/>
	 * [30] south->inside<br/>
	 * [37] inside->south<br/>
	 * [40] west->inside<br/>
	 * [47] inside->west<br/>
	 * [0] line does not cross any border
	 */
	@Deprecated
	public int getBorderCrossType(Coord SWcut, Coord NEcut, Coord fromCoord, Coord toCoord) {
		int fromSector = getAreaOfInterestSector(SWcut, NEcut, fromCoord);
		int toSector = getAreaOfInterestSector(SWcut, NEcut, toCoord);

		if(fromSector == toSector) {
			return 0;
		}

		double azFromTo = getAzimuth(fromCoord, toCoord);
		double azFromSW = getAzimuth(fromCoord, SWcut);
		double azFromNE = getAzimuth(fromCoord, NEcut);

		double azToFrom = getAzimuth(toCoord, fromCoord);
		double azToSW = getAzimuth(toCoord, SWcut);
		double azToNE = getAzimuth(toCoord, NEcut);

		if(fromSector != 0 ) {
			switch (fromSector) {
				case 1:
					return 10;
				case 2: {
					if(azFromTo > azFromNE)
						return 10;
					else
						return 20;
				}
				case 3:
					return 20;
				case 4: {
					if(azFromTo > azFromNE)
						return 20;
					else
						return 30;
				}
				case 5:
					return 30;
				case 6: {
					if(azFromTo > azFromSW)
						return 30;
					else
						return 40;
				}
				case 7:
					return 40;
				case 8: {
					if(azFromTo > azFromSW)
						return 40;
					else
						return 10;
				}
			}
		}

		if(toSector != 0) {
			switch (toSector) {
				case 1:
					return 17;
				case 2: {
					if(azToFrom < azToNE)
						return 17;
					else
						return 27;
				}
				case 3:
					return 27;
				case 4: {
					if(azToFrom < azToNE)
						return 27;
					else
						return 37;
				}
				case 5:
					return 37;
				case 6: {
					if(azToFrom < azToSW)
						return 37;
					else
						return 47;
				}
				case 7:
					return 47;
				case 8: {
					if(azToFrom< azToSW)
						return 47;
					else
						return 17;
				}
			}
		}

		return 0;
	}

	@Deprecated
	private int getAreaOfInterestSector(Coord SWcut, Coord NEcut, Coord c) {
		int qSW = getCompassQuarter(SWcut, c);
		int qNE = getCompassQuarter(NEcut, c);

		if(qSW == 1 && qNE == 3) {
			return 0;
		} else if(qSW == 1 && qNE == 4) {
			return 1;
		} else if(qSW == 1 && qNE == 1) {
			return 2;
		} else if(qSW == 1 && qNE == 2) {
			return 3;
		} else if(qSW == 2 && qNE == 2) {
			return 4;
		} else if(qSW == 2 && qNE == 3) {
			return 5;
		} else if(qSW == 3 && qNE == 3) {
			return 6;
		} else if(qSW == 4 && qNE == 3) {
			return 7;
		} else if(qSW == 4 && qNE == 4) {
			return 8;
		}

		return 0;
	}

	@Deprecated
	public void shortenLink(Link link, Node toNode) {
		link.setToNode(toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), toNode.getCoord()));
	}

	@Deprecated
	public void shortenLink(Node fromNode, Link link) {
		link.setFromNode(fromNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), fromNode.getCoord()));
	}

}
