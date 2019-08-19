package commercialtraffic.commercialJob;

import commercialtraffic.TestScenarioGeneration;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.config.ConfigUtils.createConfig;

public class CommercialJobManagerTest {



    @org.junit.Test
    public void notifyBeforeMobsim() {
        Carriers carriers = TestScenarioGeneration.generateCarriers();
        Scenario scenario = TestScenarioGeneration.generateScenario();
        Map<String,TravelTime> travelTimes = new HashMap<>();
        travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
        CommercialJobManager manager = new CommercialJobManager(carriers,scenario,
                                                                    new FreightAgentInserter(scenario),
                                                                    travelTimes);

        new CarrierVehicleTypeWriter(CarrierVehicleTypes.getVehicleTypes(carriers)).write("carriertypes.xml");
        manager.notifyBeforeMobsim(new BeforeMobsimEvent(null, 0));
    }


}