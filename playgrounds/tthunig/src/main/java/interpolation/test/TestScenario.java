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
	private static String filename_data100 = "zurich_carAccessibility_grid_cellsize100m_shp";
	private static String filename_data200 = "zurich_carAccessibility_grid_cellsize200m_shp";
	
	//variables to save comparison results
	private static double interpolationTime_bilinear = Double.MAX_VALUE;
	private static double interpolationTime_bicubic = Double.MAX_VALUE;
	private static double interpolationTime_idw = Double.MAX_VALUE;
	private static double difference_bilinear = Double.MAX_VALUE;
	private static double difference_bicubic = Double.MAX_VALUE;
	private static double difference_idw = Double.MAX_VALUE;
	
	/**
	 * reads the data from the scenario and interpolates the grid of known values to a higher resolution first with bilinear interpolation second with bicubic spline interpolation
	 * 
	 * @param args not used
	 */
	public static void main(String[] args) {
		java.net.URL input100 = ClassLoader.getSystemResource(filename_data100+".txt");
		java.net.URL input200 = ClassLoader.getSystemResource(filename_data200+".txt");
		
		log.info("Reading data...");
		SpatialGrid sg100 = SpatialGrid.readFromFile(input100.getFile());		
		SpatialGrid sg200 = SpatialGrid.readFromFile(input200.getFile());
		
		log.info("Interpolate file " + filename_data200 + ":");
		SpatialGrid sg200_bilinear= testOneMethod(sg200, Interpolation.BILINEAR);
		SpatialGrid sg200_bicubic= testOneMethod(sg200, Interpolation.BICUBIC);
		SpatialGrid sg200_idw= testOneMethod(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING);
		log.info("Interpolation of file " + filename_data200 + " is completed.");
		
		log.info("Writing interpolated data...");
		sg200_bilinear.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.BILINEAR+ ".txt"); //TODO in resources speichern
		sg200_bicubic.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.BICUBIC+ ".txt"); //TODO in resources speichern
		sg200_idw.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ ".txt"); //TODO in resources speichern
		
		log.info("Computation of interpolation differences of the different methods...");
		difference_bilinear = differenceComputation(sg100, sg200_bilinear);
		difference_bicubic = differenceComputation(sg100, sg200_bicubic);
		difference_idw = differenceComputation(sg100, sg200_idw);
		
		log.info("Comparison of interpolation differences and interpolation time of the different methods:");
		System.out.println("interpolation method \t\t" + "interpolation time \t" + "interpolation difference to known data at the same resolution");
		System.out.println("-----------------------------------------------------------------------------------------------------");
		System.out.println("bilinear interpolation \t\t\t" + interpolationTime_bilinear + "ms \t\t\t" + difference_bilinear);
		System.out.println("bicubic interpolation \t\t\t" + interpolationTime_bicubic + "ms \t\t" + difference_bicubic);
		System.out.println("inverse distance weighting \t\t" + interpolationTime_idw + "ms \t\t\t" + difference_idw);
	}
	
	/**
	 * Interpolates the grid with one chosen interpolation method to one resolution higher.
	 * Writes the interpolated SpatialGrid out.
	 * 
	 * @param sg
	 * @param interpolationMethod
	 */
	private static SpatialGrid testOneMethod(SpatialGrid sg, int interpolationMethod){
		long startTime= System.currentTimeMillis();
		Interpolation interpolation = new Interpolation(sg, interpolationMethod, 1);
		
		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
		// calculate new values for higher resolution
		for (double y = sg.getYmin(); y <= sg.getYmax(); y += sg.getResolution()/2) {
			for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()/2) {
				sg_new.setValue(interpolation.interpolate(x, y), x, y);
			}
		}
		switch(interpolationMethod){
			case 0: interpolationTime_bilinear= System.currentTimeMillis()-startTime;
			case 1: interpolationTime_bicubic= System.currentTimeMillis()-startTime;
			case 2: interpolationTime_idw= System.currentTimeMillis()-startTime;
		}
		
		return sg_new;
	}
	
	/**
	 * Compares the differences between the interpolated SpatialGrid with resolution 200 and the original SpatialGrid with resolution 100.
	 * Sums the absolute difference and returns it.
	 * 
	 * @param sg100 the original SpatialGrid with resolution 100
	 * @param sg200_interpolated the interpolated SpatialGrid with resolution 200
	 * @return the absolute difference between them
	 */
	private static double differenceComputation(SpatialGrid sg100, SpatialGrid sg200_interpolated){
		double differenceToOriginalSG = 0;
//		double quadDiffToOriginalSG = 0;
		//sum difference at all coordinates where interpolated values are known
		for (double y = sg200_interpolated.getYmin(); y <= sg200_interpolated.getYmax(); y += sg200_interpolated.getResolution()){
			for (double x = sg200_interpolated.getXmin(); x <= sg200_interpolated.getXmax(); x += sg200_interpolated.getResolution()){
				double value100= sg100.getValue(x, y);
				double value200= sg200_interpolated.getValue(x, y);
				//calculate difference only in the zurich area
				if(!Double.isNaN(value100) && !Double.isNaN(value200)){
					differenceToOriginalSG += Math.abs(value100 - value200);
//					quadDiffToOriginalSG += Math.sqrt((value100 - value200)*(value100 - value200));
				}
			}
		}
		return differenceToOriginalSG;
	}

}
