package commercialtraffic.replanning;

import commercialtraffic.TestScenarioGeneration;
import commercialtraffic.commercialJob.ChangeCommercialJobOperator;
import commercialtraffic.commercialJob.CommercialJobUtils;
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

public class ChangeCommercialJobOperatorTest {

    @Test
    public void getPlanAlgoInstance() {


        Carriers carriers = TestScenarioGeneration.generateCarriers();
        Scenario scenario = TestScenarioGeneration.generateScenario();
        Map<String, TravelTime> travelTimes = new HashMap<>();
        travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
        ChangeCommercialJobOperator changeCommercialJobOperator = new ChangeCommercialJobOperator(scenario.getConfig().global(), carriers);

        Plan testPlan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
        Activity work = (Activity) testPlan.getPlanElements().get(2);

        Id<Carrier> carrierId = CommercialJobUtils.getCurrentCarrierForJob(work,1);
        Assert.assertEquals("the person should expect a pizza","pizza", CommercialJobUtils.getCarrierMarket(carrierId));
        Assert.assertEquals("the person should expect a pizza from the italian place","italian", CommercialJobUtils.getCarrierOperator(carrierId));

        changeCommercialJobOperator.getPlanAlgoInstance().run(testPlan);

        String operator = CommercialJobUtils.getJobOperator(work,1);
        Assert.assertEquals("american", operator);

        changeCommercialJobOperator.getPlanAlgoInstance().run(testPlan);

        operator = CommercialJobUtils.getJobOperator(work,1);
        Assert.assertEquals("italian", operator);

    }

}