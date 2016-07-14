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

/*
 * test for playground.vsp.emissions.events.WarmEmissionEventImpl
 * 1 test normal functionality
 * 2 test incomplete data
 * 3 test the number of attributes returned
 */

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;


public class TestWarmEmissionEventImpl {
	
	private final Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
	private final Id<Link> linkId = Id.create("link 1", Link.class);
	private final Double co = 20.;
    private final Double c2 = 8.;
    private final Double fc = 30.;
    private final Double hc=4.;
    private final Double nm=5.;
    private final Double n2=6.;
    private final Double nx=7.;
    private final Double pm=8.;
    private final Double so=1.6;
	
	@Test
	public final void testGetAttributesForCompleteEmissionMaps(){
		//test normal functionality
		
		//create a normal event impl
		Map<WarmPollutant, Double> warmEmissionsMap = new HashMap<>();
		setWarmEmissions(warmEmissionsMap);
		WarmEmissionEvent we = new WarmEmissionEvent(0.0, linkId, vehicleId, warmEmissionsMap);
		
		Map<String, String> weg = we.getAttributes();
		Assert.assertEquals("the CO value of this warm emission event was "+ Double.parseDouble(weg.get("CO"))+ "but should have been "+ co, Double.parseDouble(weg.get("CO")), co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the CO2 value of this warm emission event was "+ Double.parseDouble(weg.get("CO2_TOTAL"))+ "but should have been "+ c2, Double.parseDouble(weg.get("CO2_TOTAL")), c2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the FC value of this warm emission event was "+ Double.parseDouble(weg.get("FC"))+ "but should have been "+ fc, Double.parseDouble(weg.get("FC")), fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the HC value of this warm emission event was "+ Double.parseDouble(weg.get("HC"))+ "but should have been "+ hc, Double.parseDouble(weg.get("HC")), hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NMHC value of this warm emission event was "+ Double.parseDouble(weg.get("NMHC"))+ "but should have been "+ nm, Double.parseDouble(weg.get("NMHC")), nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NO2 value of this warm emission event was "+ Double.parseDouble(weg.get("NO2"))+ "but should have been "+ n2, Double.parseDouble(weg.get("NO2")), n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NOX value of this warm emission event was "+ Double.parseDouble(weg.get("NOX"))+ "but should have been "+ nx, Double.parseDouble(weg.get("NOX")), nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the PM value of this warm emission event was "+ Double.parseDouble(weg.get("PM"))+ "but should have been "+ pm, Double.parseDouble(weg.get("PM")), pm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the SO2 value of this warm emission event was "+ Double.parseDouble(weg.get("SO2"))+ "but should have been "+ so, Double.parseDouble(weg.get("SO2")), so, MatsimTestUtils.EPSILON);
	}

	private void setWarmEmissions(Map<WarmPollutant, Double> warmEmissionsMap) {
		warmEmissionsMap.put(WarmPollutant.CO, co);
		warmEmissionsMap.put(WarmPollutant.CO2_TOTAL, c2);
		warmEmissionsMap.put(WarmPollutant.FC, fc);
		warmEmissionsMap.put(WarmPollutant.HC, hc);
		warmEmissionsMap.put(WarmPollutant.NMHC, nm);
		warmEmissionsMap.put(WarmPollutant.NO2, n2);
		warmEmissionsMap.put(WarmPollutant.NOX, nx);
		warmEmissionsMap.put(WarmPollutant.PM, pm);
		warmEmissionsMap.put(WarmPollutant.SO2, so);
	}
	
		@Test
		public final void testGetAttributesForIncompleteMaps(){
			//the getAttributesMethod should
			// - return null if the emission map is empty
			// - throw NullPointerExceptions if the emission values are not set
			// - throw NullPointerExceptions if no emission map is assigned 
			
		//empty map
		Map<WarmPollutant, Double> emptyMap = new HashMap<>();
		WarmEmissionEvent emptyMapEvent = new WarmEmissionEvent(22., linkId, vehicleId, emptyMap);
		
		//values not set
		Map<WarmPollutant, Double> valuesNotSet = new HashMap<>();
		valuesNotSet.put(WarmPollutant.CO, null);
		valuesNotSet.put(WarmPollutant.FC, null);
		valuesNotSet.put(WarmPollutant.HC, null);
		valuesNotSet.put(WarmPollutant.NMHC, null);
		valuesNotSet.put(WarmPollutant.NO2, null);
		valuesNotSet.put(WarmPollutant.NOX, null);
		valuesNotSet.put(WarmPollutant.PM, null);
		WarmEmissionEvent valuesNotSetEvent = new WarmEmissionEvent(44., linkId, vehicleId, valuesNotSet);
		
		//no map
		WarmEmissionEvent noMap = new WarmEmissionEvent(23, linkId, vehicleId, null);
		
		int numberOfWarmPollutants = WarmPollutant.values().length;	

		int valuesNotSetNullPointers =0, noMapNullPointers=0;
		
		for(WarmPollutant wp : WarmPollutant.values()){
			String key= wp.toString();

			//empty map
			Assert.assertNull(emptyMapEvent.getAttributes().get(key));
			
			//values not set
			try{
				valuesNotSetEvent.getAttributes().get(key);
			}
			catch(NullPointerException e){
				valuesNotSetNullPointers++;
			}
			
			//no map
			try{
				noMap.getAttributes().get(key);
			}
			catch(NullPointerException e){
				noMapNullPointers++;
			}
		}
		Assert.assertEquals(numberOfWarmPollutants, valuesNotSetNullPointers);
		Assert.assertEquals(numberOfWarmPollutants, noMapNullPointers);
		}

}
