/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestHbefaColdEmissionFactorKey.java                                     *
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

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.matsim.contrib.emissions.Pollutant.CO;
import static org.matsim.contrib.emissions.Pollutant.FC;


/*
	* test for playground.vsp.emissions.types.HbefaColdEmissionFactorKey
	* 1 test equals method for complete input: two equal hbefaColdEmissionFactorKey objects, two unequal objects
	* 2 test equals method for incomplete input: one complete hbefaColdEmissionFactorKey against one with a missing argument
	*  
	*  @author julia
	*/

public class TestHbefaColdEmissionFactorKey {
	
	private Integer distance;
    private Integer parkingTime;
	private Pollutant coldPollutant;
	private HbefaVehicleAttributes hbefaVehicleAttributes;
	private HbefaVehicleCategory hbefaVehCategory;
	private String message;
    private String message2;
	private boolean equalErr;
	private HbefaColdEmissionFactorKey normal;
	
	private void setUp(){
		normal = new HbefaColdEmissionFactorKey();
		hbefaVehicleAttributes = new HbefaVehicleAttributes();
		hbefaVehicleAttributes.setHbefaEmConcept("concept");
		hbefaVehicleAttributes.setHbefaSizeClass("size class");
		hbefaVehicleAttributes.setHbefaTechnology("technology");
		distance = 4;
		parkingTime = 5;
		coldPollutant = FC;
		hbefaVehCategory = HbefaVehicleCategory.PASSENGER_CAR;
		equalErr = false;
	}
	
	
	@Test
	public final void testEqualsForCompleteKeys(){

		setUp();

		//normal HbefaColdEmissionFactorKey	- default case
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		setToNormal(normal);

		//another HbefaColdEmissionFactorKey, copy of normal
		HbefaColdEmissionFactorKey compare = new HbefaColdEmissionFactorKey();
		compare.setComponent(coldPollutant);
		compare.setDistance(distance);
		compare.setParkingTime(parkingTime);
		HbefaVehicleAttributes attForCompare = new HbefaVehicleAttributes();
		attForCompare.setHbefaEmConcept("concept");
		attForCompare.setHbefaSizeClass("size class");
		attForCompare.setHbefaTechnology("technology");
		compare.setVehicleAttributes(attForCompare);
		compare.setVehicleCategory(hbefaVehCategory);

		message = "these two objects should be the same but are not: " + normal.toString() + " and " + compare.toString();
		Assert.assertTrue(message, normal.equals(compare));
		Assert.assertTrue(message, compare.equals(normal));

		//different HbefaColdEmissionFactorKey, does not equal 'normal'
		HbefaColdEmissionFactorKey different = new HbefaColdEmissionFactorKey();
		different.setComponent(CO);
		different.setDistance(2);
		different.setParkingTime(50);
		HbefaVehicleAttributes attForDifferent = new HbefaVehicleAttributes();
		attForDifferent.setHbefaEmConcept("concept 2");
		attForDifferent.setHbefaSizeClass("size class 2");
		attForDifferent.setHbefaTechnology("technology 2");
		different.setVehicleAttributes(attForDifferent);
		different.setVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);

		message = "these two objects should not be the same: " + normal.toString() + " and " + different.toString();

