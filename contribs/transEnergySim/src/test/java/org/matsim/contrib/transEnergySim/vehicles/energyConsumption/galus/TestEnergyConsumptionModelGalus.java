/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.core.network.LinkImplTest;

import junit.framework.TestCase;

public class TestEnergyConsumptionModelGalus extends TestCase {

	public void testLowerSpeedThanMinSpeed() {
		EnergyConsumptionModelGalus ecmg=new EnergyConsumptionModelGalus();
		
		double drivenDistanceInMeters=10;
		double maxSpeedOnLink=-1;
		double averageSpeedDriven=5;
		double energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals(3.173684E+02*10, energyConsumptionForLinkInJoule);
	}
	
	public void testHigherSpeedThanMaxSpeed() {
		EnergyConsumptionModelGalus ecmg=new EnergyConsumptionModelGalus();
		
		double drivenDistanceInMeters=10;
		double maxSpeedOnLink=-1;
		double averageSpeedDriven=42;
		double energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals(2.905639E+03*10, energyConsumptionForLinkInJoule);
	}
	
	public void testSpeedAtIntervalBorders() {
		EnergyConsumptionModelGalus ecmg=new EnergyConsumptionModelGalus();
		
		double drivenDistanceInMeters=10;
		double maxSpeedOnLink=-1;
		double averageSpeedDriven=25;
		double energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals(6.490326E+02*10, energyConsumptionForLinkInJoule);
		
		averageSpeedDriven=5.555555556;
		energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals(3.173684E+02*10, energyConsumptionForLinkInJoule);
		
		averageSpeedDriven=41.66666667;
		energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals(2.905639E+03*10, energyConsumptionForLinkInJoule);
	}
	
	public void testInterpolatedValues() {
		EnergyConsumptionModelGalus ecmg=new EnergyConsumptionModelGalus();
		
		double epsilon=0.1;
		double drivenDistanceInMeters=1;
		double maxSpeedOnLink=-1;
		double averageSpeedDriven=(5.555555556+8.333333333)/2;
		double energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals((3.173684E+02+4.231656E+02)/2, energyConsumptionForLinkInJoule);
		
		averageSpeedDriven=38.88888889+ (41.66666667-38.88888889)/3;
		energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals(2.418100E+03 + (2.905639E+03-2.418100E+03)/3, energyConsumptionForLinkInJoule,epsilon);
		
		averageSpeedDriven=(33.33333333+ 36.11111111)/2;
		energyConsumptionForLinkInJoule = ecmg.getEnergyConsumptionForLinkInJoule(drivenDistanceInMeters, maxSpeedOnLink,averageSpeedDriven);
		assertEquals((1.179291E+03 + 1.825931E+03)/2, energyConsumptionForLinkInJoule,epsilon);
		
	}
	
}
