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
		
		//TODO error messages
		
		String message ="";
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
		Assert.assertEquals(message, Double.parseDouble(ceg.get("CO")), co, MatsimTestUtils.EPSILON);
		Assert.assertEquals(message, Double.parseDouble(ceg.get("FC")), fc, MatsimTestUtils.EPSILON);
		Assert.assertEquals(message, Double.parseDouble(ceg.get("HC")), hc, MatsimTestUtils.EPSILON);
		Assert.assertEquals(message, Double.parseDouble(ceg.get("NMHC")), nm, MatsimTestUtils.EPSILON);
		Assert.assertEquals(message, Double.parseDouble(ceg.get("NO2")), n2, MatsimTestUtils.EPSILON);
		Assert.assertEquals(message, Double.parseDouble(ceg.get("NOX")), nx, MatsimTestUtils.EPSILON);
		Assert.assertEquals(message, Double.parseDouble(ceg.get("PM")), pm, MatsimTestUtils.EPSILON);
		
		
		//hier: leere Map, null usw.
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


		//neben den emissionen kennen events time, type, linkId, vehicleId = 4
		int halfNullPointers =0;
		
		for(ColdPollutant cp : ColdPollutant.values()){
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
		Assert.assertEquals(numberOfColdPollutants, halfNullPointers);
		
		int numberOfEventAttributes; // = 4; 
		//linkId, vehicleId, coldEmissions
		numberOfEventAttributes = ColdEmissionEventImpl.class.getDeclaredFields().length;
		//time as double, time as string, type
		numberOfEventAttributes += ColdEmissionEventImpl.class.getSuperclass().getDeclaredFields().length;

		//-1 weil die Zeit in der Ausgabe von getAttributes nur einmal auftaucht
		//-1 weil in der Ausgabe von getAttributes die Map der Pollutants nicht mehr als ein Attribut auftaucht
		//stattdessen werden die Polls einzeln aufgezaehlt, also plus deren Anzahl
		Assert.assertEquals(numberOfEventAttributes -1 -1 +numberOfColdPollutants, ceg.size());

		
	}

}
