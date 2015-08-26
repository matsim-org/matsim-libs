package playground.dhosse.gap.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.analysis.SpatialAnalysis;

public class GAPScenarioRunner {

	private static final String inputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/INPUT/";
	private static final String simInputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/run5/input/";
	private static final String outputPath = "/run/user/1009/gvfs/smb-share:server=innoz-dc01,share=innoz/2_MediengestützteMobilität/10_Projekte/eGAP/30_Modellierung/OUTPUT/" + Global.runID + "/ouput_/";
	
	public static void main(String args[]){
		
		runScenario();
		
//		runAnalysis();
		
	}

	private static void runScenario() {
		
		Config config = ConfigUtils.createConfig();
		ConfigUtils.loadConfig(config, simInputPath + "config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.run();
		
	}
	
	private static void runAnalysis() {
		
		SpatialAnalysis.writePopulationToShape(inputPath + "Pläne/plans_mid.xml.gz", "/home/dhosse/Dokumente/01_eGAP/MATSim_input/pop2.shp");
	
	}
	
}
