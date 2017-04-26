package playground.dhosse.prt.launch;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.controler.*;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dhosse.prt.PrtModule;

public class PrtLauncher {
	
	private final static Logger log = Logger.getLogger(PrtLauncher.class);
	
	public static void main(String args[]){
		
//		OTFVis.playMVI("C:/Users/Daniel/Desktop/dvrp/cottbus_scenario/NOS/ITERS/it.30/prt.30.otfvis.mvi");
		
		if(args.length == 0){
			log.info("Input config file (arg[0] equals null. Aborting...");
			System.exit(1);
		}
		
		Config config = ConfigUtils.createConfig();
		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		config.addModule(taxiCfg);
		config.addModule(new DvrpConfigGroup());
		ConfigUtils.loadConfig(config, args[0]);
		
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		PrtModule module = new PrtModule();
		module.configureControler(controler, null);
		controler.run();

	}

}
