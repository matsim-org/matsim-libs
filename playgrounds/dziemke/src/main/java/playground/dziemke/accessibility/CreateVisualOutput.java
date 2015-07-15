package playground.dziemke.accessibility;

import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

public class CreateVisualOutput {

	public static void main(String[] args) {
		// Input and output
		String workingDirectory = "../../accessibility-sa/data/01/";

		// Parameters
		String actType = "composite4";
		boolean includeDensityLayer = true;
		String crs = TransformationFactory.WGS84_SA_Albers;
		Double lowerBound = 0.;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 1010;
		double[] mapViewExtent = {100000,-3720000,180000,-3675000};
		
		
		// create...
		String osName = System.getProperty("os.name");
		String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
		
		for ( Modes4Accessibility mode : Modes4Accessibility.values()) {

			VisualizationUtilsDZ.createQGisOutput(actType, mode, mapViewExtent, workingDirectory, crs, includeDensityLayer,
					lowerBound, upperBound, range, symbolSize);
			VisualizationUtilsDZ.createSnapshot(actSpecificWorkingDirectory, mode, osName);
		}

	}

}
