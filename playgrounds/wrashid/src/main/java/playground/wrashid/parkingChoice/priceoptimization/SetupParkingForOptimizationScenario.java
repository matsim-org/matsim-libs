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
package playground.wrashid.parkingChoice.priceoptimization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingBetas;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.parkingchoice.PC2.scoring.RandomErrorTermManager;
import org.matsim.contrib.parking.parkingchoice.lib.obj.DoubleValueHashMap;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.wrashid.ABMT.rentParking.ParkingCostModelZH;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.priceoptimization.infrastracture.OptimizableParking;
import playground.wrashid.parkingChoice.priceoptimization.scoring.FreeFloatingParkingScoringFunctionFactory;
import playground.wrashid.parkingChoice.priceoptimization.scoring.ParkingScoreManager;
import playground.wrashid.parkingChoice.priceoptimization.simulation.ParkingInfrastructureManager;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZone;
import playground.wrashid.parkingSearch.withindayFW.zhCity.CityZones;
import playground.wrashid.parkingSearch.withindayFW.zhCity.HighStreetTariffZonesZHCity;

public class SetupParkingForOptimizationScenario {


	public static void prepare(OptimizationParkingModuleZH parkingModule, Controler controler) throws IOException{
		

		Config config = controler.getConfig();
		
		String baseDir = config.getParam("parkingChoice.ZH", "parkingDataDirectory");
		
		LinkedList<PParking> parkings = getParking(config, baseDir);
		String permitsFilePath = "C:\\LocalDocuments\\Projects\\Parking\\Tests\\Input\\permits.txt"; 		
		Map<Id<Person>, Set<String>> permitsPerPerson = readPermits(permitsFilePath);
	
		ParkingScoreManager parkingScoreManager = prepareParkingScoreManager(parkingModule, parkings, 
				controler, permitsPerPerson);
		
		
		//TODO: input permits from a file
		
		
		ParkingInfrastructureManager pim = new ParkingInfrastructureManager(parkingScoreManager, null,
				permitsPerPerson);
		
		String cityZonesFilePath = config.getParam("parkingChoice.ZH", "cityZonesFile");
		CityZones cityZones = new CityZones(cityZonesFilePath);
		HighStreetTariffZonesZHCity highTariffParkingZone = new HighStreetTariffZonesZHCity();

		ParkingCostModel pcm=new ParkingCostModelZH(config,parkings);
		LinkedList<PublicParking> publicParkings=new LinkedList<PublicParking>();
		LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities=new LinkedList<PPRestrictedToFacilities>();
		for (PParking parking: parkings){
			String groupName=null;
			if (parking.getId().toString().contains("stp")){
				groupName="streetParking";
			} else if (parking.getId().toString().contains("gp")){
				groupName="garageParking";
			} else if (parking.getId().toString().contains("publicPOutsideCity")){
				groupName="publicPOutsideCity";
			}
			if (groupName!=null){
				PublicParking publicParking;

				if (groupName.equals("garageParking") || groupName.equals("streetParking")) {
					double cost = 0.0;
					if (groupName.equals("garageParking")) {
						
						cost = cityZones.getClosestZone(parking.getCoord()).getParkingGarageFee2h()/2;
						
					}
					else {
						CityZone closestZone = cityZones.getClosestZone(parking.getCoord());
						
						if (MatsimRandom.getRandom().nextInt(100) < closestZone.getPctNonFreeParking()){						
							
							if (highTariffParkingZone.isInHighTariffZone(parking.getCoord()))
								cost = 1.5;
							else
								cost = 0.5;
							
						}
						else
							cost = 0.0;
						
					}
					publicParking = new OptimizableParking(Id.create(parking.getId(), 
							org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking.class), 
							parking.getIntCapacity(), parking.getCoord(), pcm, groupName, cost, cost,
							highTariffParkingZone.isInHighTariffZone(parking.getCoord()));
				}
				else {
					publicParking = new PublicParking(Id.create(parking.getId(), 
						org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking.class), 
						parking.getIntCapacity(), parking.getCoord(), pcm, groupName);
				}
				publicParkings.add(publicParking);
			} else {
				PrivateParking pp=(PrivateParking) parking;
				HashSet<Id<ActivityFacility>> hs=new HashSet<>();
				hs.add(pp.getActInfo().getFacilityId());
				groupName="privateParking";
				PPRestrictedToFacilities PPRestrictedToFacilitiesTmp=new PPRestrictedToFacilities(Id.create(parking.getId(), org.matsim.contrib.parking.parkingchoice.PC2.infrastructure.PC2Parking.class), parking.getIntCapacity(),parking.getCoord(),pcm,groupName,hs);
				ppRestrictedToFacilities.add(PPRestrictedToFacilitiesTmp);
			}
		}
		
		pim.setPublicParkings(publicParkings); 
		pim.setPrivateParkingRestrictedToFacilities(ppRestrictedToFacilities);
		
		parkingModule.setParkingInfrastructurManager(pim);
		parkingModule.setParkingScoreManager(parkingScoreManager);
		appendScoringFactory(parkingModule, controler);
	}

	private static Map<Id<Person>, Set<String>> readPermits(String permitsFilePath) throws IOException {

		Map<Id<Person>, Set<String>> permits = new HashMap<>();
		
		final BufferedReader readerPermits = IOUtils.getBufferedReader(permitsFilePath);
		
		String s = readerPermits.readLine();
		
		while (s != null) {
			Set<String> allPermits = new HashSet<String>();	
			String[] arr = s.split(";");
			for (String p : arr[1].split(","))
				allPermits.add(p);			
			
			permits.put(Id.createPersonId(arr[0]), allPermits);
			
			
		}
		return permits;
	}

