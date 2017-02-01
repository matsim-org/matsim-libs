package playground.clruch;



import java.io.File;
import java.net.MalformedURLException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;

import javax.inject.*;


public class RunAVScenario {
    public static Network NETWORKINSTANCE;

    public static void main(String[] args) throws MalformedURLException {
        File configFile = new File(args[0]);

        Config config = ConfigUtils.loadConfig(configFile.toString(), new AVConfigGroup());
        Scenario scenario = ScenarioUtils.loadScenario(config);
        NETWORKINSTANCE = scenario.getNetwork();

        System.out.println("Population size:" + scenario.getPopulation().getPersons().values().size());

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());

        controler.run();
    }
}
