package playground.dziemke.accessibility;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class CreateVisualOutput {

	public static void main(String[] args) {
		// Input and output
//		String workingDirectory = "../../accessibility-sa/data/18/";
		String workingDirectory = "../../../../Workspace/data/accessibility/nairobi/output/02/";
		String networkFile = "../../../../Workspace/data/accessibility/nairobi/network/2015-11-05_kibera_paths_detailed.xml";

		// Parameters
//		String actType = "composite";
		String actType = "drinking_water";
//		boolean includeDensityLayer = true;
		boolean includeDensityLayer = false;
//		String crs = TransformationFactory.WGS84_SA_Albers;
		String crs = "EPSG:21037";
		Double lowerBound = 0.;
		Double upperBound = 3.5;
		Integer range = 9;
//		int symbolSize = 205;
//		int cellSize = 200;
		int symbolSize = 105;
		int cellSize = 100;
//		double[] mapViewExtent = {100000,-3720000,180000,-3675000};
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		
		
		// set mapViewExtent
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		BoundingBox boundingBox = BoundingBox.createBoundingBox(scenario.getNetwork());
		double xMin = boundingBox.getXMin();
		double xMax = boundingBox.getXMax();
		double yMin = boundingBox.getYMin();
		double yMax = boundingBox.getYMax();
		double[] mapViewExtent = {xMin, yMin, xMax, yMax};
		
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