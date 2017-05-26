package playground.sebhoerl.renault;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;

public class RunIleDeFranceScenario {
    public static void main(String[] args) {
        String configFile = args[0];

        Config config = ConfigUtils.loadConfig(configFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler = new Controler(scenario);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(TransitRouter.class).toProvider(IleDeFranceTransitRouterProvider.class);
            }
        });

        controler.run();
    }
}
