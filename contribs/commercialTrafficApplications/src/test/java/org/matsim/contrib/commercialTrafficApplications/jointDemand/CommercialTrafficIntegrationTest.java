package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.freight.carriers.FreightCarriersConfigGroup;
import org.matsim.freight.carriers.CarriersUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;


public class CommercialTrafficIntegrationTest {

	@Test
	void runCommercialTrafficIT() {
        Config config = ConfigUtils.loadConfig("./scenarios/grid/jointDemand_config.xml");
        config.controller().setLastIteration(5);
        ConfigUtils.addOrGetModule(config, JointDemandConfigGroup.class);
        FreightCarriersConfigGroup freightCarriersConfigGroup = ConfigUtils.addOrGetModule(config, FreightCarriersConfigGroup.class);
        freightCarriersConfigGroup.setCarriersFile("jointDemand_carriers_car.xml");
        freightCarriersConfigGroup.setCarriersVehicleTypesFile("jointDemand_vehicleTypes.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        CarriersUtils.loadCarriersAccordingToFreightConfig(scenario);
        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new JointDemandModule());
        controler.run();
    }


}
