package euler_project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunEuler {
    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("C:/Users/Claudio/Documents/matsim_Simulations/2017_03_21 SiouxBare/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        
        Controler controller = new Controler(scenario);
    }
}