package tutorial.adaptiveSignals;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.binder.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.otfvis.OTFVisWithSignalsLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * This example starts the simulation of a single crossing scenario where the
 * intersection is signalized by traffic-adaptive signals. The logic of the
 * traffic-adaptive signals is based on the basic version of Stefan Laemmer from
 * his phd from 2007 and can be found in LaemmerSignalController.
 * 
 * If you want to use other implementations of traffic-adaptive signals you have
 * to adjust the identifiers in the signal control file and add a provider class
 * for your specific implementation to the SignalsModule (see comment below).
 * 
 * @author tthunig
 */
public class RunAdaptiveSignalsExample {
	
	private static final Logger log = Logger.getLogger(RunAdaptiveSignalsExample.class);

	public static void main(String[] args) {
		String configFileName = "./examples/tutorial/singleCrossingScenario/config.xml";
		if (args != null && args.length != 0) {
			log.info("Your config file " + args[0] + " will be loaded and run with signals and otfvis-visualization.");
			configFileName = args[0];
		}
		run(configFileName, "runAdaptiveSignalsExampleOutput/", true);
	}
	
	public static void run(String configFileName, String outputDir, boolean visualize) {
		
		Config config = ConfigUtils.loadConfig(configFileName) ;
		
		config.controler().setOutputDirectory(outputDir);
		
		// adjustments for live visualization
		OTFVisConfigGroup otfvisConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.class);
		otfvisConfig.setDrawTime(true);
		otfvisConfig.setAgentSize(80f);
		config.qsim().setSnapshotStyle(QSimConfigGroup.SnapshotStyle.withHoles);
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		
		if (signalsConfigGroup.isUseSignalSystems()) {
			scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		}
		
		Controler controler = new Controler( scenario );
        
		// add the signals module if signal systems are used
		if (signalsConfigGroup.isUseSignalSystems()) {
			/*
			 * The signals module binds everything that is necessary for the simulation with
			 * signals. If you like to use your own signal controller you can add it to the
			 * signals module by the method addSignalControlProvider.
			 */
			controler.addOverridingModule(new SignalsModule());
		}
		
		// add live visualization module
		if (visualize) { 
			controler.addOverridingModule(new OTFVisWithSignalsLiveModule());
		}
				
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				// add analysis tools here if you like
				// ...
			}
		});
		
		controler.run();
		
	}
	
}
