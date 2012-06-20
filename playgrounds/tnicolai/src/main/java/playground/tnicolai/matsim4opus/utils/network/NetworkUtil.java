package playground.tnicolai.matsim4opus.utils.network;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Point;

public class NetworkUtil {
	
	// logger
	private static final Logger log = Logger.getLogger(NetworkUtil.class);
	
	public static double meterPerSecWalkSpeed = 1.38888889; // 1,38888889 m/s corresponds to 5km/h

	/**
	 * returns the orthogonal distance between a point and a network link (a straight line)
	 * @param link
	 * @param coord
	 * @return
	 */
	public static double getOrthogonalDistance2NearestLink(Link link, Coord point){
		
		return getOrthogonalDistance(link, point.getX(), point.getY());
	}
	
	/**
	 * returns the orthogonal distance between a point and a network link (a straight line)
	 * @param link
	 * @param point
	 * @return
	 */
	public static double getOrthogonalDistance2NearestLink(Link link, Point point){
		
		return getOrthogonalDistance(link, point.getX(), point.getY());
	}
	
	private static double getOrthogonalDistance(Link link, double pointx, double pointy){
		
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();

		double normalzation = Math.sqrt( Math.pow( bx - ax , 2) + Math.pow( by - ay, 2));
		double distance = Math.abs( ((pointx - ax) * (by - ay)) - ((pointy -ay) * (bx - ax)) );
		
		return distance/normalzation;
	}
	
	
	/**
	 * in accessibility computation travel costs are calculated between a start and an end node.
	 * the distance to a start node is calculated as follows:
	 * 1) the orthogonal distance between a start point and the nearest network link is calculated than ...
	 * 2) the distance between the intersection (of the projection of the start point) on the network link to the "start" node is taken.
	 * than it returns the distances of (1) + (2)
	 * 
	 * @param link
	 * @param point
	 * @param destinationNode
	 * @return
	 */
	public static double getDistance2Node(LinkImpl link, Coord point, Node destinationNode){
		
		return getDistance2Node(link, point.getX(), point.getY(), destinationNode);
	}
	
	/**
	 * in accessibility computation travel costs are calculated between a start and an end node.
	 * the distance to a start node is calculated as follows:
	 * 1) the orthogonal distance between a start point and the nearest network link is calculated than ...
	 * 2) the distance between the intersection (of the projection of the start point) on the network link to the "start" node is taken.
	 * than it returns the distances of (1) + (2)
	 * 
	 * @param link
	 * @param point
	 * @param destinationNode
	 * @return
	 */
	public static double getDistance2Node(LinkImpl link, Point point, Node destinationNode){
		
		return getDistance2Node(link, point.getX(), point.getY(), destinationNode);
	}
	
	/**
	 * in accessibility computation travel costs are calculated between a start and an end node.
	 * the distance to a start node is calculated as follows:
	 * 1) the orthogonal distance between a start point and the nearest network link is calculated than ...
	 * 2) the distance between the intersection (of the projection of the start point) on the network link to the "start" node is taken.
	 * than it returns the distances of (1) + (2)
	 * 
	 * @param link
	 * @param pointx (x coordinate)
	 * @param pointy (y coordinate)
	 * @param destinationNode
	 * @return
	 */
	private static double getDistance2Node(LinkImpl link, double pointx, double pointy, Node destinationNode){
		
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		double normalzation = Math.sqrt( (bx - ax)*(bx - ax) + (by - ay)*(by - ay));
		double distance = Math.abs( ((pointx - ax) * (by - ay)) - ((pointy -ay) * (bx - ax)) );
		
		double bax = bx - ax;
		double bay = by - ay;
		
		double pax = pointx - ax;
		double pay = pointy - ay;
		
		double numerator = pax * bax + pay * bay;
		double denominator = bax*bax + bay*bay;
		double fraction = numerator/denominator; 
		double vectorx = bax * fraction;
		double vectory = bay * fraction;
		
		double intersectionx = ax + vectorx;
		double intersectiony = ay + vectory;
		
		double distance2DestinationNode = Math.sqrt( (intersectionx - destinationNode.getCoord().getX())*(intersectionx - destinationNode.getCoord().getX()) + (intersectiony - destinationNode.getCoord().getY())*(intersectiony - destinationNode.getCoord().getY()));
		
		return (distance/normalzation) + distance2DestinationNode;
	}
	
