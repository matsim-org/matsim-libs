package org.matsim.contrib.drt.routing;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripRouter;

public class MultiModeDrtMainModeIdentifierTest {

	@Test
	void test() {
		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		String drtMode = "drt";
		drtConfigGroup.mode = drtMode;
		Config config = ConfigUtils.createConfig();
		MultiModeDrtConfigGroup multiModeDrtConfigGroup = new MultiModeDrtConfigGroup();
		multiModeDrtConfigGroup.addParameterSet(drtConfigGroup);
		config.addModule(multiModeDrtConfigGroup);
		MultiModeDrtMainModeIdentifier mmi = new MultiModeDrtMainModeIdentifier(multiModeDrtConfigGroup);
		{
			List<PlanElement> testElements = new ArrayList<>();
			Leg l1 = PopulationUtils.createLeg(TransportMode.car);
			testElements.add(l1);
			Assertions.assertEquals(TransportMode.car, mmi.identifyMainMode(testElements));
		}
		{
			List<PlanElement> testElements = new ArrayList<>();
			Leg l1 = PopulationUtils.createLeg(drtMode);
			testElements.add(l1);
			Assertions.assertEquals(drtMode, mmi.identifyMainMode(testElements));
		}
		{
			String drtStageActivityType = ScoringConfigGroup.createStageActivityType(drtMode);
			List<PlanElement> testElements = new ArrayList<>();

			// #deleteBeforeRelease : only used to retrofit plans created since the merge of fallback routing module (sep'-dec'19)
			Leg l1 = PopulationUtils.createLeg(TripRouter.getFallbackMode(drtMode));
			Activity a2 = PopulationUtils.createActivityFromCoord(drtStageActivityType, new Coord(0, 0));
			Leg l2 = PopulationUtils.createLeg(drtMode);
			Activity a3 = PopulationUtils.createActivityFromCoord(drtStageActivityType, new Coord(0, 0));
			Leg l3 = PopulationUtils.createLeg(TripRouter.getFallbackMode(drtMode));

			testElements.add(l1);
			testElements.add(a2);
			testElements.add(l2);
			testElements.add(a3);
			testElements.add(l3);
			Assertions.assertEquals(drtMode, mmi.identifyMainMode(testElements));
		}
	}
}
