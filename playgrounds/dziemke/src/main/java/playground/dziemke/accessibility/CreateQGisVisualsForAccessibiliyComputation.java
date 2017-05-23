package playground.dziemke.accessibility;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

import com.vividsolutions.jts.geom.Envelope;

/**
 * This is a starter class to create visual QGis-based output from a readily-done accessibility computation
 * where the accessibilities.csv file already exists
 * 
 * @author dziemke
 */
public class CreateQGisVisualsForAccessibiliyComputation {

	public static void main(String[] args) {
//		String workingDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/17neuRestrictedFile/";
//		String workingDirectory = "/Users/dominik/Workspace/matsim/contribs/integration/test/output/org/matsim/integration/daily/accessibility/AccessibilityComputationNairobiTest_-7.0-3.5_new/runAccessibilityComputation/";
		String workingDirectory = "/Users/dominik/Workspace/matsim/contribs/integration/test/output/org/matsim/integration/daily/accessibility/AccessibilityComputationNMBTest_-5.5--2.0/runAccessibilityComputation/";
		
//		int cellSize = 200;
		int cellSize = 1000;
//		final List<String> activityTypes = Arrays.asList(new String[]{"composite"});
//		final List<String> activityTypes = Arrays.asList(new String[]{"work"});
		final List<String> activityTypes = Arrays.asList(new String[]{"education"});
//		final List<String> modes = Arrays.asList(new String[]{TransportMode.car, TransportMode.bike, TransportMode.walk, "freespeed"});
		final List<String> modes = Arrays.asList(new String[]{"pt-walk"});
		
		final boolean includeDensityLayer = true;
		final Integer range = 9; // In the current implementation, this must always be 9
		final Double lowerBound = -3.5; // (upperBound - lowerBound) ideally nicely divisible by (range - 2)
		final Double upperBound = 3.5;
		final int populationThreshold = (int) (50 / (1000/cellSize * 1000/cellSize));

		String osName = System.getProperty("os.name");
		for (String actType : activityTypes) {
			String actSpecificWorkingDirectory = workingDirectory + actType + "/";
			for (String mode : modes) {
				VisualizationUtils.createQGisOutput(actType, mode, new Envelope(100000,180000,-3720000,-3675000), workingDirectory, TransformationFactory.WGS84_SA_Albers, includeDensityLayer,
						lowerBound, upperBound, range, cellSize, populationThreshold);
//				VisualizationUtils.createQGisOutput(actType, mode, new Envelope(251800.0, 258300.0, 9854300.0, 9858700.0), workingDirectory, "EPSG:21037", includeDensityLayer,
//						lowerBound, upperBound, range, cellSize, populationThreshold);
				VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
			}
		}  
	}
}