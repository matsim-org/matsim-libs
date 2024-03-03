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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.Pollutant;
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
	final void testEmissionPerPersonWarmEventHandler(){

		EmissionsPerPersonWarmEventHandler handler = new EmissionsPerPersonWarmEventHandler();

		//create vehicles and links
		Id<Vehicle> vehicle1 = Id.create("v1", Vehicle.class);
		Id<Vehicle> vehicle2 = Id.create("v2", Vehicle.class);
		Id<Link> link1 = Id.create("1", Link.class);
		Id<Link> link2 = Id.create("2", Link.class);

		//first event: create and handle
		Map<Pollutant,Double> warmEm1 = new HashMap<>();
		//		return key;
		warmEm1.put( Pollutant.CO, 7.1 );
		//		return key;
		warmEm1.put( Pollutant.NOx, 11.9 );
		WarmEmissionEvent event1 = new WarmEmissionEvent(0., link2, vehicle1, warmEm1);
		handler.handleEvent(event1);

		//second event: create and handle
		Map<Pollutant,Double> warmEm2 = new HashMap<>();
		//		return key;
		warmEm2.put( Pollutant.CO, 23.9 );
		//		return key;
		warmEm2.put( Pollutant.PM, 18.1 );
		WarmEmissionEvent event2 = new WarmEmissionEvent(0.8, link1, vehicle2, warmEm2);
		handler.handleEvent(event2);

		//third event: create and handle
		Map<Pollutant,Double> warmEm3 = new HashMap<>();
		//		return key;
		warmEm3.put( Pollutant.NOx, 12.4 );
		WarmEmissionEvent event3 = new WarmEmissionEvent(0., link2, vehicle1, warmEm3);
		handler.handleEvent(event3);

		//fourth event: create and handle
		Map<Pollutant,Double> warmEm4 = new HashMap<>();
		WarmEmissionEvent event4 = new WarmEmissionEvent(20., link2, vehicle2, warmEm4);
		handler.handleEvent(event4);

		//fifth event: create and handle
		Map<Pollutant,Double> warmEm5 = new HashMap<>();
		//		return key;
		warmEm5.put( Pollutant.NOx, 19.8 );
		//		return key;
		warmEm5.put( Pollutant.CO, 10.0 );
		WarmEmissionEvent event5 = new WarmEmissionEvent(55., link1, vehicle1, warmEm5);
		handler.handleEvent(event5);


//		if ( true ) {
//			throw new RuntimeException("code below will probably no longer work since keys are now strings not enums.  kai, dec'18") ;
//		}

		Map<Id<Person>, Map<Pollutant, Double>> wepp = handler.getWarmEmissionsPerPerson();
		//CO vehicle 1
		//		return key;
		if(wepp.get(Id.create("v1", Person.class)).containsKey( Pollutant.CO )){
			//		return key;
			Double actualCO1 = wepp.get(Id.create("v1", Person.class)).get( Pollutant.CO );
			Assertions.assertEquals(17.1, actualCO1, MatsimTestUtils.EPSILON, "CO of vehicle 1 should be 17.1 but was "+actualCO1 );
		}else{
			Assertions.fail("No CO values for car 1 found.");
		}
		//NOx vehicle 1
		//		return key;
		if(wepp.get(Id.create("v1", Person.class)).containsKey( Pollutant.NOx )){
			//		return key;
			Double actualNOx1 = wepp.get(Id.create("v1", Person.class)).get( Pollutant.NOx );
			Assertions.assertEquals(44.1, actualNOx1, MatsimTestUtils.EPSILON, "NOx of vehicle 1 should be 44.1 but was "+actualNOx1 );
		}else{
			Assertions.fail("No NOx values for car 1 found.");
		}
		//PM vehicle 1
		//		return key;
		if(wepp.get(Id.create("v1", Person.class)).containsKey( Pollutant.PM )){
			Assertions.fail("There should be no PM values for car 1.");
		}else{
			//		return key;
			Assertions.assertNull(wepp.get(Id.create("v1", Person.class)).get( Pollutant.PM ),"PM of vehicle 1 should be null." );
		}
		//CO vehicle 2
		//		return key;
		if(wepp.get(Id.create("v2", Person.class)).containsKey( Pollutant.CO )){
			//		return key;
			Double actualCO2 = wepp.get(Id.create("v2", Person.class)).get( Pollutant.CO );
			Assertions.assertEquals(23.9, actualCO2, MatsimTestUtils.EPSILON, "CO of vehicle 2 should be 23.9" );
		}else{
			Assertions.fail("No CO values for car 2 found.");
		}
		//NOx vehicle 2
		//		return key;
		if(wepp.get(Id.create("v2", Person.class)).containsKey( Pollutant.NOx )){
			Assertions.fail("There should be no NOx values for car 2.");
		}else{
			//		return key;
			Assertions.assertNull(wepp.get(Id.create("v2", Person.class)).get( Pollutant.NOx ) );
		}
		//PM vehicle 2
		//		return key;
		if(wepp.get(Id.create("v2", Person.class)).containsKey( Pollutant.PM )){
			//		return key;
			Double actualPM2 = wepp.get(Id.create("v2", Person.class)).get( Pollutant.PM );
			Assertions.assertEquals(18.1, actualPM2, MatsimTestUtils.EPSILON, "PM of vehicle 2 should be 18.1" );
		}else{
			Assertions.fail("No PM values for car 2 found.");
		}
		//FC
		//		return key;
		Assertions.assertNull(wepp.get(Id.create("v1", Person.class)).get( Pollutant.FC ) );

	}
}
	

	

