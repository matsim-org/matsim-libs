/**
 * 
 */
package scenarios.braess.conversion;

import java.util.Calendar;

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
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/signalSystems.xml";
		String signalGroupsFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/signalGroups.xml";
		String signalControlFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/signalControl_green.xml";
		String networkFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/network.xml";
		String lanesFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/trivialLanes.xml.gz";
		String populationFilename = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/scenarios/braess_scenario/plans3600_woInitRoutes.xml";

		// output files
		String outputDirectory = DgPaths.REPOS
				+ "shared-svn/projects/cottbus/data/optimization/braess2ks/";
		
		// get the current date in format "yyyy-mm-dd"
		Calendar cal = Calendar.getInstance();
		// this class counts months from 0, but days from 1
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month + "";
		if (month < 10)
			monthStr = "0" + month;
		String date = cal.get(Calendar.YEAR) + "-"	+ monthStr + "-" + cal.get(Calendar.DAY_OF_MONTH);		
				
		/* parameters for the time interval */
		double startTime = 8 * 3600.0;
		double endTime = 9 * 3600.0;
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
		String scenarioDescription = "run braess with 3600 agents";

		TtMatsim2KS2015.convertMatsim2KS(signalSystemsFilename,
				signalGroupsFilename, signalControlFilename, networkFilename,
				lanesFilename, populationFilename, startTime, endTime,
				signalsBoundingBoxOffset, cuttingBoundingBoxOffset,
				freeSpeedFilter, useFreeSpeedTravelTime, maximalLinkLength,
				matsimPopSampleSize, ksModelCommoditySampleSize,
				minCommodityFlow, cellsX, cellsY, scenarioDescription,
				date, outputDirectory);
	}
}
