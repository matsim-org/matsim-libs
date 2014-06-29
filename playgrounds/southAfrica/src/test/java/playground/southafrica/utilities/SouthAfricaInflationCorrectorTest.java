/* *********************************************************************** *
 * project: org.matsim.*
 * SouthAfricanInflationCorrectorTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities;


import org.matsim.testcases.MatsimTestCase;

import playground.southafrica.utilities.SouthAfricaInflationCorrector;

public class SouthAfricaInflationCorrectorTest extends MatsimTestCase {

	public void testConvert() {
		assertEquals("Wrong conversion", 1.0, SouthAfricaInflationCorrector.convert(1, 1981, 1981));
		assertEquals("Wrong conversion", 1.0, SouthAfricaInflationCorrector.convert(1, 2000, 2000));
		assertEquals("Wrong conversion", 1.147, SouthAfricaInflationCorrector.convert(1, 1981, 1982));
		assertEquals("Wrong conversion", 1.289228, Double.parseDouble(String.format("%.6f", SouthAfricaInflationCorrector.convert(1, 1981, 1983))));
		assertEquals("Wrong conversion", 0.871840, Double.parseDouble(String.format("%.6f", SouthAfricaInflationCorrector.convert(1, 1982, 1981))));
		assertEquals("Wrong conversion", 0.775658, Double.parseDouble(String.format("%.6f", SouthAfricaInflationCorrector.convert(1, 1983, 1981))));
		try{
			double d = SouthAfricaInflationCorrector.convert(1, 1970, 1970);
			fail("Should have caught an IllegalArgumentException.");
		} catch (IllegalArgumentException  e){
		}
	}

}

