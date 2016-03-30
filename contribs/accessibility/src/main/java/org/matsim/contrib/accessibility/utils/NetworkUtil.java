package org.matsim.contrib.accessibility.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;

public final class NetworkUtil {
	private NetworkUtil(){} // do not instantiate

	private static final Logger log = Logger.getLogger(NetworkUtil.class);
	
	/**
	 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
	 * - the orthogonal distance between the point and the link and
	 * - the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
	 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the euclidean distance between the point and the node.
	 * @param point
	 * @param link
	 * @param destinationNode
	 * 
	 * @return Distances
	 */
	public static Distances getDistances2Node(Coord point, Link link, Node destinationNode){
		return getDistances2Node(point.getX(), point.getY(), link, destinationNode);
	}
		
	/**
	 * This method calculates the distance between a point and a node via a link (which should contains the node) as the sum of
	 * <ul>
	 * <li> the orthogonal distance between the point and the link and
	 * <li> the distance between the intersection point of the orthogonal projection (from the point to the link) to the node.
	 * </ul>
	 * If the orthogonal projection of the point to the line does not intersects the link, the method returns the Euclidean distance between the point and the node.
	 * @param pointx
	 * @param pointy
	 * @param link
	 * @param destinationNode
	 * 
	 * @return
	 */
	private static Distances getDistances2Node(double pointx, double pointy, Link link, Node destinationNode){
		
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
		
		if(bax == 0 && 0 <= lambday && lambday <= 1){ // for vertical links
			// test if lambday is true for x and y
			double testx = ax + (lambday * bax);
			double testy = ay + (lambday * bay);
			if(testx == intersectionx && testy == intersectiony){
				return orthogonalDistancePlusLinkIntersection(pointx, pointy,
						destinationNode, intersectionx, intersectiony);
			}
		}
		else if(bay == 0 && 0 <= lambdax && lambdax <= 1){ // for horizontal links
			// test if lambdax is true for x and y
			double testx = ax + (lambdax * bax);
			double testy = ay + (lambday * bay);
			if(testx == intersectionx && testy == intersectiony){
				return orthogonalDistancePlusLinkIntersection(pointx, pointy,
						destinationNode, intersectionx, intersectiony);
			}
		}
		// hier liegt der Schnittpunkt "intersection auf der geraden (link)
		else if(lambdaxInt == lambdayInt && 0 <= lambdax && lambdax <= 1 ){
			return orthogonalDistancePlusLinkIntersection(pointx, pointy,
					destinationNode, intersectionx, intersectiony);
		}
		
		
		Distances d = new Distances();
		double distance = NetworkUtils.getEuclideanDistance(pointx, pointy, destinationNode.getCoord().getX(), destinationNode.getCoord().getY()) ;
		d.setDisatancePoint2Road(distance);
		// yyyy I have no idea what it is doing here. kai, mar'14

		return d ;
		
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
//		Node node4 = network.createAndAddNode(Id.create("4", Node.cla)s, new CoordImpl(2000, 2000));
//		Node node5 = network.createAndAddNode(Id.create("5", Node.cla)s, new CoordImpl(1000, 0));
		LinkImpl link1 = (LinkImpl) network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000, 1, 3600, 1);
		LinkImpl link2 = (LinkImpl) network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1500, 1, 3600, 1);
//		LinkImpl link3 = (LinkImpl) network.createAndAddLink(Id.create("3", Link.class), node3, node4, 1000, 1, 3600, 1);
//		LinkImpl link4 = (LinkImpl) network.createAndAddLink(Id.create("4", Link.class), node4, node5, 2800, 1, 3600, 1);

		Distances distance1 = NetworkUtil.getDistances2Node(new Coord((double) 100, (double) 0), link1, node1);
		log.info(distance1.getDistancePoint2Road() + distance1.getDistanceRoad2Node() + " distance1");

		final double y = -10;
		Distances distance2 = NetworkUtil.getDistances2Node(new Coord((double) 100, y), link1, node1);
		log.info(distance2.getDistancePoint2Road() + distance2.getDistanceRoad2Node() + " distance2");

		Distances distance3 = NetworkUtil.getDistances2Node(new Coord((double) 100, (double) 1000), link2, node2);
		log.info(distance3.getDistancePoint2Road() + distance3.getDistanceRoad2Node() + " distance3");

		final double x = -100;
		Distances distance4 = NetworkUtil.getDistances2Node(new Coord(x, (double) 1000), link2, node2);
		log.info(distance4.getDistancePoint2Road() + distance4.getDistanceRoad2Node() + " distance4");
	}
}
