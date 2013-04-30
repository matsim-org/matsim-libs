/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestColdEmissionEventImplementation.java                                *
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

package playground;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.basic.v01.IdImpl;

import playground.vsp.emissions.events.*;
import playground.vsp.emissions.types.ColdPollutant;

//test for playground.vsp.emissions.events.ColdEmissionEventImpl

public class TestColdEmissionEventImplementation {
	
	@Test
	public final void testGetAttributes(){
		
		Map<ColdPollutant, Double> coldEmissionMap1 = new HashMap<ColdPollutant, Double>();
		Map<ColdPollutant, Double> coldEmissionMap2 = new HashMap<ColdPollutant, Double>();
		Map<ColdPollutant, Double> coldEmissionMap3 = new HashMap<ColdPollutant, Double>();
		coldEmissionMap1.put(ColdPollutant.NO2, 33.3);
		coldEmissionMap1.put(ColdPollutant.NOX, 44.4);
		coldEmissionMap2.put(ColdPollutant.NOX, 44.4);
		coldEmissionMap2.put(ColdPollutant.NO2, 33.3);
		
		coldEmissionMap3.put(ColdPollutant.NO2, 31.3);
		coldEmissionMap3.put(ColdPollutant.NOX, 41.4);
		
		ColdEmissionEventImpl ceei1 = new ColdEmissionEventImpl(2, new IdImpl("5"), new IdImpl("veh1"), coldEmissionMap1);
		ColdEmissionEventImpl ceei2 = new ColdEmissionEventImpl(2, new IdImpl("5"), new IdImpl("veh1"), coldEmissionMap2);
		ColdEmissionEventImpl ceei3 = new ColdEmissionEventImpl(2, new IdImpl("5"), new IdImpl("veh1"), coldEmissionMap3);
		
		Assert.assertEquals(ceei1.getAttributes().toString(), ceei1.getAttributes(), ceei2.getAttributes());
		Assert.assertNotSame(ceei3.getAttributes(), ceei2.getAttributes());
		
		coldEmissionMap3.put(ColdPollutant.NOX, 44.4);
		coldEmissionMap3.put(ColdPollutant.NO2, 33.3);
		
		Assert.assertEquals(ceei2.getAttributes()+" does not match " + ceei3.getAttributes(), ceei2.getAttributes(), ceei3.getAttributes());
		//java.lang.AssertionError: {time=2.0, type=coldEmissionEvent, linkId=5, vehicleId=veh1, NO2=33.3, NOX=44.4} 
		// does not match {time=500.0, type=coldEmissionEvent, linkId=7, vehicleId=vehicle2, NO2=33.3, NOX=44.4} 
		// expected:<{time=2.0, type=coldEmissionEvent, linkId=5, vehicleId=veh1, NO2=33.3, NOX=44.4}> 
		// but was:<{time=500.0, type=coldEmissionEvent, linkId=7, vehicleId=vehicle2, NO2=33.3, NOX=44.4}>
			}
	
}
	

	

