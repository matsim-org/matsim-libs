/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkUtils.java
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

package org.matsim.core.network;

import java.util.*;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.algorithms.NetworkModeRestriction;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

/**
 * Contains several helper methods for working with {@link Network networks}.
 *
 * @author mrieser
 */
public final class NetworkUtils {

	private static final Logger log = LogManager.getLogger(NetworkUtils.class);

	private NetworkUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * This will create a time invariant network.
	 *
	 * @return Empty time invariant MATSim network
	 */
	public static Network createNetwork() {
		return createNetwork(ConfigUtils.createConfig());
	}

	/**
	 * Override for {@link NetworkUtils#createNetwork(NetworkConfigGroup)}
	 */
	public static Network createNetwork(Config config) {
		return createNetwork(config.network());
	}

	/**
	 * Creates a network based on the properties found in network config group. Either returns a default network or
	 * a time variable network
	 *
	 * @param networkConfigGroup the 'isTimeVariantNetwork' property is read
	 * @return Empty MATSim network
	 */
	public static Network createNetwork(NetworkConfigGroup networkConfigGroup) {
		LinkFactory linkFactory = new LinkFactoryImpl();

		if (networkConfigGroup.isTimeVariantNetwork()) {
			linkFactory = new VariableIntervalTimeVariantLinkFactory();
		}

		return new NetworkImpl(linkFactory);
	}

	/**
	 * @return The bounding box of all the given nodes as <code>double[] = {minX, minY, maxX, maxY}</code>
	 */
	public static double[] getBoundingBox(final Collection<? extends Node> nodes) {
		double[] bBox = new double[4];
		bBox[0] = Double.POSITIVE_INFINITY;
		bBox[1] = Double.POSITIVE_INFINITY;
		bBox[2] = Double.NEGATIVE_INFINITY;
		bBox[3] = Double.NEGATIVE_INFINITY;

		for (Node n : nodes) {
			if (n.getCoord().getX() < bBox[0]) {
				bBox[0] = n.getCoord().getX();
			}
			if (n.getCoord().getX() > bBox[2]) {
				bBox[2] = n.getCoord().getX();
			}
			if (n.getCoord().getY() > bBox[3]) {
				bBox[3] = n.getCoord().getY();
			}
			if (n.getCoord().getY() < bBox[1]) {
				bBox[1] = n.getCoord().getY();
			}
		}
		return bBox;
	}

	/**
	 * @return array containing the nodes, sorted ascending by id.
	 */
	public static Node[] getSortedNodes(final Network network) {
		Node[] nodes = network.getNodes().values().toArray(Node[]::new);
		Arrays.sort(nodes, Comparator.comparing(Identifiable::getId));
		return nodes;
	}

	/**
	 * @param nodes list of node ids, separated by one or multiple whitespace (space, \t, \n)
	 * @return list containing the specified nodes.
	 * @throws IllegalArgumentException if a specified node is not found in the network
	 */
	public static List<Node> getNodes(final Network network, final String nodes) {
		if (nodes == null) {
			return new ArrayList<>(0);
		}
		String trimmed = nodes.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<Node> nodesList = new ArrayList<>(parts.length);

		for (String id : parts) {
			Node node = network.getNodes().get(Id.create(id, Node.class));
			if (node == null) {
				throw new IllegalArgumentException("no node with id " + id);
			}
			nodesList.add(node);
		}
		return nodesList;
	}

	/**
	 * @return array containing the links, sorted ascending by id.
	 */
	public static Link[] getSortedLinks(final Network network) {
		Link[] links = network.getLinks().values().toArray(Link[]::new);
		Arrays.sort(links, Comparator.comparing(Identifiable::getId));
		return links;
	}

	/**
	 * @param links list of link ids, separated by one or multiple whitespace (space, \t, \n)
	 * @return list containing the specified links.
	 * @throws IllegalArgumentException if a specified node is not found in the network
	 */
	public static List<Link> getLinks(final Network network, final String links) {
		if (links == null) {
			return new ArrayList<>(0);
		}
		String trimmed = links.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<Link> linksList = new ArrayList<>(parts.length);

		for (String id : parts) {
			Link link = network.getLinks().get(Id.create(id, Link.class));
			if (link == null) {
				throw new IllegalArgumentException("no link with id " + id);
			}
			linksList.add(link);
		}
		return linksList;
	}

