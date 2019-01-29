package org.matsim.contrib.accessibility.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public final class NetworkUtil {
	private NetworkUtil(){} // do not instantiate

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(NetworkUtil.class);
	
	/**
	 * This method calculates the distance between a point and a node via a link (which should contains
	 * the node) as the sum of
	 * <ul>
	 * <li> the orthogonal distance between the point and the link and
	 * <li> the distance between the intersection point of the orthogonal
	 * projection (from the point to the link) to the node.
	 * </ul>
	 * If the orthogonal projection of the point to the line does not intersects the link, the method
	 * returns the Euclidean distance between the point and the node.
	 */
	public static Distances getDistances2NodeViaGivenLink(Coord coord, Link link, Node destinationNode){
		Coord intersection = CoordUtils.orthogonalProjectionOnLineSegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
		
		Distances distances = new Distances();
		
		double distanceCoord2Intersection = NetworkUtils.getEuclideanDistance(coord, intersection);
		double distanceIntersection2Node = NetworkUtils.getEuclideanDistance(intersection, destinationNode.getCoord());

		distances.setDistanceCoord2Intersection(distanceCoord2Intersection);
		distances.setDistanceIntersetion2Node(distanceIntersection2Node);
		return distances;
	}
}