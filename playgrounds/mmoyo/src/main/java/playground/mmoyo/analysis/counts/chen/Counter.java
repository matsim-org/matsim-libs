package playground.mmoyo.analysis.counts.chen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;

import playground.yu.run.TrCtl;

/**uses Yu transit controler to have counts results**/
public class Counter {

	public static void main(String[] args) throws IOException {
		//It makes sure that "res" folder exists otherwise it won't write anything at the end
		File resFile = new File("./res/"); 
		if (!resFile.exists()){
			throw new FileNotFoundException("the resource folder -res- does not exist");
		}
		
		String configsDir;
		if(args.length>0){ 
			configsDir = args[0];
		}else{	
			configsDir ="../playgrounds/mmoyo/output/eightth/configs";
		}
		
		//TrCtl.main(new String[]{configFile});

		 //experimenting
		//ScenarioImpl scenario = new TransScenarioLoader().loadScenario(configFile); 
		//ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(configFile);
		//ScenarioImpl scenario = scenarioLoader.getScenario();
		//scenarioLoader.loadScenario();
		//TrCtl.main(new String[]{configFile}, scenario);
		
		//read many configs:
		//String configsDir = "../playgrounds/mmoyo/output/eightth/configs"; 
		File dir = new File(configsDir);
		for (String configName : dir.list()){
			String completePath= configsDir + "/" + configName;
			
			//validate that counts files exist
			Config config = new Config();
			MatsimConfigReader matsimConfigReader = new MatsimConfigReader(config);
			matsimConfigReader.readFile(completePath);
			File boardFile = new File(config.getParam("ptCounts", "inputBoardCountsFile")); 
			File occupFile = new File(config.getParam("ptCounts", "inputOccupancyCountsFile"));
			File alightFile = new File(config.getParam("ptCounts", "inputAlightCountsFile"));

			if (!boardFile.exists()){
				throw new FileNotFoundException("Can not find boarding counts file : " + boardFile.getPath());
			}
			if (!occupFile.exists()){
				throw new FileNotFoundException("Can not find occupancy counts file : " + occupFile.getPath());
			}
			if (!alightFile.exists()){
				throw new FileNotFoundException("Can not find alighting counts file : " + alightFile.getPath());
			}
			
			System.out.println("\n\n  procesing: " + completePath);
			TrCtl.main(new String[]{completePath});
		}
	}
}
