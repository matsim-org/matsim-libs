package interpolation.test;

import interpolation.Interpolation;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Class for testing the implemented interpolation methods visually.
 * Uses the car accessibilities of the zurich scenario at resolution 100m.
 * Reads them as SpatialGrid and interpolates them to one resolution higher.
 * 
 * @author tthunig
 *
 */
public class TestScenario {

	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	//information about the given data
	private static String filename = "zurich_accessibility_grid_cellsize100m";
	
	/**
	 * reads the data from the scenario and interpolates the grid of known values to a higher resolution first with bilinear interpolation second with bicubic spline interpolation
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		java.net.URL input = ClassLoader.getSystemResource(filename+".txt");
		
		log.info("interpolate file " + filename + ":");

		log.info("reading data...");
		SpatialGrid sg = SpatialGrid.readFromFile(input.getFile());		
		
		testOneMethod(sg, Interpolation.BILINEAR);
		testOneMethod(sg, Interpolation.BICUBIC);
		testOneMethod(sg, Interpolation.INVERSE_DISTANCE_WEIGHTING);
		
		log.info("done");
	}
	
	/**
	 * Interpolates the grid with one chosen interpolation method to one resolution higher.
	 * Writes the interpolated SpatialGrid out.
	 * 
	 * @param sg
	 * @param interpolationMethod
	 */
	private static void testOneMethod(SpatialGrid sg, int interpolationMethod){
		log.info("interpolating data with the interpolation method " + interpolationMethod + "...");
		Interpolation interpolation = new Interpolation(sg, interpolationMethod, 1);
		
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		// calculate new values for higher resolution
		for (double y = sg.getYmin(); y <= sg.getYmax(); y += sg.getResolution()/2) {
			for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()/2) {
				sg_new.setValue(interpolation.interpolate(x, y), x, y);
			}
		}
		
		log.info("writing interpolated data...");
		sg_new.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename + "_" + interpolationMethod + ".txt"); //TODO in resources speichern
	}

}
