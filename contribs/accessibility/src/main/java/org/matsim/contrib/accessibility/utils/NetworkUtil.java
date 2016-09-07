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
		
		Network network = NetworkUtils.createNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 0, (double) 1000));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 1000, (double) 2000));
//		Node node4 = network.createAndAddNode(Id.create("4", Node.cla)s, new CoordImpl(2000, 2000));
//		Node node5 = network.createAndAddNode(Id.create("5", Node.cla)s, new CoordImpl(1000, 0));
		Link link1 = (Link) NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), node1, node2, (double) 1000, (double) 1, (double) 3600, (double) 1 );
		Link link2 = (Link) NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), node2, node3, (double) 1500, (double) 1, (double) 3600, (double) 1 );
//		LinkImpl link3 = (LinkImpl) network.createAndAddLink(Id.create("3", Link.class), node3, node4, 1000, 1, 3600, 1);
//		LinkImpl link4 = (LinkImpl) network.createAndAddLink(Id.create("4", Link.class), node4, node5, 2800, 1, 3600, 1);

		Distances distance1 = NetworkUtil.getDistances2NodeViaGivenLink(new Coord((double) 100, (double) 0), link1, node1);
		log.info(distance1.getDistancePoint2Road() + distance1.getDistanceRoad2Node() + " distance1");
		
		Distances distance1a = NetworkUtil.getDistances2NodeViaGivenLink(new Coord(100., 100.), link1, node2);
		log.info(distance1a.getDistancePoint2Road() + distance1a.getDistanceRoad2Node() + " distance1a");
		log.info(distance1a.getDistancePoint2Road() + " distance1a part 1");
		log.info(distance1a.getDistanceRoad2Node() + " distance1a part 2");

		final double y = -10;
		Distances distance2 = NetworkUtil.getDistances2NodeViaGivenLink(new Coord((double) 100, y), link1, node1);
		log.info(distance2.getDistancePoint2Road() + distance2.getDistanceRoad2Node() + " distance2");

		Distances distance3 = NetworkUtil.getDistances2NodeViaGivenLink(new Coord((double) 100, (double) 1000), link2, node2);
		log.info(distance3.getDistancePoint2Road() + distance3.getDistanceRoad2Node() + " distance3");

		final double x = -100;
		Distances distance4 = NetworkUtil.getDistances2NodeViaGivenLink(new Coord(x, (double) 1000), link2, node2);
		log.info(distance4.getDistancePoint2Road() + distance4.getDistanceRoad2Node() + " distance4");
	}
}
