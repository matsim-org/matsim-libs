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


package org.matsim.contrib.parking.run;

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.PC2.GeneralParkingModule;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.example.ParkingBetaExample;
import org.matsim.contrib.parking.example.ParkingCostCalculatorExample;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
/**
 * @author jbischoff
 * Sample main file for setting up an Matsim run with parking. Check the output events for Parking events and the scores.
 * 
 * 
 */
public class RunParkingExample {

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("src/main/ressources/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		Controler controler = new Controler(scenario); 
		
		//setting Parking Costs - charged parking @ work, free parking @ home
		ParkingCostModel parkingCostModelWork = new ParkingCostCalculatorExample(1);
		ParkingCostModel parkingCostModelHome = new ParkingCostCalculatorExample(0);
		
		
		// we need some settings to walk from parking to destination - in this case, we are using default values.
		PlansCalcRouteConfigGroup cg = new PlansCalcRouteConfigGroup();
		WalkTravelTime walkTravelTime = new WalkTravelTime(cg);
		ParkingScoreManager parkingScoreManager = new ParkingScoreManager(walkTravelTime, controler);
		parkingScoreManager.setParkingScoreScalingFactor(1);
		parkingScoreManager.setParkingBetas(new ParkingBetaExample());
		
		
		ParkingInfrastructureManager parkingInfrastructureManager = new ParkingInfrastructureManager(parkingScoreManager, controler.getEvents());
		
		//parking 1: we place this near the workplace
		PublicParking workPark = new PublicParking(Id.create("workPark", PC2Parking.class), 98, new CoordImpl(10000,0), parkingCostModelWork, "park");
		
		//parking : we place this at home
		PublicParking homePark = new PublicParking(Id.create("homePark", PC2Parking.class), 98, new CoordImpl(-25000,0), parkingCostModelHome, "park");

		LinkedList<PublicParking> publicParkings = new LinkedList<PublicParking>();
		
		publicParkings.add(workPark);
		publicParkings.add(homePark);
		parkingInfrastructureManager.setPublicParkings(publicParkings);
		
		
		
		
		

		//setting up the Parking Module
		GeneralParkingModule generalParkingModule = new GeneralParkingModule(controler);
		generalParkingModule.setParkingScoreManager(parkingScoreManager);
		generalParkingModule.setParkingCostModel(parkingCostModelHome);
		generalParkingModule.setParkingInfrastructurManager(parkingInfrastructureManager);
		
//		controler.setOverwriteFiles(true);
		controler.run();
	}

}
