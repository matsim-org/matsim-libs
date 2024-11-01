/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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


package org.matsim.contrib.parking.parkingchoice.run;

import java.util.LinkedList;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.parkingchoice.PC2.GeneralParkingModule;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingScore;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingInfrastructure;
import org.matsim.contrib.parking.parkingchoice.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 * Sample main file for setting up an Matsim run with parking. Check the output events for Parking events and the scores.
 *
 *
 */
class RunParkingChoiceExample {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("parkingchoice/config.xml");
		run(config);
	}

	public static void run(Config config) {
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.getConfig().controller().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles );

		// ---

		// we need some settings to walk from parking to destination:
		ParkingScore parkingScoreManager = new ParkingScoreManager(new WalkTravelTime(controler.getConfig().routing()), scenario);
		{
			parkingScoreManager.setParkingScoreScalingFactor( 1 );
			parkingScoreManager.setParkingBetas( new ParkingBetaExample() );
		}
		// ---

		ParkingInfrastructure parkingInfrastructureManager = new ParkingInfrastructureManager(parkingScoreManager, controler.getEvents());
		{
			LinkedList<PublicParking> publicParkings = new LinkedList<>();

			//parking 1: we place this near the workplace
			publicParkings.add(new PublicParking(Id.create("workPark", PC2Parking.class), 98, new Coord( 10000, 0 ),
					new ParkingCostCalculatorExample(1), "park"));

			//parking 2: we place this at home
			publicParkings.add(new PublicParking(Id.create("homePark", PC2Parking.class), 98, new Coord( -25000, 0 ),
					new ParkingCostCalculatorExample(0), "park"));

			parkingInfrastructureManager.setPublicParkings(publicParkings);
		}


		//setting up the Parking Module
		GeneralParkingModule generalParkingModule = new GeneralParkingModule(controler);
		generalParkingModule.setParkingScoreManager(parkingScoreManager);
		generalParkingModule.setParkingInfrastructurManager(parkingInfrastructureManager);

		controler.run();
	}

}
