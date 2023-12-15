package org.matsim.contrib.commercialTrafficApplications.jointDemand;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.Carriers;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.HashMap;
import java.util.Map;

public class ChangeCommercialJobOperatorTest {

	@Test
	void getPlanAlgoInstance() {


        Carriers carriers = TestScenarioGeneration.generateCarriers();
        Scenario scenario = TestScenarioGeneration.generateScenario();
        Map<String, TravelTime> travelTimes = new HashMap<>();
        travelTimes.put(TransportMode.car, new FreeSpeedTravelTime());
        ChangeCommercialJobOperator changeCommercialJobOperator = new ChangeCommercialJobOperator(scenario.getConfig().global(), carriers);

        Plan testPlan = scenario.getPopulation().getPersons().get(Id.createPersonId(1)).getSelectedPlan();
        Activity work = (Activity) testPlan.getPlanElements().get(2);

        Id<Carrier> carrierId = JointDemandUtils.getCurrentlySelectedCarrierForJob(work, 1);
        Assertions.assertEquals("pizza", JointDemandUtils.getCarrierMarket(carriers.getCarriers().get(carrierId)), "the person should expect a pizza");
        Assertions.assertTrue(carrierId.toString().contains("italian"), "the person should expect a pizza from the italian place");

        changeCommercialJobOperator.getPlanAlgoInstance().run(testPlan);

        carrierId = JointDemandUtils.getCurrentlySelectedCarrierForJob(work, 1);
        Assertions.assertTrue(carrierId.toString().contains("american"), "the person should expect a pizza from the american place");

        changeCommercialJobOperator.getPlanAlgoInstance().run(testPlan);

        carrierId = JointDemandUtils.getCurrentlySelectedCarrierForJob(work, 1);
        Assertions.assertTrue(carrierId.toString().contains("italian"), "the person should expect a pizza from the italian place");

    }

}
