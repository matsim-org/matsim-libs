package interpolation_old;


import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4opus.gis.SpatialGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * class for testing the implemented interpolation methods on a grid
 * @deprecated
 * 
 * @author tthunig
 *
 */
public class TestGridInterpolation {
	
	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	//information about the given data
	private static String resolution = "6400.0"; //bicubic schafft bis 400.0, bilinear bis 100.0 (100.0 kann R nicht mehr zeichnen)
	private static String directory = "java-versuch4-SpatialGridTest";
	
	//information about the interpolation method
	private static int interpolationMethod = Interpolation.BILINEAR;
	private static double expForIDW = 1.;

	
	/**
	 * reads the data from the defined path
	 * and interpolates it to one resolution higher with the defined interpolation method
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		
		log.info("interpolating file " + resolution + " with interpolation method " + interpolationMethod + ":");

		log.info("reading data...");
		SpatialGrid sg = SpatialGrid
				.readFromFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/results/"
						+ resolution + "travel_time_accessibility.txt");		
		
		log.info("interpolating...");
		Interpolation interpolation = new Interpolation(sg, interpolationMethod, true, expForIDW);
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		GeometryFactory factory = new GeometryFactory();
		// calculate new values for higher resolution
		for (double y = sg.getYmin(); y <= sg.getYmax(); y = y+ sg.getResolution()/2) {
			for (double x = sg.getXmin(); x <= sg.getXmax(); x = x+ sg.getResolution()/2) {
				sg_new.setValue(interpolation.interpolate(x, y), factory.createPoint(new Coordinate(x, y)));
			}
		}
		
		log.info("writing interpolated data");
		sg_new.writeToFile("Z:/WinHome/opus_home_shared/data/seattle_parcel/results/interpolationQuickTest/interpolation/" + directory + "/" + resolution + "_" + interpolationMethod + ".txt");
	}

}