	/**
	 * Splits the given string at whitespace (one or more space, tab, newline) into single pieces, which are interpreted as ids.
	 *
	 */
	public static List<Id<Link>> getLinkIds(final String links) {
		if (links == null) {
			return new ArrayList<>(0);
		}
		String trimmed = links.trim();
		if (trimmed.length() == 0) {
			return new ArrayList<>(0);
		}
		String[] parts = trimmed.split("[ \t\n]+");
		final List<Id<Link>> linkIdsList = new ArrayList<>(parts.length);

		for (String id : parts) {
			linkIdsList.add(Id.create(id, Link.class));
		}
		return linkIdsList;
	}

	public static List<Link> getLinks(final Network network, final List<Id<Link>> linkIds) {
		List<Link> links = new ArrayList<>();
		for (Id<Link> linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			if (link == null) {
				throw new IllegalArgumentException("no link with id " + linkId);
			}
			links.add(link);
		}
		return links;
	}

	public static List<Id<Link>> getLinkIds(final List<Link> links) {
		List<Id<Link>> linkIds = new ArrayList<>();
		if (links != null) {
			for (Link link : links) {
				linkIds.add(link.getId());
			}
		}
		return linkIds;
	}

	/**
	 * @return formerly, the maximum of 1 and the mathematically rounded number of lanes
	 * attribute's value at time "time" of the link given as parameter
	 *	now, the number is truncated, but 0 is never returned.
	 *	math.round is way, way too slow.
	 */
	public static int getNumberOfLanesAsInt(final double time, final Link link) {
		int numberOfLanes = (int) link.getNumberOfLanes(time);
		return Math.max(1, numberOfLanes);
	}

	public static int getNumberOfLanesAsInt(final Link link) {
		int numberOfLanes = (int) link.getNumberOfLanes();
		return Math.max(1, numberOfLanes);
	}


	public static boolean isMultimodal(final Network network) {
		String mode = null;
		boolean hasEmptyModes = false;
		for (Link link : network.getLinks().values()) {
			Set<String> modes = link.getAllowedModes();
			if (modes.size() > 1) {
				return true; // it must be multimodal with more than 1 mode
			} else if (modes.size() == 1) {
				String m2 = modes.iterator().next();
				if (mode == null) {
					if (hasEmptyModes) {
						// i.e. we have a mode restriction on the current link but not mode restriction on some other link. => multi-modal
						return true;
					}
					mode = m2;
					// (memorize that we have seen a link with a mode restriction)
				} else {
					if (!m2.equals(mode)) {
						// i.e. we have a mode restriction on the current link, and some other mode restriction on some other link. => multi-modal
						return true;
					}
					// position here can be reached with "mode!=null" and "hasEmptyModes==true".  Should return "true" here but does not.
					// Works anyways, since (it seems to me) that that state (mode!=null, hasEmptyModes==true) can never be reached.
					// ???  kai, feb'15
				}
			} else {
				if (mode != null) {
					// i.e. we have no mode restriction on the current link, but a mode restriction on some other link. => multi-modal:
					return true;
				}
				hasEmptyModes = true;
				// (memorize that we have seen a link without mode restrictions)
			}
		}
		return false;

	}

	public static Link getConnectingLink(final Node fromNode, final Node toNode) {
		for (Link link : fromNode.getOutLinks().values()) {
			if (link.getToNode() == toNode) {
				return link;
			}
		}
		return null;
	}

    /**
	 * This method expects the nearest link to a given measure point.
	 * It calculates the euclidean distance for both nodes of the link,
	 * "fromNode" and "toNode" and returns the node with shorter distance
	 */
	public static Node getCloserNodeOnLink(Coord coord, Link link) {
		// yyyy I don't think there is a test for this anywhere.  kai, mar'14

		Node toNode = link.getToNode();
		Node fromNode= link.getFromNode();

		double distanceToNode = getEuclideanDistance(coord, toNode.getCoord());
		double distanceFromNode= getEuclideanDistance(coord, fromNode.getCoord());

		if(distanceToNode < distanceFromNode)
			return toNode;
		return fromNode;
	}

	/**
	 * returns the euclidean distance between two coordinates
	 *
	 */
	public static double getEuclideanDistance(Coord origin, Coord destination){
		return CoordUtils.calcEuclideanDistance(origin, destination);
	}

	/**
	 * returns the euclidean distance between two points (x1,y1) and (x2,y2)
	 */
	public static double getEuclideanDistance(double x1, double y1, double x2, double y2){
		return getEuclideanDistance(new Coord(x1,y1), new Coord(x2, y2));
	}

