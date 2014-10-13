/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestWarmEmissionHandler.java                                                       *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.vsp.analysis.modules.emissions;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonWarmEventHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * test for playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonWarmEventHandler
 * 
 * create and handle some events for different link and vehicle id
 * assert that all used pollutants are found in the corresponding maps but nothing else
 * 
 * @author julia 
 **/

public class TestWarmEmissionHandler {
		
	@Test
	public final void testEmissionPerPersonWarmEventHandler(){
		
		EmissionsPerPersonWarmEventHandler handler = new EmissionsPerPersonWarmEventHandler();
		
		//create vehicles and links
		IdImpl vehicle1 = new IdImpl("v1");
		IdImpl vehicle2 = new IdImpl("v2");
		IdImpl link1 = new IdImpl("1");
		IdImpl link2 = new IdImpl("2");
		
		//first event: create and handle
			Map<WarmPollutant,Double> warmEm1 = new HashMap<WarmPollutant, Double>();
			warmEm1.put(WarmPollutant.CO, 7.1);
			warmEm1.put(WarmPollutant.NOX, 11.9);
			WarmEmissionEvent event1 = new WarmEmissionEvent(0., link2, vehicle1, warmEm1);
			handler.handleEvent(event1);
		
		//second event: create and handle
				Map<WarmPollutant,Double> warmEm2 = new HashMap<WarmPollutant, Double>();
				warmEm2.put(WarmPollutant.CO, 23.9);
				warmEm2.put(WarmPollutant.PM, 18.1);
				WarmEmissionEvent event2 = new WarmEmissionEvent(0.8, link1, vehicle2, warmEm2);
				handler.handleEvent(event2);
				
		//third event: create and handle
				Map<WarmPollutant,Double> warmEm3 = new HashMap<WarmPollutant, Double>();
				warmEm3.put(WarmPollutant.NOX, 12.4);
				WarmEmissionEvent event3 = new WarmEmissionEvent(0., link2, vehicle1, warmEm3);
				handler.handleEvent(event3);
						
		//fourth event: create and handle
				Map<WarmPollutant,Double> warmEm4 = new HashMap<WarmPollutant, Double>();
				WarmEmissionEvent event4 = new WarmEmissionEvent(20., link2, vehicle2, warmEm4);
				handler.handleEvent(event4);
				
		//fifth event: create and handle
				Map<WarmPollutant,Double> warmEm5 = new HashMap<WarmPollutant, Double>();
				warmEm5.put(WarmPollutant.NOX, 19.8);
				warmEm5.put(WarmPollutant.CO, 10.0);
				WarmEmissionEvent event5 = new WarmEmissionEvent(55., link1, vehicle1, warmEm5);
				handler.handleEvent(event5);
				
		Map<Id<Person>, Map<WarmPollutant, Double>> wepp = handler.getWarmEmissionsPerPerson();
		//CO vehicle 1
		if(wepp.get(new IdImpl("v1")).containsKey(WarmPollutant.CO)){
			Double actualCO1 = wepp.get(new IdImpl("v1")).get(WarmPollutant.CO);
			Assert.assertEquals("CO of vehicle 1 should be 17.1 but was "+actualCO1, new Double(17.1), actualCO1, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No CO values for car 1 found.");
		}			
		//NOX vehicle 1
		if(wepp.get(new IdImpl("v1")).containsKey(WarmPollutant.NOX)){
			Double actualNOX1 = wepp.get(new IdImpl("v1")).get(WarmPollutant.NOX);
			Assert.assertEquals("NOX of vehicle 1 should be 44.1 but was "+actualNOX1, new Double(44.1), actualNOX1, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No NOX values for car 1 found.");
		}
		//PM vehicle 1
		if(wepp.get(new IdImpl("v1")).containsKey(WarmPollutant.PM)){
			Assert.fail("There should be no PM values for car 1.");
		}else{
			Assert.assertNull("PM of vehicle 1 should be null.",wepp.get(new IdImpl("v1")).get(WarmPollutant.PM));
		}
		//CO vehicle 2
		if(wepp.get(new IdImpl("v2")).containsKey(WarmPollutant.CO)){
			Double actualCO2 = wepp.get(new IdImpl("v2")).get(WarmPollutant.CO);
			Assert.assertEquals("CO of vehicle 2 should be 23.9",  new Double(23.9), actualCO2, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No CO values for car 2 found.");
		}
		//NOX vehicle 2
		if(wepp.get(new IdImpl("v2")).containsKey(WarmPollutant.NOX)){
			Assert.fail("There should be no NOX values for car 2.");
		}else{
			Assert.assertNull(wepp.get(new IdImpl("v2")).get(WarmPollutant.NOX));
		}
		//PM vehicle 2
		if(wepp.get(new IdImpl("v2")).containsKey(WarmPollutant.PM)){
			Double actualPM2 = wepp.get(new IdImpl("v2")).get(WarmPollutant.PM);
			Assert.assertEquals("PM of vehicle 2 should be 18.1", new Double(18.1), actualPM2, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No PM values for car 2 found.");
		}
		//FC
		Assert.assertNull(wepp.get(new IdImpl("v1")).get(WarmPollutant.FC));
		
	}
}
	

	

