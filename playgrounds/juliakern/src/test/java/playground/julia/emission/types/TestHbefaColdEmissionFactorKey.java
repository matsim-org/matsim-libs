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

package playground.julia.emission.types;

import org.junit.Assert;
import org.junit.Test;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;

//test for playground.vsp.emissions.types.HbefaColdEmissionFactorKey

public class TestHbefaColdEmissionFactorKey {
	
	@Test
	public final void testEquals(){

		boolean equalErr = false;

		//TODO equals funktioniert nicht auf leeren Objekten weil getHbefaVehicleCat 'null' ... 
		//in die eine richtung!!!
		// vollstaendigeDaten.equals(unvollstaendigeDaten) -> Nullpointer
		// unvollstaendigeDaten.equals(vollstaendigeDaten) -> laeuft, Rueckgabe false
		// dass das so ist, wird jetzt getestet!
		
		//test with content

		//normal HbefaColdEmissionFactorKey	- default case
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		normal.setHbefaComponent(ColdPollutant.FC);
		normal.setHbefaDistance(4);
		normal.setHbefaParkingTime(5);
		HbefaVehicleAttributes attForNormal = new HbefaVehicleAttributes("a","b","c");
		normal.setHbefaVehicleAttributes(attForNormal);
		normal.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		

		//another HbefaColdEmissionFactorKey, copy of normal
		HbefaColdEmissionFactorKey second = new HbefaColdEmissionFactorKey();
		second.setHbefaComponent(ColdPollutant.FC);
		second.setHbefaDistance(4);
		second.setHbefaParkingTime(5);
		HbefaVehicleAttributes attForSecond = new HbefaVehicleAttributes();
		attForSecond.setHbefaEmConcept("c");
		attForSecond.setHbefaSizeClass("b");
		attForSecond.setHbefaTechnology("a");
		second.setHbefaVehicleAttributes(attForSecond);
		second.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		
		Assert.assertTrue(normal.equals(second));
		Assert.assertTrue(second.equals(normal));
		
		//third HbefaColdEmissionFactorKey, does not equal 'normal'
		HbefaColdEmissionFactorKey third = new HbefaColdEmissionFactorKey();
		third.setHbefaComponent(ColdPollutant.FC);
		third.setHbefaDistance(4);
		third.setHbefaParkingTime(5);
		HbefaVehicleAttributes attForThird = new HbefaVehicleAttributes("att","att", "att");
		third.setHbefaVehicleAttributes(attForThird);
		third.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		
		Assert.assertFalse(normal.equals(third));
		Assert.assertFalse(third.equals(normal));
		
		//empty HbefaColdEmissionFactorKey
		HbefaColdEmissionFactorKey emptyKey = new HbefaColdEmissionFactorKey();
		equalErr = false;
		try{
			emptyKey.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}
		Assert.assertTrue(equalErr);
		Assert.assertFalse(normal.equals(emptyKey)); 
						
		
		//wenn eins der 'ersten' vier Attribute fehlt, soll equal fehlschlagen
		//HbefaVehicleCategory hbefaVehicleCategory; ColdPollutant hbefaComponent;Integer hbefaParkingTime;Integer hbefaDistance;
		
		//no vehicle category set
		HbefaColdEmissionFactorKey noVehCat = new HbefaColdEmissionFactorKey();
		noVehCat.setHbefaComponent(ColdPollutant.FC);
		noVehCat.setHbefaDistance(4);
		noVehCat.setHbefaParkingTime(5);
			
		equalErr = false;	
		try{
			noVehCat.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}	
		Assert.assertTrue(equalErr);
		Assert.assertFalse(normal.equals(noVehCat));

		//no pollutant set
		HbefaColdEmissionFactorKey noColdPollutant = new HbefaColdEmissionFactorKey();
		noColdPollutant.setHbefaDistance(4);
		noColdPollutant.setHbefaParkingTime(5);
		noColdPollutant.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		noColdPollutant.setHbefaVehicleAttributes(attForNormal);
		
		equalErr = false;	
		try{	
			noColdPollutant.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}	
		Assert.assertTrue(equalErr);
		Assert.assertFalse(normal.equals(noColdPollutant));


		//no parking time set
		HbefaColdEmissionFactorKey noParkingTime = new HbefaColdEmissionFactorKey();
		noParkingTime.setHbefaComponent(ColdPollutant.FC);
		noParkingTime.setHbefaDistance(4);
		noParkingTime.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);	
		noParkingTime.setHbefaVehicleAttributes(attForNormal);
		
		equalErr = false;	
		try{
			noParkingTime.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}
		Assert.assertTrue(equalErr);
		Assert.assertFalse(normal.equals(noParkingTime));
		
		//no distance set
		HbefaColdEmissionFactorKey noDistance = new HbefaColdEmissionFactorKey();
		noDistance.setHbefaComponent(ColdPollutant.FC);
		noDistance.setHbefaParkingTime(5);
		noDistance.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		noDistance.setHbefaVehicleAttributes(attForNormal);
		
		equalErr = false;
		try{	
			noDistance.equals(normal);
		}
		catch(NullPointerException e){
			equalErr = true;
		}	
		Assert.assertTrue(equalErr);
		Assert.assertFalse(normal.equals(noDistance));
	}
	
}
	

	

