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
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifier;

public class DrtMainModeIdentifierTest {

	@Test
	public void test() {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		drtConfigGroup.setMode("drt");
		Config config = ConfigUtils.createConfig();
		config.addModule(drtConfigGroup);
		MainModeIdentifier mmi = new DrtMainModeIdentifier(drtConfigGroup);
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
			DrtStageActivityType drtStageActivityType = new DrtStageActivityType("drt");
			List<PlanElement> testElements = new ArrayList<>();
			Leg l1 = PopulationUtils.createLeg(drtStageActivityType.DRT_WALK);
			Activity a2 = PopulationUtils.createActivityFromCoord(drtStageActivityType.DRT_STAGE_ACTIVITY,
					new Coord(0, 0));
			Leg l2 = PopulationUtils.createLeg("drt");
			Activity a3 = PopulationUtils.createActivityFromCoord(drtStageActivityType.DRT_STAGE_ACTIVITY,
					new Coord(0, 0));
			Leg l3 = PopulationUtils.createLeg(drtStageActivityType.DRT_WALK);

			testElements.add(l1);
			testElements.add(a2);
			testElements.add(l2);
			testElements.add(a3);
			testElements.add(l3);
			Assert.assertEquals("drt", mmi.identifyMainMode(testElements));
		}
	}
}
