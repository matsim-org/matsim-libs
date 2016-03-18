package playground.dhosse.scenarios.generic;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryLogging;

import playground.dhosse.scenarios.generic.facilities.FacilitiesCreator;
import playground.dhosse.scenarios.generic.network.NetworkCreator;
import playground.dhosse.scenarios.generic.population.PopulationCreator;
import playground.dhosse.scenarios.generic.utils.Geoinformation;

public class ScenarioBuilder {

	private static final Logger log = Logger.getLogger(ScenarioBuilder.class);
	
	//is not meant to be instantiated
	private ScenarioBuilder(){}
	
	public static void main(String args[]){
		
		if(args.length < 1){
			
			throw new RuntimeException("No configuration given. Scenario generation aborts...");
			
		}
		
		Configuration configuration = new Configuration(args[0]);
		
		String matsimFilesDir = configuration.getWorkingDirectory() + "/matsimInput/";
		new File(matsimFilesDir).mkdirs();
		
		try {
			
			OutputDirectoryLogging.initLoggingWithOutputDirectory(matsimFilesDir);
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		log.info("########## Creating a scenario from parameters given in configuration file " + args[0]);
		
		log.info("########## network generation");
		
		NetworkCreator.main(new String[]{
				configuration.getCrs(),
				configuration.getWorkingDirectory() + configuration.getOsmFile(),
				matsimFilesDir + "network.xml.gz"
				});
		
		log.info("########## activity facilities generation");
		
		FacilitiesCreator.main(new String[]{
				configuration.getCrs(),
				configuration.getWorkingDirectory() + configuration.getOsmFile(),
				matsimFilesDir + "facilities.xml.gz",
				matsimFilesDir + "facilityAttributes.xml.gz"
		});
		
		log.info("########## creating transit schedule");
		//TODO pt
		
		log.info("########## loading administrative borders");
		//TODO load admin borders
		//TODO load adjacency matrix
		Set<String> filterIds = new HashSet<>();
		
		//first, add the survey area id(s)
		for(String id : configuration.getSurveyAreaIds()){
			
			filterIds.add(id);
			
		}
		
		try {
			
			Geoinformation.readGeodataFromDatabase(filterIds);
			
		} catch (Exception e) {
			
			e.printStackTrace();
			
		}
		
		log.info("########## demand generation");
		
		if(configuration.getPopulationType() != null){
			
			PopulationCreator.run(configuration);
			
		} else {
			
			log.warn("Population type was not defined.");
			log.warn("No population will be created.");
			
		}
		
		log.info("########## Scenario created!");
		
		OutputDirectoryLogging.closeOutputDirLogging();
		
	}
	
}
