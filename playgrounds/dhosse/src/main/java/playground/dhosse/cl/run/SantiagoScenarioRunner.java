package playground.dhosse.cl.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.otfvis.OTFVis;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;


public class SantiagoScenarioRunner {

	private static String inputPath = "C:/Users/Daniel/Documents/work/runs-svn/santiago/input/new/";
//	private static String inputPath = "C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/runs/MATSimParameters/routeChoiceAndModeChoice/input/";
	
//	private static String inputPath = "C:/Users/dhosse/workspace/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/runs/alejandros_parameters/routeChoiceOnly/input/";
//	private static String inputPath = "C:/Users/Daniel/Documents/work/shared-svn/studies/countries/cl/Kai_und_Daniel/inputFiles/runs/alejandros_parameters/routeChoiceAndModeChoice/input/";
	
	public static void main(String args[]){
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, inputPath + "config_tastePars_rCmC.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.failIfDirectoryExists);
		Controler controler = new Controler(scenario);
		
		controler.getEvents().addHandler(new PTFlatFareHandler(controler));
		
		controler.run();
		
//		OTFVis.playMVI(inputPath + "vis.mvi");
//		OTFVis.convert(new String[]{
//					"",
//					"C:/Users/Daniel/Documents/work/runs-svn/santiago/output/new/ITERS/it.100/100.events.xml.gz",
//					inputPath + "santiago_secondary.xml.gz",
//					inputPath + "vis.mvi",
//					"60"
//		});
		
	}
	
}
