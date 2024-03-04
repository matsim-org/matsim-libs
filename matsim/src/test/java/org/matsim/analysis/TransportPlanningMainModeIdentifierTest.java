/**
 *
 */
package org.matsim.analysis;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author Aravind
 *
 */
public class TransportPlanningMainModeIdentifierTest {

	private final List<String> modeHierarchy = new ArrayList<>();

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testIterationTravelStatsControlerListener() {

		modeHierarchy.add(TransportMode.non_network_walk);
		modeHierarchy.add("undefined");
		modeHierarchy.add(TransportMode.other);
		modeHierarchy.add(TransportMode.transit_walk);
		modeHierarchy.add(TransportMode.walk);
		modeHierarchy.add(TransportMode.bike);
		modeHierarchy.add(TransportMode.drt);
		modeHierarchy.add(TransportMode.pt);
		modeHierarchy.add(TransportMode.ride);
		modeHierarchy.add(TransportMode.car);

		ArrayList<Plan> plansList = new ArrayList<Plan>();
		Plans plans = new Plans();

		/****************************
		 * Plan 1 - creating plan 1
		 ************************************/
		Plan plan1 = plans.createPlanOne();
		plansList.add(plan1);
		/********************************
		 * Plan 2 - creating plan 2
		 ********************************/
		Plan plan2 = plans.createPlanTwo();
		plansList.add(plan2);
		/*****************************
		 * Plan 3 - creating plan 3
		 ************************************/
		Plan plan3 = plans.createPlanThree();
		plansList.add(plan3);
		/************************
		 * Plan 4-----creating plan 4
		 **************************************/
		Plan plan4 = plans.createPlanFour();
		plansList.add(plan4);

		performTest(plansList);

	}

	private void performTest(ArrayList<Plan> plansList) {

		String mainMode = null;
		TransportPlanningMainModeIdentifier transportPlanningMainModeIdentifier = new TransportPlanningMainModeIdentifier();
		mainMode = transportPlanningMainModeIdentifier.identifyMainMode(plansList.get(0).getPlanElements());
		validateValues(plansList.get(0), mainMode);
		mainMode = transportPlanningMainModeIdentifier.identifyMainMode(plansList.get(1).getPlanElements());
		validateValues(plansList.get(1), mainMode);
		mainMode = transportPlanningMainModeIdentifier.identifyMainMode(plansList.get(2).getPlanElements());
		validateValues(plansList.get(2), mainMode);
		mainMode = transportPlanningMainModeIdentifier.identifyMainMode(plansList.get(3).getPlanElements());
		validateValues(plansList.get(3), mainMode);
	}

	private String identifyMainMode(List<? extends PlanElement> planElements) {

		int modeIndex = -1;
		for (PlanElement pe : planElements) {
			if (pe instanceof Leg) {
				int index = modeHierarchy.indexOf(((Leg) pe).getMode());
				if (index > modeIndex) {
					modeIndex = index;
				}
			}
		}
		return modeHierarchy.get(modeIndex);
	}

	private void validateValues(Plan plan, String mainMode) {
		String mainModeIdentified = identifyMainMode(plan.getPlanElements());
		Assertions.assertEquals(mainModeIdentified, mainMode);
	}
}