    /**
     * Finds the (approx.) nearest link to a given point on the map,
     * such that the point lies on the right side of the directed link,
     * if such a link exists.
	 *
     * It searches first for the nearest node, and then for the nearest link
     * originating or ending at that node and fulfilling the above constraint.
     * <p>
     * <b>Special cases:</b> {@code nodes:o ; links:<-- ; coord:x}
     * <i>No right entry link exists</i>
     * <pre>
	 * {@code
     * o<-1--o returning
     * | . . ^ nearest left
     * |2 . 4| entry link
     * v .x. | (link.id=3)
     * o--3->o<br>
	 * }
     * </pre>
     * <i>No right entry link exists but more than one nearest left entry link exist</i>
	 * <pre>
	 * {@code
	 * o<-1--o returning
     * | . . ^ nearest left
     * |2 x 4| entry link with the
     * v . . | lowest link id
     * o--3->o (link.id=1)
	 * }
	 * </pre>
     * <i>More than one nearest right entry link exist</i>
	 * <pre>
	 * {@code
     * o--1->o returning
     * ^ . . | nearest right
     * |2 x 4| entry link with the
     * | . . v lowest link id
     * o<-3--o (link.id=1)
	 *
     * o<----7&8--x->o (link.id=7)
	 * }
	 * </pre>
     *
     * @param coord
     *          the coordinate for which the closest link should be found
     * @return the link found closest to <code>coord</code> and oriented such that the
     * point lies on the right of the link.
     */
    // TODO [balmermi] there should be only one 'getNearestLink' method
    // which returns either the nearest 'left' or 'right' entry link, based on a global
    // config param.
    public static Link getNearestRightEntryLink(Network network, final Coord coord) {
        Link nearestRightLink = null;
        Link nearestOverallLink = null;
        Node nearestNode = NetworkUtils.getNearestNode((network),coord);

        double[] coordVector = new double[2];
        coordVector[0] = nearestNode.getCoord().getX() - coord.getX();
        coordVector[1] = nearestNode.getCoord().getY() - coord.getY();

        // now find nearest link from the nearest node
        double shortestRightDistance = Double.MAX_VALUE; // reset the value
        double shortestOverallDistance = Double.MAX_VALUE; // reset the value
        List<Link> incidentLinks = new ArrayList<>(nearestNode.getInLinks().values());
        incidentLinks.addAll(nearestNode.getOutLinks().values());
        for (Link link : incidentLinks) {
		double dist = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
            if (dist <= shortestRightDistance) {
                // Generate a vector representing the link
                double[] linkVector = new double[2];
                linkVector[0] = link.getToNode().getCoord().getX()
                        - link.getFromNode().getCoord().getX();
                linkVector[1] = link.getToNode().getCoord().getY()
                        - link.getFromNode().getCoord().getY();

                // Calculate the z component of cross product of coordVector and the link
                double crossProductZ = coordVector[0]*linkVector[1] - coordVector[1]*linkVector[0];
                // If coord lies to the right of the directed link, i.e. if the z component
                // of the cross product is negative, set it as new nearest link
                if (crossProductZ < 0) {
                    if (dist < shortestRightDistance) {
                        shortestRightDistance = dist;
                        nearestRightLink = link;
                    }
                    else { // dist == shortestRightDistance
                        if (link.getId().compareTo(nearestRightLink.getId()) < 0) {
                            shortestRightDistance = dist;
                            nearestRightLink = link;
                        }
                    }
                }
            }
            if (dist < shortestOverallDistance) {
                shortestOverallDistance = dist;
                nearestOverallLink = link;
            }
            else if (dist == shortestOverallDistance) {
                if (link.getId().compareTo(nearestOverallLink.getId()) < 0) {
                    shortestOverallDistance = dist;
                    nearestOverallLink = link;
                }
            }
        }

        // Return the nearest overall link if there is no nearest link
        // such that the given coord is on the right side of it
        if (nearestRightLink == null) {
            return nearestOverallLink;
        }
		return nearestRightLink;
	}

