package playground.acmarmol.Avignon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.StringUtils;



public class MainAvignon {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException{
		
		final  Logger log = Logger.getLogger(MainAvignon.class);
		Config config = ConfigUtils.createConfig();
		
		String[] filenames = {"bais_1_01_02_01", "homo_1_01_02_01", "norm_1_01_02_01"};
		
		String inputBase = "C:/local/marmolea/input/Avignon/zurich_1pc/";
		String outputBase = "C:/local/marmolea/output/Avignon/zurich_1pc/";
		
		
		
		for (String file:filenames){
		
		log.info("Starting Processing file: " + file);	
			
		String[] params = StringUtils.explode(file, '_');
				
		//network		
			if (params[0].equals("norm")){
				config.setParam("network", "inputNetworkFile", inputBase + "network.xml");
			}else if (params[0].equals("homo")){
				config.setParam("network", "inputNetworkFile", inputBase + "nethomo.xml");
			}else if (params[0].equals("bais")){
				config.setParam("network", "inputNetworkFile", inputBase + "netbais.xml");
			}else
				log.error("Error with network filename", new Error());
			
		//facilities	
			if (params[1].equals("1")){
				config.setParam("facilities", "inputFacilitiesFile", inputBase +"facilities.xml");
			}else if (params[1].equals("2")){
				config.setParam("facilities", "inputFacilitiesFile", inputBase + "facilities2.xml");
			}else
				log.error("Error with facilities filename", new Error());
			
		//plans
			config.setParam("plans", "inputPlansFile", inputBase +"output_plans/run0.output_plans_" + file +".xml.gz");
		
		
		
			ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.loadScenario(config);
			
			PersonPlanInfoExtractor ppie = new PersonPlanInfoExtractor();
			ppie.run(scenario);
			
			PersonSubTourExtractor pste = new PersonSubTourExtractor();
			pste.run(scenario.getPopulation());
			TreeMap<Id, ArrayList<SubtourInfo>> subtours = pste.getPopulationSubtours();
			
			
			AvignonWriter writer = new AvignonWriter(ppie.getPlansInfo(),SubtourDistances.calcDistancesForPurpose(subtours, scenario));
			writer.setOutputDirectory( outputBase + "results_ " + file + ".txt");
			writer.write();
						
			
			log.info("Finished Processing file: " + file);	
			
		
		}//for filenames
							
		
		log.info("Finished Processing all " + filenames.length  + " files");	
		
		
	}

}
