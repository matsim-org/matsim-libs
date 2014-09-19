/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestEmission.java                                                       *
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

package playgrund.vsp.analysis.modules.emissions;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventImpl;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler;

/**
 * test for playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonColdEventHandler
 * 
 * create and handle some events for different link and vehicle id
 * assert that all used pollutants are found in the corresponding maps but nothing else
 * 
 * @author julia
 */

public class TestColdEmissionHandler {
	
	@Test
	public final void testEmissionPerPersonColdEventHandler(){
		
		//vehicle=person. this handler counts the cold emission events per vehicle id
		EmissionsPerPersonColdEventHandler handler = new EmissionsPerPersonColdEventHandler();
		
		//create vehicles
		IdImpl vehicle1 = new IdImpl("v1");
		IdImpl vehicle2 = new IdImpl("v2");
		IdImpl link1 = new IdImpl("1");
		IdImpl link2 = new IdImpl("2");
		Double time1 = .0, time2 = 2.0;
		
		//first event: create and handle
			Map<ColdPollutant,Double> coldEm1 = new HashMap<ColdPollutant, Double>();
			coldEm1.put(ColdPollutant.CO, 7.1);
			coldEm1.put(ColdPollutant.NOX, 11.9);
			ColdEmissionEvent event1 = new ColdEmissionEventImpl(time1, link1, vehicle1, coldEm1);
			handler.handleEvent(event1);
		
		//second event: create and handle
				Map<ColdPollutant,Double> coldEm2 = new HashMap<ColdPollutant, Double>();
				coldEm2.put(ColdPollutant.CO, 23.9);
				coldEm2.put(ColdPollutant.PM, 18.1);
				ColdEmissionEvent event2 = new ColdEmissionEventImpl(time1, link1, vehicle2, coldEm2);
				handler.handleEvent(event2);
				
		//third event: create and handle
				Map<ColdPollutant,Double> coldEm3 = new HashMap<ColdPollutant, Double>();
				coldEm3.put(ColdPollutant.NOX, 12.4);
				ColdEmissionEvent event3 = new ColdEmissionEventImpl(time2, link2, vehicle1, coldEm3);
				handler.handleEvent(event3);
						
		//fourth event: create and handle
				Map<ColdPollutant,Double> coldEm4 = new HashMap<ColdPollutant, Double>();
				ColdEmissionEvent event4 = new ColdEmissionEventImpl(time1, link2, vehicle2, coldEm4);
				handler.handleEvent(event4);
				
		//fifth event: create and handle
				Map<ColdPollutant,Double> coldEm5 = new HashMap<ColdPollutant, Double>();
				coldEm5.put(ColdPollutant.NOX, 19.8);
				coldEm5.put(ColdPollutant.CO, 10.0);
				ColdEmissionEvent event5 = new ColdEmissionEventImpl(time2, link1, vehicle1, coldEm5);
				handler.handleEvent(event5);
				
		Map<Id<Person>, Map<ColdPollutant, Double>> cepp = handler.getColdEmissionsPerPerson(); 
		Double actualCO1 =0.0, actualNOX1 =0.0, actualPM1=0.0, actualCO2=.0, actualNOX2=.0, actualPM2 =0.0;
		
		if(cepp.get(new IdImpl("v1")).containsKey(ColdPollutant.CO))actualCO1 = cepp.get(new IdImpl("v1")).get(ColdPollutant.CO);  
		if(cepp.get(new IdImpl("v1")).containsKey(ColdPollutant.NOX))actualNOX1 = cepp.get(new IdImpl("v1")).get(ColdPollutant.NOX); 
		if(cepp.get(new IdImpl("v1")).containsKey(ColdPollutant.PM))actualPM1 = cepp.get(new IdImpl("v1")).get(ColdPollutant.PM); 
		if(cepp.get(new IdImpl("v2")).containsKey(ColdPollutant.CO))actualCO2 = cepp.get(new IdImpl("v2")).get(ColdPollutant.CO); 
		if(cepp.get(new IdImpl("v2")).containsKey(ColdPollutant.NOX))actualNOX2 = cepp.get(new IdImpl("v2")).get(ColdPollutant.NOX); 
		if(cepp.get(new IdImpl("v2")).containsKey(ColdPollutant.PM))actualPM2 = cepp.get(new IdImpl("v2")).get(ColdPollutant.PM); 
		
		// assert that values were set correctly
		Assert.assertEquals("CO of vehicle 1 should be 17.1 but was "+actualCO1, new Double(17.1), actualCO1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOX of vehicle 1 should be 44.1 but was "+actualNOX1, new Double(44.1), actualNOX1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM of vehicle 1 should be 0 but was " +actualPM1, new Double(0.0), actualPM1, MatsimTestUtils.EPSILON);
		Assert.assertEquals("CO of vehicle 2 should be 23.9 but was " +actualCO2, new Double(23.9), actualCO2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("NOX of vehicle 2 should be 0 but was " +actualNOX2 , new Double(0.0), actualNOX2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("PM of vehicle 2 should be 18.1 but was " +actualPM2, new Double(18.1), actualPM2, MatsimTestUtils.EPSILON);
		
		// nothing else in the map
		Assert.assertEquals("There should be two types of emissions in this map but " +
				"there were " + cepp.get(new IdImpl("v1")).size()+".", 
				2, cepp.get(new IdImpl("v1")).keySet().size());
		Assert.assertEquals("There should be two types of emissions in this map but " +
				"there were " + cepp.get(new IdImpl("v2")).size()+".",
				2, cepp.get(new IdImpl("v2")).keySet().size());
	}
}
	

	

