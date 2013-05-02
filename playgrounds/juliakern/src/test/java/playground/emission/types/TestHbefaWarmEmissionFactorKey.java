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

package playground.emission.types;

import org.junit.Assert;
import org.junit.Test;

import playground.vsp.emissions.types.HbefaTrafficSituation;
import playground.vsp.emissions.types.HbefaVehicleAttributes;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.types.HbefaWarmEmissionFactorKey;
import playground.vsp.emissions.types.HbefaVehicleCategory;

//test for playground.vsp.emissions.types.HbefaWarmEmissionFactorKey

public class TestHbefaWarmEmissionFactorKey{

	
	HbefaVehicleAttributes hbefaVehicleAttributes;
	String hbefaRoadCategory;		
	HbefaTrafficSituation hbefaTrafficSituation;
	HbefaVehicleCategory hbefaVehicleCategory;
	WarmPollutant warmPollutant;
	String message;
	
	private void setUp(){
		hbefaVehicleAttributes = new HbefaVehicleAttributes();
		hbefaVehicleAttributes.setHbefaEmConcept("concept");
		hbefaVehicleAttributes.setHbefaSizeClass("size class");
		hbefaVehicleAttributes.setHbefaTechnology("technology");
		hbefaRoadCategory = "road type";		
		hbefaTrafficSituation = HbefaTrafficSituation.FREEFLOW;
		hbefaVehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
		warmPollutant = WarmPollutant.FC;
	}

