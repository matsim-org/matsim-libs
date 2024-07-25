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
	private final Double co_value = 20.;
    private final Double co2total_value = 8.;
    private final Double fc_value = 30.;
    private final Double hc_value =4.;
    private final Double nmhc_value =5.;
    private final Double no2_value =6.;
    private final Double nox_value =7.;
    private final Double pm_value =8.;
    private final Double so2_value =1.6;
	private final Set<Pollutant> pollutants = new HashSet<>( Arrays.asList( Pollutant.values() ) );


	@Test
	final void testGetAttributesForCompleteEmissionMaps(){
		//test normal functionality

		//create a normal event impl
		Map<Pollutant, Double> warmEmissionsMap = new HashMap<>();

		warmEmissionsMap.put(CO, co_value);
		warmEmissionsMap.put(CO2_TOTAL, co2total_value);
		warmEmissionsMap.put(FC, fc_value);
		warmEmissionsMap.put(HC, hc_value);
		warmEmissionsMap.put(NMHC, nmhc_value);
		warmEmissionsMap.put(NO2, no2_value);
		warmEmissionsMap.put(NOx, nox_value);
		warmEmissionsMap.put(PM, pm_value);
		warmEmissionsMap.put(SO2, so2_value);

		Map<Pollutant, Double> map = new LinkedHashMap<>(warmEmissionsMap);

		WarmEmissionEvent we = new WarmEmissionEvent(0.0, linkId, vehicleId, map);

		Map<String, String> weg = we.getAttributes();
		Assertions.assertEquals(Double.parseDouble(weg.get(CO.name())), co_value, MatsimTestUtils.EPSILON, "the CO value of this warm emission event was "+ Double.parseDouble(weg.get(CO.name()))+ "but should have been "+ co_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(CO2_TOTAL.name())), co2total_value, MatsimTestUtils.EPSILON, "the CO2 value of this warm emission event was "+ Double.parseDouble(weg.get(CO2_TOTAL.name()))+ "but should have been "+ co2total_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(FC.name())), fc_value, MatsimTestUtils.EPSILON, "the FC value of this warm emission event was "+ Double.parseDouble(weg.get(FC.name()))+ "but should have been "+ fc_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(HC.name())), hc_value, MatsimTestUtils.EPSILON, "the HC value of this warm emission event was "+ Double.parseDouble(weg.get(HC.name()))+ "but should have been "+ hc_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(NMHC.name())), nmhc_value, MatsimTestUtils.EPSILON, "the NMHC value of this warm emission event was "+ Double.parseDouble(weg.get(NMHC.name()))+ "but should have been "+ nmhc_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(NO2.name())), no2_value, MatsimTestUtils.EPSILON, "the NO2 value of this warm emission event was "+ Double.parseDouble(weg.get(NO2.name()))+ "but should have been "+ no2_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(NOx.name())), nox_value, MatsimTestUtils.EPSILON, "the NOx value of this warm emission event was "+ Double.parseDouble(weg.get(NOx.name()))+ "but should have been "+ nox_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(PM.name())), pm_value, MatsimTestUtils.EPSILON, "the PM value of this warm emission event was "+ Double.parseDouble(weg.get(PM.name()))+ "but should have been "+ pm_value);
		Assertions.assertEquals(Double.parseDouble(weg.get(SO2.name())), so2_value, MatsimTestUtils.EPSILON, "the SO2 value of this warm emission event was "+ Double.parseDouble(weg.get(SO2.name()))+ "but should have been "+ so2_value);
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

		Map<Pollutant, Double> map = new LinkedHashMap<>(valuesNotSet);

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
