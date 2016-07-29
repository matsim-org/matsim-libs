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

package playground.wrashid.parkingChoice;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.facilities.ActivityFacility;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;

public class PrivateParkingTest extends MatsimTestCase {

	public void testBaseCase(){
	//	assertEquals(2744, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(1),5.0);
	}
	
	public void testHigherParkingCapacityMakesWalkingDistanceShorter(){
	//	assertEquals(998, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(3),5.0);
	}
	
	public void testMakingTheCapacityHigherThanNumberOfCarsWillNotMakeWalkingDistanceShorter(){
	//	assertEquals(998, walkingDistanceFor3CarScenarioWithVariableParkingCapacity(10),5.0);
	}
	
	private double walkingDistanceFor3CarScenarioWithVariableParkingCapacity(int parkingCapacity) {
		ParkingChoiceLib.isTestCaseRun=true;
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler=new Controler(config);
		
		// setup parking infrastructure
		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				ParkingImpl parking = new ParkingImpl(new Coord((double) (i * 1000 + 500), (double) (j * 1000 + 500)));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}

		PrivateParking privateParking=new PrivateParking(new Coord(8500.0, (double) 9000),new ActInfo(Id.create(36, ActivityFacility.class), "work"));
		privateParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(privateParking);
		
		
		ParkingModule parkingModule = new ParkingModule(controler,parkingCollection);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.run();
		
		return parkingModule.getAverageWalkingDistance();
	}
	
	public void testChagingTheActTypeOfPrivateParkingShouldLeadToLongerWalkingDistances(){
		Config config= super.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");

		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		// too many things don't work with access/egress walk true. kai, jun'16

		Controler controler=new Controler(config);
		
		// setup parking infrastructure
		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		for (int i=0;i<10;i++){
			for (int j=0;j<10;j++){
				ParkingImpl parking = new ParkingImpl(new Coord((double) (i * 1000 + 500), (double) (j * 1000 + 500)));
				parking.setMaxCapacity(1);
				parkingCollection.add(parking);
			}
		}

		PrivateParking privateParking=new PrivateParking(new Coord(8500.0, (double) 9000),new ActInfo(Id.create(36, ActivityFacility.class), "home"));
		privateParking.setMaxCapacity(1);
		parkingCollection.add(privateParking);
		
		
		ParkingModule parkingModule = new ParkingModule(controler,parkingCollection);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.run();
		
	//	assertEquals(3489,parkingModule.getAverageWalkingDistance(),5.0);
	}
	
}