		assertFalse(message, normal.equals(different));
		assertFalse(message, different.equals(normal));
	}
	
	// the following tests each compare a incomplete key to a complete key
	// wanted result:
	// completeData.equals(partialData) -> return false
	// uncompleteData.equals(completeData) -> throw nullpointer
	//exception: if the vehicleAttributes are set to 'average' by default
	
	@Test
	public final void testEqualsForIncompleteKeys_vehicleCategory(){
		setUp();
		boolean equalErr = false;
		String message2;

		//normal HbefaColdEmissionFactorKey
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		setToNormal(normal);

		//no vehicle category set
		HbefaColdEmissionFactorKey noVehCat = new HbefaColdEmissionFactorKey();
		noVehCat.setComponent(coldPollutant);
		noVehCat.setDistance(distance);
		noVehCat.setParkingTime(parkingTime);
		noVehCat.setVehicleAttributes(hbefaVehicleAttributes);

		var result = noVehCat.equals(normal);

		assertFalse(result);
	}

	@Test
	public final void testEqualsForIncompleteKeys_pollutant() {

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		//no pollutant set
		HbefaColdEmissionFactorKey noColdPollutant = new HbefaColdEmissionFactorKey();
		noColdPollutant.setDistance(distance);
		noColdPollutant.setParkingTime(parkingTime);
		noColdPollutant.setVehicleCategory(hbefaVehCategory);
		noColdPollutant.setVehicleAttributes(hbefaVehicleAttributes);

		var result = noColdPollutant.equals(normal);

		assertFalse(result);
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_parkingTime() {

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		//no parking time set
		HbefaColdEmissionFactorKey noParkingTime = new HbefaColdEmissionFactorKey();
		noParkingTime.setComponent(coldPollutant);
		noParkingTime.setDistance(distance);
		noParkingTime.setVehicleCategory(hbefaVehCategory);
		noParkingTime.setVehicleAttributes(hbefaVehicleAttributes);

		equalErr = false;
		try {
			noParkingTime.equals(normal);
		} catch (NullPointerException e) {
			equalErr = true;
		}
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noParkingTime.toString();
		message2 = "this key should be comparable even though no parking time is set";
		assertFalse(message2, equalErr);
		assertFalse(message, normal.equals(noParkingTime));
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_distance() {

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		//no distance set
		HbefaColdEmissionFactorKey noDistance = new HbefaColdEmissionFactorKey();
		noDistance.setComponent(coldPollutant);
		noDistance.setParkingTime(parkingTime);
		noDistance.setVehicleCategory(hbefaVehCategory);
		noDistance.setVehicleAttributes(hbefaVehicleAttributes);

		equalErr = false;
		try {
			noDistance.equals(normal);
		} catch (NullPointerException e) {
			equalErr = true;
		}
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noDistance.toString();
		message2 = "this key should be comparable even though no distance is set";
		assertFalse(message2, equalErr);
		assertFalse(message, normal.equals(noDistance));
	}

	@Test
	public final void testEqualsForIncompleteKeys_emptyKey() {

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		HbefaColdEmissionFactorKey emptyKey = new HbefaColdEmissionFactorKey();

		var result = emptyKey.equals(normal);

		assertFalse(result);
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_VehicleAttributes(){
		
		//if no vehicle attributes are set manually they are set to 'average' by default
		// thus, the equals method should not throw nullpointer exceptions but return false or respectively true

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		//normal HbefaColdEmissionFactorKey
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		setToNormal(normal);

		//no veh attributes
		HbefaColdEmissionFactorKey noVehAtt = new HbefaColdEmissionFactorKey();
		noVehAtt.setComponent(coldPollutant);
		noVehAtt.setDistance(distance);
		noVehAtt.setParkingTime(parkingTime);
		noVehAtt.setVehicleCategory(hbefaVehCategory);

		equalErr = false;
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		try {
			noVehAtt.equals(normal);
			assertFalse(noVehAtt.equals(normal));
		} catch (NullPointerException e) {
			equalErr = true;
		}
		message2 = "vehicle attributes should be 'average' by default and therefore comparable";
		assertFalse(message2, equalErr);
		assertFalse(message, normal.equals(noVehAtt));


		//set the vehicle attributes of the normal hbefacoldemissionfactorkey to 'average'
		//then noVehAtt is equal to normal
		HbefaVehicleAttributes hbefaVehicleAttributesAverage = new HbefaVehicleAttributes();
		hbefaVehicleAttributesAverage.setHbefaEmConcept("average");
		hbefaVehicleAttributesAverage.setHbefaSizeClass("average");
		hbefaVehicleAttributesAverage.setHbefaTechnology("average");
		normal.setVehicleAttributes(hbefaVehicleAttributesAverage);

		equalErr = false;
		try {
			noVehAtt.equals(normal);
			Assert.assertTrue(noVehAtt.equals(normal));
		} catch (NullPointerException e) {
			equalErr = true;
		}
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		assertFalse(message2, equalErr);
		Assert.assertTrue(message, normal.equals(noVehAtt));


	}


	private void setToNormal(HbefaColdEmissionFactorKey normal) {
		normal.setComponent(coldPollutant);
		normal.setDistance(distance);
		normal.setParkingTime(parkingTime);
		normal.setVehicleAttributes(hbefaVehicleAttributes);
		normal.setVehicleCategory(hbefaVehCategory);

	}
	
}
	

	

