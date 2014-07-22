package playground.artemc.networkTools.jcoord;

/**
 * Class to illustrate the use of the various functions of the classes in the
 * Jcoord package.
 * 
 * (c) 2006 Jonathan Stott
 * 
 * Created on 11-Feb-2006
 * 
 * @author Jonathan Stott
 * @version 1.0
 * @since 1.0
 */
public class Test {

  /**
   * Main method
   * 
   * @param args
   *          not used
   * @since 1.0
   */
  public static void main(String[] args) {

    /*
     * Calculate Surface Distance between two Latitudes/Longitudes
     * 
     * The distance() function takes a reference to a LatLng object as a
     * parameter and calculates the surface distance between the the given
     * object and this object in kilometres:
     */

    System.out
        .println("Calculate Surface Distance between two Latitudes/Longitudes");
    LatLng lld1 = new LatLng(40.718119, -73.995667); // New York
    System.out.println("New York Lat/Long: " + lld1.toString());
    LatLng lld2 = new LatLng(51.499981, -0.125313); // London
    System.out.println("London Lat/Long: " + lld2.toString());
    double d = lld1.distance(lld2);
    System.out.println("Surface Distance between New York and London: " + d
        + "km");
    System.out.println();

    /*
     * Convert OS Grid Reference to Latitude/Longitude
     * 
     * Note that the OSGB-Latitude/Longitude conversions use the OSGB36 datum by
     * default. The majority of applications use the WGS84 datum, for which the
     * appropriate conversions need to be added. See the examples below to see
     * the difference between the two data.
     */

    System.out.println("Convert OS Grid Reference to Latitude/Longitude");
    // Using OSGB36 (convert an OSGB grid reference to a latitude and longitude
    // using the OSGB36 datum):
    System.out.println("Using OSGB36");
    OSRef os1 = new OSRef(651409.903, 313177.270);
    System.out.println("OS Grid Reference: " + os1.toString() + " - "
        + os1.toSixFigureString());
    LatLng ll1 = os1.toLatLng();
    System.out.println("Converted to Lat/Long: " + ll1.toString());
    System.out.println();

    // Using WGS84 (convert an OSGB grid reference to a latitude and longitude
    // using the WGS84 datum):
    System.out.println("Using WGS84");
    OSRef os1w = new OSRef(651409.903, 313177.270);
    System.out.println("OS Grid Reference: " + os1w.toString() + " - "
        + os1w.toSixFigureString());
    LatLng ll1w = os1w.toLatLng();
    ll1w.toWGS84();
    System.out.println("Converted to Lat/Long: " + ll1w.toString());
    System.out.println();

    /*
     * Convert Latitude/Longitude to OS Grid Reference
     * 
     * Note that the OSGB-Latitude/Longitude conversions use the OSGB36 datum by
     * default. The majority of applications use the WGS84 datum, for which the
     * appropriate conversions need to be added. See the examples below to see
     * the difference between the two data.
     */

    System.out.println("Convert Latitude/Longitude to OS Grid Reference");
    // Using OSGB36 (convert a latitude and longitude using the OSGB36 datum to
    // an OSGB grid reference):
    System.out.println("Using OSGB36");
    LatLng ll2 = new LatLng(52.657570301933, 1.7179215806451);
    System.out.println("Latitude/Longitude: " + ll2.toString());
    OSRef os2 = ll2.toOSRef();
    System.out.println("Converted to OS Grid Ref: " + os2.toString() + " - "
        + os2.toSixFigureString());
    System.out.println();

    // Using WGS84 (convert a latitude and longitude using the WGS84 datum to an
    // OSGB grid reference):
    System.out.println("Using WGS84");
    LatLng ll2w = new LatLng(52.657570301933, 1.7179215806451);
    System.out.println("Latitude/Longitude: " + ll2.toString());
    ll2w.toOSGB36();
    OSRef os2w = ll2w.toOSRef();
    System.out.println("Converted to OS Grid Ref: " + os2w.toString() + " - "
        + os2w.toSixFigureString());
    System.out.println();

    /*
     * Convert Six-Figure OS Grid Reference String to an OSRef Object
     * 
     * To convert a string representing a six-figure OSGB grid reference:
     */

    System.out
        .println("Convert Six-Figure OS Grid Reference String to an OSRef Object");
    String os6 = "TG514131";
    System.out.println("Six figure string: " + os6);
    OSRef os6x = new OSRef(os6);
    System.out.println("Converted to OS Grid Ref: " + os6x.toString() + " - "
        + os6x.toSixFigureString());
    System.out.println();

    /*
     * Convert UTM Reference to Latitude/Longitude
     */

    System.out.println("Convert UTM Reference to Latitude/Longitude");
    UTMRef utm1 = new UTMRef(456463.99, 3335334.05, 'E', 12);
    System.out.println("UTM Reference: " + utm1.toString());
    LatLng ll3 = utm1.toLatLng();
    System.out.println("Converted to Lat/Long: " + ll3.toString());
    System.out.println();

    /*
     * Convert Latitude/Longitude to UTM Reference
     */

    System.out.println("Convert Latitude/Longitude to UTM Reference");
    LatLng ll4 = new LatLng(-60.1167, -111.7833);
    System.out.println("Latitude/Longitude: " + ll4.toString());
    UTMRef utm2 = ll4.toUTMRef();
    System.out.println("Converted to UTM Ref: " + utm2.toString());
    System.out.println();
  }

}
