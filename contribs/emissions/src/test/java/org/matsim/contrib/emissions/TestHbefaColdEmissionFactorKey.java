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
import org.matsim.contrib.emissions.HbefaColdEmissionFactorKey;
import org.matsim.contrib.emissions.HbefaVehicleAttributes;
import org.matsim.contrib.emissions.HbefaVehicleCategory;


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
	private String coldPollutant;
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
		coldPollutant = "FC";
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
		compare.setHbefaComponent(coldPollutant);
		compare.setHbefaDistance(distance);
		compare.setHbefaParkingTime(parkingTime);
		HbefaVehicleAttributes attForCompare = new HbefaVehicleAttributes();
		attForCompare.setHbefaEmConcept("concept");
		attForCompare.setHbefaSizeClass("size class");
		attForCompare.setHbefaTechnology("technology");
		compare.setHbefaVehicleAttributes(attForCompare);
		compare.setHbefaVehicleCategory(hbefaVehCategory);
		
		message = "these two objects should be the same but are not: " + normal.toString() + " and " + compare.toString();
		Assert.assertTrue(message, normal.equals(compare));
		Assert.assertTrue(message, compare.equals(normal));
		
		//different HbefaColdEmissionFactorKey, does not equal 'normal'
		HbefaColdEmissionFactorKey different = new HbefaColdEmissionFactorKey();
		different.setHbefaComponent("CO");
		different.setHbefaDistance(2);
		different.setHbefaParkingTime(50);
		HbefaVehicleAttributes attForDifferent= new HbefaVehicleAttributes();
		attForDifferent.setHbefaEmConcept("concept 2"); attForDifferent.setHbefaSizeClass("size class 2"); attForDifferent.setHbefaTechnology("technology 2");
		different.setHbefaVehicleAttributes(attForDifferent);
		different.setHbefaVehicleCategory( HbefaVehicleCategory.HEAVY_GOODS_VEHICLE );
		
		message = "these two objects should not be the same: " + normal.toString() + " and " + different.toString();
		
		Assert.assertFalse(message, normal.equals(different));
		Assert.assertFalse(message, different.equals(normal));
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
		noVehCat.setHbefaComponent(coldPollutant);
		noVehCat.setHbefaDistance(distance);
		noVehCat.setHbefaParkingTime(parkingTime);
		noVehCat.setHbefaVehicleAttributes(hbefaVehicleAttributes);
			
		equalErr = false;	
		try{
			noVehCat.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}	
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehCat.toString();
		message2 = "this key should not be comparable since no vehicle category is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noVehCat));
		
	}

	@Test
	public final void testEqualsForIncompleteKeys_pollutant(){
		
		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);
		
		//no pollutant set
		HbefaColdEmissionFactorKey noColdPollutant = new HbefaColdEmissionFactorKey();
		noColdPollutant.setHbefaDistance(distance);
		noColdPollutant.setHbefaParkingTime(parkingTime);
		noColdPollutant.setHbefaVehicleCategory(hbefaVehCategory);
		noColdPollutant.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		
		equalErr = false;	
		try{	
			noColdPollutant.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}	
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noColdPollutant.toString();
		message2 = "this key should not be comparable since no cold pollutant is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noColdPollutant));
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_parkingTime(){

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);

		//no parking time set
		HbefaColdEmissionFactorKey noParkingTime = new HbefaColdEmissionFactorKey();
		noParkingTime.setHbefaComponent(coldPollutant);
		noParkingTime.setHbefaDistance(distance);
		noParkingTime.setHbefaVehicleCategory(hbefaVehCategory);	
		noParkingTime.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		
		equalErr = false;	
		try{
			noParkingTime.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noParkingTime.toString();
		message2 = "this key should be comparable even though no parking time is set";
		Assert.assertFalse(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noParkingTime));
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_distance(){

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);
		
		//no distance set
		HbefaColdEmissionFactorKey noDistance = new HbefaColdEmissionFactorKey();
		noDistance.setHbefaComponent(coldPollutant);
		noDistance.setHbefaParkingTime(parkingTime);
		noDistance.setHbefaVehicleCategory(hbefaVehCategory);
		noDistance.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		
		equalErr = false;
		try{	
			noDistance.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}	
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noDistance.toString();
		message2 = "this key should be comparable even though no distance is set";
		Assert.assertFalse(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noDistance));
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_emptyKey(){

		// generate a complete HbefaColdEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);
		
		//empty HbefaColdEmissionFactorKey
				HbefaColdEmissionFactorKey emptyKey = new HbefaColdEmissionFactorKey();
				equalErr = false;
				try{
					emptyKey.equals(normal);
				}
				catch(NullPointerException e){
					equalErr = true;
				}
				message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + emptyKey.toString();
				message2 = "this key should not be comparable since nothing is set";
				Assert.assertTrue(message2, equalErr);
				Assert.assertFalse(message, normal.equals(emptyKey)); 
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
		noVehAtt.setHbefaComponent(coldPollutant);
		noVehAtt.setHbefaDistance(distance);
		noVehAtt.setHbefaParkingTime(parkingTime);
		noVehAtt.setHbefaVehicleCategory(hbefaVehCategory);
		
		equalErr = false;
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		try{
			noVehAtt.equals(normal);
			Assert.assertFalse(noVehAtt.equals(normal));
		}
		catch(NullPointerException e){
			equalErr = true;
		}
		message2 ="vehicle attributes should be 'average' by default and therefore comparable";
		Assert.assertFalse(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noVehAtt));
		
		
		//set the vehicle attributes of the normal hbefacoldemissionfactorkey to 'average'
		//then noVehAtt is equal to normal
		HbefaVehicleAttributes hbefaVehicleAttributesAverage = new HbefaVehicleAttributes();
		hbefaVehicleAttributesAverage.setHbefaEmConcept("average");
		hbefaVehicleAttributesAverage.setHbefaSizeClass("average");
		hbefaVehicleAttributesAverage.setHbefaTechnology("average");
		normal.setHbefaVehicleAttributes(hbefaVehicleAttributesAverage);
		
		equalErr = false;
		try{
			noVehAtt.equals(normal);
			Assert.assertTrue(noVehAtt.equals(normal));
		}
		catch(NullPointerException e){
			equalErr = true;
		}
		message = "these two HbefaColdEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		Assert.assertFalse(message2, equalErr);
		Assert.assertTrue(message, normal.equals(noVehAtt));
		
		
	}


	private void setToNormal(HbefaColdEmissionFactorKey normal) {
		normal.setHbefaComponent(coldPollutant);
		normal.setHbefaDistance(distance);
		normal.setHbefaParkingTime(parkingTime);
		normal.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		normal.setHbefaVehicleCategory(hbefaVehCategory);
		
	}
	
}
	

	