	/**
	 * Finds the (approx.) nearest link to a given point on the map.
	 * It searches first for the nearest node, and then for the nearest link
	 * originating or ending at that node.
	 *
	 * @param coord
	 *          the coordinate for which the closest link should be found
	 * @return the link found closest to coord
	 *
	 * @see NetworkUtils#getNearestLinkExactly(Network, Coord)
     */
    public static Link getNearestLink(Network network, final Coord coord) {
        Link nearestLink = null;
        Node nearestNode = NetworkUtils.getNearestNode((network),coord);
        if ( nearestNode == null ) {
            log.warn("nearestNode not found. Will probably crash eventually.  Maybe network for requested mode does not exist (i.e. links not annotated accordingly)?  Maybe run NetworkCleaner?  " +
							 network) ;
            return null ;
        }

        if ( nearestNode.getInLinks().isEmpty() && nearestNode.getOutLinks().isEmpty() ) {
            log.warn(network + "[found nearest node that has no incident links.  Will probably crash eventually ...  Maybe run NetworkCleaner?][node = " + nearestNode.getId() + "]" ) ;
        }

        // now find nearest link from the nearest node
        // [balmermi] it checks now ALL incident links, not only the outgoing ones.
        // TODO [balmermi] Now it finds the first of the typically two nearest links (same nodes, other direction)
        // It would be nicer to find the nearest link on the "right" side of the coordinate.
        // (For Great Britain it would be the "left" side. Could be a global config param...)
        double shortestDistance = Double.MAX_VALUE;
        for (Link link : getIncidentLinks(nearestNode).values()) {
		double dist = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
            if (dist < shortestDistance) {
                shortestDistance = dist;
                nearestLink = link;
            }
        }
        if ( nearestLink == null ) {
            log.warn(network + "[nearestLink not found.  Will probably crash eventually ...  Maybe run NetworkCleaner?]" ) ;
        }
        return nearestLink;
    }

	/**
	 * Calculates the most 'left' outLink for a given inLink (oriented from north to south).
	 * That's the link a driver would refer to when turning left (no u-turn),
	 * even if there is only one link going to the right.
	 *
	 * @param inLink The inLink given
	 * @return outLink, or null if there is only one outLink back to the inLink's fromNode.
	 */
	public static Link getLeftmostTurnExcludingU(Link inLink){

		TreeMap<Double, Link> result = getOutLinksSortedClockwiseByAngle(inLink);

		if (result.size() == 0){
			return null;
		}
		return result.get(result.firstKey());
	}

	/**
	 * Calculates the orientation of outgoing links for a given
	 * incoming link beginning from the right if the inLink goes
	 * north to south. The most 'left' outLink comes last. The link back to the
	 * inLink's upstream Node is ignored.
	 * <br><br>
	 * Comments/questions:<ul>
	 * <li> What does the "north to south" part mean?  Can't we just sort outLinks right to left no matter where we come from?  kai, aug'16
	 * <li> In fact, a test (NetworkUtilsTest.getOutLinksSortedByAngleTest()) does not confirm the javadoc.  The "north to south"
	 * is irrelevant.  But instead, it sorts links <i> left to right </i> instead of right to left.
	 * <br>
	 * </ul>
	 * If someone can confirm this, please comment out all these remarks. kai, aug'16
	 * <br><br>
	 * @param inLink The inLink given
	 * @return Collection of outLinks, or an empty collection if there are only
	 *  outLinks back to the inLink's fromNode.
	 */
	public static TreeMap<Double, Link> getOutLinksSortedClockwiseByAngle(Link inLink){
		Coord coordInLink = getVector(inLink);
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		TreeMap<Double, Link> outLinksByOrientation = new TreeMap<>();

		for (Link outLink : inLink.getToNode().getOutLinks().values()) {
			if (!(outLink.getToNode().equals(inLink.getFromNode()))) {
				Coord coordOutLink = getVector(outLink);
				double thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
				double thetaDiff = thetaOutLink - thetaInLink;
				if (thetaDiff < -Math.PI) {
					thetaDiff += 2 * Math.PI;
				} else if (thetaDiff > Math.PI) {
					thetaDiff -= 2 * Math.PI;
				}
				outLinksByOrientation.put(-thetaDiff, outLink);
			}
		}
		return outLinksByOrientation;
	}

	private static Coord getVector(Link link){
		double x = link.getToNode().getCoord().getX() - link.getFromNode().getCoord().getX();
		double y = link.getToNode().getCoord().getY() - link.getFromNode().getCoord().getY();
		return new Coord(x, y);
	}


	public static Map<Id<Node>, ? extends Node> getOutNodes(Node node) {
		Map<Id<Node>, Node> nodes = new TreeMap<>();
		for (Link link : node.getOutLinks().values()) {
			Node outNode = link.getToNode();
			nodes.put(outNode.getId(), outNode);
		}
		return nodes;
	}


	public static Map<Id<Link>, ? extends Link> getIncidentLinks(Node node) {
		Map<Id<Link>, Link> links = new TreeMap<>(node.getInLinks());
		links.putAll(node.getOutLinks());
		return links;
	}


	public static Map<Id<Node>, ? extends Node> getInNodes(Node node) {
		Map<Id<Node>, Node> nodes = new TreeMap<>();
		for (Link link : node.getInLinks().values()) {
			Node inNode = link.getFromNode();
			nodes.put(inNode.getId(), inNode);
		}
		return nodes;
	}