	/**
	 * this is just another implementation of getDistance2Node, its implemented to test whether a faster implementation
	 * is possible. However, getDistance2Node is faster. Don't use this method
	 * 
	 * @param link
	 * @param point
	 * @param destinationNode
	 * @return
	 */
	public static double  getDistance2NodeV2(LinkImpl link, Coord point, Node destinationNode){
		return getDistance2NodeV2(link, point.getX(), point.getY(), destinationNode);
	}
	
	/**
	 * this is just another implementation of getDistance2Node, its implemented to test whether a faster implementation
	 * is possible. However, getDistance2Node is faster. Don't use this method
	 * 
	 * @param link
	 * @param point
	 * @param destinationNode
	 * @return
	 */
	public static double  getDistance2NodeV2(LinkImpl link, Point point, Node destinationNode){
		return getDistance2NodeV2(link, point.getX(), point.getY(), destinationNode);
	}
	
	private static double getDistance2NodeV2(LinkImpl link, double pointx, double pointy, Node destinationNode){
		
		// line A B
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		// vector ba (r)
		double bax = bx - ax;
		double bay = by - ay;
		// vector pa (v)
		double pax = pointx - ax;
		double pay = pointy - ay;
		
		// calculation the vector p (projection of point P on line A B)
		double numerator = pax * bax + pay * bay;
		double denominator = bax*bax + bay*bay;
		double fraction = numerator/denominator;
		double vectorx = bax * fraction;
		double vectory = bay * fraction;
		
		double intersectionx = ax + vectorx;
		double intersectiony = ay + vectory;
		
		// TEST 
		// is bax or bay == 0?
		double lambdax = (intersectionx-ax)/bax;
		double lambday = (intersectiony-ay)/bay;
		
		if(bax == 0 && 0 <= lambday && lambday <= 1){ // for vertical links
			// test if lambday is true for x and y
			double testx = ax + (lambday * bax);
			double testy = ay + (lambday * bay);
			if(testx == intersectionx && testy == intersectiony)
				return orthogonalDistancePlusLinkIntersection(pointx, pointy,
						destinationNode, intersectionx, intersectiony);
		}
		else if(bay == 0 && 0 <= lambdax && lambdax <= 1){ // for horizontal links
			// test if lambdax is true for x and y
			double testx = ax + (lambdax * bax);
			double testy = ay + (lambday * bay);
			if(testx == intersectionx && testy == intersectiony)
				return orthogonalDistancePlusLinkIntersection(pointx, pointy,
						destinationNode, intersectionx, intersectiony);
		}
		// hier liegt der Schnittpunkt "intersection auf der geraden (link)
		else if(lambdax == lambday && 0 <= lambdax && lambdax <= 1 ){
			return orthogonalDistancePlusLinkIntersection(pointx, pointy,
					destinationNode, intersectionx, intersectiony);
		}
		
		return getEuclidianDistance(pointx, pointy, destinationNode.getCoord().getX(), destinationNode.getCoord().getY());
		
		// TEST
		
//		double distancePoint2Link = Math.sqrt( (pointx-intersectionx)*(pointx-intersectionx) + (pointy-intersectiony)*(pointy-intersectiony));
//		double distanceIntersection2Node = Math.sqrt( (intersectionx - destinationNode.getCoord().getX())*(intersectionx - destinationNode.getCoord().getX()) + (intersectiony - destinationNode.getCoord().getY())*(intersectiony - destinationNode.getCoord().getY()) );
//		
//		return distancePoint2Link + distanceIntersection2Node;
	}

