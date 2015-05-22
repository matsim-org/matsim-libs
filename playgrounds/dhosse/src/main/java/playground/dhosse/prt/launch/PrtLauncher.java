package playground.dhosse.prt.launch;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

import org.matsim.core.controler.OutputDirectoryHierarchy;
import playground.dhosse.prt.PrtConfigGroup;
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
		PrtConfigGroup pcg = new PrtConfigGroup();
		config.addModule(pcg);
		ConfigUtils.loadConfig(config, args[0]);
		
		Controler controler = new Controler(config);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		PrtModule module = new PrtModule();
		module.configureControler(controler);
		controler.run();

	}

}
