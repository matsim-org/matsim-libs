package tutorial.adaptiveSignals;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
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

public class RunAdaptiveSignalsExample {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			throw new RuntimeException("Please provide a config file name as first argument");
		}
		run(args[0], "runAdaptiveSignalsExampleOutput/", true);
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
			 * The combined signals module binds everything that is necessary for the
			 * simulation with signals. If you like to use your own signal controller you
			 * can add it to the signals module by the method addSignalControlProvider.
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
