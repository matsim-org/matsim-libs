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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;

import static org.matsim.contrib.emissions.Pollutant.*;


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
//	private final Set<String> pollutants = new HashSet<>(Arrays.asList(CO, CO2_TOTAL, FC, HC, NMHC, NOx, NO2,PM, SO2));
	private final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ) );


	@Test
	final void testGetAttributesForCompleteEmissionMaps(){
		//test normal functionality
		
		//create a normal event impl
		Map<Pollutant, Double> warmEmissionsMap = new HashMap<>();

		warmEmissionsMap.put(CO, co );
		warmEmissionsMap.put(CO2_TOTAL, c2 );
		warmEmissionsMap.put(FC, fc );
		warmEmissionsMap.put(HC, hc );
		warmEmissionsMap.put(NMHC, nm );
		warmEmissionsMap.put(NO2, n2 );
		warmEmissionsMap.put(NOx, nx );
		warmEmissionsMap.put(PM, pm );
		warmEmissionsMap.put(SO2, so );

		Map<Pollutant,Double> map = new LinkedHashMap<>();
		warmEmissionsMap.forEach( (key,value) -> map.put(  key, value ) );
		// (this could be made more direct)

		WarmEmissionEvent we = new WarmEmissionEvent(0.0, linkId, vehicleId, map);
		
		Map<String, String> weg = we.getAttributes();
		Assertions.assertEquals(Double.parseDouble(weg.get(CO.name())), co, MatsimTestUtils.EPSILON, "the CO value of this warm emission event was "+ Double.parseDouble(weg.get(CO.name()))+ "but should have been "+ co);
		Assertions.assertEquals(Double.parseDouble(weg.get(CO2_TOTAL.name())), c2, MatsimTestUtils.EPSILON, "the CO2 value of this warm emission event was "+ Double.parseDouble(weg.get(CO2_TOTAL.name()))+ "but should have been "+ c2);
		Assertions.assertEquals(Double.parseDouble(weg.get(FC.name())), fc, MatsimTestUtils.EPSILON, "the FC value of this warm emission event was "+ Double.parseDouble(weg.get(FC.name()))+ "but should have been "+ fc);
		Assertions.assertEquals(Double.parseDouble(weg.get(HC.name())), hc, MatsimTestUtils.EPSILON, "the HC value of this warm emission event was "+ Double.parseDouble(weg.get(HC.name()))+ "but should have been "+ hc);
		Assertions.assertEquals(Double.parseDouble(weg.get(NMHC.name())), nm, MatsimTestUtils.EPSILON, "the NMHC value of this warm emission event was "+ Double.parseDouble(weg.get(NMHC.name()))+ "but should have been "+ nm);
		Assertions.assertEquals(Double.parseDouble(weg.get(NO2.name())), n2, MatsimTestUtils.EPSILON, "the NO2 value of this warm emission event was "+ Double.parseDouble(weg.get(NO2.name()))+ "but should have been "+ n2);
		Assertions.assertEquals(Double.parseDouble(weg.get(NOx.name())), nx, MatsimTestUtils.EPSILON, "the NOx value of this warm emission event was "+ Double.parseDouble(weg.get(NOx.name()))+ "but should have been "+ nx);
		Assertions.assertEquals(Double.parseDouble(weg.get(PM.name())), pm, MatsimTestUtils.EPSILON, "the PM value of this warm emission event was "+ Double.parseDouble(weg.get(PM.name()))+ "but should have been "+ pm);
		Assertions.assertEquals(Double.parseDouble(weg.get(SO2.name())), so, MatsimTestUtils.EPSILON, "the SO2 value of this warm emission event was "+ Double.parseDouble(weg.get(SO2.name()))+ "but should have been "+ so);
	}

	@Test
	final void testGetAttributesForIncompleteMaps(){
			//the getAttributesMethod should
			// - return null if the emission map is empty
			// - throw NullPointerExceptions if the emission values are not set
			// - throw NullPointerExceptions if no emission map is assigned 
			
		//empty map
		Map<Pollutant, Double> emptyMap = new HashMap<>();
		WarmEmissionEvent emptyMapEvent = new WarmEmissionEvent(22., linkId, vehicleId, emptyMap);
		
		//values not set
		Map<Pollutant, Double> valuesNotSet = new HashMap<>();
		valuesNotSet.put(CO, null);
		valuesNotSet.put(FC, null);
		valuesNotSet.put(HC, null);
		valuesNotSet.put(NMHC, null);
		valuesNotSet.put(NO2, null);
		valuesNotSet.put(NOx, null);
		valuesNotSet.put(PM, null);

		Map<Pollutant,Double> map = new LinkedHashMap<>();
		valuesNotSet.forEach( (key,value) -> map.put(  key, value ) );
		// (this could be made more direct)

		WarmEmissionEvent valuesNotSetEvent = new WarmEmissionEvent(44., linkId, vehicleId, map);
		
		//no map
		WarmEmissionEvent noMap = new WarmEmissionEvent(23, linkId, vehicleId, null);
		
		int numberOfWarmPollutants = pollutants.size();

		int valuesNotSetNullPointers =0, noMapNullPointers=0;
		
		for( Pollutant wpEnum : pollutants){
			String wp=wpEnum.name();

			//empty map
			Assertions.assertNull(emptyMapEvent.getAttributes().get(wp));
			
			//values not set
			try{
				valuesNotSetEvent.getAttributes().get(wp);
			}
			catch(NullPointerException e){
				valuesNotSetNullPointers++;
			}
			
			//no map
			try{
				noMap.getAttributes().get(wp);
			}
			catch(NullPointerException e){
				noMapNullPointers++;
			}
		}
		Assertions.assertEquals(numberOfWarmPollutants, valuesNotSetNullPointers);
		Assertions.assertEquals(numberOfWarmPollutants, noMapNullPointers);
		}

}
