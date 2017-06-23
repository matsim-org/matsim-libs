package playground.sebhoerl.recharging_avs;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import playground.sebhoerl.avtaxi.framework.AVConfigGroup;
import playground.sebhoerl.avtaxi.framework.AVModule;
import playground.sebhoerl.avtaxi.framework.AVQSimProvider;
import playground.sebhoerl.avtaxi.routing.AVRoute;
import playground.sebhoerl.avtaxi.routing.AVRouteFactory;
import playground.sebhoerl.recharging_avs.calculators.BinnedChargeCalculatorConfig;
import playground.sebhoerl.recharging_avs.calculators.BinnedChargeCalculatorModule;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculatorConfig;
import playground.sebhoerl.recharging_avs.calculators.StaticChargeCalculatorModule;

import java.net.MalformedURLException;

public class RunSimulation {
    public static void main(String[] args) throws MalformedURLException {
        String configFile = args[0];

        DvrpConfigGroup dvrpConfigGroup = new DvrpConfigGroup();
        dvrpConfigGroup.setTravelTimeEstimationAlpha(0.05);

        Config config = ConfigUtils.loadConfig(configFile, new AVConfigGroup(), dvrpConfigGroup, new StaticChargeCalculatorConfig(), new BinnedChargeCalculatorConfig());

        Scenario scenario = ScenarioUtils.createScenario(config);
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(AVRoute.class, new AVRouteFactory());
        ScenarioUtils.loadScenario(scenario);

        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new DvrpTravelTimeModule());
        controler.addOverridingModule(new DynQSimModule<>(AVQSimProvider.class));
        controler.addOverridingModule(new AVModule());
        controler.addOverridingModule(new RechargingModule());
        controler.addOverridingModule(new StaticChargeCalculatorModule());
        controler.addOverridingModule(new BinnedChargeCalculatorModule());

        controler.run();
    }
}
