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

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import playground.vsp.analysis.modules.emissionsAnalyzer.EmissionsPerPersonWarmEventHandler;

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
		Id<Vehicle> vehicle1 = Id.create("v1", Vehicle.class);
		Id<Vehicle> vehicle2 = Id.create("v2", Vehicle.class);
		Id<Link> link1 = Id.create("1", Link.class);
		Id<Link> link2 = Id.create("2", Link.class);

		//first event: create and handle
		Map<String,Double> warmEm1 = new HashMap<String, Double>();
		//		return key;
		warmEm1.put( WarmPollutant.CO.name(), 7.1 );
		//		return key;
		warmEm1.put( WarmPollutant.NOX.name(), 11.9 );
		WarmEmissionEvent event1 = new WarmEmissionEvent(0., link2, vehicle1, warmEm1);
		handler.handleEvent(event1);

		//second event: create and handle
		Map<String,Double> warmEm2 = new HashMap<String, Double>();
		//		return key;
		warmEm2.put( WarmPollutant.CO.name(), 23.9 );
		//		return key;
		warmEm2.put( WarmPollutant.PM.name(), 18.1 );
		WarmEmissionEvent event2 = new WarmEmissionEvent(0.8, link1, vehicle2, warmEm2);
		handler.handleEvent(event2);

		//third event: create and handle
		Map<String,Double> warmEm3 = new HashMap<String, Double>();
		//		return key;
		warmEm3.put( WarmPollutant.NOX.name(), 12.4 );
		WarmEmissionEvent event3 = new WarmEmissionEvent(0., link2, vehicle1, warmEm3);
		handler.handleEvent(event3);

		//fourth event: create and handle
		Map<String,Double> warmEm4 = new HashMap<String, Double>();
		WarmEmissionEvent event4 = new WarmEmissionEvent(20., link2, vehicle2, warmEm4);
		handler.handleEvent(event4);

		//fifth event: create and handle
		Map<String,Double> warmEm5 = new HashMap<String, Double>();
		//		return key;
		warmEm5.put( WarmPollutant.NOX.name(), 19.8 );
		//		return key;
		warmEm5.put( WarmPollutant.CO.name(), 10.0 );
		WarmEmissionEvent event5 = new WarmEmissionEvent(55., link1, vehicle1, warmEm5);
		handler.handleEvent(event5);


//		if ( true ) {
//			throw new RuntimeException("code below will probably no longer work since keys are now strings not enums.  kai, dec'18") ;
//		}

		Map<Id<Person>, Map<String, Double>> wepp = handler.getWarmEmissionsPerPerson();
		//CO vehicle 1
		//		return key;
		if(wepp.get(Id.create("v1", Person.class)).containsKey( WarmPollutant.CO.name() )){
			//		return key;
			Double actualCO1 = wepp.get(Id.create("v1", Person.class)).get( WarmPollutant.CO.name() );
			Assert.assertEquals("CO of vehicle 1 should be 17.1 but was "+actualCO1, new Double(17.1), actualCO1, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No CO values for car 1 found.");
		}
		//NOX vehicle 1
		//		return key;
		if(wepp.get(Id.create("v1", Person.class)).containsKey( WarmPollutant.NOX.name() )){
			//		return key;
			Double actualNOX1 = wepp.get(Id.create("v1", Person.class)).get( WarmPollutant.NOX.name() );
			Assert.assertEquals("NOX of vehicle 1 should be 44.1 but was "+actualNOX1, new Double(44.1), actualNOX1, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No NOX values for car 1 found.");
		}
		//PM vehicle 1
		//		return key;
		if(wepp.get(Id.create("v1", Person.class)).containsKey( WarmPollutant.PM.name() )){
			Assert.fail("There should be no PM values for car 1.");
		}else{
			//		return key;
			Assert.assertNull("PM of vehicle 1 should be null.",wepp.get(Id.create("v1", Person.class)).get( WarmPollutant.PM.name() ) );
		}
		//CO vehicle 2
		//		return key;
		if(wepp.get(Id.create("v2", Person.class)).containsKey( WarmPollutant.CO.name() )){
			//		return key;
			Double actualCO2 = wepp.get(Id.create("v2", Person.class)).get( WarmPollutant.CO.name() );
			Assert.assertEquals("CO of vehicle 2 should be 23.9",  new Double(23.9), actualCO2, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No CO values for car 2 found.");
		}
		//NOX vehicle 2
		//		return key;
		if(wepp.get(Id.create("v2", Person.class)).containsKey( WarmPollutant.NOX.name() )){
			Assert.fail("There should be no NOX values for car 2.");
		}else{
			//		return key;
			Assert.assertNull(wepp.get(Id.create("v2", Person.class)).get( WarmPollutant.NOX.name() ) );
		}
		//PM vehicle 2
		//		return key;
		if(wepp.get(Id.create("v2", Person.class)).containsKey( WarmPollutant.PM.name() )){
			//		return key;
			Double actualPM2 = wepp.get(Id.create("v2", Person.class)).get( WarmPollutant.PM.name() );
			Assert.assertEquals("PM of vehicle 2 should be 18.1", new Double(18.1), actualPM2, MatsimTestUtils.EPSILON);
		}else{
			Assert.fail("No PM values for car 2 found.");
		}
		//FC
		//		return key;
		Assert.assertNull(wepp.get(Id.create("v1", Person.class)).get( WarmPollutant.FC.name() ) );

	}
}
	

	

