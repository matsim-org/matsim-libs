package interpolation.test;

import interpolation.Interpolation;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

public class TestScenario {

	private static final Logger log = Logger.getLogger(Interpolation.class);
	
	//information about the given data
	private static String filename = "zurich_accessibility_grid_cellsize100m.txt";
	
	//information about the interpolation method
	private static int interpolationMethod = Interpolation.BILINEAR;
	private static double expForIDW = 1.;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		java.net.URL input = ClassLoader.getSystemResource(filename);
		
		log.info("interpolate file " + filename + " with interpolation method " + interpolationMethod + ":");

		log.info("reading data...");
		SpatialGrid sg = SpatialGrid.readFromFile(input.getFile());		
		
		log.info("interpolating...");
		Interpolation interpolation = new Interpolation(sg, interpolationMethod, expForIDW);
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		// calculate new values for higher resolution
		for (double y = sg.getYmin(); y <= sg.getYmax(); y = y+ sg.getResolution()/2) {
			for (double x = sg.getXmin(); x <= sg.getXmax(); x = x+ sg.getResolution()/2) {
				sg_new.setValue(sg_new.getRow(y), sg_new.getColumn(x), interpolation.interpolate(x, y));
			}
		}
		
		log.info("writing interpolated data");
		sg_new.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename + "_" + interpolationMethod + ".txt");
	}

}
