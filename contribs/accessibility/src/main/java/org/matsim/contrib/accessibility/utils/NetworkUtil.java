package org.matsim.contrib.accessibility.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public final class NetworkUtil {
	private NetworkUtil(){} // do not instantiate

	private static final Logger log = Logger.getLogger(NetworkUtil.class);
	
	
//	/**
//	 * Just for forwarding to method which uses x- and y-coord individually
//	 * @param coord
//	 * @param link
//	 * @param destinationNode
//	 * @return
//	 */
//	public static Distances getDistances2NodeViaGivenLink(Coord coord, Link link, Node destinationNode){
//		return getDistances2Node(coord.getX(), coord.getY(), link, destinationNode);
//	}
//	
	
	/**
	 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
	 * <ul>
	 * <li> the orthogonal distance between the point and the link and
	 * <li> the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
	 * </ul>
	 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the Euclidean distance between the point and the node.
	 * @param coord
	 * @param link
	 * @param destinationNode
	 * 
	 * @return
	 */
	@Deprecated
	public static Distances getDistances2NodeViaGivenLink(Coord coord, Link link, Node destinationNode){
		
		// line A B
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		// vector ba (r)
		double abx = bx - ax;
		double aby = by - ay;
		
		// vector pa (v)
		double apx = coord.getX() - ax;
		double apy = coord.getY() - ay;
		
		// calculation the vector p (orthogonal projection of point P on line A B)
		// see here: https://de.wikipedia.org/wiki/Orthogonalprojektion#Beispiele
		double numerator = apx * abx + apy * aby;
		double denominator = abx * abx + aby * aby;
		double fraction = numerator/denominator;
		double vectorx = abx * fraction;
		double vectory = aby * fraction;
		double intersectionX = ax + vectorx;
		double intersectionY = ay + vectory;
		Coord intersection = new Coord(intersectionX, intersectionY);
		
		// TEST 
		// is bax or bay == 0?
		double lambdax = vectorx/abx; // vectorx = intersectionx-ax
		double lambday = vectory/aby; // vectory = intersectiony-ay
		double lambdaxInt = Math.rint( lambdax * 1000);
		double lambdayInt = Math.rint( lambday * 1000);
		
		if(abx == 0 && 0 <= lambday && lambday <= 1){ // for vertical links
			// test if lambday is true for x and y
			double testx = ax + (lambday * abx);
			double testy = ay + (lambday * aby);
			if(testx == intersectionX && testy == intersectionY){
				return orthogonalDistancePlusLinkIntersection(coord, destinationNode, intersection);
			}
		}
		else if(aby == 0 && 0 <= lambdax && lambdax <= 1){ // for horizontal links
			// test if lambdax is true for x and y
			double testx = ax + (lambdax * abx);
			double testy = ay + (lambday * aby);
			if(testx == intersectionX && testy == intersectionY){
				return orthogonalDistancePlusLinkIntersection(coord, destinationNode, intersection);
			}
		}
		// hier liegt der Schnittpunkt "intersection auf der geraden (link)
		else if(lambdaxInt == lambdayInt && 0 <= lambdax && lambdax <= 1 ){
			return orthogonalDistancePlusLinkIntersection(coord, destinationNode, intersection);
		}
		
		
		Distances d = new Distances();
		double distance = NetworkUtils.getEuclideanDistance(coord.getX(), coord.getY(), destinationNode.getCoord().getX(), destinationNode.getCoord().getY()) ;
		d.setDistanceCoord2Intersection(distance);

		return d ;
		
	}
	

	public static Coord getInteresectionOfProjection(Coord coord, Link link, Node destinationNode) {
		
		/// line A B
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		// vector ba (r)
		double abx = bx - ax;
		double aby = by - ay;
		
		// vector pa (v)
		double apx = coord.getX() - ax;
		double apy = coord.getY() - ay;
		
		// calculation the vector p (orthogonal projection of point P on line A B)
		// see here: https://de.wikipedia.org/wiki/Orthogonalprojektion#Beispiele
		double numerator = apx * abx + apy * aby;
		double denominator = abx * abx + aby * aby;
		double fraction = numerator/denominator;
		double vectorx = abx * fraction;
		double vectory = aby * fraction;
		double intersectionX = ax + vectorx;
		double intersectionY = ay + vectory;
		Coord intersection = new Coord(intersectionX, intersectionY);
		
		return intersection;
	}
	

	private static Distances orthogonalDistancePlusLinkIntersection(Coord coord, Node destinationNode, Coord intersection) {
		Distances distances = new Distances();
		
		double distanceCoord2Intersection = NetworkUtils.getEuclideanDistance(coord, intersection);
		Coord destinationNodeCoord = destinationNode.getCoord();
		double distanceIntersection2Node = NetworkUtils.getEuclideanDistance(intersection, destinationNodeCoord);

		distances.setDistanceCoord2Intersection(distanceCoord2Intersection);
		distances.setDistanceIntersetion2Node(distanceIntersection2Node);
		return distances;
	}
}