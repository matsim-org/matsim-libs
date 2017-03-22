/* *********************************************************************** *
 * project: org.matsim.*
 * AddVehicleToContainerAlgorithmTest.java
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

package playground.southafrica.freight.digicore.io.algorithms;

import static org.junit.Assert.*;

import org.junit.Test;

import playground.southafrica.freight.digicore.containers.DigicoreVehicles;

public class AddVehicleToContainerAlgorithmTest {

	@Test
	@SuppressWarnings("unused")
	public void testConstructor() {
		try{
			DigicoreVehiclesAlgorithm algorithm = new AddVehicleToContainerAlgorithm(null);
			fail("Should not allow null container.");
		} catch(IllegalArgumentException e){
			/* Successfully caught exception. */
		}
		
		DigicoreVehicles vehicles = new DigicoreVehicles();
		try{
			DigicoreVehiclesAlgorithm algorithm = new AddVehicleToContainerAlgorithm(vehicles);
		} catch(Exception e){
			fail("Should successfully instantitate algorithm.");
		}
	}

}
