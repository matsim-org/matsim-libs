package commercialtraffic.commercialJob;

import commercialtraffic.TestScenarioGeneration;
import org.junit.Rule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CommercialJobGeneratorTest {


    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();


    @org.junit.Test
    public void notifyBeforeMobsim() {
        Carriers carriers = TestScenarioGeneration.generateCarriers();
        Scenario scenario = TestScenarioGeneration.generateScenario();
        Map<String,TravelTime> travelTimes = new HashMap<>();
        travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());

        CommercialJobGenerator generator = new CommercialJobGenerator(scenario,travelTimes, carriers );
        new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write(utils.getOutputDirectory() + "vehicleTypes.xml");
        scenario.getConfig().controler().setOutputDirectory(utils.getOutputDirectory());
        int iteration = 0;
        File file = new File(utils.getOutputDirectory() +  "/ITERS/it." + iteration + "/");
        file.mkdirs();
        generator.notifyBeforeMobsim(new BeforeMobsimEvent(new Controler(scenario), iteration));
    }


}
