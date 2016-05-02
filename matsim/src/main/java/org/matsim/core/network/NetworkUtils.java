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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.*;

/**
 * Contains several helper methods for working with {@link Network networks}.
 *
 * @author mrieser
 */
public class NetworkUtils {

    private static Logger log = Logger.getLogger(NetworkUtils.class);

    public static Network createNetwork() {
        return new NetworkImpl() ;
    }


    public static Network createNetwork(Config config) {
        return createNetwork(config.network());
    }


    public static Network createNetwork(NetworkConfigGroup networkConfigGroup) {
        NetworkImpl network = new NetworkImpl();
        
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

	/**
	 * @param list of links
	 * @return list of link IDs
	 */
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

	public static Map<Id<Link>, Link> getIncidentLinks(final Node n) {
		Map<Id<Link>, Link> links = new TreeMap<>(n.getInLinks());
		links.putAll(n.getOutLinks());
		return links;
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
     * if such a link exists.<br />
     * It searches first for the nearest node, and then for the nearest link
     * originating or ending at that node and fulfilling the above constraint.
     * <p>
     * <b>Special cases:</b> <tt>nodes:o ; links:<-- ; coord:x</tt><br/>
     * <i>No right entry link exists</i><br/>
     * <tt>
     * o<-1--o returning<br/>
     * | . . ^ nearest left<br/>
     * |2 . 4| entry link<br/>
     * v .x. | (link.id=3)<br/>
     * o--3->o<br/>
     * </tt>
     * <br/>
     * <i>No right entry link exists but more than one nearest left entry link exist</i><br/>
     * <tt>
     * o<-1--o returning<br/>
     * | . . ^ nearest left<br/>
     * |2 x 4| entry link with the<br/>
     * v . . | lowest link id<br/>
     * o--3->o (link.id=1)<br/>
     * </tt>
     * <br/>
     * <i>More than one nearest right entry link exist</i><br/>
     * <tt>
     * o--1->o returning<br/>
     * ^ . . | nearest right<br/>
     * |2 x 4| entry link with the<br/>
     * | . . v lowest link id<br/>
     * o<-3--o (link.id=1)<br/>
     * <br/>
     * o<----7&8--x->o (link.id=7)<br/>
     * </tt>
     * </p>
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
        if (!(network instanceof NetworkImpl)) {
            throw new IllegalArgumentException("Only NetworkImpl can be queried like this.");
        }
        Link nearestRightLink = null;
        Link nearestOverallLink = null;
        Node nearestNode = ((NetworkImpl) network).getNearestNode(coord);

        double[] coordVector = new double[2];
        coordVector[0] = nearestNode.getCoord().getX() - coord.getX();
        coordVector[1] = nearestNode.getCoord().getY() - coord.getY();

        // now find nearest link from the nearest node
        double shortestRightDistance = Double.MAX_VALUE; // reset the value
        double shortestOverallDistance = Double.MAX_VALUE; // reset the value
        List<Link> incidentLinks = new ArrayList<>(nearestNode.getInLinks().values());
        incidentLinks.addAll(nearestNode.getOutLinks().values());
        for (Link link : incidentLinks) {
            double dist = ((LinkImpl) link).calcDistance(coord);
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
     * Finds the (approx.) nearest link to a given point on the map.<br />
     * It searches first for the nearest node, and then for the nearest link
     * originating or ending at that node.
     *
     * @param coord
     *          the coordinate for which the closest link should be found
     * @return the link found closest to coord
     */
    public static Link getNearestLink(Network network, final Coord coord) {
        if (!(network instanceof NetworkImpl)) {
            throw new IllegalArgumentException("Only NetworkImpl can be queried like this.");
        }
        Link nearestLink = null;
        Node nearestNode = ((NetworkImpl) network).getNearestNode(coord);
        if ( nearestNode == null ) {
            log.warn("[nearestNode not found.  Will probably crash eventually ...  Maybe run NetworkCleaner?]" + network) ;
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
            double dist = ((LinkImpl) link).calcDistance(coord);
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
	 * @return outLink, or null if there is only one outLink back to the inLinks fromNode.
	 */
	public static Link getLeftLane(Link inLink){

		TreeMap<Double, Link> result = getOutLinksSortedByAngle(inLink);

		if (result.size() == 0){
			return null;
		}
		return result.get(result.firstKey());
	}

	/**
	 * Calculates the orientation of downstream links (MATSim slang is 'outLinks') for a given 
	 * upstream link (slang inLink)beginning from the right if the inLink goes 
	 * north to south. The most 'left' outLink comes last. The link back to the 
	 * inLinks upstream Node (slang fromNode) is ignored. 
	 *
	 * @param inLink The inLink given
	 * @return Collection of outLinks, or an empty collection, if there is only
	 * one outLink back to the inLinks fromNode.
	 */
	public static TreeMap<Double, Link> getOutLinksSortedByAngle(Link inLink){
		Coord coordInLink = getVector(inLink);
		double thetaInLink = Math.atan2(coordInLink.getY(), coordInLink.getX());
		TreeMap<Double, Link> outLinksByOrientation = new TreeMap<Double, Link>();

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
}
