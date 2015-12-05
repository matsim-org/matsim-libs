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
package playground.wrashid.ABMT.rentParking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.infrastructure.RentableParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingBetas;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoringFunctionFactory;
import org.matsim.contrib.parking.PC2.scoring.RandomErrorTermManager;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Injector;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.balac.freefloating.scoring.FreeFloatingParkingScoringFunctionFactory;
import playground.ivt.analysis.scoretracking.ScoreTrackingListener;
import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;

public class SetupParkingForZHScenario {

	public static ParkingScoreManager parkingScoreManager;
	
	
	public static void prepare(RentPrivateParkingModel parkingModule,Controler controler){
		
		
		Config config = controler.getConfig();
		
		String baseDir = config.getParam("parkingChoice.ZH", "parkingDataDirectory");
		
		LinkedList<PParking> parkings = getParking(config, baseDir);
	
		parkingScoreManager = prepareParkingScoreManager(parkingModule, parkings);
		
//		EventsManager events = EventsUtils.createEventsManager();
//		EventWriterXML eventsWriter = new EventWriterXML("c:\\tmp\\events.xml.gz");
//		events.addHandler(eventsWriter);
//		
//		events.resetHandlers(0);
//		eventsWriter.init("c:\\tmp\\events.xml.gz");
		
		ParkingInfrastructureManager pim=new ParkingInfrastructureManager(parkingScoreManager,null);
		
		ParkingCostModel pcm=new ParkingCostModelZH(config,parkings);
		LinkedList<PublicParking> publicParkings=new LinkedList<PublicParking>();
		LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities=new LinkedList<PPRestrictedToFacilities>();
		LinkedList<RentableParking> rentableParkings=new LinkedList<>();
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
				PublicParking publicParking=new PublicParking(Id.create(parking.getId(), PC2Parking.class),parking.getIntCapacity(),parking.getCoord(),pcm,groupName);
				publicParkings.add(publicParking);
			} else {
				PrivateParking pp=(PrivateParking) parking;
				HashSet<Id<ActivityFacility>> hs=new HashSet<>();
				hs.add(pp.getActInfo().getFacilityId());
				groupName="privateParking";
				PPRestrictedToFacilities PPRestrictedToFacilitiesTmp=new PPRestrictedToFacilities(Id.create(parking.getId(), PC2Parking.class), parking.getIntCapacity(),parking.getCoord(),pcm,groupName,hs);
				ppRestrictedToFacilities.add(PPRestrictedToFacilitiesTmp);
			}
		}
		
		
		// TODO: 
		// fill in "rentableParking"
		RentableParking rp=new RentableParking(Id.create("rentableParking", PC2Parking.class), 1, new Coord(682922.588,247474.957), null, "rentableParking");
		rp.setStartRentableTime(1000);
		rp.setEndRentableTime(70000);
		rp.setOwnerId(Id.create("dummyUser", Person.class));
		rp.setRentingPricePerHourInCurrencyUnit(0.0);
		rentableParkings.add(rp);
		
		
		
		
		//=====================
		
		publicParkings.addAll(rentableParkings);
		
		pim.setPublicParkings(publicParkings); 
		pim.setPrivateParkingRestrictedToFacilities(ppRestrictedToFacilities);
		pim.setRentableParking(rentableParkings);
		
		parkingModule.setParkingInfrastructurManager(pim);
		parkingModule.setParkingScoreManager(parkingScoreManager);
		//appendScoringFactory(parkingModule);
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
	
	public static void appendScoringFactory(RentPrivateParkingModel parkingModule){

		//parkingModule.getControler().setScoringFunctionFactory(new FreeFloatingParkingScoringFunctionFactory (parkingModule.getControler().getScenario() ,parkingModule.getParkingScoreManager()));
	}
	
	public static ParkingScoreManager prepareParkingScoreManager(RentPrivateParkingModel parkingModule, LinkedList<PParking> parkings) {
		Controler controler=parkingModule.getControler();
		ParkingScoreManager parkingScoreManager = new ParkingScoreManager(getWalkTravelTime(controler), controler.getScenario() );


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

            parkingScoreManager.setRandomErrorTermManger(new RandomErrorTermManager(epsilonDistribution, parkingIds, parkingModule.getControler().getScenario().getPopulation().getPersons().values(),seed));
		}
		
		return parkingScoreManager;
	}
	
	// based on: playground.wrashid.parkingSearch.withindayFW.controllers.kti.HUPCControllerKTIzh.getHouseHoldIncomeCantonZH
	public static DoubleValueHashMap<Id> getHouseHoldIncomeCantonZH(Population population) {
		DoubleValueHashMap<Id> houseHoldIncome=new DoubleValueHashMap<Id>();
		
		for (Id<Person> personId : population.getPersons().keySet()) {
			double rand = MatsimRandom.getRandom().nextDouble();
			if (rand<0.032) {
				houseHoldIncome.put(personId, 1000+MatsimRandom.getRandom().nextDouble()*1000);
			} else if (rand<0.206) {
				houseHoldIncome.put(personId, 2000+MatsimRandom.getRandom().nextDouble()*2000);
			} else if (rand<0.471) {
				houseHoldIncome.put(personId, 4000+MatsimRandom.getRandom().nextDouble()*2000);
			} else if (rand<0.674) {
				houseHoldIncome.put(personId, 6000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.803) {
				houseHoldIncome.put(personId, 8000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.885) {
				houseHoldIncome.put(personId, 10000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.927) {
				houseHoldIncome.put(personId, 12000+MatsimRandom.getRandom().nextDouble()*2000);
			}else if (rand<0.952) {
				houseHoldIncome.put(personId, 14000+MatsimRandom.getRandom().nextDouble()*2000);
			} else {
				houseHoldIncome.put(personId, 16000+MatsimRandom.getRandom().nextDouble()*16000);
			}
		}
		
		return houseHoldIncome;
	}
	
	
	
	private static WalkTravelTime getWalkTravelTime(Controler controler){
		Map<Id<Link>, Double> linkSlopes=new HashMap<>();
		String linkSlopeAttributeFile = controler.getConfig().getParam("parkingChoice.ZH", "networkLinkSlopes");
		ObjectAttributes lp = new ObjectAttributes();
		new ObjectAttributesXmlReader(lp).parse(linkSlopeAttributeFile);

        for (Id<Link> linkId : controler.getScenario().getNetwork().getLinks().keySet()) {
			linkSlopes.put(linkId, (Double) lp.getAttribute(linkId.toString(), "slope"));
		}
		
		WalkTravelTime walkTravelTime = new WalkTravelTime(new PlansCalcRouteConfigGroup(), linkSlopes);
		return walkTravelTime;
	}

}