	public static Map<Id<Node>, ? extends Node> getIncidentNodes(Node node) {
		Map<Id<Node>, Node> nodes = new TreeMap<>(getInNodes(node));
		nodes.putAll(getOutNodes(node));
		return nodes;
	}


	public static Node createNode(Id<Node> id) {
		return new NodeImpl(id);
	}


	public static Node createNode(Id<Node> id, Coord coord, String type) {
		return new NodeImpl(id, coord, type);
	}


	public static Node createNode(Id<Node> id, Coord coord) {
		return new NodeImpl(id, coord);
	}


	@Deprecated // use link.getAttributes()... directly.  kai, dec'16
	public static void setOrigId( final Node node, final String id ) {
		if ( node instanceof NodeImpl ) {
			((NodeImpl)node).setOrigId( id ) ;
		} else {
			throw new RuntimeException("wrong implementation of interface Node to do this") ;
		}
	}


	@Deprecated // use link.getAttributes()... directly.  kai, dec'16
	public static void setType(Node node, final String type) {
		if ( node instanceof NodeImpl ) {
			((NodeImpl)node).setType( type ) ;
		} else {
			throw new RuntimeException("wrong implementation of interface Node to do this") ;
		}
	}

	@Deprecated // use link.getAttributes()... directly.  kai, dec'16
	public static String getOrigId( Node node ) {
		if ( node instanceof NodeImpl ) {
			return ((NodeImpl) node).getOrigId() ;
		} else {
			throw new RuntimeException("wrong implementation of interface Node to do this") ;
		}
	}


	@Deprecated // use link.getAttributes()... directly.  kai, dec'16
	public static String getType( Node node ) {
		if ( node instanceof NodeImpl ) {
			return ((NodeImpl) node).getType() ;
		} else {
			throw new RuntimeException("wrong implementation of interface Node to do this") ;
		}
	}


	/**
	 * @deprecated -- I don't know why this method exists; it makes reading code harder rather than easier.  Maybe there used to be something more
	 * complicated which eventually got refactored into the current version?  kai, feb'20
	 */
	public static double getFreespeedTravelTime( Link link ) {
		return link.getLength() / link.getFreespeed() ;
	}
	/**
	 * @deprecated -- I don't know why this method exists; it makes reading code harder rather than easier.  Maybe there used to be something more
	 * complicated which eventually got refactored into the current version?  kai, feb'20
	 */
	public static double getFreespeedTravelTime( Link link, double time ) {
		return link.getLength() / link.getFreespeed(time) ;
	}

	public static final String ALLOWED_SPEED = "allowed_speed";
	public static final String TYPE="type" ;
	public static void setType( Link link , String type ) {
//		if ( link instanceof LinkImpl ) {
//			((LinkImpl)link).setType2( type ) ;
//		} else {
//			throw new RuntimeException("wrong implementation of interface Link to do this") ;
//		}
		if ( type != null ) {
			link.getAttributes().putAttribute(TYPE, type) ;
		}
	}
	public static String getType(Link link) {
//		if ( link instanceof LinkImpl ) {
//			return ((LinkImpl)link).getType2() ;
//		} else {
//			throw new RuntimeException( "getType not possible for this implementation of interface Link" ) ;
//		}
		return (String) link.getAttributes().getAttribute(TYPE);
	}

	/**
	 * Returns the road type of a highway link. In OSM highway links contain links for car traffic.
	 * If not set this method will return "unclassified".
	 */
	public static String getHighwayType(Link link) {

		String type = (String) link.getAttributes().getAttribute(TYPE);

		if (type != null)
			type = type.replaceFirst("^highway\\.", "");

		if (type == null || type.isBlank())
			type = "unclassified";

		return type;
	}

	/**
	 * Return the allowed speed attribute if set, otherwise freeflow speed.
	 */
	public static double getAllowedSpeed(Link link) {

		Object speed = link.getAttributes().getAttribute(ALLOWED_SPEED);
		if (speed == null)
			return link.getFreespeed();

		if (speed instanceof Double s)
			return s;
		return Double.parseDouble(speed.toString());
	}

	public static String getOrigId( Link link ) {
//		if ( link instanceof LinkImpl ) {
//			return ((LinkImpl)link).getOrigId2() ;
//		} else {
//			throw new RuntimeException("wrong implementation of Link interface do getOrigId" ) ;
//		}
		Object o = link.getAttributes().getAttribute(ORIGID);
		return o == null ? null : o.toString();
	}

