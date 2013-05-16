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


package playground.emissions.events;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.vsp.emissions.events.WarmEmissionEventImpl;
import playground.vsp.emissions.types.WarmPollutant;

public class TestWarmEmissionEventImpl {
	
	@Test
	public final void testGetAttributes(){
		
		//values
		Double co = 20., c2 = 8., fc = 30., hc=4., nm=5., n2=6., nx=7., pm=8., so=1.6;
		
		Map<WarmPollutant, Double> warmEmissionsMap = new HashMap<WarmPollutant, Double>();
		warmEmissionsMap.put(WarmPollutant.CO, co);
		warmEmissionsMap.put(WarmPollutant.CO2_TOTAL, c2);
		warmEmissionsMap.put(WarmPollutant.FC, fc);
		warmEmissionsMap.put(WarmPollutant.HC, hc);
		warmEmissionsMap.put(WarmPollutant.NMHC, nm);
		warmEmissionsMap.put(WarmPollutant.NO2, n2);
		warmEmissionsMap.put(WarmPollutant.NOX, nx);
		warmEmissionsMap.put(WarmPollutant.PM, pm);
		warmEmissionsMap.put(WarmPollutant.SO2, so);
		
		Id vehicleId =new IdImpl("veh 1");
		Id linkId = new IdImpl("link 1");
		WarmEmissionEventImpl ce = new WarmEmissionEventImpl(0.0, linkId, vehicleId, warmEmissionsMap);
		
		Map<String, String> ceg = ce.getAttributes();
		Assert.assertEquals("the CO value of this warm emission event was "+ Double.parseDouble(ceg.get("CO"))+ "but should have been "+ co, Double.parseDouble(ceg.get("CO")), co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the CO2 value of this warm emission event was "+ Double.parseDouble(ceg.get("CO2"))+ "but should have been "+ c2, Double.parseDouble(ceg.get("CO2_TOTAL")), c2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the FC value of this warm emission event was "+ Double.parseDouble(ceg.get("FC"))+ "but should have been "+ fc, Double.parseDouble(ceg.get("FC")), fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the HC value of this warm emission event was "+ Double.parseDouble(ceg.get("HC"))+ "but should have been "+ hc, Double.parseDouble(ceg.get("HC")), hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NMHC value of this warm emission event was "+ Double.parseDouble(ceg.get("NMHC"))+ "but should have been "+ nm, Double.parseDouble(ceg.get("NMHC")), nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NO2 value of this warm emission event was "+ Double.parseDouble(ceg.get("NO2"))+ "but should have been "+ n2, Double.parseDouble(ceg.get("NO2")), n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NOX value of this warm emission event was "+ Double.parseDouble(ceg.get("NOX"))+ "but should have been "+ nx, Double.parseDouble(ceg.get("NOX")), nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the PM value of this warm emission event was "+ Double.parseDouble(ceg.get("PM"))+ "but should have been "+ pm, Double.parseDouble(ceg.get("PM")), pm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the SO2 value of this warm emission event was "+ Double.parseDouble(ceg.get("SO2"))+ "but should have been "+ so, Double.parseDouble(ceg.get("SO2")), so, MatsimTestUtils.EPSILON);
		
		//empty map, no map
		Map<WarmPollutant, Double> emptyMap = new HashMap<WarmPollutant, Double>();
		WarmEmissionEventImpl emptyMapEvent = new WarmEmissionEventImpl(22., linkId, vehicleId, emptyMap);
		
		Map<WarmPollutant, Double> halfMap = new HashMap<WarmPollutant, Double>();
		halfMap.put(WarmPollutant.CO, null);
		halfMap.put(WarmPollutant.FC, null);
		halfMap.put(WarmPollutant.HC, null);
		halfMap.put(WarmPollutant.NMHC, null);
		halfMap.put(WarmPollutant.NO2, null);
		halfMap.put(WarmPollutant.NOX, null);
		halfMap.put(WarmPollutant.PM, null);
		

		WarmEmissionEventImpl halfEvent = new WarmEmissionEventImpl(44., linkId, vehicleId, halfMap);
		
		int numberOfWarmPollutants = WarmPollutant.values().length;	


		int halfNullPointers =0;
		
		for(WarmPollutant cp : WarmPollutant.values()){
			String key= cp.toString();
			//normal event
			Assert.assertNotNull("A value for " +key+ " was initialised.", ce.getAttributes().get(key));
			
			//TODO codeliste: half empty event -- this should fail -> nullpointer
			//aber es wird null zurueck gegeben... ggf den test hier aendern

			Assert.assertNull(emptyMapEvent.getAttributes().get(key));
			
			//half empty map event
			try{
				halfEvent.getAttributes().get(key);
			}
			catch(NullPointerException e){
				halfNullPointers ++;
			}

		}
		Assert.assertEquals(numberOfWarmPollutants, halfNullPointers);
		
		//event parameters beside emissions: time, type, linkId, vehicleId = 4
		int numberOfEventAttributes; // = 4; 
		//linkId, vehicleId, warmEmissions
		numberOfEventAttributes = WarmEmissionEventImpl.class.getDeclaredFields().length;
		//time as double, time as string, type
		numberOfEventAttributes += WarmEmissionEventImpl.class.getSuperclass().getDeclaredFields().length;

		//-1 because the time parameter appears only once in the output of getAttributes
		//-1 because getAttributes does not return the list of pollutants
		//but each pollutant seperatly -> +1 for each
		Assert.assertEquals(numberOfEventAttributes -1 -1 +numberOfWarmPollutants, ceg.size());

		
	}

}
