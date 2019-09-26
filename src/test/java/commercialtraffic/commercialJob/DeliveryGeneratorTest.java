package commercialtraffic.commercialJob;

import commercialtraffic.TestScenarioGeneration;
import org.junit.Rule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

public class DeliveryGeneratorTest {


    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();


    @org.junit.Test
    public void notifyBeforeMobsim() {
        Carriers carriers = TestScenarioGeneration.generateCarriers();
        Scenario scenario = TestScenarioGeneration.generateScenario();
        Map<String,TravelTime> travelTimes = new HashMap<>();
        travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());

        DeliveryGenerator generator = new DeliveryGenerator(scenario,travelTimes, carriers, carrierId -> TransportMode.car, carrierId -> 50);
        new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write("vehicleTypes.xml");
        scenario.getConfig().controler().setOutputDirectory(utils.getOutputDirectory());
        int iteration = 0;
        File file = new File(utils.getOutputDirectory() +  "/ITERS/it." + iteration + "/");
        file.mkdirs();
        generator.notifyBeforeMobsim(new BeforeMobsimEvent(new Controler(scenario), iteration));
    }


}