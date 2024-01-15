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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
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
	final void testEqualsForCompleteKeys() {

		setUp();

		//normal HbefaColdEmissionFactorKey	- default case
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		setToNormal(normal);

		//another HbefaColdEmissionFactorKey, copy of normal
		{
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

			String message = "these two objects should be the same but are not: " + normal.toString() + " and " + compare.toString();
			Assertions.assertEquals(normal, compare, message);
			Assertions.assertEquals(compare, normal, message);
		}

		{
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

			String message = "these two objects should not be the same: " + normal.toString() + " and " + different.toString();
			assertNotEquals(normal, different, message);
			assertNotEquals(different, normal, message);
		}
	}

	// the following tests each compare a incomplete key to a complete key
	// wanted result:
	// completeData.equals(partialData) -> return false
	// uncompleteData.equals(completeData) -> throw nullpointer
	//exception: if the vehicleAttributes are set to 'average' by default

	@Test
	final void testEqualsForIncompleteKeys_vehicleCategory(){
		setUp();

		//normal HbefaColdEmissionFactorKey
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		setToNormal(normal);

		//no vehicle category set
		HbefaColdEmissionFactorKey noVehCat = new HbefaColdEmissionFactorKey();
		noVehCat.setComponent(coldPollutant);
		noVehCat.setDistance(distance);
		noVehCat.setParkingTime(parkingTime);
		noVehCat.setVehicleAttributes(hbefaVehicleAttributes);

		String message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehCat.toString();
		assertFalse(noVehCat.equals(normal), message);
	}

	@Test
	final void testEqualsForIncompleteKeys_pollutant(){

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

		String message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noColdPollutant.toString();
		assertFalse(noColdPollutant.equals(normal), message);
	}

	@Test
	final void testEqualsForIncompleteKeys_parkingTime() {

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

		String message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noParkingTime.toString();
		assertFalse(noParkingTime.equals(normal), message);
	}

	@Test
	final void testEqualsForIncompleteKeys_distance() {

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

		String message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noDistance.toString();
		assertFalse(noDistance.equals(normal), message);
	}

	@Test
	final void testEqualsForIncompleteKeys_emptyKey() {

		// generate a complete HbefaColdEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		//empty HbefaColdEmissionFactorKey
		HbefaColdEmissionFactorKey emptyKey = new HbefaColdEmissionFactorKey();

		String message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + emptyKey.toString();
		assertFalse(emptyKey.equals(normal), message);
	}

	@Test
	final void testEqualsForIncompleteKeys_VehicleAttributes(){

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

		String message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		assertFalse(noVehAtt.equals(normal), message);


		//set the vehicle attributes of the normal hbefaColdWmissionFactorKey to 'average'
		//then noVehAtt is equal to normal
		{
			HbefaVehicleAttributes hbefaVehicleAttributesAverage = new HbefaVehicleAttributes();
			hbefaVehicleAttributesAverage.setHbefaEmConcept("average");
			hbefaVehicleAttributesAverage.setHbefaSizeClass("average");
			hbefaVehicleAttributesAverage.setHbefaTechnology("average");
			normal.setVehicleAttributes(hbefaVehicleAttributesAverage);

			Assertions.assertTrue(noVehAtt.equals(normal), message);
		}

	}


	private void setToNormal(HbefaColdEmissionFactorKey normal) {
		normal.setComponent(coldPollutant);
		normal.setDistance(distance);
		normal.setParkingTime(parkingTime);
		normal.setVehicleAttributes(hbefaVehicleAttributes);
		normal.setVehicleCategory(hbefaVehCategory);
	}

}




