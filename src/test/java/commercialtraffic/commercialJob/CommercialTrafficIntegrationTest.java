package commercialtraffic.commercialJob;

import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.FreightConfigGroup;
import org.matsim.contrib.freight.utils.FreightUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class CommercialTrafficIntegrationTest {

    @Test
    public void runCommercialTrafficIT() {
        Config config = ConfigUtils.loadConfig("input/commercialtrafficIT/config.xml");
        config.controler().setLastIteration(5);
        ConfigUtils.addOrGetModule(config, CommercialTrafficConfigGroup.class);
        FreightConfigGroup freightConfigGroup = ConfigUtils.addOrGetModule(config, FreightConfigGroup.class);
        freightConfigGroup.setCarriersFile("test-carriers-car.xml");
        freightConfigGroup.setCarriersVehicleTypesFile("vehicleTypes.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        FreightUtils.loadCarriersAccordingToFreightConfig(scenario);
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new CommercialTrafficModule());
        controler.run();
    }


}
