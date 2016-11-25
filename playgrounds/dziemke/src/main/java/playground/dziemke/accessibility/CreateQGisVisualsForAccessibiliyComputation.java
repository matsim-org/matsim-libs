package playground.dziemke.accessibility;

import com.vividsolutions.jts.geom.Envelope;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.accessibility.Modes4Accessibility;
import org.matsim.contrib.accessibility.utils.VisualizationUtils;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;

/**
 * This is a starter class to create visual QGis-based output from a readily-done accessibility computation
 * where the accessibilities.csv file already exists
 * 
 * @author dziemke
 */
public class CreateQGisVisualsForAccessibiliyComputation {

	public static void main(String[] args) {
		// Input and output
		String workingDirectory = "../../../shared-svn/projects/maxess/data/nmb/output/34a/";
		String networkFile = "../../../../Workspace/data/accessibility/nairobi/network/2015-11-05_kibera_paths_detailed.xml";

		// Parameters
		String actType = "s";
//		String crs = "EPSG:21037";
		String crs = TransformationFactory.WGS84_SA_Albers;
		
		// QGis
		boolean includeDensityLayer = true;
		Double lowerBound = 0.;
		Double upperBound = 3.5;
		Integer range = 9;
		int symbolSize = 1010;
		int cellSize = 1000;
		int populationThreshold = (int) (200 / (1000/cellSize * 1000/cellSize));
		Envelope envelope = new Envelope(115000,-3718000,161000,-3679000); // NMB

		// Set mapViewExtent by bounding box around network
//		Network network = NetworkUtils.createNetwork();
//		MatsimNetworkReader networkReader = new MatsimNetworkReader(network);
//		networkReader.readFile(networkFile);
//		BoundingBox boundingBox = BoundingBox.createBoundingBox(network);
//		double[] mapViewExtent = {boundingBox.getXMin(), boundingBox.getYMin(), boundingBox.getXMax(), boundingBox.getYMax()};
		
		// create...
		String osName = System.getProperty("os.name");
		String actSpecificWorkingDirectory =  workingDirectory + actType + "/";
		
		for ( Modes4Accessibility modeOld : Modes4Accessibility.values()) {
			String mode = modeOld.toString();
			VisualizationUtils.createQGisOutput(actType, mode, envelope, workingDirectory, crs, includeDensityLayer,
					lowerBound, upperBound, range, symbolSize, populationThreshold);
			VisualizationUtils.createSnapshot(actSpecificWorkingDirectory, mode, osName);
		}
	}
}