	@Test
	public final void testEqualsForCorrectObjects(){

		setUp();
		
		//TODO diesen text anpassen und entsprechend in die codeliste schreiben
		//TODO equals funktioniert nicht auf leeren Objekten weil getHbefaVehicleCat 'null' ... 
		//in die eine richtung!!!
		// vollstaendigeDaten.equals(unvollstaendigeDaten) -> Nullpointer
		// unvollstaendigeDaten.equals(vollstaendigeDaten) -> laeuft, Rueckgabe false
		// dass das so ist, wird jetzt getestet!
		
		
		//HbefaVehicleCategory hbefaVehicleCategory; WarmPollutant hbefaComponent; String hbefaRoadCategory;
		//HbefaTrafficSituation hbefaTrafficSituation; HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();

		//normal HbefaWarmEmissionFactorKey	- default case
		HbefaWarmEmissionFactorKey normal = new HbefaWarmEmissionFactorKey();
		setToNormal(normal);
		
		//zwei gleiche, korrekte Objekte 
		HbefaWarmEmissionFactorKey compare = new HbefaWarmEmissionFactorKey();
		compare.setHbefaComponent(warmPollutant);
		compare.setHbefaRoadCategory(hbefaRoadCategory);
		compare.setHbefaTrafficSituation(hbefaTrafficSituation);
		compare.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		compare.setHbefaVehicleCategory(hbefaVehicleCategory);
		
		message = "these two objects should be the same but are not: " + normal.toString() + " and " + compare.toString();
		Assert.assertTrue(message, normal.equals(compare));
		Assert.assertTrue(message, compare.equals(normal));
		
		//zwei ungleiche, korrekte Objekte
		HbefaWarmEmissionFactorKey different = new HbefaWarmEmissionFactorKey();
		different.setHbefaComponent(WarmPollutant.CO);
		different.setHbefaRoadCategory("another road category");
		different.setHbefaTrafficSituation(HbefaTrafficSituation.SATURATED);
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
	
	@Test
	public final void testEqualsForIncorrectObjects(){
	
		setUp();
		boolean equalErr = false;
		String message2;
		
		//HbefaVehicleCategory hbefaVehicleCategory; WarmPollutant hbefaComponent; String hbefaRoadCategory;
		//HbefaTrafficSituation hbefaTrafficSituation; HbefaVehicleAttributes hbefaVehicleAttributes = new HbefaVehicleAttributes();
		
		//normal HbefaWarmEmissionFactorKey	- default case
		HbefaWarmEmissionFactorKey normal = new HbefaWarmEmissionFactorKey();
		setToNormal(normal);

		//empty VehicleCategory
		HbefaWarmEmissionFactorKey noVehCat = new HbefaWarmEmissionFactorKey();
		noVehCat.setHbefaComponent(warmPollutant);
		noVehCat.setHbefaRoadCategory(hbefaRoadCategory);
		noVehCat.setHbefaTrafficSituation(hbefaTrafficSituation);
		noVehCat.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		
		equalErr = false;
		try{
			noVehCat.equals(normal);
		}
		catch(NullPointerException e){
			equalErr= true;
		}
		
		message = "these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noVehCat.toString();
		message2 = "this key should not be comparable since no vehicle category is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noVehCat));
		
		//empty warm pollutant
		HbefaWarmEmissionFactorKey noPollutant = new HbefaWarmEmissionFactorKey();
		noPollutant.setHbefaRoadCategory(hbefaRoadCategory);
		noPollutant.setHbefaTrafficSituation(hbefaTrafficSituation);
		noPollutant.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		noPollutant.setHbefaVehicleCategory(hbefaVehicleCategory);
		
		equalErr = false;
		try{
			noPollutant.equals(normal);
		}
		catch(NullPointerException e){
			equalErr= true;
		}

		message ="these two HbefaWarmEmissionFactorKeys should not be the same: " + normal.toString() + " and " + noPollutant.toString();
		message2 = "this key should not be comparable since no pollutant is set";
		Assert.assertTrue(message2, equalErr);
		Assert.assertFalse(message, normal.equals(noPollutant));
		
		//empty road category
		HbefaWarmEmissionFactorKey noRoadCat = new HbefaWarmEmissionFactorKey();
		noRoadCat.setHbefaComponent(warmPollutant);
		noRoadCat.setHbefaTrafficSituation(hbefaTrafficSituation);
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
		
		//empty vehicle attributes
		HbefaWarmEmissionFactorKey noVehAtt = new HbefaWarmEmissionFactorKey();
		noVehAtt.setHbefaComponent(warmPollutant);
		noVehAtt.setHbefaRoadCategory(hbefaRoadCategory);
		noVehAtt.setHbefaTrafficSituation(hbefaTrafficSituation);
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
		Assert.assertFalse(equalErr); //TODO dokumentieren! das hier ist anderes als alle anderen faelle!
		//veh attributes sind das einzige Attribut von warm em factor key, dass ungesetzt bleiben darf
		//insb. sollen keine fehler geworfen werden, wenn es ungesetzt ist.
		Assert.assertFalse(message, normal.equals(noVehAtt));
		
		HbefaVehicleAttributes hbefaVehicleAttributesAverage = new HbefaVehicleAttributes();
		hbefaVehicleAttributesAverage.setHbefaEmConcept("average");
		hbefaVehicleAttributesAverage.setHbefaSizeClass("average");
		hbefaVehicleAttributesAverage.setHbefaTechnology("average");
		normal.setHbefaVehicleAttributes(hbefaVehicleAttributesAverage);
		
		message ="these two HbefaWarmEmissionFactorKeys should be the same: " + normal.toString() + " and " + noVehAtt.toString();
		Assert.assertTrue(message, normal.equals(noVehAtt));
		Assert.assertTrue(message, noVehAtt.equals(normal));
		
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
	
	private void setToNormal(HbefaWarmEmissionFactorKey normal) {
		normal.setHbefaComponent(warmPollutant);
		normal.setHbefaRoadCategory(hbefaRoadCategory);
		normal.setHbefaTrafficSituation(hbefaTrafficSituation);
		normal.setHbefaVehicleAttributes(hbefaVehicleAttributes);
		normal.setHbefaVehicleCategory(hbefaVehicleCategory);
	}
	
	
}
	

	

