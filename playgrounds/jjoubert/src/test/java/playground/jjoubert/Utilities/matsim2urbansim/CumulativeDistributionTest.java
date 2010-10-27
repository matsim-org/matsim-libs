/* *********************************************************************** *
 * project: org.matsim.*
 * CumulativedistributionTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.demandmodeling.primloc.CumulativeDistribution;
import org.matsim.testcases.MatsimTestCase;

public class CumulativeDistributionTest extends MatsimTestCase{
	private final Logger log = Logger.getLogger(CumulativeDistributionTest.class);
	private CumulativeDistribution cd1;
	private CumulativeDistribution cd2;
	
	public void testGetValue() throws Exception{
		setupCdf();
		cd1.print();
		cd2.print();
//		double v = cd1.mapCdf(5, cd2);
		// TODO Must first sort out the CumulativeDistribution before I can write tests.
//		assertEquals("Mapped the value 1 incorrectly.", 1.0, cd1.mapCdf(1, cd2));
//		assertEquals("Mapped the value 1 incorrectly.", 1.0, cd2.mapCdf(1, cd1));
//		assertEquals("Mapped the value 5 incorrectly.", 5.0, cd1.mapCdf(5, cd2));
//		assertEquals("Mapped the value 5 incorrectly.", 5.0, cd2.mapCdf(5, cd1));
//		assertEquals("Mapped the value 3 incorrectly.", 1.6, cd1.mapCdf(3, cd2));
//		assertEquals("Mapped the value 1.6 incorrectly.", 3.0, cd2.mapCdf(1.6, cd1));
		
		log.info("Some message");
	}
	
	private void setupCdf(){
		List<Double> l1 = new ArrayList<Double>(15);
		List<Double> l2 = new ArrayList<Double>(15);
		for(int i = 1; i <= 5; i++){
			for(int j = 0; j < i; j++){
				l1.add(Double.valueOf(i));
			}
			for(int j = 6; j > i; j--){
				l2.add(Double.valueOf(i));
			}
		}
		cd1 = new CumulativeDistribution(1.0, 5.0, 4);
		cd2 = new CumulativeDistribution(1.0, 5.0, 4);
		
		for(int i = 0; i < l1.size(); i++){
			cd1.addObservation(l1.get(i));
			cd2.addObservation(l2.get(i));
		}
	}

}

