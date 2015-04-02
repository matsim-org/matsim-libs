/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.braessscenario;

import playground.dgrether.DgPaths;
import playground.dgrether.koehlerstrehlersignal.conversion.TtMatsim2KS2015;

/**
 * Class to convert the Braess scenario into KS format.
 * Uses the general conversion tool TtMatsim2KS2015.
 * 
 * @author tthunig 
 */
public class ConvertBraess2KS2015 {

	public static void main(String[] args) throws Exception {
		// input files
		String signalSystemsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/signalSystems_v2.0.xml";
		String signalGroupsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/signalGroups_v2.0.xml";
		String signalControlFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/signalControl_v2.0.xml";
		String networkFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/network_mixSoft_0s.xml";
		String lanesFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/laneDefinitions_v2.0.xml";
		String populationFilename = DgPaths.REPOS
				+ "runs-svn/cottbus/braess/2015-03-31_tbs1_netmixSoft-0s_basecase/output_plans.xml";

		// output files
		String outputDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/braess2ks/";
		String dateFormat = "2015-04-01";

		/* parameters for the time interval */
		double startTime = 8 * 3600.0;
		double endTime = 8 * 3600.0 + 1 * 60;
		/* parameters for the network area */
		double signalsBoundingBoxOffset = 500;
		double cuttingBoundingBoxOffset = 100;
		/* parameters for the interior link filter */
		double freeSpeedFilter = 1.0; // = default value
		boolean useFreeSpeedTravelTime = true; // = default value
		double maximalLinkLength = Double.MAX_VALUE; // = default value
		/* parameters for the demand filter */
		double matsimPopSampleSize = 1.0; // = default value
		double ksModelCommoditySampleSize = 1.0; // = default value
		double minCommodityFlow = 1.0; // = default value
		int cellsX = 5; // = default value
		int cellsY = 5; // = default value
		/* other parameters */
		String scenarioDescription = "run braess output plans between 08:00 and 08:01";

		TtMatsim2KS2015 converter = new TtMatsim2KS2015(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, cellsX, cellsY, scenarioDescription,
				dateFormat, outputDirectory);

		converter.convertMatsim2KS();
	}
}
