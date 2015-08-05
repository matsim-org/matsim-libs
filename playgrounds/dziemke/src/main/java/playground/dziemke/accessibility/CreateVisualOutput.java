package playground.dziemke.accessibility;

import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class CreateVisualOutput {

	public static void main(String[] args) {
		// Input and output
		String workingDirectory = "../../accessibility-sa/data/18/";

		// Parameters
		String actType = "composite";
		boolean includeDensityLayer = true;
		String crs = TransformationFactory.WGS84_SA_Albers;
		Double lowerBound = 0.;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 205;
		int cellSize = 200;
		double[] mapViewExtent = {100000,-3720000,180000,-3675000};
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
		
		// create...
		String osName = System.getProperty("os.name");
		String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
		
		for ( Modes4Accessibility mode : Modes4Accessibility.values()) {

			VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
					lowerBound, upperBound, range, symbolSize, populationThreshold);
			VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
		}

	}

}
