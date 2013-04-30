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
import playground.julia.emission.types.HbefaVehicleAttributes;
import playground.julia.emission.types.HbefaWarmEmissionFactorKey;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.types.HbefaVehicleCategory;
//TODO explain/change
//import playground.vsp.emissions.types.HbefaWarmEmissionFactorKey;

//test for playground.vsp.emissions.types.HbefaWarmEmissionFactorKey

public class TestHbefaWarmEmissionFactorKey {
	
	@Test
	public final void testEquals(){

		//default case
		//TODO HbefaWarmEmissionFactorKey hat keinen DefaultKonstruktor bzw nur einen leeren. Soll das so bleiben?
		HbefaWarmEmissionFactorKey hcefk1 = new HbefaWarmEmissionFactorKey();
		HbefaWarmEmissionFactorKey hcefk2 = new HbefaWarmEmissionFactorKey();
	//	Assert.assertTrue(hcefk1.equals(hcefk2));	
		
		//default constructor
		HbefaWarmEmissionFactorKey hcefk3 = new HbefaWarmEmissionFactorKey();
		//TODO wenn es defaultwerte gibt, dann hier testen. sonst test unnoetig
		
		
		//test with content
		//TODO die HbefaWarmEmissionFactorKey hat keinen schoenen Konstruktor... ergaenzt ... uebernehmen?
		hcefk3.setHbefaComponent(WarmPollutant.FC);
		hcefk3.setHbefaDistance(4);
		hcefk3.setHbefaParkingTime(5);
		HbefaVehicleAttributes abc = new HbefaVehicleAttributes("a","b","c");
		hcefk3.setHbefaVehicleAttributes(abc);
		hcefk3.setHbefaVehicleCategory(HbefaVehicleCategory.PASSENGER_CAR);
		
		HbefaWarmEmissionFactorKey hcefk4 = new HbefaWarmEmissionFactorKey(WarmPollutant.FC, 4, 5, "a", "b","c",HbefaVehicleCategory.PASSENGER_CAR);

		Assert.assertTrue(hcefk4.toString(),hcefk3.equals(hcefk4));
	
		
	}
	
}
	

	

