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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;

import static org.matsim.contrib.emissions.Pollutant.*;


/*
 * test for playground.vsp.emissions.events.ColdEmissionEventImpl
 * 1 test normal functionality
 * 2 test incomplete data
 * 3 test the number of attributes returned
 */

public class TestColdEmissionEventImpl {
			private final Double co = 20.;
    private final Double fc = 30.;
    private final Double hc=4.;
    private final Double nm=5.;
    private final Double n2=6.;
    private final Double nx=7.;
    private final Double pm=8.;
    private final Id<Vehicle> vehicleId = Id.create("veh 1", Vehicle.class);
    private final Id<Link> linkId = Id.create("link 1", Link.class);

	private final Set<Pollutant> coldPollutants = new HashSet<>(Arrays.asList(CO, FC, HC, NMHC, NOx, NO2,PM));

	@Test
	final void testGetAttributesForCompleteEmissionMaps(){
		//test normal functionality

		//create a normal event impl
		Map<Pollutant, Double> coldEmissionsMap = new HashMap<>();
		setColdEmissions(coldEmissionsMap);
		ColdEmissionEvent ce = new ColdEmissionEvent(0.0, linkId, vehicleId, coldEmissionsMap);

		Map<String, String> ceg = ce.getAttributes();
		Assertions.assertEquals(Double.parseDouble(ceg.get(CO.name())), co, MatsimTestUtils.EPSILON, "the CO value of this cold emission event was "+ Double.parseDouble(ceg.get(CO.name()))+ "but should have been "+ co);
		Assertions.assertEquals(Double.parseDouble(ceg.get(FC.name())), fc, MatsimTestUtils.EPSILON, "the FC value of this cold emission event was "+ Double.parseDouble(ceg.get(FC.name()))+ "but should have been "+ fc);
		Assertions.assertEquals(Double.parseDouble(ceg.get(HC.name())), hc, MatsimTestUtils.EPSILON, "the HC value of this cold emission event was "+ Double.parseDouble(ceg.get(HC.name()))+ "but should have been "+ hc);
		Assertions.assertEquals(Double.parseDouble(ceg.get(NMHC.name())), nm, MatsimTestUtils.EPSILON, "the NMHC value of this cold emission event was "+ Double.parseDouble(ceg.get(NMHC.name()))+ "but should have been "+ nm);
		Assertions.assertEquals(Double.parseDouble(ceg.get(NO2.name())), n2, MatsimTestUtils.EPSILON, "the NO2 value of this cold emission event was "+ Double.parseDouble(ceg.get(NO2.name()))+ "but should have been "+ n2);
		Assertions.assertEquals(Double.parseDouble(ceg.get(NOx.name())), nx, MatsimTestUtils.EPSILON, "the NOx value of this cold emission event was "+ Double.parseDouble(ceg.get(NOx.name()))+ "but should have been "+ nx);
		Assertions.assertEquals(Double.parseDouble(ceg.get(PM.name())), pm, MatsimTestUtils.EPSILON, "the PM value of this cold emission event was "+ Double.parseDouble(ceg.get(PM.name()))+ "but should have been "+ pm);

	}

	private void setColdEmissions( Map<Pollutant, Double> coldEmissionsMap ) {
		coldEmissionsMap.put(CO, co);
		coldEmissionsMap.put(FC, fc);
		coldEmissionsMap.put(HC, hc);
		coldEmissionsMap.put(NMHC, nm);
		coldEmissionsMap.put(NO2, n2);
		coldEmissionsMap.put(NOx, nx);
		coldEmissionsMap.put(PM, pm);

	}

	@Test
	final void testGetAttributesForIncompleteMaps(){
		//the getAttributesMethod should
		// - return null if the emission map is empty
		// - throw NullPointerExceptions if the emission values are not set
		// - throw NullPointerExceptions if no emission map is assigned

		//empty map
		Map<Pollutant, Double> emptyMap = new HashMap<>();
		ColdEmissionEvent emptyMapEvent = new ColdEmissionEvent(22., linkId, vehicleId, emptyMap);

		//values not set
		Map<Pollutant, Double> valuesNotSet = new HashMap<>();
		valuesNotSet.put(CO, null);
		valuesNotSet.put(FC, null);
		valuesNotSet.put(HC, null);
		valuesNotSet.put(NMHC, null);
		valuesNotSet.put(NO2, null);
		valuesNotSet.put(NOx, null);
		valuesNotSet.put(PM, null);
		ColdEmissionEvent valuesNotSetEvent = new ColdEmissionEvent(44., linkId, vehicleId, valuesNotSet);

		//no map
		ColdEmissionEvent noMap = new ColdEmissionEvent(50., linkId, vehicleId, null);

		int numberOfColdPollutants = coldPollutants.size();

		int valNullPointers = 0, noMapNullPointers=0;

		for(Pollutant cp : coldPollutants){

			//empty map
			Assertions.assertNull(emptyMapEvent.getAttributes().get(cp.name()));

			//values not set
			try{
				valuesNotSetEvent.getAttributes().get(cp.name());
			}
			catch(NullPointerException e){
				valNullPointers ++;
			}

			//no map
			try{
				noMap.getAttributes().get(cp.name());
			}
			catch(NullPointerException e){
				noMapNullPointers++;
			}
		}
		Assertions.assertEquals(numberOfColdPollutants, valNullPointers);
		Assertions.assertEquals(numberOfColdPollutants, noMapNullPointers);
	}

}
