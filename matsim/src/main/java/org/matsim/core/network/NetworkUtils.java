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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Contains several helper methods for working with {@link Network networks}.
 *
 * @author mrieser
 */
public final class NetworkUtils {

    private static Logger log = Logger.getLogger(NetworkUtils.class);

    public static Network createNetwork(Config config) {
        return createNetwork(config.network());
    }


    public static Network createNetwork(NetworkConfigGroup networkConfigGroup) {
        Network network = new NetworkImpl();
        
        if (networkConfigGroup.isTimeVariantNetwork()) {
            network.getFactory().setLinkFactory(new VariableIntervalTimeVariantLinkFactory());
        }
        
        return network;
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
		Node[] nodes = network.getNodes().values().toArray(new Node[network.getNodes().size()]);
		Arrays.sort(nodes, new Comparator<Node>() {
			@Override
			public int compare(Node o1, Node o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
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
		Link[] links = network.getLinks().values().toArray(new Link[network.getLinks().size()]);
		Arrays.sort(links, new Comparator<Link>() {
			@Override
			public int compare(Link o1, Link o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});
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
		if (numberOfLanes == 0) {
			return 1;
		} else {
			return numberOfLanes;
		}
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
			} else if (modes.size() == 0) {
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
     * @see {@link NetworkUtils#getNearestLinkExactly(Network, Coord)}
     */
    public static Link getNearestLink(Network network, final Coord coord) {
        Link nearestLink = null;
        Node nearestNode = NetworkUtils.getNearestNode((network),coord);
        if ( nearestNode == null ) {
            log.warn("[nearestNode not found.  Will probably crash eventually. Maybe run NetworkCleaner?  " +
							 "Also may mean that network for mode is not defined.]" + network) ;
            return null ;
        }

        if ( nearestNode.getInLinks().isEmpty() && nearestNode.getOutLinks().isEmpty() ) {
            log.warn(network + "[found nearest node that has no incident links.  Will probably crash eventually ...  Maybe run NetworkCleaner?]" ) ;
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
			if (!(outLink.getToNode().equals(inLink.getFromNode()))){
				Coord coordOutLink = getVector(outLink);
				double thetaOutLink = Math.atan2(coordOutLink.getY(), coordOutLink.getX());
				double thetaDiff = thetaOutLink - thetaInLink;
				if (thetaDiff < -Math.PI){
					thetaDiff += 2 * Math.PI;
				} else if (thetaDiff > Math.PI){
					thetaDiff -= 2 * Math.PI;
				}
				outLinksByOrientation.put(Double.valueOf(-thetaDiff), outLink);
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


	public static double getFreespeedTravelTime( Link link ) {
		return link.getLength() / link.getFreespeed() ;
	}
	public static double getFreespeedTravelTime( Link link, double time ) {
		return link.getLength() / link.getFreespeed(time) ;
	}

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

	public static String getOrigId( Link link ) {
//		if ( link instanceof LinkImpl ) {
//			return ((LinkImpl)link).getOrigId2() ;
//		} else {
//			throw new RuntimeException("wrong implementation of Link interface do getOrigId" ) ;
//		}
		return (String) link.getAttributes().getAttribute(ORIGID);
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


	public static Network createNetwork() {
		return new NetworkImpl();
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

	@Deprecated // use network.getFactory() instead
	public static LinkFactoryImpl createLinkFactory() {
		// yyyyyy Make LinkFactoryImpl invisible outside package.  Does the LinkFactory interface have to be public at all?  kai, aug'16
		// the different factory types need to be visible, or at least configurable, during initialization: User needs to be able to select which factory to
		// insert into NetworkFactory.  kai, may'17
		return new LinkFactoryImpl();
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
		for ( Link candidateLink : link.getToNode().getOutLinks().values() ) {
			if ( candidateLink.getToNode().equals( link.getFromNode() ) ) {
				return candidateLink ;
			}
		}
		return null ;
	}
	public static void readNetwork( Network network, String string ) {
		new MatsimNetworkReader(network).readFile(string);
	}
	public static Network readNetwork( String string ) {
		Network network = ScenarioUtils.createScenario(ConfigUtils.createConfig() ).getNetwork() ;
		new MatsimNetworkReader(network).readFile(string);
		return network ;
	}
}