	public static void setOrigId( Link link, String id ) {
//		if ( link instanceof LinkImpl ) {
//			((LinkImpl) link).setOrigId2(id);
//		} else {
//			throw new RuntimeException("wrong implementation of interface Link to do setOrigId") ;
//		}
		if ( id != null ) {
			link.getAttributes().putAttribute(ORIGID, id) ;
		}
	}


	public static Link createLink(Id<Link> id, Node from, Node to, Network network, double length, double freespeed,
			double capacity, double lanes) {
		return new LinkImpl(id, from, to, network, length, freespeed, capacity, lanes);
	}

	public static Link createAndAddLink(Network network, final Id<Link> id, final Node fromNode, final Node toNode, final double length, final double freespeed,
			final double capacity, final double numLanes) {
		return createAndAddLink(network, id, fromNode, toNode, length, freespeed, capacity, numLanes, null, null ) ;
	}

	public static Link createAndAddLink(Network network, final Id<Link> id, final Node fromNode, final Node toNode, final double length, final double freespeed,
				final double capacity, final double numLanes, final String origId, final String type) {
		if (network.getNodes().get(fromNode.getId()) == null) {
			throw new IllegalArgumentException(network+"[from="+fromNode+" does not exist]");
		}

		if (network.getNodes().get(toNode.getId()) == null) {
			throw new IllegalArgumentException(network+"[to="+toNode+" does not exist]");
		}

		Link link = network.getFactory().createLink(id, fromNode, toNode) ;
		link.setLength(length);
		link.setFreespeed(freespeed);
		link.setCapacity(capacity);
		link.setNumberOfLanes(numLanes);
		setType( link, type);
		setOrigId( link, origId ) ;

		network.addLink( link ) ;

		return link;
	}


	public static void setNetworkChangeEvents(Network network, List<NetworkChangeEvent> events) {
		if ( network instanceof TimeDependentNetwork ) {
			((TimeDependentNetwork)network).setNetworkChangeEvents(events);
		} else {
			throw new RuntimeException( Gbl.WRONG_IMPLEMENTATION + "Network, TimeDependentNetwork" ) ;
		}
	}


	public static Node createAndAddNode(Network network, final Id<Node> id, final Coord coord) {
		if (network.getNodes().containsKey(id)) {
			throw new IllegalArgumentException(network + "[id=" + id + " already exists]");
		}
		Node n = network.getFactory().createNode(id, coord);
		network.addNode(n) ;
		return n;
	}


	public static void addNetworkChangeEvent( Network network, NetworkChangeEvent event ) {
		if ( network instanceof TimeDependentNetwork ) {
			((TimeDependentNetwork) network).addNetworkChangeEvent(event);
		} else {
			throw new RuntimeException( Gbl.WRONG_IMPLEMENTATION + " Network, TimeDependentNetwork " ) ;
		}
	}


	public static Queue<NetworkChangeEvent> getNetworkChangeEvents(Network network ) {
		if ( network instanceof TimeDependentNetwork ) {
			return ((TimeDependentNetwork) network).getNetworkChangeEvents() ;
		} else {
			throw new RuntimeException( Gbl.WRONG_IMPLEMENTATION + " Network, TimeDependentNetwork " ) ;
		}
	}


	public static Link getNearestLinkExactly(Network network, Coord coord) {
		if ( network instanceof SearchableNetwork ) {
			return ((SearchableNetwork) network).getNearestLinkExactly(coord) ;
		} else {
			throw new RuntimeException( Gbl.WRONG_IMPLEMENTATION + " Network, SearchableNetwork " ) ;
		}
	}


	public static Node getNearestNode(Network network, final Coord coord) {
		if ( network instanceof SearchableNetwork ) {
			return ((SearchableNetwork)network).getNearestNode(coord);
		} else {
			throw new RuntimeException( Gbl.WRONG_IMPLEMENTATION + " Network, SearchableNetwork " ) ;
		}
	}


	public static Collection<Node> getNearestNodes(Network network, final Coord coord, final double distance) {
		if ( network instanceof SearchableNetwork ) {
			return ((SearchableNetwork)network).getNearestNodes(coord, distance);
		} else {
			throw new RuntimeException( Gbl.WRONG_IMPLEMENTATION + " Network, SearchableNetwork" ) ;
		}
	}

	public static final String ORIGID = "origid";

	public static void runNetworkCleaner( Network network ) {
		new org.matsim.core.network.algorithms.NetworkCleaner().run( network );
	}
	public static void runNetworkSimplifier( Network network ) {
		new NetworkSimplifier().run(network) ;
	}
	public static void writeNetwork(Network network, String string) {
		new NetworkWriter(network).write(string) ;
	}

