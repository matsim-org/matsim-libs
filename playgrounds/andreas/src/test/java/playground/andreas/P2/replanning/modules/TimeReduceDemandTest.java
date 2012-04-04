package playground.andreas.P2.replanning.modules;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.andreas.P2.PScenarioHelper;
import playground.andreas.P2.helper.PConfigGroup;
import playground.andreas.P2.pbox.Cooperative;
import playground.andreas.P2.plan.PPlan;
import playground.andreas.P2.replanning.modules.TimeReduceDemand;


public class TimeReduceDemandTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
    public final void testRun() {
	
		EventsFactoryImpl eF = new EventsFactoryImpl();
		PConfigGroup pC = new PConfigGroup();
		
		Cooperative coop = PScenarioHelper.createTestCooperative();
		ArrayList<String> param = new ArrayList<String>();
		param.add("900");		
		TimeReduceDemand strat = new TimeReduceDemand(param);
		strat.setPIdentifier(pC.getPIdentifier());
		
		Id veh1 = new IdImpl(pC.getPIdentifier() + 1 + "-" + 1);
		Id veh2 = new IdImpl(pC.getPIdentifier() + 2 + "-" + 1);
		
		PPlan testPlan = null;
		
		coop.getBestPlan().setStartTime(10000.0);
		coop.getBestPlan().setEndTime(30000.0);

		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 10000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 30000.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		// too few vehicles for testing - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 10000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 30000.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		coop.getBestPlan().setNVehicles(2);
		
		// enough vehicles for testing but still no demand - nothing should change
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 2.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 10000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 30000.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNull("Test plan should be null", testPlan);
		
		strat.handleEvent(eF.createTransitDriverStartsEvent(18000.0, new IdImpl("d1"), veh1, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep1")));
		strat.handleEvent(eF.createTransitDriverStartsEvent(18000.0, new IdImpl("d2"), veh2, new IdImpl("line2"), new IdImpl("route2"), new IdImpl("dep2")));
		strat.handleEvent(eF.createPersonEntersVehicleEvent(20000.0, new IdImpl("p1"), veh1));
		strat.handleEvent(eF.createPersonEntersVehicleEvent(15000.0, new IdImpl("p1"), veh2));
		strat.handleEvent(eF.createTransitDriverStartsEvent(22500.0, new IdImpl("d2"), veh1, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep2")));
		strat.handleEvent(eF.createPersonLeavesVehicleEvent(23000.0, new IdImpl("p1"), veh1));
		strat.handleEvent(eF.createPersonLeavesVehicleEvent(17000.0, new IdImpl("p1"), veh2));
		strat.handleEvent(eF.createTransitDriverStartsEvent(25000.0, new IdImpl("d3"), veh1, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep3")));
		
		// should be able to replan
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 10000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 30000.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle moved from best plan", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 18000.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 23400.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
		
		
		strat.reset(0);
		coop.getBestPlan().setNVehicles(2);
		strat.handleEvent(eF.createTransitDriverStartsEvent(21350.0, new IdImpl("d1"), veh1, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep1")));
		strat.handleEvent(eF.createPersonEntersVehicleEvent(22123.0, new IdImpl("p1"), veh1));
		
		strat.handleEvent(eF.createTransitDriverStartsEvent(22899.0, new IdImpl("d2"), veh1, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep2")));
		strat.handleEvent(eF.createPersonLeavesVehicleEvent(23222.0, new IdImpl("p1"), veh1));
		
		strat.handleEvent(eF.createTransitDriverStartsEvent(24999.0, new IdImpl("d3"), veh1, new IdImpl("line1"), new IdImpl("route1"), new IdImpl("dep3")));
		
		// should be able to replan
		testPlan = strat.run(coop);
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 10000.0, coop.getBestPlan().getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 30000.0, coop.getBestPlan().getEndTime(), MatsimTestUtils.EPSILON);
		Assert.assertNotNull("Test plan should be not null", testPlan);
		Assert.assertEquals("There should be one vehicle moved from best plan", 1.0, testPlan.getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start time", 20700.0, testPlan.getStartTime(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare end time", 23400.0, testPlan.getEndTime(), MatsimTestUtils.EPSILON);
	}
}