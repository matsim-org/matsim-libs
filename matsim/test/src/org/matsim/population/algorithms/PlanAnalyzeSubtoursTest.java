/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAnalyzeSubtoursTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.Layer;

/**
 * Test class for {@link PlanAnalyzeSubtours}.
 * 
 * Contains illustrative examples for subtour analysis. See documentation <a href="http://matsim.org/node/266">here</a>.
 * 
 * @author meisterk
 *
 */
public class PlanAnalyzeSubtoursTest extends MatsimTestCase {
	
	public static <S, T> MapBuilder<S, T> map(S key, T value) {
        return new MapBuilder<S, T>().map(key, value);
    }

    public static class MapBuilder<S, T> extends HashMap<S, T> {

		private static final long serialVersionUID = 1L;

		public MapBuilder() {
        }

        public MapBuilder<S, T> map(S key, T value) {
            put(key, value);
            return this;
        }
    }

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";

	public void testNetworkBased() {
		Config config = loadConfig(PlanAnalyzeSubtoursTest.CONFIGFILE);
		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.link);
		this.runDemo(network, config.planomat());
	}
	
	public void testFacilitiesBased() {
		Config config = loadConfig(PlanAnalyzeSubtoursTest.CONFIGFILE);
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());
		config.planomat().setTripStructureAnalysisLayer(PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility);
		this.runDemo(facilities, config.planomat());
	}

	@SuppressWarnings("unchecked")
	protected void runDemo(Layer layer, PlanomatConfigGroup planomatConfigGroup) {
		String UNDEFINED_UNDEFINED_UNDEFINED = Integer
				.toString(PlanAnalyzeSubtours.UNDEFINED)
				+ " "
				+ Integer.toString(PlanAnalyzeSubtours.UNDEFINED)
				+ " "
				+ Integer.toString(PlanAnalyzeSubtours.UNDEFINED);
		Integer NULL = null;	
		doTest(layer, planomatConfigGroup, "1 2 1", "0 0", 1, map(0,NULL));
		doTest(layer, planomatConfigGroup, "1 2 20 1", "0 0 0", 1, map(0,NULL));
		doTest(layer, planomatConfigGroup, "1 2 1 2 1", "0 0 1 1", 2, map(0,NULL).map(1,NULL));
		doTest(layer, planomatConfigGroup, "1 2 1 3 1", "0 0 1 1", 2, map(0,NULL).map(1,NULL));
		doTest(layer, planomatConfigGroup, "1 2 2 1", "1 0 1", 2, map(0,1).map(1,NULL));
		doTest(layer, planomatConfigGroup, "1 2 2 2 2 2 2 2 1", "6 0 1 2 3 4 5 6", 7, map(0,6).map(1,6).map(2,6). map(3,6).map(4,6).map(5,6).map(6,NULL));
		doTest(layer, planomatConfigGroup, "1 2 3 2 1", "1 0 0 1", 2, map(0,1).map(1,NULL));
		doTest(layer, planomatConfigGroup, "1 2 3 4 3 2 1", "2 1 0 0 1 2", 3, map(0,1).map(1,2).map(2,NULL));
		doTest(layer, planomatConfigGroup, "1 2 14 2 14 2 1", "2 0 0 1 1 2", 3, map(0,2).map(1,2).map(2,NULL));
		doTest(layer, planomatConfigGroup, "1 2 14 14 2 14 2 1", "3 1 0 1 2 2 3", 4, map(0,1).map(1,3).map(2,3).map(3,NULL));
		doTest(layer, planomatConfigGroup, "1 2 3 4 3 2 5 4 5 1", "3 1 0 0 1 3 2 2 3", 4, map(0,1).map(1,3).map(2,3).map(3,NULL));
		doTest(layer, planomatConfigGroup, "1 2 3 2 3 2 1 2 1", "2 0 0 1 1 2 3 3", 4, map(0,2).map(1,2).map(2,NULL).map(3,NULL));
		doTest(layer, planomatConfigGroup, "1 1 1 1 1 2 1", "0 1 2 3 4 4", 5, map(0,NULL).map(1,NULL).map(2,NULL).map(3,NULL).map(4,NULL));
		doTest(layer, planomatConfigGroup, "1 2 1 1", "0 0 1", 2, map(0,NULL).map(1,NULL));
		doTest(layer, planomatConfigGroup, "1 2 3 4", UNDEFINED_UNDEFINED_UNDEFINED, 0, Collections.EMPTY_MAP);
		doTest(layer, planomatConfigGroup, "1 2 2 3 2 2 2 1 4 1", "4 0 1 1 2 3 4 5 5", 6, map(0,4).map(1,4).map(2,4).map(3,4).map(4,NULL).map(5,NULL));
		doTest(layer, planomatConfigGroup, "1 2 3 4 3 1", "1 1 0 0 1", 2, map(0,1).map(1,NULL));
		// doTest(layer, planomatConfigGroup, "1 2 3 4 2 5 3 6 1", " 2 0 0 0 1 1 2 2", 3, map(2,NULL).map(0,2).map(1,2));
		doTest(layer, planomatConfigGroup, "1 2 3 4 2 5 3 6 1", "1 0 0 0 1 1 1 1", 2, map(1,NULL).map(0,1));
	}

	private void doTest(Layer layer, PlanomatConfigGroup planomatConfigGroup,
			String facString, String expectedSubtourIndexationString,
			int expectedNumSubtoursForThis, Map<Integer, Integer> childToParent) {
		PlanAnalyzeSubtours testee = new PlanAnalyzeSubtours(planomatConfigGroup);
		PersonImpl person = new PersonImpl(new IdImpl("1000"));
		PlanImpl plan = TestsUtil.createPlan(layer, person, TransportMode.car, facString,
				planomatConfigGroup);
		testee.run(plan);
		StringBuilder builder = new StringBuilder();
		List<Integer> actualSubtourIndexation = toList(testee.getSubtourIndexation());
		for (int value : actualSubtourIndexation) {
			builder.append(Integer.toString(value));
			builder.append(' ');
		}
		String actualSubtourIndexationString = builder.substring(0, builder.length() - 1);
		assertEquals("failure testing " + facString, expectedSubtourIndexationString, actualSubtourIndexationString);
		int actualNumSubtours = testee.getNumSubtours();
		assertEquals("failure testing " + facString, expectedNumSubtoursForThis, actualNumSubtours);
		for (int i = 0; i < actualNumSubtours; i++) {
			int leftPlanIndex = 2 * actualSubtourIndexation.indexOf(i);
			int rightPlanIndex = 2 * (1 + actualSubtourIndexation.lastIndexOf(i));
			List<? extends PlanElement> expectedSubTour = plan.getPlanElements().subList(leftPlanIndex, rightPlanIndex);
			List<? extends PlanElement> actualSubTour = testee.getSubtours().get(i);
			assertEquals(expectedSubTour, actualSubTour);
			for (Entry<Integer, Integer> entry : childToParent.entrySet()) {
				assertEquals(entry.getValue(), testee.getParentTours().get(entry.getKey()));
			}
		}
	}


	private List<Integer> toList(int[] subtourIndexation) {
		ArrayList<Integer> l = new ArrayList<Integer>();
		for (int index : subtourIndexation) {
			l.add(index);
		}
		return l;
	}

}
