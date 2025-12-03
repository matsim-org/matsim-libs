package org.matsim.contrib.ev.extensions.battery_chargers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.controler.Controller;
import org.matsim.testcases.MatsimTestUtils;

public class BatteryChargerPowerTest {
        @RegisterExtension
        public MatsimTestUtils utils = new MatsimTestUtils();

        @Test
        public void testDischargeToZeroAndLimitChargingPowerForVehicle() throws IOException {
                Controller controller = new TestScenarioBuilder(utils, 50.0, 3000.0) //
                                .addVehicle("veh1", 250.0, 2000.0, 0.2) //
                                .addCharger("charger1", 1, 70.0, attributes -> {
                                        BatteryChargerSettings.write(attributes,
                                                        new BatteryChargerSettings(20.0, 100.0));
                                        BatteryChargerSettings.setInitialSoc(attributes, 0.15);
                                }) //
                                .build();

                controller.addOverridingModule(new BatteryChargerModule());
                controller.run();

                assertTrue(new File(utils.getOutputDirectory(), "ITERS/it.0/0.battery_charger_states.csv").exists());
        }

        @Test
        public void testDistributeChargingPowerOverTwoVehiclecs() throws IOException {
                Controller controller = new TestScenarioBuilder(utils, 50.0, 3000.0) //
                                .addVehicle("veh1", 250.0, 2000.0, 0.2) //
                                .addVehicle("veh2", 1600.0, 3000.0, 0.2) //
                                .addCharger("charger1", 2, 70.0, attributes -> {
                                        BatteryChargerSettings.write(attributes,
                                                        new BatteryChargerSettings(20.0, 100.0));
                                        BatteryChargerSettings.setInitialSoc(attributes, 0.15);
                                }) //
                                .build();

                controller.addOverridingModule(new BatteryChargerModule());
                controller.run();

                assertTrue(new File(utils.getOutputDirectory(), "ITERS/it.0/0.battery_charger_states.csv").exists());
        }
}