	public static Link findLinkInOppositeDirection(Link link) {
		for (Link candidateLink : link.getToNode().getOutLinks().values()) {
			if (candidateLink.getToNode().equals(link.getFromNode())) {
				return candidateLink;
			}
		}
		return null;
	}

	private static final String ACCESSTIMELINKATTRIBUTEPREFIX = "accesstime_";
	private static final String EGRESSTIMELINKATTRIBUTEPREFIX = "egresstime_";

	public static OptionalTime getLinkAccessTime(Link link, String routingMode){
		String attribute = ACCESSTIMELINKATTRIBUTEPREFIX+routingMode;
		Object o = link.getAttributes().getAttribute(attribute);
		if (o!=null){
			return OptionalTime.defined((double) o);
		}
		else return OptionalTime.undefined();
	}

	public static void setLinkAccessTime(Link link, String routingMode, double accessTime){
		String attribute = ACCESSTIMELINKATTRIBUTEPREFIX+routingMode;
		link.getAttributes().putAttribute(attribute,accessTime);
	}

	public static OptionalTime getLinkEgressTime(Link link, String routingMode) {
		String attribute = EGRESSTIMELINKATTRIBUTEPREFIX + routingMode;
		Object o = link.getAttributes().getAttribute(attribute);
		if (o != null) {
			return OptionalTime.defined((double) o);
		} else return OptionalTime.undefined();
	}

	public static void setLinkEgressTime(Link link, String routingMode, double egressTime) {
		String attribute = EGRESSTIMELINKATTRIBUTEPREFIX + routingMode;
		link.getAttributes().putAttribute(attribute, egressTime);
	}

	public static Network readNetwork(String filename) {
		return readNetwork(filename, ConfigUtils.createConfig());
	}

	public static Network readNetwork(String filename, Config config) {
		return readNetwork(filename, config.network());
	}

	public static Network readNetwork(String filename, NetworkConfigGroup networkConfigGroup) {
		Network network = createNetwork(networkConfigGroup);
		new MatsimNetworkReader(network).readFile(filename);
		return network;
	}

	/**
	 * reads network form file and applies a coordinate transformation.
	 * @param filename network file name
	 * @param transformation coordinate transformation as from @{{@link org.matsim.core.utils.geometry.transformations.TransformationFactory#getCoordinateTransformation(String, String)}}
	 * @return network from file transformed onto target CRS
	 */
	public static Network readNetwork(String filename, NetworkConfigGroup networkConfigGroup, CoordinateTransformation transformation) {
		var network = readNetwork(filename, networkConfigGroup);
		network.getNodes().values().parallelStream()
				.forEach(node -> {
					var transformedCoord = transformation.transform(node.getCoord());
					node.setCoord(transformedCoord);
				});
		return network;
	}

	public static void readNetwork(Network network, String string) {
		new MatsimNetworkReader(network).readFile(string);
	}

	/**
	 * Check whether networks are (technically) identical. This only considers
	 * {@link DisallowedNextLinks} and no other link/node attributes.
	 * 
	 * @param expected
	 * @param actual
	 * @return true if the network's links and nodes are the same incl.
	 *         DisallowedNextLinks
	 */
	public static boolean compare(Network expected, Network actual) {

		// check that all element from expected result are in tested network
		for (Link link : expected.getLinks().values()) {
			Link testLink = actual.getLinks().get(link.getId());
			if (testLink == null) return false;
			if (!testLinksAreEqual(link, testLink)) return false;
		}

		for (Node node : expected.getNodes().values()) {
			Node testNode = actual.getNodes().get(node.getId());
			if (testNode == null) return false;
			if (!testNodesAreEqual(node, testNode)) return false;
		}

		// also check the other way around, to make sure there are no extra elements in the network
		for (Link link : actual.getLinks().values()) {
			Link expectedLink = expected.getLinks().get(link.getId());
			if (expectedLink == null) return false;
		}

		for (Node node : actual.getNodes().values()) {
			Node expectedNode = expected.getNodes().get(node.getId());
			if (expectedNode == null) return false;
		}
		return true;
	}

	public static NetworkCollector getCollector() {
		NetworkConfigGroup networkConfigGroup = new NetworkConfigGroup();
		networkConfigGroup.setTimeVariantNetwork(false);
		return getCollector(networkConfigGroup);
	}

	public static NetworkCollector getCollector(Config config) {
		return getCollector(config.network());
	}

	public static NetworkCollector getCollector(NetworkConfigGroup networkConfigGroup) {
		return new NetworkCollector(networkConfigGroup);
	}