	private static LinkedList<PParking> getParking(Config config, String baseDir) {
		ParkingLoader.garageParkingCalibrationFactor=Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_garageParking"));
		ParkingLoader.parkingsOutsideZHCityScaling =Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_publicPOutsideCity"));
		ParkingLoader.populationScalingFactor =Double.parseDouble(config.getParam("parkingChoice.ZH", "populationScalingFactor"));
		ParkingLoader.privateParkingCalibrationFactorZHCity  =Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_privateParking"));
		ParkingLoader.streetParkingCalibrationFactor  =Double.parseDouble(config.getParam("parkingChoice.ZH", "parkingGroupCapacityScalingFactor_streetParking"));
		
		LinkedList<PParking> parkings = ParkingLoader.getParkingsForScenario(baseDir);
		return parkings;
	}
	
	public static void appendScoringFactory(OptimizationParkingModuleZH parkingModule, Controler controler){

		controler.setScoringFunctionFactory(new FreeFloatingParkingScoringFunctionFactory (controler.getScenario() ,parkingModule.getParkingScoreManager()));
	}
	
	public static ParkingScoreManager prepareParkingScoreManager(OptimizationParkingModuleZH parkingModule, 
			LinkedList<PParking> parkings, Controler controler, Map<Id<Person>, Set<String>> permitsPerPerson) {
		ParkingScoreManager parkingScoreManager = new ParkingScoreManager(getWalkTravelTime(controler), controler.getScenario(), permitsPerPerson );


        ParkingBetas parkingBetas=new ParkingBetas(getHouseHoldIncomeCantonZH(controler.getScenario().getPopulation()));
		parkingBetas.setParkingWalkBeta(controler.getConfig().getParam("parkingChoice.ZH", "parkingWalkBeta"));
		parkingBetas.setParkingCostBeta(controler.getConfig().getParam("parkingChoice.ZH", "parkingCostBeta"));
		parkingScoreManager.setParkingBetas(parkingBetas);
		
		double parkingScoreScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice.ZH", "parkingScoreScalingFactor"));
		parkingScoreManager.setParkingScoreScalingFactor(parkingScoreScalingFactor);
		double randomErrorTermScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice.ZH", "randomErrorTermScalingFactor"));
		parkingScoreManager.setRandomErrorTermScalingFactor(randomErrorTermScalingFactor);
		
		String epsilonDistribution=controler.getConfig().findParam("parkingChoice.ZH", "randomErrorTermEpsilonDistribution");
		if (epsilonDistribution!=null){
			LinkedList<Id> parkingIds=new LinkedList<Id>();
			for (PParking parking:parkings){
				parkingIds.add(parking.getId());
			}
			
			int seed=Integer.parseInt(controler.getConfig().findParam("parkingChoice.ZH", "randomErrorTerm.seed"));

            parkingScoreManager.setRandomErrorTermManger(new RandomErrorTermManager(epsilonDistribution, parkingIds, controler.getScenario().getPopulation().getPersons().values(),seed));
		}
		
		return parkingScoreManager;
	}
	
	// based on: playground.wrashid.parkingSearch.withindayFW.controllers.kti.HUPCControllerKTIzh.getHouseHoldIncomeCantonZH
	public static DoubleValueHashMap<Id> getHouseHoldIncomeCantonZH(Population population) {
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("./incomedis.txt");

		DoubleValueHashMap<Id> houseHoldIncome=new DoubleValueHashMap<Id>();
		try {
		for (Id<Person> personId : population.getPersons().keySet()) {
			double rand = MatsimRandom.getRandom().nextDouble();
			double x = 0.0;
			if (rand<0.032) {
				x = 1000+MatsimRandom.getRandom().nextDouble()*1000;
			} else if (rand<0.206) {
				x = 2000+MatsimRandom.getRandom().nextDouble()*2000;

			} else if (rand<0.471) {
				x = 4000+MatsimRandom.getRandom().nextDouble()*2000;

			} else if (rand<0.674) {
				x = 6000+MatsimRandom.getRandom().nextDouble()*2000;

			}else if (rand<0.803) {
				x = 8000+MatsimRandom.getRandom().nextDouble()*2000;

			}else if (rand<0.885) {
				x = 10000+MatsimRandom.getRandom().nextDouble()*2000;

			}else if (rand<0.927) {
				x = 12000+MatsimRandom.getRandom().nextDouble()*2000;

			}else if (rand<0.952) {
				x = 14000+MatsimRandom.getRandom().nextDouble()*2000;

			} else {
				x = 16000+MatsimRandom.getRandom().nextDouble()*16000;

			}
			
				outLink.write(personId.toString() + ";" + Double.toString(x));
			
			outLink.newLine();
			houseHoldIncome.put(personId, x);

		}
		outLink.flush();
		outLink.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return houseHoldIncome;
	}
	
	
	
	private static WalkTravelTime getWalkTravelTime(MatsimServices controler){
		Map<Id<Link>, Double> linkSlopes=new HashMap<>();
		String linkSlopeAttributeFile = controler.getConfig().getParam("parkingChoice.ZH", "networkLinkSlopes");
		ObjectAttributes lp = new ObjectAttributes();
		new ObjectAttributesXmlReader(lp).readFile(linkSlopeAttributeFile);

        for (Id<Link> linkId : controler.getScenario().getNetwork().getLinks().keySet()) {
			linkSlopes.put(linkId, (Double) lp.getAttribute(linkId.toString(), "slope"));
		}
		
		WalkTravelTime walkTravelTime = new WalkTravelTime(new PlansCalcRouteConfigGroup(), linkSlopes);
		return walkTravelTime;
	}

}
