package playground.sebhoerl.renault;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.TransitRouter;

import java.util.List;

public class RunRenaultScenario {
    public static void main(String[] args) {
        String configFile = args[0];

        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TransitRouter.class).toProvider(RenaultTransitRouterProvider.class);
            }
        });

        controler.run();
    }
}
