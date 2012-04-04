package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;


public class RandomStartTimeAllocatorTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		Cooperative coop = PScenarioHelper.createTestCooperative();
		ArrayList<String> params = new ArrayList<String>();
		params.add("900");
		RandomStartTimeAllocator strat = new RandomStartTimeAllocator(params);
		PPlan testPlan = null;
		
		coop.getBestPlan().setStartTime(1200.0);

		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 1200.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		// too few vehicles for testing - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 1200.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.getBestPlan().setNVehicles(2);
		
		// enough vehicles for testing
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 1200.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle bought", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 982.4141207607663, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
	}
}