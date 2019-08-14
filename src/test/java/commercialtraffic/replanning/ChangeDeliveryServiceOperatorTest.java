package commercialtraffic.replanning;

import commercialtraffic.TestScenarioGeneration;
import commercialtraffic.integration.CommercialTrafficChecker;
import commercialtraffic.jobGeneration.CommercialJobManager;
import commercialtraffic.jobGeneration.CommercialJobUtils;
import commercialtraffic.jobGeneration.FreightAgentInserter;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.HashMap;
import java.util.Map;

public class ChangeDeliveryServiceOperatorTest {

    @Test
    public void getPlanAlgoInstance() {


        Carriers carriers = TestScenarioGeneration.generateCarriers();
        Scenario scenario = TestScenarioGeneration.generateScenario();
        Map<String, TravelTime> travelTimes = new HashMap<>();
        travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
        CommercialJobManager manager = new CommercialJobManager(carriers,scenario,
                new FreightAgentInserter(scenario),
                new CommercialTrafficChecker(),
                travelTimes);
        ChangeDeliveryServiceOperator changeDeliveryServiceOperator = new ChangeDeliveryServiceOperator(scenario.getConfig().global(), manager);

        Plan testPlan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
        Activity work = (Activity) testPlan.getPlanElements().get(2);

        String serviceId = CommercialJobUtils.getServiceIdsFromActivity(work).iterator().next().toString();
        Assert.assertEquals("the person should expect a salamipizza","salamipizza", serviceId);

        String carrier = manager.getCurrentCarrierOfService(Id.create(serviceId, CarrierService.class)).toString();
        Assert.assertEquals("pizza_1", carrier);

        changeDeliveryServiceOperator.getPlanAlgoInstance().run(testPlan);

        carrier = manager.getCurrentCarrierOfService(Id.create(serviceId, CarrierService.class)).toString();
        Assert.assertEquals("pizza_2", carrier);

        changeDeliveryServiceOperator.getPlanAlgoInstance().run(testPlan);

        carrier = manager.getCurrentCarrierOfService(Id.create(serviceId, CarrierService.class)).toString();
        Assert.assertEquals("pizza_1", carrier);

    }

}