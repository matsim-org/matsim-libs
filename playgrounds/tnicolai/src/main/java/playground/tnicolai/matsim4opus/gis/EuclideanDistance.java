package playground.tnicolai.matsim4opus.gis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.tnicolai.matsim4opus.matsim4urbansim.ERSAControlerListener;

public class EuclideanDistance {
	// logger
	private static final Logger log = Logger.getLogger(ERSAControlerListener.class);
	
	public static double meterPerSecWalkSpeed = 1.38888889; // 1,38888889 m/s corresponds to 5km/h

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
