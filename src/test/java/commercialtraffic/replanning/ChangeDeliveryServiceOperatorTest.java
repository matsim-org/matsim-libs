package commercialtraffic.replanning;

import commercialtraffic.TestScenarioGeneration;
import commercialtraffic.commercialJob.CommercialJobUtilsV2;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.freight.carrier.Carrier;
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
        ChangeDeliveryServiceOperator changeDeliveryServiceOperator = new ChangeDeliveryServiceOperator(scenario.getConfig().global(), carriers);

        Plan testPlan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
        Activity work = (Activity) testPlan.getPlanElements().get(2);

        Id<Carrier> carrierId = CommercialJobUtilsV2.getCarrierId(work);
        Assert.assertEquals("the person should expect a salami","pizza", CommercialJobUtilsV2.getCarrierMarket(carrierId));
        Assert.assertEquals("the person should expect a salami from operator 1","1", CommercialJobUtilsV2.getCarrierOperator(carrierId));

        changeDeliveryServiceOperator.getPlanAlgoInstance().run(testPlan);

        String operator = CommercialJobUtilsV2.getServiceOperator(work);
        Assert.assertEquals("2", operator);

        changeDeliveryServiceOperator.getPlanAlgoInstance().run(testPlan);

        operator = CommercialJobUtilsV2.getServiceOperator(work);
        Assert.assertEquals("1", operator);

    }

}