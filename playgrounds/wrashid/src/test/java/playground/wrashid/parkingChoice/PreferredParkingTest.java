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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.parkingChoice.api.PreferredParkingManager;
import playground.wrashid.parkingChoice.infrastructure.ActInfo;
import playground.wrashid.parkingChoice.infrastructure.ParkingImpl;
import playground.wrashid.parkingChoice.infrastructure.PreferredParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;

public class PreferredParkingTest extends MatsimTestCase {

	public void testBaseTestCase() {
		PreferredParkingManager preferredParkingManager = new PreferredParkingManager() {

			@Override
			public boolean considerForChoiceSet(PreferredParking preferredParking, Id personId,
					double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {

				if (preferredParking.getAttributes().contains("EV")) {
					return true;
				}

				return false;
			}

			@Override
			public boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (personId.equals(Id.create(1, Person.class))) {
					return true;
				}
				return false;
			}

			
		};

		//assertEquals(9800, walkingDistanceFor3CarScenario(preferredParkingManager, 1), 5.0);
	}

	public void testAllAgentsWantToUseFarAwayPreferredParkingShouldIncreaseAverageWalkingDistance() {
		PreferredParkingManager preferredParkingManager = new PreferredParkingManager() {

			@Override
			public boolean considerForChoiceSet(PreferredParking preferredParking, Id personId,
					double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {
				return true;
			}

			@Override
			public boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				return true;
			}

			
		};

		//assertEquals(24167, walkingDistanceFor3CarScenario(preferredParkingManager, 3), 5.0);
	}
	
	public void testOnlyUsePreferredParkingAtWorkAndNoteHomeShouldDecreaseWalkingDistance() {
		PreferredParkingManager preferredParkingManager = new PreferredParkingManager() {

			@Override
			public boolean considerForChoiceSet(PreferredParking preferredParking, Id personId,
					double OPTIONALtimeOfDayInSeconds, ActInfo targetActInfo) {
				return true;
			}

			@Override
			public boolean isPersonLookingForCertainTypeOfParking(Id personId, double oPTIONALtimeOfDayInSeconds,
					ActInfo targetActInfo) {
				if (targetActInfo.getActType().equalsIgnoreCase("work")){
					return true;
				}
				return false;
			}

			
		};

		//assertEquals(22929, walkingDistanceFor3CarScenario(preferredParkingManager, 10), 5.0);
	}

	private double walkingDistanceFor3CarScenario(PreferredParkingManager preferredParkingManager, int parkingCapacity) {
		ParkingChoiceLib.isTestCaseRun=true;
		Config config = super.loadConfig("test/input/playground/wrashid/parkingChoice/chessConfig.xml");
		Controler controler = new Controler(config);

		// setup parking infrastructure
		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 10; j++) {
				ParkingImpl parking = new ParkingImpl(new CoordImpl(i * 1000 + 500, j * 1000 + 500));
				parking.setMaxCapacity(parkingCapacity);
				parkingCollection.add(parking);
			}
		}

		PreferredParking preferredParking = new PreferredParking(new CoordImpl(1000.0, 1000), "EV, Mobility");
		preferredParking.setMaxCapacity(parkingCapacity);
		parkingCollection.add(preferredParking);

		ParkingModule parkingModule = new ParkingModule(controler, parkingCollection);

		parkingModule.setPreferredParkingManager(preferredParkingManager);

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		controler.run();

		return parkingModule.getAverageWalkingDistance();
	}
}
