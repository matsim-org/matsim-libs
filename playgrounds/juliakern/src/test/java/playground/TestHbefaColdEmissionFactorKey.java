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

import org.junit.Test;
//import org.matsim.testcases.MatsimTestUtils;


import playground.julia.emission.types.HbefaVehicleAttributes;
import playground.julia.emission.types.HbefaColdEmissionFactorKey;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.HbefaVehicleCategory;
//TODO explain/change
//import playground.vsp.emissions.types.HbefaColdEmissionFactorKey;

//test for playground.vsp.emissions.types.HbefaColdEmissionFactorKey

public class TestHbefaColdEmissionFactorKey {
	
	@Test
	public final void testEquals(){

		//default case
		//TODO HbefaColdEmissionFactorKey hat keinen DefaultKonstruktor bzw nur einen leeren. Soll das so bleiben?
		HbefaColdEmissionFactorKey hcefk1 = new HbefaColdEmissionFactorKey();
		HbefaColdEmissionFactorKey hcefk2 = new HbefaColdEmissionFactorKey();
	//	Assert.assertTrue(hcefk1.equals(hcefk2));	
		
		//default constructor
		HbefaColdEmissionFactorKey hcefk3 = new HbefaColdEmissionFactorKey();
		//TODO wenn es defaultwerte gibt, dann hier testen. sonst test unnoetig
		
		
		//test with content
		//TODO die HbefaColdEmissionFactorKey hat keinen schoenen Konstruktor... ergaenzt ... uebernehmen?
		hcefk3.setHbefaComponent(ColdPollutant.FC);
		hcefk3.setHbefaDistance(4);
		hcefk3.setHbefaParkingTime(5);
		HbefaVehicleAttributes abc = new HbefaVehicleAttributes("a","b","c");
		hcefk3.setHbefaVehicleAttributes(abc);
		hcefk3.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		
		HbefaColdEmissionFactorKey hcefk4 = new HbefaColdEmissionFactorKey(ColdPollutant.FC, 4, 5, "a", "b","c",HbefaVehicleCategory.PASSENGER_CAR);

		Assert.assertTrue(hcefk4.toString(),hcefk3.equals(hcefk4));
	
		
	}
	
}
	

	

