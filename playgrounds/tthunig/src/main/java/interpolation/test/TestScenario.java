package interpolation.test;

import interpolation.Interpolation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import playground.tnicolai.matsim4opus.gis.SpatialGrid;

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
//	private static String filename_data100 = "zurich_carAccessibility_grid_cellsize_100m_SF";
//	private static String filename_data200 = "zurich_carAccessibility_grid_cellsize_200m_SF";
	private static String filename_data100 = "zurich_carAccessibility_grid_cellsize_100m_NW";
	private static String filename_data200 = "zurich_carAccessibility_grid_cellsize_200m_NW";
	
	private static SpatialGrid sg100;
	private static SpatialGrid sg200;
	
//	private static File outputFile= new File("src/main/resources/Evaluation.txt"); TODO
	private static File outputFile= new File("Z:/WinHome/Docs/Interpolation/zurich_test/Evaluation_" + filename_data200 + ".txt");
	private static FileWriter out;
	
//	//variables to save comparison results
//	private static double interpolationTime_bilinear = Double.MAX_VALUE;
//	private static double interpolationTime_bicubic = Double.MAX_VALUE;
//	private static double interpolationTime_idw = Double.MAX_VALUE;
//	private static double[] difference_bilinear;
//	private static double[] difference_bicubic;
//	private static double[] difference_idw_all_exp1;
//	private static double[] difference_idw_all_exp2;
//	private static double[] difference_idw_all_exp5;
//	private static double[] difference_idw_all_exp6;
//	private static double[] difference_idw_all_exp7;
//	private static double[] difference_idw_four_exp1;
//	private static double[] difference_idw_four_exp2;
//	private static double[] difference_idw_four_exp5;
//	private static double[] difference_idw_four_exp6;
//	private static double[] difference_idw_four_exp7;
	
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
			out.write("interpolation method \t\t\t\t interp. time \t sum of abs. differences \t rel. abs. difference \t sum of quadr. differences \t rel. quadr. difference\n");
			out.write("----------------------------------------------------------------------------------------------------------------------------------------------------\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("Start interpolation of file " + filename_data200 + " with the different interpolation methods:");
		testOneMethod(Interpolation.BILINEAR, true, Double.NaN);
		testOneMethod(Interpolation.BICUBIC, true, Double.NaN);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 1.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 2.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 3.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 4.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 5.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 6.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 7.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 1.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 2.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 3.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 4.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 5.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 6.);
		testOneMethod(Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 7.);
		
		try {
			out.write("\nRemark: The interpolation difference is calculated to known data at the same resolution.\n");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		log.info("interpolation test done");
		
		//TODO: raus
//		log.info("Interpolate file " + filename_data200 + ":");
//		SpatialGrid sg200_bilinear= interpolateSG(sg200, Interpolation.BILINEAR);
//		SpatialGrid sg200_bicubic= interpolateSG(sg200, Interpolation.BICUBIC);
//		SpatialGrid sg200_idw_all_exp1= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 1.);
//		SpatialGrid sg200_idw_all_exp2= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 2.);
//		SpatialGrid sg200_idw_all_exp5= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 5.);
//		SpatialGrid sg200_idw_all_exp6= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 6.);
//		SpatialGrid sg200_idw_all_exp7= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, true, 7.);
//		SpatialGrid sg200_idw_four_exp1= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 1.);
//		SpatialGrid sg200_idw_four_exp2= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 2.);
//		SpatialGrid sg200_idw_four_exp5= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 5.);
//		SpatialGrid sg200_idw_four_exp6= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 6.);
//		SpatialGrid sg200_idw_four_exp7= interpolateSG(sg200, Interpolation.INVERSE_DISTANCE_WEIGHTING, false, 7.);
//		log.info("Interpolation of file " + filename_data200 + " is completed.");
//		
//		log.info("Writing interpolated data...");
//		sg200_bilinear.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.BILINEAR+ ".txt"); //TODO in resources speichern
//		sg200_bicubic.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.BICUBIC+ ".txt"); //TODO in resources speichern
//		sg200_idw_all_exp1.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_all_exp1.txt"); //TODO in resources speichern
//		sg200_idw_all_exp2.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_all_exp2.txt"); //TODO in resources speichern
//		sg200_idw_all_exp5.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_all_exp5.txt"); //TODO in resources speichern
//		sg200_idw_all_exp6.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_all_exp6.txt"); //TODO in resources speichern
//		sg200_idw_all_exp7.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_all_exp7.txt"); //TODO in resources speichern
//		sg200_idw_four_exp1.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_four_exp1.txt"); //TODO in resources speichern
//		sg200_idw_four_exp2.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_four_exp2.txt"); //TODO in resources speichern
//		sg200_idw_four_exp5.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_four_exp5.txt"); //TODO in resources speichern
//		sg200_idw_four_exp6.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_four_exp6.txt"); //TODO in resources speichern
//		sg200_idw_four_exp7.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + Interpolation.INVERSE_DISTANCE_WEIGHTING+ "_four_exp7.txt"); //TODO in resources speichern
//		
//		log.info("Computation of interpolation differences of the different methods...");
//		difference_bilinear = differenceComputation(sg100, sg200_bilinear);
//		difference_bicubic = differenceComputation(sg100, sg200_bicubic);
//		difference_idw_all_exp1 = differenceComputation(sg100, sg200_idw_all_exp1);
//		difference_idw_all_exp2 = differenceComputation(sg100, sg200_idw_all_exp2);
//		difference_idw_all_exp5 = differenceComputation(sg100, sg200_idw_all_exp5);
//		difference_idw_all_exp6 = differenceComputation(sg100, sg200_idw_all_exp6);
//		difference_idw_all_exp7 = differenceComputation(sg100, sg200_idw_all_exp7);
//		difference_idw_four_exp1 = differenceComputation(sg100, sg200_idw_four_exp1);
//		difference_idw_four_exp2 = differenceComputation(sg100, sg200_idw_four_exp1);
//		difference_idw_four_exp5 = differenceComputation(sg100, sg200_idw_four_exp1);
//		difference_idw_four_exp6 = differenceComputation(sg100, sg200_idw_four_exp1);
//		difference_idw_four_exp7 = differenceComputation(sg100, sg200_idw_four_exp1);
		
//		log.info("Comparison of interpolation differences and interpolation time of the different methods:");
//		System.out.println("interpolation method \t\t" + "interpolation time \t" + "sum of absolute interpolation difference to known data at the same resolution" + "relative absolute difference" + "sum of quadratic difference" + "relative quadratic difference");
//		System.out.println("-----------------------------------------------------------------------------------------------------");
//		System.out.println("bilinear interpolation \t\t\t" + interpolationTime_bilinear + "ms \t\t\t" + difference_bilinear[0] + "\t\t" + difference_bilinear[0]/difference_bilinear[2] + "\t\t" + difference_bilinear[1] + "\t\t" + difference_bilinear[1]/difference_bilinear[2]);
//		System.out.println("bicubic interpolation \t\t\t" + interpolationTime_bicubic + "ms \t\t" + difference_bicubic[0] + "\t\t" + difference_bicubic[0]/difference_bicubic[2] + "\t\t" + difference_bicubic[1] + "\t\t" + difference_bicubic[1]/difference_bicubic[2]);
//		System.out.println("idw with all neighbors and exp 1 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_all_exp1[0] + "\t\t" + difference_idw_all_exp1[0]/difference_idw_all_exp1[2] + "\t\t" + difference_idw_all_exp1[1] + "\t\t" + difference_idw_all_exp1[1]/difference_idw_all_exp1[2]);
//		System.out.println("idw with all neighbors and exp 2 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_all_exp2[0] + "\t\t" + difference_idw_all_exp2[0]/difference_idw_all_exp2[2] + "\t\t" + difference_idw_all_exp2[1] + "\t\t" + difference_idw_all_exp2[1]/difference_idw_all_exp2[2]);
//		System.out.println("idw with all neighbors and exp 5 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_all_exp5[0] + "\t\t" + difference_idw_all_exp5[0]/difference_idw_all_exp5[2] + "\t\t" + difference_idw_all_exp5[1] + "\t\t" + difference_idw_all_exp5[1]/difference_idw_all_exp5[2]);
//		System.out.println("idw with all neighbors and exp 6 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_all_exp6[0] + "\t\t" + difference_idw_all_exp6[0]/difference_idw_all_exp6[2] + "\t\t" + difference_idw_all_exp6[1] + "\t\t" + difference_idw_all_exp6[1]/difference_idw_all_exp6[2]);
//		System.out.println("idw with all neighbors and exp 7 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_all_exp7[0] + "\t\t" + difference_idw_all_exp7[0]/difference_idw_all_exp7[2] + "\t\t" + difference_idw_all_exp7[1] + "\t\t" + difference_idw_all_exp7[1]/difference_idw_all_exp7[2]);
//		System.out.println("idw with four neighbors and exp 1 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_four_exp1[0] + "\t\t" + difference_idw_four_exp1[0]/difference_idw_four_exp1[2] + "\t\t" + difference_idw_four_exp1[1] + "\t\t" + difference_idw_four_exp1[1]/difference_idw_four_exp1[2]);
//		System.out.println("idw with four neighbors and exp 2 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_four_exp2[0] + "\t\t" + difference_idw_four_exp2[0]/difference_idw_four_exp2[2] + "\t\t" + difference_idw_four_exp2[1] + "\t\t" + difference_idw_four_exp2[1]/difference_idw_four_exp2[2]);
//		System.out.println("idw with four neighbors and exp 5 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_four_exp5[0] + "\t\t" + difference_idw_four_exp5[0]/difference_idw_four_exp5[2] + "\t\t" + difference_idw_four_exp5[1] + "\t\t" + difference_idw_four_exp5[1]/difference_idw_four_exp5[2]);
//		System.out.println("idw with four neighbors and exp 6 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_four_exp6[0] + "\t\t" + difference_idw_four_exp6[0]/difference_idw_four_exp6[2] + "\t\t" + difference_idw_four_exp6[1] + "\t\t" + difference_idw_four_exp6[1]/difference_idw_four_exp6[2]);
//		System.out.println("idw with four neighbors and exp 7 \t" + interpolationTime_idw + "ms \t\t" + difference_idw_four_exp7[0] + "\t\t" + difference_idw_four_exp7[0]/difference_idw_four_exp7[2] + "\t\t" + difference_idw_four_exp7[1] + "\t\t" + difference_idw_four_exp7[1]/difference_idw_four_exp7[2]);
//		
	}
	
	private static void testOneMethod(int interpolationMethod, boolean allNeighbors, double exponent){
		String neighbors;
		if (allNeighbors)
			neighbors= "all";
		else
			neighbors= "four";
		
		log.info("Interpolate file " + filename_data200 + " with interpolation method " + interpolationMethod +":");
		long startTime= System.currentTimeMillis();
		Interpolation interpolation = new Interpolation(sg200, interpolationMethod, allNeighbors, exponent);
		SpatialGrid interp_sg = new SpatialGrid(sg200.getXmin(), sg200.getYmin(), sg200.getXmax(), sg200.getYmax(), sg200.getResolution() / 2);
		// calculate new values for higher resolution
		for (double y = sg200.getYmin(); y <= sg200.getYmax(); y += sg200.getResolution()/2) {
			for (double x = sg200.getXmin(); x <= sg200.getXmax(); x += sg200.getResolution()/2) {
				interp_sg.setValue(interpolation.interpolate(x, y), x, y);
			}
		}
		long interpolationTime= System.currentTimeMillis()-startTime;
		
		log.info("Writing interpolated data...");
		if (interpolationMethod == 2)
			interp_sg.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + interpolationMethod + "_" + neighbors + "_exp" + exponent + ".txt"); //TODO in resources speichern
		else
			interp_sg.writeToFile("Z:/WinHome/Docs/Interpolation/zurich_test/" + filename_data200 + "_" + interpolationMethod + ".txt"); //TODO in resources speichern
		
		log.info("Computation of interpolation differences...");
		double[] difference = differenceComputation(sg100, interp_sg);
		
		//TODO: Auswertungszeile in .txt schreiben
		String evalMethod= "";
		switch (interpolationMethod){
			case 0: evalMethod= "bilinear interpolation \t\t\t"; break;
			case 1: evalMethod= "bicubic spline interpolation \t"; break;
			case 2: evalMethod= "idw with " + neighbors + " neighbors and exp " + exponent;
		}
		String eval= evalMethod +  "\t\t" + interpolationTime + " ms \t\t\t" + Math.round(difference[0]*100)/100. + "\t\t\t\t\t\t" + Math.round((difference[0]/difference[2])*10000)/10000. + "\t\t\t\t\t" + Math.round(difference[1]*100)/100. + "\t\t\t\t\t\t" + Math.round((difference[1]/difference[2])*10000)/10000.;
		System.out.println("Evaluation: " + eval);
		try {
			out.write(eval+"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	/**
//	 * Interpolates the grid with one chosen interpolation method to one resolution higher.
//	 * Writes the interpolated SpatialGrid out.
//	 * 
//	 * @param sg
//	 * @param interpolationMethod
//	 * @param allNeighbors only necessary, if interpolationMethod is 2 (inverse distance weighting). Sets whether inverse distance weighting with all or four neighbors should be used.
//	 * @param exponent only necessary, if interpolationMethod is 2 (inverse distance weighting). Sets the exponent for the inverse distance weighting.
//	 */
//	private static SpatialGrid interpolateSG(SpatialGrid sg, int interpolationMethod, boolean allNeighbors , double exponent){
//		long startTime= System.currentTimeMillis();
//		Interpolation interpolation = new Interpolation(sg, interpolationMethod, allNeighbors, exponent);
//		
//		SpatialGrid sg_new = new SpatialGrid(sg.getXmin(), sg.getYmin(), sg.getXmax(), sg.getYmax(), sg.getResolution() / 2);
//		// calculate new values for higher resolution
//		for (double y = sg.getYmin(); y <= sg.getYmax(); y += sg.getResolution()/2) {
//			for (double x = sg.getXmin(); x <= sg.getXmax(); x += sg.getResolution()/2) {
//				sg_new.setValue(interpolation.interpolate(x, y), x, y);
//			}
//		}
//		//TODO: zeit zurückgeben. wird dort eingetragen
//		switch(interpolationMethod){
//			case 0: interpolationTime_bilinear= System.currentTimeMillis()-startTime;
//			case 1: interpolationTime_bicubic= System.currentTimeMillis()-startTime;
//			case 2: if (allNeighbors){
//						interpolationTime_idw_all= System.currentTimeMillis()-startTime;
//					}else{
//						interpolationTime_idw_four= System.currentTimeMillis()-startTime;
//					}
//		}
//		
//		return sg_new;
//	}
	
	/**
	 * Compares the differences between the interpolated SpatialGrid with resolution 200 and the original SpatialGrid with resolution 100.
	 * Sums the absolute difference and returns it.
	 * 
	 * @param sg100 the original SpatialGrid with resolution 100
	 * @param sg200_interpolated the interpolated SpatialGrid with resolution 200
	 * @return the absolute and quadratic difference between them and the number of interpolated values to calculate the relative difference
	 */
	private static double[] differenceComputation(SpatialGrid sg100, SpatialGrid sg200_interpolated){
		double differenceToOriginalSG = 0;
		double quadDiffToOriginalSG = 0;
		int numberOfIntpValues = 0;
		//sum difference at all coordinates where interpolated values are known
		for (double y = sg200_interpolated.getYmin(); y <= sg200_interpolated.getYmax(); y += sg200_interpolated.getResolution()){
			for (double x = sg200_interpolated.getXmin(); x <= sg200_interpolated.getXmax(); x += sg200_interpolated.getResolution()){
				double value100= sg100.getValue(x, y);
				double value200= sg200_interpolated.getValue(x, y);
				//calculate difference only in the zurich area
				if(!Double.isNaN(value100) && !Double.isNaN(value200)){
					differenceToOriginalSG += Math.abs(value100 - value200);
					quadDiffToOriginalSG += (value100 - value200)*(value100 - value200);
					numberOfIntpValues++;
				}
			}
		}
		double[] differences= {differenceToOriginalSG, quadDiffToOriginalSG, numberOfIntpValues};
		return differences;
	}

}
