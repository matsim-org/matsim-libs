package org.matsim.contrib.ev.extensions.battery_chargers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.controler.Controller;
import org.matsim.testcases.MatsimTestUtils;

public class HysteresisChargerPowerTest {
        @RegisterExtension
        public MatsimTestUtils utils = new MatsimTestUtils();

        @Test
        public void testLongChargingWithHysteresis() throws IOException {
                Controller controller = new TestScenarioBuilder(utils, 70.0, 8000.0) //
                                .addVehicle("veh1", 1000.0, 8000.0, 0.2) //
                                .addCharger("charger1", 1, 100.0, attributes -> {
                                        HysteresisChargerSettings.write(attributes, new HysteresisChargerSettings(85.0,
                                                        100.0, 75.0, 100.0, 15.0, 25.0, 75.0));
                                        BatteryChargerSettings.setInitialSoc(attributes, 0.2);
                                }) //
                                .build();

                controller.addOverridingModule(new BatteryChargerModule());
                controller.run();

                assertTrue(new File(utils.getOutputDirectory(), "ITERS/it.0/0.battery_charger_states.csv").exists());
        }
}
