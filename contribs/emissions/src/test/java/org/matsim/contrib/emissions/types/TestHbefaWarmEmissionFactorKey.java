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

package org.matsim.contrib.emissions.types;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;



public class TestHbefaWarmEmissionFactorKey{


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
	private String warmPollutant;
	private String message;
    private String message2;
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
		warmPollutant = "FC";
		
	}

	@Test
	public final void testEqualsForCompleteKeys(){

		// generate a complete HbefaWarmEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);
		
		// generate a warm emission factor key equal to 'normal' 
		HbefaWarmEmissionFactorKey compare = new HbefaWarmEmissionFactorKey();
		compare.setHbefaComponent("FC");
		compare.setHbefaRoadCategory(hbefaRoadCategory);
		compare.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		compare.setHbefaVehicleCategory(hbefaVehicleCategory);
		
		message = "these two objects should be the same but are not: " + normal.toString() + " and " + compare.toString();
		Assert.assertTrue(message, normal.equals(compare));
		Assert.assertTrue(message, compare.equals(normal));
		
		//two unequal but complete objects
		HbefaWarmEmissionFactorKey different = new HbefaWarmEmissionFactorKey();
		different.setHbefaComponent("CO");
		different.setHbefaRoadCategory("another road category");
		HbefaVehicleAttributes attrForDifferent = new HbefaVehicleAttributes();
		attrForDifferent.setHbefaEmConcept("em concept2");
		attrForDifferent.setHbefaSizeClass("size class2");
		attrForDifferent.setHbefaTechnology("technology 2");
		different.setHbefaVehicleAttributes(attrForDifferent);
		different.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
		
		message = "these two objects should not be the same: " + normal.toString() + " and " + different.toString();
		
		Assert.assertFalse(message, different.equals(normal));
		Assert.assertFalse(message, normal.equals(different));
	}
	
	// the following tests each compare a incomplete key to a complete key
	// wanted result:
	// completeData.equals(partialData) -> return false
	// uncompleteData.equals(completeData) -> throw nullpointer
	// exception: if the vehicleAttributes are set to 'average' by default
	
	@Test
	public final void testEqualsForIncompleteKeys_vehicleCategory() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		// empty VehicleCategory
		HbefaWarmEmissionFactorKey noVehCat = new HbefaWarmEmissionFactorKey();
		noVehCat.setHbefaComponent(warmPollutant);
		noVehCat.setHbefaRoadCategory(hbefaRoadCategory);
		noVehCat.setHbefaVehicleAttributes(hbefaVehicleAttributes);

		equalErr = false;
		try {
			noVehCat.equals(normal);
		} catch (NullPointerException e) {
			equalErr = true;
		}

		message = "these two HbefaWarmEmissionFactorKeys should not be the same: "
				+ normal.toString() + " and " + noVehCat.toString();
		message2 = "this key should not be comparable since no vehicle category is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noVehCat));

	}
	
	@Test
	public final void testEqualsForIncompleteKeys_pollutant() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);

		// empty warm pollutant
		HbefaWarmEmissionFactorKey noPollutant = new HbefaWarmEmissionFactorKey();
		noPollutant.setHbefaRoadCategory(hbefaRoadCategory);
		noPollutant.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		noPollutant.setHbefaVehicleCategory(hbefaVehicleCategory);

		equalErr = false;
		try {
			noPollutant.equals(normal);
		} catch (NullPointerException e) {
			equalErr = true;
		}

		message = "these two HbefaWarmEmissionFactorKeys should not be the same: "
				+ normal.toString() + " and " + noPollutant.toString();
		message2 = "this key should not be comparable since no pollutant is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noPollutant));
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_roadCategory() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);
		
		//empty road category
		HbefaWarmEmissionFactorKey noRoadCat = new HbefaWarmEmissionFactorKey();
		noRoadCat.setHbefaComponent(warmPollutant);
		noRoadCat.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		noRoadCat.setHbefaVehicleCategory(hbefaVehicleCategory);
		
		equalErr = false;
		try{
			noRoadCat.equals(normal);
		}
		catch(NullPointerException e){
			equalErr= true;
		}
		
		message ="these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noRoadCat.toString();
		message2 = "this key should not be comparable since no road category is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noRoadCat));
	}

	@Ignore //This test is now ignored as trafficSituation has been extracted down a level in the HBEFA detailed and average Maps
	@Test
	public final void testEqualsForIncompleteKeys_trafficSituation() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);	
		
		//empty traffic situation
		HbefaWarmEmissionFactorKey noTrafficSit = new HbefaWarmEmissionFactorKey();
		noTrafficSit.setHbefaComponent(warmPollutant);
		noTrafficSit.setHbefaRoadCategory(hbefaRoadCategory);
		noTrafficSit.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		noTrafficSit.setHbefaVehicleCategory(hbefaVehicleCategory);
		
		equalErr = false;
		try{
			noTrafficSit.equals(normal);
		}
		catch(NullPointerException e){
			equalErr= true;
		}
		
		message ="these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noTrafficSit.toString();
		message2 = "this key should not be comparable since no traffic situation is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noTrafficSit));		
}
	
	@Test
	public final void testEqualsForIncompleteKeys_emptyKey() {
		// generate a complete HbefaWarmEmissionFactorKey: 'normal'
		// and set some parameters
		setUp();
		setToNormal(normal);	
		
		//empty HbefaWarmEmissionFactorKey
		HbefaWarmEmissionFactorKey emptyKey = new HbefaWarmEmissionFactorKey();
		equalErr = false;
		try{
			emptyKey.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}
		message ="these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + emptyKey.toString();
		message2 = "this key should not be comparable since it is not initiated";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(emptyKey)); 
	}
	
	@Test
	public final void testEqualsForIncompleteKeys_vehicleAttributes(){
		
		// if no vehicle attributes are set manually they are set to 'average' by default
		// thus, the equals method should not throw nullpointer exceptions but return false or respectively true
		
		// generate a complete HbefaWarmEmissionFactorKey: 'normal' 
		// and set some parameters
		setUp();
		setToNormal(normal);
		
		//empty vehicle attributes
		HbefaWarmEmissionFactorKey noVehAtt = new HbefaWarmEmissionFactorKey();
		noVehAtt.setHbefaComponent(warmPollutant);
		noVehAtt.setHbefaRoadCategory(hbefaRoadCategory);
		noVehAtt.setHbefaVehicleCategory(hbefaVehicleCategory);
		
		equalErr = false;
		try{
			noVehAtt.equals(normal);
		}
		catch(NullPointerException e){
			equalErr= true;
		}
		
		message ="these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehAtt.toString();
		Assert.assertFalse(message, noVehAtt.equals(normal));
		Assert.assertFalse(equalErr); 
		// veh attributes are allowed to be not initiated
		// therefore this should not throw a nullpointer but return false
		Assert.assertFalse(message, normal.equals(noVehAtt));
		
		//set the vehicle attributes of the normal hbefacoldemissionfactorkey to 'average'
		//then noVehAtt is equal to normal
		HbefaVehicleAttributes hbefaVehicleAttributesAverage = new HbefaVehicleAttributes();
		hbefaVehicleAttributesAverage.setHbefaEmConcept("average");
		hbefaVehicleAttributesAverage.setHbefaSizeClass("average");
		hbefaVehicleAttributesAverage.setHbefaTechnology("average");
		normal.setHbefaVehicleAttributes(hbefaVehicleAttributesAverage);
		
		message ="these two HbefaWarmEmissionFactorKeys should be the same: " + normal.toString() + " and " + noVehAtt.toString();
		Assert.assertTrue(message, normal.equals(noVehAtt));
		Assert.assertTrue(message, noVehAtt.equals(normal));
		

	}
	
	private void setToNormal(HbefaWarmEmissionFactorKey normal) {
		normal.setHbefaComponent(warmPollutant);
		normal.setHbefaRoadCategory(hbefaRoadCategory);
		normal.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		normal.setHbefaVehicleCategory(hbefaVehicleCategory);
	}
	
	
}
	

	

