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


package playground.vsp.emissions.events;

/*
 * test for playground.vsp.emissions.events.WarmEmissionEventImpl
 * 1 test normal functionality
 * 2 test incomplete data
 * 3 test the number of attributes returned
 */

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.emissions.events.ColdEmissionEventImpl;
import playground.vsp.emissions.events.WarmEmissionEventImpl;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

public class TestWarmEmissionEventImpl {
	
	Id vehicleId =new IdImpl("veh 1");
	Id linkId = new IdImpl("link 1");
	Double co = 20., c2 = 8., fc = 30., hc=4., nm=5., n2=6., nx=7., pm=8., so=1.6;
	
	@Test
	public final void testGetAttributesForCompleteEmissionMaps(){
		//test normal functionality
		
		//create a normal event impl
		Map<WarmPollutant, Double> warmEmissionsMap = new HashMap<WarmPollutant, Double>();
		setWarmEmissions(warmEmissionsMap);
		WarmEmissionEventImpl we = new WarmEmissionEventImpl(0.0, linkId, vehicleId, warmEmissionsMap);
		
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
		Map<WarmPollutant, Double> emptyMap = new HashMap<WarmPollutant, Double>();
		WarmEmissionEventImpl emptyMapEvent = new WarmEmissionEventImpl(22., linkId, vehicleId, emptyMap);
		
		//values not set
		Map<WarmPollutant, Double> valuesNotSet = new HashMap<WarmPollutant, Double>();
		valuesNotSet.put(WarmPollutant.CO, null);
		valuesNotSet.put(WarmPollutant.FC, null);
		valuesNotSet.put(WarmPollutant.HC, null);
		valuesNotSet.put(WarmPollutant.NMHC, null);
		valuesNotSet.put(WarmPollutant.NO2, null);
		valuesNotSet.put(WarmPollutant.NOX, null);
		valuesNotSet.put(WarmPollutant.PM, null);
		WarmEmissionEventImpl valuesNotSetEvent = new WarmEmissionEventImpl(44., linkId, vehicleId, valuesNotSet);
		
		//no map
		WarmEmissionEventImpl noMap = new WarmEmissionEventImpl(23, linkId, vehicleId, null);
		
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
		
		public final void testgetAttributes_numberOfAttributes(){
			//the number of attributes returned by getAttributes is related to the number of fields of warmEmissionEvent
			
			// create a normal event impl
			Map<WarmPollutant, Double> warmEmissionsMap = new HashMap<WarmPollutant, Double>();
			setWarmEmissions(warmEmissionsMap);
			WarmEmissionEventImpl we = new WarmEmissionEventImpl(0.0, linkId, vehicleId, warmEmissionsMap);
			Map<String, String> weg = we.getAttributes();
		
			int numberOfWarmPollutants = WarmPollutant.values().length;	

		// event parameters beside emissions: time, type, linkId, vehicleId = 4
		int numberOfEventAttributes; // = 4; 
		// linkId, vehicleId, coldEmissions
		numberOfEventAttributes = ColdEmissionEventImpl.class.getFields().length;
		//time as double, time as string, type

		// -1 because the event type appears twice - once from the coldEmissionEvent and once from the superclass event
		// the list of pollutants is not a field of coldEmissionEventImpl
		// getAttributes does return each pollutant separately
		// -> +1 for each pollutant
		Assert.assertEquals(numberOfEventAttributes -1 + numberOfWarmPollutants, weg.size());

		
	}

}
