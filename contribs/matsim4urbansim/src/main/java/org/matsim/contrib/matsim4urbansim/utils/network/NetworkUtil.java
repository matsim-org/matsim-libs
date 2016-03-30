package org.matsim.contrib.matsim4urbansim.utils.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.matsim4urbansim.utils.helperobjects.Distances;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;

import com.vividsolutions.jts.geom.Point;

/**
 * @deprecated this class is nowhere used because it is a former version of NetworkUtil in contrib accessibility. 
 * just delete it? tt feb'16
 */
public class NetworkUtil {
	
	// logger
	private static final Logger log = Logger.getLogger(NetworkUtil.class);
	
	public static double meterPerSecWalkSpeed = 1.38888889; // 1,38888889 m/s corresponds to 5km/h
	
	public static long euclidianCounter = 0;
	public static long othogonalCounter = 0;
	public static long totalCounter = 0;

	/**
	 * Returns the orthogonal distance between a point and a network link (a straight line).
	 * It assumes that a link has unlimited length.
	 * So it gives just the distance between a point and a line.
	 * 
	 * tnicolai feb'13: not used any more for accessibility calculation
	 * 
	 * @param link
	 * @param coord
	 * @return
	 */
	public static double getOrthogonalDistance2Link(Link link, Coord point){
		
		return getOrthogonalDistance(link, point.getX(), point.getY());
	}
	
	/**
	 * Returns the orthogonal distance between a point and a network link (a straight line).
	 * It assumes that a link has unlimited length.
	 * So it gives just the distance between a point and a line.
	 * 
	 * tnicolai feb'13: not used any more for accessibility calculation
	 * 
	 * @param link
	 * @param point
	 * @return
	 */
	public static double getOrthogonalDistance2Link(Link link, Point point){
		
		return getOrthogonalDistance(link, point.getX(), point.getY());
	}
	
