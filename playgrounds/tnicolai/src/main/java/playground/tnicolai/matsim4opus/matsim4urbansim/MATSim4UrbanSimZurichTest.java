package playground.tnicolai.matsim4opus.matsim4urbansim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.scenario.ZurichUtilities;

/**
 * Potential MATSim input files (from shared-svn/studies/schweiz-ivtch/baseCase/)
 * Network: network/ivtch-changed.xml
 * Persons: plans/plans_complete/plans.xml
 * Facilities: facilities/facilities.xml
 * 
 * @author thomas
 */

public class MATSim4UrbanSimZurichTest extends MATSim4UrbanSim{
	
	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimZurichTest.class);
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimZurichTest(String args[]){
		super(args);
	}
	
	/**
	 * This modifies the MATSim network according to the given
	 * test parameter in the MATSim config file (from UrbanSim)
	 */
	@Override
	void modifyNetwork(NetworkImpl network){
		log.info("");
		log.info("Checking for network modifications ...");
		// check given test parameter for desired modifications
		String testParameter = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TEST_PARAMETER_PARAM);
		if(testParameter.equals("")){
			log.info("No modifications to perform.");
			log.info("");
			return;
		}
		else{
			String scenarioArray[] = testParameter.split(",");
			ZurichUtilities.modifyNetwork(network, scenarioArray);
			log.info("Done modifying network.");
			log.info("");
		}
	}
	
	/**
	 * This removes plan elements from existing plans that
	 * contain a removed link
	 */
	@Override
	void modifyPopulation(Population population){
		ZurichUtilities.deleteRoutesContainingRemovedLinks(population);
	}
	
	/**
	 * Entry point
	 * @param args urbansim command prompt
	 */
	public static void main(String args[]){
		MATSim4UrbanSimZurichTest zurichTest = new MATSim4UrbanSimZurichTest(args);
		zurichTest.runMATSim();
		MATSim4UrbanSimZurichTest.isSuccessfulMATSimRun = Boolean.TRUE;
	}
}
