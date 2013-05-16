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

import playground.vsp.emissions.events.ColdEmissionEventImpl;
import playground.vsp.emissions.types.ColdPollutant;

public class TestColdEmissionEventImpl {
	
	@Test
	public final void testGetAttributes(){
		
		//values
		Double co = 20., fc = 30., hc=4., nm=5., n2=6., nx=7., pm=8.;
		
		Map<ColdPollutant, Double> coldEmissionsMap = new HashMap<ColdPollutant, Double>();
		coldEmissionsMap.put(ColdPollutant.CO, co);
		coldEmissionsMap.put(ColdPollutant.FC, fc);
		coldEmissionsMap.put(ColdPollutant.HC, hc);
		coldEmissionsMap.put(ColdPollutant.NMHC, nm);
		coldEmissionsMap.put(ColdPollutant.NO2, n2);
		coldEmissionsMap.put(ColdPollutant.NOX, nx);
		coldEmissionsMap.put(ColdPollutant.PM, pm);
		
		Id vehicleId =new IdImpl("veh 1");
		Id linkId = new IdImpl("link 1");
		ColdEmissionEventImpl ce = new ColdEmissionEventImpl(0.0, linkId, vehicleId, coldEmissionsMap);
		
		Map<String, String> ceg = ce.getAttributes();
		Assert.assertEquals("the CO value of this cold emission event was "+ Double.parseDouble(ceg.get("CO"))+ "but should have been "+ co, Double.parseDouble(ceg.get("CO")), co, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the FC value of this cold emission event was "+ Double.parseDouble(ceg.get("FC"))+ "but should have been "+ fc, Double.parseDouble(ceg.get("FC")), fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the HC value of this cold emission event was "+ Double.parseDouble(ceg.get("HC"))+ "but should have been "+ hc, Double.parseDouble(ceg.get("HC")), hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NMHC value of this cold emission event was "+ Double.parseDouble(ceg.get("NMHC"))+ "but should have been "+ nm, Double.parseDouble(ceg.get("NMHC")), nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NO2 value of this cold emission event was "+ Double.parseDouble(ceg.get("NO2"))+ "but should have been "+ n2, Double.parseDouble(ceg.get("NO2")), n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the NOX value of this cold emission event was "+ Double.parseDouble(ceg.get("NOX"))+ "but should have been "+ nx, Double.parseDouble(ceg.get("NOX")), nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals("the PM value of this cold emission event was "+ Double.parseDouble(ceg.get("PM"))+ "but should have been "+ pm, Double.parseDouble(ceg.get("PM")), pm, MatsimTestUtils.EPSILON);
		
		//empty map, no map
		Map<ColdPollutant, Double> emptyMap = new HashMap<ColdPollutant, Double>();
		ColdEmissionEventImpl emptyMapEvent = new ColdEmissionEventImpl(22., linkId, vehicleId, emptyMap);
		
		Map<ColdPollutant, Double> halfMap = new HashMap<ColdPollutant, Double>();
		halfMap.put(ColdPollutant.CO, null);
		halfMap.put(ColdPollutant.FC, null);
		halfMap.put(ColdPollutant.HC, null);
		halfMap.put(ColdPollutant.NMHC, null);
		halfMap.put(ColdPollutant.NO2, null);
		halfMap.put(ColdPollutant.NOX, null);
		halfMap.put(ColdPollutant.PM, null);
		
		ColdEmissionEventImpl halfEvent = new ColdEmissionEventImpl(44., linkId, vehicleId, halfMap);
		
		int numberOfColdPollutants = ColdPollutant.values().length;	

		int halfNullPointers = 0;
		
		for(ColdPollutant cp : ColdPollutant.values()){
			String key= cp.toString();
			
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
		Assert.assertEquals(numberOfColdPollutants, halfNullPointers);
		
		//event parameters beside emissions: time, type, linkId, vehicleId = 4
		int numberOfEventAttributes; // = 4; 
		//linkId, vehicleId, coldEmissions
		numberOfEventAttributes = ColdEmissionEventImpl.class.getDeclaredFields().length;
		//time as double, time as string, type
		numberOfEventAttributes += ColdEmissionEventImpl.class.getSuperclass().getDeclaredFields().length;

		//-1 because the time parameter appears only once in the output of getAttributes
		//-1 because getAttributes does not return the list of pollutants
		//but each pollutant seperatly -> +1 for each
		Assert.assertEquals(numberOfEventAttributes -1 -1 + numberOfColdPollutants, ceg.size());
	}
}
