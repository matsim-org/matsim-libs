package commercialtraffic.jobGeneration;

import commercialtraffic.TestScenarioGeneration;
import commercialtraffic.integration.CommercialTrafficChecker;
import org.junit.Ignore;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.config.ConfigUtils.createConfig;
import static org.matsim.core.scenario.ScenarioUtils.createScenario;

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