package playground.sergioo.mixedTraffic2017;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.qsim.*;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Created by sergioo on 24/2/17.
 */
public class RunControler {
    public static void main(String[] args) {
        final Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(args[0]));
        final Controler controler = new Controler(scenario);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                install(new QSimModule());
            }
        });
        controler.run();
    }
}
