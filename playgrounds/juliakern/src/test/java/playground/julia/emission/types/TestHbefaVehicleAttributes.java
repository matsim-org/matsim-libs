/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * TestHbefaVehicleAttributesEmission.java                                 *
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
import playground.julia.emission.types.*;
//TODO entweder folgende Klasse benutzen oder Test umschreiben -> siehe unten
//import playground.vsp.emissions.types.HbefaVehicleAttributes;

//test for playground.vsp.emissions.types.HbefaVehicleAttributes

public class TestHbefaVehicleAttributes {
	
	@Test
	public final void testEquals(){
		//default case
		HbefaVehicleAttributes hva = new HbefaVehicleAttributes();
		HbefaVehicleAttributes hva2 = new HbefaVehicleAttributes();
		Assert.assertTrue(hva.equals(hva2));
		//default constructor
		hva.setHbefaEmConcept("average");
		hva.setHbefaSizeClass("average");
		hva.setHbefaTechnology("average");
		Assert.assertTrue(hva.equals(hva2));
		//test with content
		//TODO die HbefaVehicleAttributes hat keinen schoenen Konstuktor.... sowas wie HbefaVehicleAttributes (String, String, String) waer schoen
		//hab ich mal ergaenzt... uebernehmen?
		hva.setHbefaEmConcept("hbefaEmConcept");
		hva.setHbefaSizeClass("hbefaSizeClass");
		hva.setHbefaTechnology("hbefaTechnology");
		HbefaVehicleAttributes hva3 = new HbefaVehicleAttributes("hbefaTechnology", "hbefaSizeClass", "hbefaEmConcept");
		Assert.assertTrue(hva3.equals(hva));
		//TODO null constructor
		HbefaVehicleAttributes hva4 = new HbefaVehicleAttributes("","","");
		Assert.assertTrue(hva2.equals(hva4));
		
	}
	
}
	

	

