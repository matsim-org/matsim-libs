/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestWarmEmissionEventImplementation.java                                *
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
import playground.vsp.emissions.types.WarmPollutant;

//test for playground.vsp.emissions.events.WarmEmissionEventImpl

public class TestWarmEmissionEventImplementation {
	
	@Test
	
	public final void testGetAttributes(){
		
		Map<WarmPollutant, Double> warmEmissionMap1 = new HashMap<WarmPollutant, Double>();
		Map<WarmPollutant, Double> warmEmissionMap2 = new HashMap<WarmPollutant, Double>();
		Map<WarmPollutant, Double> warmEmissionMap3 = new HashMap<WarmPollutant, Double>();
		warmEmissionMap1.put(WarmPollutant.NO2, 33.3);
		warmEmissionMap1.put(WarmPollutant.NOX, 44.4);
		warmEmissionMap2.put(WarmPollutant.NOX, 44.4);
		warmEmissionMap2.put(WarmPollutant.NO2, 33.3);
		
		warmEmissionMap3.put(WarmPollutant.NO2, 31.3);
		warmEmissionMap3.put(WarmPollutant.NOX, 41.4);
		
		WarmEmissionEventImpl ceei1 = new WarmEmissionEventImpl(2, new IdImpl("5"), new IdImpl("veh1"), warmEmissionMap1);
		WarmEmissionEventImpl ceei2 = new WarmEmissionEventImpl(2, new IdImpl("5"), new IdImpl("veh1"), warmEmissionMap2);
		WarmEmissionEventImpl ceei3 = new WarmEmissionEventImpl(2, new IdImpl("5"), new IdImpl("veh1"), warmEmissionMap3);
		
		Assert.assertEquals(ceei1.getAttributes().toString(), ceei1.getAttributes(), ceei2.getAttributes());
		Assert.assertNotSame(ceei3.getAttributes(), ceei2.getAttributes());
		
		warmEmissionMap3.put(WarmPollutant.NOX, 44.4);
		warmEmissionMap3.put(WarmPollutant.NO2, 33.3);
		
		Assert.assertEquals(ceei2.getAttributes()+" does not match " + ceei3.getAttributes(), ceei2.getAttributes(), ceei3.getAttributes());
		//java.lang.AssertionError: {time=2.0, type=warmEmissionEvent, linkId=5, vehicleId=veh1, NO2=33.3, NOX=44.4} does not match {time=500.0, type=warmEmissionEvent, linkId=7, vehicleId=vehicle2, NO2=33.3, NOX=44.4} expected:<{time=2.0, type=warmEmissionEvent, linkId=5, vehicleId=veh1, NO2=33.3, NOX=44.4}> but was:<{time=500.0, type=warmEmissionEvent, linkId=7, vehicleId=vehicle2, NO2=33.3, NOX=44.4}>
			}
	
}
	

	

