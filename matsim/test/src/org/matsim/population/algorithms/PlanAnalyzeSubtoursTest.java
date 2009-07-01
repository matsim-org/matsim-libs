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

import java.util.HashMap;
import java.util.Map.Entry;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.facilities.ActivityFacility;
import org.matsim.core.api.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.world.Layer;
import org.matsim.world.Location;

/**
 * Test class for {@link PlanAnalyzeSubtours}.
 * 
 * Contains illustrative examples for subtour analysis. See documentation <a href="http://matsim.org/node/266">here</a>.
 * 
 * @author meisterk
 *
 */
public class PlanAnalyzeSubtoursTest extends MatsimTestCase {

	private static final String CONFIGFILE = "test/scenarios/equil/config.xml";

	public void testNetworkBased() {
		Config config = loadConfig(PlanAnalyzeSubtoursTest.CONFIGFILE);

		NetworkLayer network = new NetworkLayer();
		new MatsimNetworkReader(network).readFile(config.network().getInputFile());
		
		config.planomat().setTripStructureAnalysisLayer("link");
		this.runDemo(network, config);
	}
	
	public void testFacilitiesBased() {
		Config config = loadConfig(PlanAnalyzeSubtoursTest.CONFIGFILE);
		ActivityFacilitiesImpl facilities = new ActivityFacilitiesImpl();
		new MatsimFacilitiesReader(facilities).readFile(config.facilities().getInputFile());

		config.planomat().setTripStructureAnalysisLayer("facility");
		this.runDemo(facilities, config);
	}
	
	protected void runDemo(Layer layer, Config config) {

		PlanAnalyzeSubtours testee = new PlanAnalyzeSubtours();

		PersonImpl person = new PersonImpl(new IdImpl("1000"));

		// test different types of activity plans
		HashMap<String, String> expectedSubtourIndexations = new HashMap<String, String>();
		HashMap<String, Integer> expectedNumSubtours = new HashMap<String, Integer>();
		
		Integer i0 = Integer.valueOf(0);
		Integer i1 = Integer.valueOf(1);
		Integer i2 = Integer.valueOf(2);
		Integer i3 = Integer.valueOf(3);
		Integer i4 = Integer.valueOf(4);
		Integer i5 = Integer.valueOf(5);
		Integer i6 = Integer.valueOf(6);
		Integer i7 = Integer.valueOf(7);
		
		String testedActChainLocations = "1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0");
		expectedNumSubtours.put(testedActChainLocations, i1);
		
		testedActChainLocations = "1 2 20 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 0");
		expectedNumSubtours.put(testedActChainLocations, i1);

		testedActChainLocations = "1 2 1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 1 1");
		expectedNumSubtours.put(testedActChainLocations, i2);

		testedActChainLocations = "1 2 1 3 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 1 1");
		expectedNumSubtours.put(testedActChainLocations, i2);

		testedActChainLocations = "1 2 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "1 0 1");
		expectedNumSubtours.put(testedActChainLocations, i2);
		
		testedActChainLocations = "1 2 2 2 2 2 2 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "6 0 1 2 3 4 5 6");
		expectedNumSubtours.put(testedActChainLocations, i7);

		testedActChainLocations = "1 2 3 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "1 0 0 1");
		expectedNumSubtours.put(testedActChainLocations, i2);

		testedActChainLocations = "1 2 3 4 3 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "2 1 0 0 1 2");
		expectedNumSubtours.put(testedActChainLocations, i3);

		testedActChainLocations = "1 2 14 2 14 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "2 0 0 1 1 2");
		expectedNumSubtours.put(testedActChainLocations, i3);

		testedActChainLocations = "1 2 14 14 2 14 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "3 1 0 1 2 2 3");
		expectedNumSubtours.put(testedActChainLocations, i4);

		testedActChainLocations = "1 2 3 4 3 2 5 4 5 1";
		expectedSubtourIndexations.put(testedActChainLocations, "3 1 0 0 1 3 2 2 3");
		expectedNumSubtours.put(testedActChainLocations, i4);

		testedActChainLocations = "1 2 3 2 3 2 1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "2 0 0 1 1 2 3 3");
		expectedNumSubtours.put(testedActChainLocations, i4);

		testedActChainLocations = "1 1 1 1 1 2 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 1 2 3 4 4");
		expectedNumSubtours.put(testedActChainLocations, i5);

		testedActChainLocations = "1 2 1 1";
		expectedSubtourIndexations.put(testedActChainLocations, "0 0 1");
		expectedNumSubtours.put(testedActChainLocations, i2);

		testedActChainLocations = "1 2 3 4";
		expectedSubtourIndexations.put(
				testedActChainLocations, 
						Integer.toString(PlanAnalyzeSubtours.UNDEFINED) + " " + 
						Integer.toString(PlanAnalyzeSubtours.UNDEFINED) + " " + 
						Integer.toString(PlanAnalyzeSubtours.UNDEFINED));
		expectedNumSubtours.put(testedActChainLocations, i0);

		testedActChainLocations = "1 2 2 3 2 2 2 1 4 1";
		expectedSubtourIndexations.put(testedActChainLocations, "4 0 1 1 2 3 4 5 5");
		expectedNumSubtours.put(testedActChainLocations, i6);
		
		testedActChainLocations = "1 2 3 4 3 1";
		expectedSubtourIndexations.put(testedActChainLocations, "1 1 0 0 1");
		expectedNumSubtours.put(testedActChainLocations, i2);
		
		PlanomatConfigGroup.TripStructureAnalysisLayerOption subtourAnalysisLocationType = config.planomat().getTripStructureAnalysisLayer();
		Location location = null;
		ActivityImpl act = null;
		for (Entry<String, String> entry: expectedSubtourIndexations.entrySet()) {
			String facString  = entry.getKey();

			PlanImpl plan = new org.matsim.core.population.PlanImpl(person);

			String[] locationIdSequence = facString.split(" ");
			for (int aa=0; aa < locationIdSequence.length; aa++) {
				location = layer.getLocation(new IdImpl(locationIdSequence[aa]));
				if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.facility.equals(subtourAnalysisLocationType)) {
					act = plan.createActivity("actAtFacility" + locationIdSequence[aa], (ActivityFacility) location);
				} else if (PlanomatConfigGroup.TripStructureAnalysisLayerOption.link.equals(subtourAnalysisLocationType)) {
					act = plan.createActivity("actOnLink" + locationIdSequence[aa], (Link) location);
				}
				act.setEndTime(10*3600);
				if (aa != (locationIdSequence.length - 1)) {
					plan.createLeg(TransportMode.car);
				}
			}
			testee.run(plan);
			
			StringBuilder builder = new StringBuilder();
			for (int value : testee.getSubtourIndexation()) {
				builder.append(Integer.toString(value));
				builder.append(' ');
			}
			String actualSubtourIndexation = builder.substring(0, builder.length() - 1);
			assertEquals("failure testing " + facString, entry.getValue(), actualSubtourIndexation);
			
			assertEquals("failure testing " + facString, expectedNumSubtours.get(facString).intValue(), testee.getNumSubtours());
		}

	}

}
