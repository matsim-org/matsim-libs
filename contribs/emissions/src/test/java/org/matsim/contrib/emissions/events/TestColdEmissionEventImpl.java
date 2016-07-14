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


package org.matsim.contrib.emissions.events;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;


/*
 * test for playground.vsp.emissions.events.ColdEmissionEventImpl
 * 1 test normal functionality
 * 2 test incomplete data
 * 3 test the number of attributes returned
 */

public class TestColdEmissionEventImpl {
			private final Double co = 20.;
    private final Double fc = 30.;
    private final Double hc=4.;
    private final Double nm=5.;
    private final Double n2=6.;
    private final Double nx=7.;
    private final Double pm=8.;
					private final Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
					private final Id<Link> linkId = Id.create("link 1", Link.class);
	@Test
	public final void testGetAttributesForCompleteEmissionMaps(){
		//test normal functionality

		//create a normal event impl
		Map<ColdPollutant, Double> coldEmissionsMap = new HashMap<>();
		setColdEmissions(coldEmissionsMap);
		ColdEmissionEvent ce = new ColdEmissionEvent(0.0, linkId, vehicleId, coldEmissionsMap);
		
		Map<String, String> ceg = ce.getAttributes();
		Assert.assertEquals("the CO value of this cold emission event was "+ Double.parseDouble(ceg.get("CO"))+ "but should have been "+ co, Double.parseDouble(ceg.get("CO")), co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the FC value of this cold emission event was "+ Double.parseDouble(ceg.get("FC"))+ "but should have been "+ fc, Double.parseDouble(ceg.get("FC")), fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the HC value of this cold emission event was "+ Double.parseDouble(ceg.get("HC"))+ "but should have been "+ hc, Double.parseDouble(ceg.get("HC")), hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NMHC value of this cold emission event was "+ Double.parseDouble(ceg.get("NMHC"))+ "but should have been "+ nm, Double.parseDouble(ceg.get("NMHC")), nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NO2 value of this cold emission event was "+ Double.parseDouble(ceg.get("NO2"))+ "but should have been "+ n2, Double.parseDouble(ceg.get("NO2")), n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NOX value of this cold emission event was "+ Double.parseDouble(ceg.get("NOX"))+ "but should have been "+ nx, Double.parseDouble(ceg.get("NOX")), nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the PM value of this cold emission event was "+ Double.parseDouble(ceg.get("PM"))+ "but should have been "+ pm, Double.parseDouble(ceg.get("PM")), pm, MatsimTestUtils.EPSILON);
		
	}

	private void setColdEmissions(Map<ColdPollutant, Double> coldEmissionsMap) {
		coldEmissionsMap.put(ColdPollutant.CO, co);
		coldEmissionsMap.put(ColdPollutant.FC, fc);
		coldEmissionsMap.put(ColdPollutant.HC, hc);
		coldEmissionsMap.put(ColdPollutant.NMHC, nm);
		coldEmissionsMap.put(ColdPollutant.NO2, n2);
		coldEmissionsMap.put(ColdPollutant.NOX, nx);
		coldEmissionsMap.put(ColdPollutant.PM, pm);
	}
	
	@Test
	public final void testGetAttributesForIncompleteMaps(){
		//the getAttributesMethod should
		// - return null if the emission map is empty
		// - throw NullPointerExceptions if the emission values are not set
		// - throw NullPointerExceptions if no emission map is assigned 
		
		//empty map
		Map<ColdPollutant, Double> emptyMap = new HashMap<>();
		ColdEmissionEvent emptyMapEvent = new ColdEmissionEvent(22., linkId, vehicleId, emptyMap);
		
		//values not set
		Map<ColdPollutant, Double> valuesNotSet = new HashMap<>();
		valuesNotSet.put(ColdPollutant.CO, null);
		valuesNotSet.put(ColdPollutant.FC, null);
		valuesNotSet.put(ColdPollutant.HC, null);
		valuesNotSet.put(ColdPollutant.NMHC, null);
		valuesNotSet.put(ColdPollutant.NO2, null);
		valuesNotSet.put(ColdPollutant.NOX, null);
		valuesNotSet.put(ColdPollutant.PM, null);
		ColdEmissionEvent valuesNotSetEvent = new ColdEmissionEvent(44., linkId, vehicleId, valuesNotSet);
		
		//no map
		ColdEmissionEvent noMap = new ColdEmissionEvent(50., linkId, vehicleId, null);
		
		int numberOfColdPollutants = ColdPollutant.values().length;	

		int valNullPointers = 0, noMapNullPointers=0;
		
		for(ColdPollutant cp : ColdPollutant.values()){
			String key= cp.toString();

			//empty map
			Assert.assertNull(emptyMapEvent.getAttributes().get(key));
			
			//values not set
			try{
				valuesNotSetEvent.getAttributes().get(key);
			}
			catch(NullPointerException e){
				valNullPointers ++;
			}
			
			//no map
			try{
				noMap.getAttributes().get(key);
			}
			catch(NullPointerException e){
				noMapNullPointers++;
			}
		}
		Assert.assertEquals(numberOfColdPollutants, valNullPointers);
		Assert.assertEquals(numberOfColdPollutants, noMapNullPointers);
	}
	
}
