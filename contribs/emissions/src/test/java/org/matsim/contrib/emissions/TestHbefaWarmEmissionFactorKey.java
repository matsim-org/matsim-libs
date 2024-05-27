/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestHbefaWarmEmissionFactorKey.java                                     *
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

package org.matsim.contrib.emissions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.matsim.contrib.emissions.Pollutant.CO;
import static org.matsim.contrib.emissions.Pollutant.FC;


public class TestHbefaWarmEmissionFactorKey{
	private static final Logger log = LogManager.getLogger( TestHbefaWarmEmissionFactorKey.class );

	/*
 	* test for playground.vsp.emissions.types.HbefaWarmEmissionFactorKey
 	* 1 test equals method for complete input: two equal hbefaWarmEmissionFactorKey objects, two unequal objects
 	* 2 test equals method for incomplete input: one complete hbefaWarmEmissionFactorKey against one with a missing argument
 	*  
 	*  @author julia
 	*/
	
	private HbefaVehicleAttributes hbefaVehicleAttributes;
	private String hbefaRoadCategory;
	private HbefaTrafficSituation hbefaTrafficSituation;
	private HbefaVehicleCategory hbefaVehicleCategory;
	private Pollutant warmPollutant;
	private HbefaWarmEmissionFactorKey normal;
	private boolean equalErr;

	private void setUp(){
		equalErr = false;
		normal = new HbefaWarmEmissionFactorKey();
		hbefaVehicleAttributes = new HbefaVehicleAttributes();
		hbefaVehicleAttributes.setHbefaEmConcept("concept");
		hbefaVehicleAttributes.setHbefaSizeClass("size class");
		hbefaVehicleAttributes.setHbefaTechnology("technology");
		hbefaRoadCategory = "road type";		
		hbefaTrafficSituation = HbefaTrafficSituation.FREEFLOW;
		hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		warmPollutant = FC;
		
	}

	@Test
	final void testEqualsForCompleteKeys(){

		// generate a complete HbefaWarmEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		// generate a warm emission factor key equal to 'normal' 
		HbefaWarmEmissionFactorKey compare = new HbefaWarmEmissionFactorKey();
		compare.setComponent(FC);
		compare.setRoadCategory(hbefaRoadCategory);
		compare.setTrafficSituation(hbefaTrafficSituation);
		compare.setVehicleAttributes(hbefaVehicleAttributes);
		compare.setVehicleCategory(hbefaVehicleCategory);

		String message = "these two objects should be the same but are not: " + normal.toString() + " and " + compare.toString();
		Assertions.assertTrue(normal.equals(compare), message);
		Assertions.assertTrue(compare.equals(normal), message);

		//two unequal but complete objects
		HbefaWarmEmissionFactorKey different = new HbefaWarmEmissionFactorKey();
		different.setComponent(CO);
		different.setRoadCategory("another road category");
		different.setTrafficSituation(HbefaTrafficSituation.SATURATED);
		HbefaVehicleAttributes attrForDifferent = new HbefaVehicleAttributes();
		attrForDifferent.setHbefaEmConcept("em concept2");
		attrForDifferent.setHbefaSizeClass("size class2");
		attrForDifferent.setHbefaTechnology("technology 2");
		different.setVehicleAttributes(attrForDifferent);
		different.setVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);

		message = "these two objects should not be the same: " + normal.toString() + " and " + different.toString();

