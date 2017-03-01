package saleem.stockholmmodel.utils;
/**
 * Class to calculate distance between two points in different coordinate systems.
 * 
 * @author Mohammad Saleem
 */
public class DistanceCalculation {
	public final static double AVERAGE_RADIUS_OF_EARTH = 6371;
	public static double calculateDistanceLatLon(double userLat, double userLng, double venueLat, double venueLng) {

	    double latDistance = Math.toRadians(userLat - venueLat);
	    double lngDistance = Math.toRadians(userLng - venueLng);

	    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
	      + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
	      * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

	    return AVERAGE_RADIUS_OF_EARTH * c;
	}
	public static double calculateDistanceUTM (double x, double y, double x1, double y1) {
	    double deltaX = x1 - x;
	    double deltaY = y1 - y;
	    double result = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
	    return result; 
	}
	public static void main(String[] args) {
		System.out.println("LatLong Distance: " + calculateDistanceLatLon(18.0368913403574, 59.3982038728158, 17.9426683336417, 59.4029594411987));
		System.out.println("UTM Distance: " + calculateDistanceUTM(331768.41000000003, 6588139.140000001, 667048.16, 6588616.87));
	}
}
