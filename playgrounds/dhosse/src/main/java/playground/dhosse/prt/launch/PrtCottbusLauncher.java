package playground.dhosse.prt.launch;

import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.controler.Controler;

public class PrtCottbusLauncher {
	
	public static void main(String args[]){
		
		String configFileName = "C:/Users/Daniel/Desktop/dvrp/"+
				"cottbus_scenario/config.xml";
		
		Controler controler = new Controler(configFileName);
		controler.setOverwriteFiles(true);
		controler.run();
		
//		OTFVis.playMVI("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/out/ITERS/it.0/1212.0.otfvis.mvi");
		
//		Config config = ConfigUtils.createConfig();
//		ConfigUtils.loadConfig(config, configFileName);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//		Controler controler = new Controler(scenario);
//		controler.setOverwriteFiles(true);
//		controler.run();
		
	}
	
}