		assertFalse(different.equals(normal), message);
		assertFalse(normal.equals(different), message);
	}

	// the following tests each compare a incomplete key to a complete key
	// wanted result:
	// completeData.equals(partialData) -> return false
	// uncompleteData.equals(completeData) -> throw nullpointer
	// exception: if the vehicleAttributes are set to 'average' by default
	
	@Test
	final void testEqualsForIncompleteKeys_vehicleCategory() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		// empty VehicleCategory
		HbefaWarmEmissionFactorKey noVehCat = new HbefaWarmEmissionFactorKey();
		noVehCat.setComponent(warmPollutant);
		noVehCat.setRoadCategory(hbefaRoadCategory);
		noVehCat.setTrafficSituation(hbefaTrafficSituation);
		noVehCat.setVehicleAttributes(hbefaVehicleAttributes);

		log.warn("normal=" + normal);
		log.warn("noVehCat=" + noVehCat);

		var result = noVehCat.equals(normal);

		assertFalse(result);
	}

	@Test
	final void testEqualsForIncompleteKeys_pollutant() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		// empty warm pollutant
		HbefaWarmEmissionFactorKey noPollutant = new HbefaWarmEmissionFactorKey();
		noPollutant.setRoadCategory(hbefaRoadCategory);
		noPollutant.setTrafficSituation(hbefaTrafficSituation);
		noPollutant.setVehicleAttributes(hbefaVehicleAttributes);
		noPollutant.setVehicleCategory(hbefaVehicleCategory);

		var result = noPollutant.equals(normal);
	}

	@Test
	final void testEqualsForIncompleteKeys_roadCategory() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		//empty road category
		HbefaWarmEmissionFactorKey noRoadCat = new HbefaWarmEmissionFactorKey();
		noRoadCat.setComponent(warmPollutant);
		noRoadCat.setTrafficSituation(hbefaTrafficSituation);
		noRoadCat.setVehicleAttributes(hbefaVehicleAttributes);
		noRoadCat.setVehicleCategory(hbefaVehicleCategory);

		equalErr = false;
		try {
			noRoadCat.equals(normal);
		} catch (NullPointerException e) {
			equalErr = true;
		}

		String message = "these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noRoadCat.toString();
		String message2 = "this key should not be comparable since no road category is set";
		Assertions.assertTrue(equalErr, message2);
		assertFalse(normal.equals(noRoadCat), message);
	}

	@Test
	final void testEqualsForIncompleteKeys_trafficSituation() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		//empty traffic situation
		HbefaWarmEmissionFactorKey noTrafficSit = new HbefaWarmEmissionFactorKey();
		noTrafficSit.setComponent(warmPollutant);
		noTrafficSit.setRoadCategory(hbefaRoadCategory);
		noTrafficSit.setVehicleAttributes(hbefaVehicleAttributes);
		noTrafficSit.setVehicleCategory(hbefaVehicleCategory);

		var result = noTrafficSit.equals(normal);

		assertFalse(result);
	}

	@Test
	final void testEqualsForIncompleteKeys_emptyKey() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		//empty HbefaWarmEmissionFactorKey
		HbefaWarmEmissionFactorKey emptyKey = new HbefaWarmEmissionFactorKey();

		var result = emptyKey.equals(normal);

		assertFalse(result);
	}

	@Test
	final void testEqualsForIncompleteKeys_vehicleAttributes(){

		// if no vehicle attributes are set manually they are set to 'average' by default
		// thus, the equals method should not throw nullpointer exceptions but return false or respectively true

		// generate a complete HbefaWarmEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		//empty vehicle attributes
		HbefaWarmEmissionFactorKey noVehAtt = new HbefaWarmEmissionFactorKey();
		noVehAtt.setComponent(warmPollutant);
		noVehAtt.setRoadCategory(hbefaRoadCategory);
		noVehAtt.setTrafficSituation(hbefaTrafficSituation);
		noVehAtt.setVehicleCategory(hbefaVehicleCategory);

		equalErr = false;
		try {
			noVehAtt.equals(normal);
		} catch (NullPointerException e) {
			equalErr = true;
		}

		String message = "these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		assertFalse(noVehAtt.equals(normal), message);
		assertFalse(equalErr);
		// veh attributes are allowed to be not initiated
		// therefore this should not throw a nullpointer but return false
		assertFalse(normal.equals(noVehAtt), message);

		//set the vehicle attributes of the normal hbefacoldemissionfactorkey to 'average'
		//then noVehAtt is equal to normal
		HbefaVehicleAttributes hbefaVehicleAttributesAverage = new HbefaVehicleAttributes();
		hbefaVehicleAttributesAverage.setHbefaEmConcept("average");
		hbefaVehicleAttributesAverage.setHbefaSizeClass("average");
		hbefaVehicleAttributesAverage.setHbefaTechnology("average");
		normal.setVehicleAttributes(hbefaVehicleAttributesAverage);

		message = "these two HbefaWarmEmissionFactorKeys should be the same: " + normal.toString() + " and " + noVehAtt.toString();
		Assertions.assertTrue(normal.equals(noVehAtt), message);
		Assertions.assertTrue(noVehAtt.equals(normal), message);


	}
	
	private void setToNormal(HbefaWarmEmissionFactorKey normal) {
		normal.setComponent(warmPollutant);
		normal.setRoadCategory(hbefaRoadCategory);
		normal.setTrafficSituation(hbefaTrafficSituation);
		normal.setVehicleAttributes(hbefaVehicleAttributes);
		normal.setVehicleCategory(hbefaVehicleCategory);
	}
	
	
}
	

	

