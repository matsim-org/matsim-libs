package playground.sebhoerl.av.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.sebhoerl.av.framework.AVConfigGroup;
import playground.sebhoerl.av.framework.AVModule;

public class RunScenario {
	public static void main(String[] args) {
		if (args.length < 1) {
			throw new RuntimeException("Configuration file must be specified as first argument.");
		}
		
		if (args.length > 1 && args[1].equals("baseline")) {
		    runBaseline(args[0]);
		} else {
		    run(args[0]);
		}
	}
	
	public static void run(String configPath) {
		Config config = ConfigUtils.loadConfig(configPath, new AVConfigGroup());
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AVModule());
		
		controler.run();
	}
	
    public static void runBaseline(String configPath) {
        Config config = ConfigUtils.loadConfig(configPath, new AVConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        
        controler.run();
    }
}