	private static boolean testLinksAreEqual(Link expected, Link actual) {

		DisallowedNextLinks actualDnl = getDisallowedNextLinks(actual);
		DisallowedNextLinks expectedDnl = getDisallowedNextLinks(expected);

		return actual.getAllowedModes().containsAll(expected.getAllowedModes())
				&& expected.getCapacity() == actual.getCapacity()
				&& expected.getCapacityPeriod() == actual.getCapacityPeriod()
				&& expected.getFreespeed() == actual.getFreespeed()
				&& expected.getLength() == actual.getLength()
				&& expected.getNumberOfLanes() == actual.getNumberOfLanes()
				&& expectedDnl == null ? actualDnl == null : expectedDnl.equals(actualDnl);
	}

	private static boolean testNodesAreEqual(Node expected, Node actual) {
		return expected.getCoord().equals(actual.getCoord());
	}

	/**
	 * Returns the closest point to on a link from a Point (either its orthogonal projection or the link's to and from node)
	 * @param coord  Coord to check from
	 * @param link the link
	 * @return the closest Point as Coord
	 */
	public static Coord findNearestPointOnLink(Coord coord, Link link) {
		return CoordUtils.orthogonalProjectionOnLineSegment(link.getFromNode().getCoord(),link.getToNode().getCoord(),coord);
	}

	public static final String ORIG_GEOM = "origgeom";
	public static List<Node> getOriginalGeometry(Link link) {

		// use a list since order is important
		List<Node> result = new ArrayList<>();
		result.add(link.getFromNode());
		var attr = (String)link.getAttributes().getAttribute(ORIG_GEOM);

		if (!StringUtils.isBlank(attr)) {
			var data = attr.split(" ");
			for (String date : data) {
				var values = date.split(",");
				if (values.length != 3) throw new RuntimeException("expected three values per node but found: " + date);
				var coord = new Coord(Double.parseDouble(values[1]), Double.parseDouble(values[2]));
				var node = new NodeImpl(Id.createNodeId(values[0]), coord);
				result.add(node);
			}
		}

		result.add(link.getToNode());
		return result;
	}

	private static final String DISALLOWED_NEXT_LINKS_ATTRIBUTE = "disallowedNextLinks";

	@Nullable
	public static DisallowedNextLinks getDisallowedNextLinks(Link link) {
		return (DisallowedNextLinks) link.getAttributes().getAttribute(DISALLOWED_NEXT_LINKS_ATTRIBUTE);
	}

	public static DisallowedNextLinks getOrCreateDisallowedNextLinks(Link link) {
		DisallowedNextLinks disallowedNextLinks = getDisallowedNextLinks(link);
		if (disallowedNextLinks == null) {
			disallowedNextLinks = new DisallowedNextLinks();
			setDisallowedNextLinks(link, disallowedNextLinks);
		}
		return disallowedNextLinks;
	}

	public static void setDisallowedNextLinks(Link link, DisallowedNextLinks disallowedNextLinks) {
		link.getAttributes().putAttribute(DISALLOWED_NEXT_LINKS_ATTRIBUTE, disallowedNextLinks);
	}

	public static boolean addDisallowedNextLinks(Link link, String mode, List<Id<Link>> linkIds) {
		DisallowedNextLinks disallowedNextLinks = getOrCreateDisallowedNextLinks(link);
		return disallowedNextLinks.addDisallowedLinkSequence(mode, linkIds);
	}

	public static void removeDisallowedNextLinks(Link link) {
		link.getAttributes().removeAttribute(DISALLOWED_NEXT_LINKS_ATTRIBUTE);
	}

	public static void copyAttributesExceptDisallowedNextLinks(Link from, Link to) {
		AttributesUtils.copyAttributesFromToExcept(from, to, DISALLOWED_NEXT_LINKS_ATTRIBUTE);
	}

	public static void addAllowedMode(Link link, String mode) {
		Set<String> modes = new HashSet<>(link.getAllowedModes());
		modes.add(mode);
		link.setAllowedModes(modes);
	}

	public static void removeAllowedMode(Link link, String mode) {
		Set<String> modes = new HashSet<>(link.getAllowedModes());
		modes.remove(mode);
		link.setAllowedModes(modes);
	}

	/**
	 * Removes the given modes from the links and runs the network cleaner afterwards. Thus, some more links may be restricted to keep the network consistent.
	 * That means, each link can be reached from each other link.
	 * @param network the network
	 * @param modesToRemoveByLinkId map of modes that should be removed from the links
	 */
	public static void restrictModesAndCleanNetwork(Network network, Function<Id<Link>, Set<String>> modesToRemoveByLinkId) {
		new NetworkModeRestriction(modesToRemoveByLinkId).run(network);
	}
}
