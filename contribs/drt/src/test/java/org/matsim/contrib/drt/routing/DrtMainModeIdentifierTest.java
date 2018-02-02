package org.matsim.contrib.drt.routing;


import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;


public class DrtMainModeIdentifierTest {

	@Test
	public void test() {
		DvrpConfigGroup dvrp = new DvrpConfigGroup();
		dvrp.setMode("drt");
		Config config = ConfigUtils.createConfig();
		config.addModule(dvrp);
		MainModeIdentifier mmi = new DrtMainModeIdentifier(config);
		{
		List<PlanElement> testElements = new ArrayList<>();
		Leg l1 = PopulationUtils.createLeg(TransportMode.car);
		testElements.add(l1);
		Assert.assertEquals(TransportMode.car, mmi.identifyMainMode(testElements));
		}
		{
		List<PlanElement> testElements = new ArrayList<>();
		Leg l1 = PopulationUtils.createLeg("drt");
		testElements.add(l1);
		Assert.assertEquals("drt", mmi.identifyMainMode(testElements));		
		}
		
		{
			List<PlanElement> testElements = new ArrayList<>();
			Leg l1 = PopulationUtils.createLeg(DrtStageActivityType.DRT_WALK);
			Activity a2 =  PopulationUtils.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY, new Coord(0,0));
			Leg l2 = PopulationUtils.createLeg("drt");
			Activity a3 =  PopulationUtils.createActivityFromCoord(DrtStageActivityType.DRT_STAGE_ACTIVITY, new Coord(0,0));
			Leg l3 = PopulationUtils.createLeg(DrtStageActivityType.DRT_WALK);

			testElements.add(l1);
			testElements.add(a2);
			testElements.add(l2);
			testElements.add(a3);
			testElements.add(l3);
			Assert.assertEquals("drt", mmi.identifyMainMode(testElements));		
			}
		
	}

}
