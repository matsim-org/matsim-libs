package playground.tnicolai.matsim4opus.utils.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import com.vividsolutions.jts.geom.Point;

public class NetworkUtil {
	
	// logger
	private static final Logger log = Logger.getLogger(NetworkUtil.class);
	
	public static double meterPerSecWalkSpeed = 1.38888889; // 1,38888889 m/s corresponds to 5km/h

	/**
	 * returns the orthogonal distance between a point and a network link (a straight line)
	 * @param link
	 * @param point
	 * @return
	 */
	public static double getOrthogonalDistance(Link link, Coord point){
		
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		double pointx = point.getX();
		double pointy = point.getY();
		
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
		
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		double pointx = point.getX();
		double pointy = point.getY();
		
		double normalzation = Math.sqrt( Math.pow( bx - ax , 2) + Math.pow( by - ay, 2));
		double distance = Math.abs( ((pointx - ax) * (by - ay)) - ((pointy -ay) * (bx - ax)) );
		
		double linkVectorX = bx - ax;
		double linkVectorY = by - ay;
		
		double projectionVectorX = pointx - ax;
		double projectionVectorY = pointy - ay;
		
		double numerator = projectionVectorX * linkVectorX + projectionVectorY * linkVectorY;
		double denominator = Math.pow(linkVectorX, 2) + Math.pow(linkVectorY, 2);
		
		double interscetionx = (linkVectorX * numerator) / denominator;
		double interscetiony = (linkVectorY * numerator) / denominator;
		
		double distance2DestinationNode = Math.abs( (interscetionx - linkVectorX) + (interscetiony - linkVectorY));
		
		return (distance/normalzation) + distance2DestinationNode;
	}
	
	public static double getDistance2Node(LinkImpl link, Point point, Node destinationNode){
		
		double ax = link.getFromNode().getCoord().getX();
		double ay = link.getFromNode().getCoord().getY();
		double bx = link.getToNode().getCoord().getX();
		double by = link.getToNode().getCoord().getY();
		
		double pointx = point.getX();
		double pointy = point.getY();
		
		double normalzation = Math.sqrt( Math.pow( bx - ax , 2) + Math.pow( by - ay, 2));
		double distance = Math.abs( ((pointx - ax) * (by - ay)) - ((pointy -ay) * (bx - ax)) );
		
		double linkVectorX = bx - ax;
		double linkVectorY = by - ay;
		
		double projectionVectorX = pointx - ax;
		double projectionVectorY = pointy - ay;
		
		double numerator = projectionVectorX * linkVectorX + projectionVectorY * linkVectorY;
		double denominator = Math.pow(linkVectorX, 2) + Math.pow(linkVectorY, 2);
		
		double interscetionx = (linkVectorX * numerator) / denominator;
		double interscetiony = (linkVectorY * numerator) / denominator;
		
		double distance2DestinationNode = Math.abs( (interscetionx - linkVectorX) + (interscetiony - linkVectorY));
		
		return (distance/normalzation) + distance2DestinationNode;
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
		
		Coord origin = new CoordImpl(0, 0);
		Coord destination = new CoordImpl(100,0);
		
		double result1 = getEuclidianDistance(origin, destination);
		assert (result1 == 100.);
		double walktime1_min = getEuclideanDistanceAsWalkTimeInSeconds(origin, destination) / 60.;
		log.info("Walk travel time for " + result1 + " meter takes " + walktime1_min + " minutes.");
		
		double result2 = getEuclidianDistance( 0., 0., 0., 200.);
		assert(result2 == 200.);
		double walktime2_min = getEuclideanDistanceAsWalkTimeInSeconds(0., 0., 0., 200.) / 60.;
		log.info("Walk travel time for " + result2 + " meter takes " + walktime2_min + " minutes.");
	}
}