	/**
	 * @param pointx
	 * @param pointy
	 * @param destinationNode
	 * @param intersectionx
	 * @param intersectiony
	 * @return
	 */
	private static double orthogonalDistancePlusLinkIntersection(double pointx,
			double pointy, Node destinationNode, double intersectionx,
			double intersectiony) {
		// return orthogonal distance + distance from intersection to nearest node			
		double distancePoint2Link = Math.sqrt( (pointx-intersectionx)*(pointx-intersectionx) + (pointy-intersectiony)*(pointy-intersectiony));
		double distanceIntersection2Node = Math.sqrt( (intersectionx - destinationNode.getCoord().getX())*(intersectionx - destinationNode.getCoord().getX()) + (intersectiony - destinationNode.getCoord().getY())*(intersectiony - destinationNode.getCoord().getY()) );
		return distancePoint2Link + distanceIntersection2Node;
	}

	/**
	 * returns the euclidean distance between two coordinates
	 * 
	 * @param origin
	 * @param destination
	 * @return distance
	 */
	public static double getEuclidianDistance(Coord origin, Coord destination){
		
		assert(origin != null);
		assert(destination != null);
		
		double xDiff = origin.getX() - destination.getX();
		double yDiff = origin.getY() - destination.getY();
		double distance = Math.sqrt( (xDiff*xDiff) + (yDiff*yDiff) );
		
		return distance;
	}
	
	/** returns the euclidean distance between two points (x1,y1) and (x2,y2)
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return distance
	 */
	public static double getEuclidianDistance(double x1, double y1, double x2, double y2){
		
		double xDiff = x1 - x2;
		double yDiff = y1 - y2;
		double distance =  Math.sqrt( (xDiff*xDiff) + (yDiff*yDiff) );
		
		return distance;
	}
	
	/**
	 * returns the walk time for a given euclidean distance
	 * @param origin
	 * @param destination
	 * @return walk time in seconds
	 */
	public static double getEuclideanDistanceAsWalkTimeInSeconds(Coord origin, Coord destination){
		
		double distance = getEuclidianDistance(origin, destination);
		return distance / meterPerSecWalkSpeed;
	}
	
	/**
	 *  returns the walk time for a given euclidean distance
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return walk time in seconds
	 */
	public static double getEuclideanDistanceAsWalkTimeInSeconds(double x1, double y1, double x2, double y2){
		
		double distance = getEuclidianDistance(x1, y1, x2, y2);
		return distance / meterPerSecWalkSpeed;
	}
	
	/**
	 * testing above methods
	 * @param args
	 */
	public static void main(String[] args) {
		
		/* create a sample network:
		 *
		 *        (3)---3---(4)
		 *       /         /
		 *     2          /
		 *   /           /
		 * (2)          4
		 *  |          /
		 *  1         /
		 *  |        /
		 * (1)    (5)
		 *
		 * The network contains an exactly horizontal, an exactly vertical, an exactly diagonal
		 * and another link with no special slope to also test possible special cases.
		 */
		
		NetworkImpl network = NetworkImpl.createNetwork();
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(0, 1000));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(1000, 2000));
		Node node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(2000, 2000));
		Node node5 = network.createAndAddNode(new IdImpl("5"), new CoordImpl(1000, 0));
		LinkImpl link1 = (LinkImpl) network.createAndAddLink(new IdImpl("1"), node1, node2, 1000, 1, 3600, 1);
		LinkImpl link2 = (LinkImpl) network.createAndAddLink(new IdImpl("2"), node2, node3, 1500, 1, 3600, 1);
		LinkImpl link3 = (LinkImpl) network.createAndAddLink(new IdImpl("3"), node3, node4, 1000, 1, 3600, 1);
		LinkImpl link4 = (LinkImpl) network.createAndAddLink(new IdImpl("4"), node4, node5, 2800, 1, 3600, 1);

		double distance1 = NetworkUtil.getDistance2NodeV2(link1, new CoordImpl(100, 0), node1);
		log.info(distance1 + " distance1");
		
		double distance2 = NetworkUtil.getDistance2NodeV2(link1, new CoordImpl(100, -10), node1);
		log.info(distance2 + " distance2");
		
		double distance3 = NetworkUtil.getDistance2NodeV2(link2, new CoordImpl(100, 1000), node2);
		log.info(distance3 + " distance3");
		
		double distance4 = NetworkUtil.getDistance2NodeV2(link2, new CoordImpl(-100, 1000), node2);
		log.info(distance4 + " distance4");
	}
}
