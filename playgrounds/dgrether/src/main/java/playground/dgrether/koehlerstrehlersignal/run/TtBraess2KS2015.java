/**
 * 
 */
package playground.dgrether.koehlerstrehlersignal.run;

import playground.dgrether.DgPaths;

/**
 * @author tthunig
 * 
 */
public class TtBraess2KS2015 {

	public static void main(String[] args) throws Exception {
		// input files
		String signalSystemsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/braess_scenario/signalSystems_v2.0.xml";
		String signalGroupsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/braess_scenario/signalGroups_v2.0.xml";
		String signalControlFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/braess_scenario/signalControl_v2.0.xml";
		String networkFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/braess_scenario/network.xml";
		String lanesFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/braess_scenario/laneDefinitions_v2.0.xml";
		String populationFilename = DgPaths.REPOS
				+ "runs-svn/cottbus/braess/2015-02-23_base_case/output_plans.xml";

		// output files
		String outputDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/braess_scenario/";
		String dateFormat = "2015-02-24";

		/* parameters for the time interval */
		double startTime = 7.5 * 3600.0;
		double endTime = 8.5 * 3600.0;
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
		String scenarioDescription = "run braess output plans between 07:30 and 08:30";

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