	/**
	 * Returns the orthogonal distance between a point and a network link (a straight line).
	 * It assumes that a link has unlimited length.
	 * So it gives just the distance between a point and a line.
	 * 
	 * tnicolai feb'13: not used any more for accessibility calculation
	 * 
	 * @param link
	 * @param pointx
	 * @param pointy
	 * @return
	 */
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
	@Deprecated
	public static double getDistance2NodeOldVersion(Link link, Coord point, Node destinationNode){
		
		return getDistance2NodeOldVersion(link, point.getX(), point.getY(), destinationNode);
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
	@Deprecated
	public static double getDistance2NodeOldVersion(Link link, Point point, Node destinationNode){
		
		return getDistance2NodeOldVersion(link, point.getX(), point.getY(), destinationNode);
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
	@Deprecated
	private static double getDistance2NodeOldVersion(Link link, double pointx, double pointy, Node destinationNode){
		
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
	 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
	 * - the orthogonal distance between the point and the link and
	 * - the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
	 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the euclidean distance between the point and the node.
	 * 
	 * @param link
	 * @param point
	 * @param destinationNode
	 * @return Distances
	 */
	public static Distances getDistance2Node(Link link, Coord point, Node destinationNode){
		return getDistance2Node(link, point.getX(), point.getY(), destinationNode);
	}
	
	/**
	 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
	 * - the orthogonal distance between the point and the link and
	 * - the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
	 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the euclidean distance between the point and the node. 
	 * 
	 * @param link
	 * @param point
	 * @param destinationNode
	 * @return Distances
	 */
	public static Distances getDistance2Node(Link link, Point point, Node destinationNode){
		return getDistance2Node(link, point.getX(), point.getY(), destinationNode);
	}
	
	/**
	 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
	 * - the orthogonal distance between the point and the link and
	 * - the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
	 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the euclidean distance between the point and the node.
	 * 
	 * @param link
	 * @param pointx
	 * @param pointy
	 * @param destinationNode
	 * @return
	 */
	private static Distances getDistance2Node(Link link, double pointx, double pointy, Node destinationNode){
		
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
		double lambdax = vectorx/bax; // vectorx = intersectionx-ax
		double lambday = vectory/bay; // vectory = intersectiony-ay
		double lambdaxInt = Math.rint( lambdax * 1000);
		double lambdayInt = Math.rint( lambday * 1000);
		
		totalCounter++;
		
		if(bax == 0 && 0 <= lambday && lambday <= 1){ // for vertical links
			// test if lambday is true for x and y
			double testx = ax + (lambday * bax);
			double testy = ay + (lambday * bay);
			if(testx == intersectionx && testy == intersectiony){
				othogonalCounter++;
				return orthogonalDistancePlusLinkIntersection(pointx, pointy,
						destinationNode, intersectionx, intersectiony);
			}
		}
		else if(bay == 0 && 0 <= lambdax && lambdax <= 1){ // for horizontal links
			// test if lambdax is true for x and y
			double testx = ax + (lambdax * bax);
			double testy = ay + (lambday * bay);
			if(testx == intersectionx && testy == intersectiony){
				othogonalCounter++;
				return orthogonalDistancePlusLinkIntersection(pointx, pointy,
						destinationNode, intersectionx, intersectiony);
			}
		}
		// hier liegt der Schnittpunkt "intersection auf der geraden (link)
		else if(lambdaxInt == lambdayInt && 0 <= lambdax && lambdax <= 1 ){
			othogonalCounter++;
			return orthogonalDistancePlusLinkIntersection(pointx, pointy,
					destinationNode, intersectionx, intersectiony);
		}
		
		euclidianCounter++;
		return getEuclidianDistance(pointx, pointy, destinationNode.getCoord().getX(), destinationNode.getCoord().getY());
		
		// TEST
		
//		double distancePoint2Link = Math.sqrt( (pointx-intersectionx)*(pointx-intersectionx) + (pointy-intersectiony)*(pointy-intersectiony));
//		double distanceIntersection2Node = Math.sqrt( (intersectionx - destinationNode.getCoord().getX())*(intersectionx - destinationNode.getCoord().getX()) + (intersectiony - destinationNode.getCoord().getY())*(intersectiony - destinationNode.getCoord().getY()) );
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
	private static Distances orthogonalDistancePlusLinkIntersection(double pointx,
																double pointy, 
																Node destinationNode, 
																double intersectionx,
																double intersectiony) {
		Distances d = new Distances();
		double distancePoint2Link = Math.sqrt( (pointx-intersectionx)*(pointx-intersectionx) + (pointy-intersectiony)*(pointy-intersectiony));
		double distanceIntersection2Node = Math.sqrt( (intersectionx - destinationNode.getCoord().getX())*(intersectionx - destinationNode.getCoord().getX()) + (intersectiony - destinationNode.getCoord().getY())*(intersectiony - destinationNode.getCoord().getY()) );
		d.setDisatancePoint2Road(distancePoint2Link);
		d.setDistanceRoad2Node(distanceIntersection2Node);
		return d;
		//		return distancePoint2Link + distanceIntersection2Node;
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
	 * @return Distances
	 */
	public static Distances getEuclidianDistance(double x1, double y1, double x2, double y2){
		
		Distances d = new Distances();
		
		double xDiff = x1 - x2;
		double yDiff = y1 - y2;
		double distance =  Math.sqrt( (xDiff*xDiff) + (yDiff*yDiff) );
		d.setDisatancePoint2Road(distance);
		
		return d;
	}
	
	/**
	 * This method expects the nearest link to a given measure point. 
	 * It calculates the euclidian distance for both nodes of the link, 
	 * "fromNode" and "toNode" and returns the node with shorter distance
	 * 
	 * @param coordFromZone
	 * @param nearestLink
	 */
	public static Node getNearestNode(Coord coordFromZone, Link nearestLink) {
		Node toNode = nearestLink.getToNode();
		Node fromNode= nearestLink.getFromNode();
		
		double distanceToNode = getEuclidianDistance(coordFromZone, toNode.getCoord());
		double distanceFromNode= getEuclidianDistance(coordFromZone, fromNode.getCoord());
		
		if(distanceToNode < distanceFromNode)
			return toNode;
		return fromNode;
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
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
		Node node4 = network.createAndAddNode(Id.create("4", Node.class), new Coord((double) 2000, (double) 2000));
		Node node5 = network.createAndAddNode(Id.create("5", Node.class), new Coord((double) 1000, (double) 0));
		LinkImpl link1 = (LinkImpl) network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000, 1, 3600, 1);
		LinkImpl link2 = (LinkImpl) network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1500, 1, 3600, 1);
		LinkImpl link3 = (LinkImpl) network.createAndAddLink(Id.create("3", Link.class), node3, node4, 1000, 1, 3600, 1);
		LinkImpl link4 = (LinkImpl) network.createAndAddLink(Id.create("4", Link.class), node4, node5, 2800, 1, 3600, 1);

		Distances distance1 = NetworkUtil.getDistance2Node(link1, new Coord((double) 100, (double) 0), node1);
		log.info(distance1.getDistancePoint2Road() + distance1.getDistanceRoad2Node() + " distance1");

		final double y = -10;
		Distances distance2 = NetworkUtil.getDistance2Node(link1, new Coord((double) 100, y), node1);
		log.info(distance2.getDistancePoint2Road() + distance2.getDistanceRoad2Node() + " distance2");

		Distances distance3 = NetworkUtil.getDistance2Node(link2, new Coord((double) 100, (double) 1000), node2);
		log.info(distance3.getDistancePoint2Road() + distance3.getDistanceRoad2Node() + " distance3");

		final double x = -100;
		Distances distance4 = NetworkUtil.getDistance2Node(link2, new Coord(x, (double) 1000), node2);
		log.info(distance4.getDistancePoint2Road() + distance4.getDistanceRoad2Node() + " distance4");
	}
}
