package playground.dhosse.cl.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


public class SantiagoScenarioRunner {

//	private static String inputPath = "C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/runs/MATSimParameters/routeChoiceOnly/input/";
//	private static String inputPath = "C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/runs/MATSimParameters/routeChoiceAndModeChoice/input/";
	
	private static String inputPath = "../../runs-svn/santiago/run7/input/";
//	private static String inputPath = "C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/runs/alejandros_parameters/routeChoiceAndModeChoice/input/";
	
	private static String outputPath = "../../runs-svn/santiago/run7/output/";
	
	public static void main(String args[]){
		
//		OTFVis.convert(new String[]{
//						"",
//						outputPath + "modeChoice.output_events.xml.gz",	//events
//						outputPath + "modeChoice.output_network.xml.gz",	//network
//						outputPath + "visualisation.mvi", 		//mvi
//						"60" 									//snapshot period
//		});
//		
//		OTFVis.playMVI(outputPath + "visualisation.mvi");
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, inputPath + "config_final.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		Controler controler = new Controler(scenario);
		
		controler.getEvents().addHandler(new PTFlatFareHandler(controler));
		
		controler.run();
		
	}
	
}