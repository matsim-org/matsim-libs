package interpolationTest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.accessibility.gis.SpatialGrid;
import org.matsim.contrib.accessibility.interpolation.Interpolation;

/**
 * Class for testing the implemented interpolation methods visually.
 * Uses the car accessibilities of the zurich scenario at resolution 100m.
 * Reads them as SpatialGrid and interpolates them to one resolution higher.
 * 
 * @author tthunig
 *
 */
public class TestScenario {

	private static final Logger log = Logger.getLogger(TestScenario.class);
	
	//information about the given data
//	private static String filename_data100 = "zurich_carAccessibility_grid_cellsize_100m_SF";
//	private static String filename_data200 = "zurich_carAccessibility_grid_cellsize_200m_SF";
	private static String filename_data100 = "zurich_carAccessibility_grid_cellsize_100m_NW";
	private static String filename_data200 = "zurich_carAccessibility_grid_cellsize_200m_NW";
	
	private static SpatialGrid sg100;
	private static SpatialGrid sg200;

//	private static File outputFile= new File("src/main/resources/Evaluation.txt"); //TODO in resources speichern
	private static String path = "Z:/WinHome/Docs/Interpolation/zurich_test/";
	private static File outputFile= new File(path + "Evaluation_" + filename_data200 + ".txt");
	private static FileWriter out;
	
	/**
	 * reads the data from the scenario and interpolates the grid of known values to a higher resolution first with bilinear interpolation second with bicubic spline interpolation
	 * 
	 * @param args not used
	 */
	public static void main(String[] args) {
		java.net.URL input100 = ClassLoader.getSystemResource(filename_data100+".txt");
		java.net.URL input200 = ClassLoader.getSystemResource(filename_data200+".txt");
		
		log.info("Reading data...");
		sg100 = SpatialGrid.readFromFile(input100.getFile());		
		sg200 = SpatialGrid.readFromFile(input200.getFile());
		
		try {
			out= new FileWriter(outputFile);
			out.write("interpolation method \t\t\t\t interp. time \t sum of abs. differences \t rel. difference \n");
			out.write("-----------------------------------------------------------------------------------------\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("Start interpolation of file " + filename_data200 + " with the different interpolation methods:");
		testOneMethod(Interpolation.BILINEAR, Double.NaN);
		testOneMethod(Interpolation.BICUBIC, Double.NaN);		
		for (int e=1; e<=12; e++){
			testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, e);
		}
		
		try {
			out.write("\nRemark: The interpolation difference is calculated to known data at the same resolution.\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("interpolation test done");
	}
	
	private static void testOneMethod(int interpolationMethod, double exponent){
		
		log.info("Interpolate file " + filename_data200 + " with interpolation method " + interpolationMethod +":");
		long startTime= System.currentTimeMillis();
		Interpolation interpolation = new Interpolation(sg200, interpolationMethod, exponent);
		SpatialGrid interp_sg = new SpatialGrid(sg200.getXmin(), sg200.getYmin(), sg200.getXmax(), sg200.getYmax(), sg200.getResolution() / 2, Double.NaN);
		// calculate new values for higher resolution
		for (double y = sg200.getYmin(); y <= sg200.getYmax(); y += sg200.getResolution()/2) {
			for (double x = sg200.getXmin(); x <= sg200.getXmax(); x += sg200.getResolution()/2) {
				interp_sg.setValue(interpolation.interpolate(x, y), x, y);
			}
		}
		long interpolationTime= System.currentTimeMillis()-startTime;
		
		log.info("Writing interpolated data...");
		if (interpolationMethod == 2)
			interp_sg.writeToFile(path + filename_data200 + "_" + interpolationMethod + "_exp" + exponent + ".txt"); //TODO in resources speichern
		else
			interp_sg.writeToFile(path + filename_data200 + "_" + interpolationMethod + ".txt"); //TODO in resources speichern
		
		log.info("Computation of interpolation differences...");
		double[] difference = differenceComputation(sg100, interp_sg);
		
		String evalMethod= "";
		switch (interpolationMethod){
			case 0: evalMethod= "bilinear interpolation \t\t\t"; break;
			case 1: evalMethod= "bicubic spline interpolation \t"; break;
			case 2: evalMethod= "idw with exp " + exponent;
		}
		String eval= evalMethod +  "\t\t" + interpolationTime + " ms \t\t\t" + Math.round(difference[0]*100)/100. + "\t\t\t\t\t\t" + Math.round((difference[0]/difference[1])*10000)/10000.;// + "\t\t\t\t\t" + Math.round(difference[1]*100)/100. + "\t\t\t\t\t\t" + Math.round((difference[1]/difference[2])*10000)/10000.;
		System.out.println("Evaluation: " + eval);
		try {
			out.write(eval+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Compares the differences between the interpolated SpatialGrid with resolution 200 and the original SpatialGrid with resolution 100.
	 * Sums the absolute difference and returns it.
	 * 
	 * @param sg100 the original SpatialGrid with resolution 100
	 * @param sg200_interpolated the interpolated SpatialGrid with resolution 200
	 * @return the absolute difference between them and the number of interpolated values to calculate the relative difference
	 */
	private static double[] differenceComputation(SpatialGrid sg100, SpatialGrid sg200_interpolated){
		double differenceToOriginalSG = 0;
		int numberOfIntpValues = 0;
		
		//sum difference at all coordinates where interpolated values are known
		for (double y = sg200_interpolated.getYmin(); y <= sg200_interpolated.getYmax(); y += sg200_interpolated.getResolution()){
			for (double x = sg200_interpolated.getXmin(); x <= sg200_interpolated.getXmax(); x += sg200_interpolated.getResolution()){
				
				double value100= sg100.getValue(x, y);
				double value200= sg200_interpolated.getValue(x, y);
				
				//calculate difference only in the zurich area
				if(!Double.isNaN(value100) && !Double.isNaN(value200)){
					
					differenceToOriginalSG += Math.abs(value100 - value200);
					numberOfIntpValues++;
				}
			}
		}
		double[] differences= {differenceToOriginalSG, numberOfIntpValues};
		return differences;
	}

}
