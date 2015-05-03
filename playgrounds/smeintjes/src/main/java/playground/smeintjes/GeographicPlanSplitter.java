package playground.smeintjes;

import playground.southafrica.utilities.Header;

/**
 * This class reads in multiple freight populations, each with 120,000 plans (vehicles).
 * For each population, for each plan: if one or more activities were performed in Gauteng, this plan is
 * classified as a "Gauteng" plan; the same is done for eThekwini and Cape Town. 
 * @author sumarie
 *
 */
public class GeographicPlanSplitter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GeographicPlanSplitter.class.toString(), args);
		String inputPopulationFolder = args[0];
		String capeTownShapefile = args[1];
		String eThekwiniShapefile = args[2];
		String gautengShapefile = args[3];
		int pmin = Integer.parseInt(args[4]);
		int radius = Integer.parseInt(args[5]);
		int numberRuns = Integer.parseInt(args[6]);
		int numberPopulations = Integer.parseInt(args[7]);
		
		classifyPlans(inputPopulationFolder, capeTownShapefile);
		Header.printFooter();
	}

	private static void classifyPlans(String inputPopulationFolder,
			String capeTownShapefile) {
		
		
		
		
	}

}
