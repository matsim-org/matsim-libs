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

package playground;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;

import org.junit.Test;
//import org.matsim.testcases.MatsimTestUtils;


//import playground.julia.emission.types.HbefaVehicleAttributes;
//import playground.julia.emission.types.HbefaColdEmissionFactorKey;
//import playground.vsp.emissions.types.ColdPollutant;
//import playground.vsp.emissions.types.HbefaVehicleCategory;

import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.HbefaVehicleCategory;

//test for playground.vsp.emissions.types.HbefaColdEmissionFactorKey

public class TestHbefaColdEmissionFactorKey {
	
	@Ignore
	@Test
	public final void testEquals(){

		//default case
		HbefaColdEmissionFactorKey normal = new HbefaColdEmissionFactorKey();
		HbefaColdEmissionFactorKey hcefk2 = new HbefaColdEmissionFactorKey();
		
		//TODO equals funktioniert nicht auf leeren Objekten weil getHbefaVehicleCat 'null' ... 
		//falls das ok ist: alle (anderen), default gesetzten parameter vergleichen? 
		// oder equals umschreiben, so dass 'null' und 'null' gleich sind?
		
		//Assert.assertTrue(hcefk1.equals(hcefk2));	
		
		//default constructor
		//TODO doku: kein default-konstruktor -> kein entspr. test
		
		
		//test with content
		//TODO die HbefaColdEmissionFactorKey hat keinen schoenen Konstruktor... ergaenzt ... uebernehmen?
		normal.setHbefaComponent(ColdPollutant.FC);
		normal.setHbefaDistance(4);
		normal.setHbefaParkingTime(5);
		HbefaVehicleAttributes abc = new HbefaVehicleAttributes("a","b","c");
		normal.setHbefaVehicleAttributes(abc);
		normal.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);

		HbefaColdEmissionFactorKey hcefk4 = new HbefaColdEmissionFactorKey();
		
		hcefk4.setHbefaComponent(ColdPollutant.FC);
		hcefk4.setHbefaDistance(4);
		hcefk4.setHbefaParkingTime(5);
		HbefaVehicleAttributes attForHcefk4 = new HbefaVehicleAttributes();
		attForHcefk4.setHbefaEmConcept("c");
		attForHcefk4.setHbefaSizeClass("b");
		attForHcefk4.setHbefaTechnology("a");
		hcefk4.setHbefaVehicleAttributes(abc);
		hcefk4.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		
		//Assert.assertTrue(hcefk4.toString(),hcefk1.equals(hcefk4));
		
		//wenn eins der 'ersten' vier Attribute fehlt, soll equal fehlschlagen
		//HbefaVehicleCategory hbefaVehicleCategory; ColdPollutant hbefaComponent;Integer hbefaParkingTime;Integer hbefaDistance;
		
		//sollte das im Folgenden lieber alles gleich sein zum Normalfall? natuerlich bis auf den testwert
		
		//no vehicle category
		HbefaColdEmissionFactorKey noVehCat = new HbefaColdEmissionFactorKey();
		noVehCat.setHbefaComponent(ColdPollutant.CO);
		noVehCat.setHbefaDistance(40);
		noVehCat.setHbefaParkingTime(20);

		try{
			boolean uu = normal.equals(noVehCat);
			//TODO message
			//TODO korrigieren! das wirft im moment fehler!!!! sollte aber nicht so sein

		//	Assert.fail();
		}
		catch(NullPointerException e){
		}			//no failure	


		//no pollutant set
		HbefaColdEmissionFactorKey noColdPollutant = new HbefaColdEmissionFactorKey();
		noColdPollutant.setHbefaDistance(11);
		noColdPollutant.setHbefaParkingTime(13);
		noColdPollutant.setHbefaVehicleCategory(HbefaVehicleCategory.HEAVY_GOODS_VEHICLE);
		
		try{
			normal.equals(noColdPollutant);
			//TODO message
			//Assert.fail();
		}
		catch(NullPointerException e){}			//no failure	
				
		HbefaColdEmissionFactorKey noParkingTime = new HbefaColdEmissionFactorKey();
		noParkingTime.setHbefaComponent(ColdPollutant.PM);
		noParkingTime.setHbefaDistance(17);
		noParkingTime.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		try{
			normal.equals(noParkingTime);
			//TODO message
			//Assert.fail();
		}
		catch(NullPointerException e){}			//no failure	
				HbefaColdEmissionFactorKey noDistanc = new HbefaColdEmissionFactorKey();
		/*
		noVehCat.setHbefaComponent(hbefaComponent);
		noVehCat.setHbefaDistance(hbefaDistance);
		noVehCat.setHbefaParkingTime(hbefaParkingTime);
		noVehCat.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		noVehCat.setHbefaVehicleCategory(hbefaVehicleCategory);*/
		
		
	}
	
}
	

	

