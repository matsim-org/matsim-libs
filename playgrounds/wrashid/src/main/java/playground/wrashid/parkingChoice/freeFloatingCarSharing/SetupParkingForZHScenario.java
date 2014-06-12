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
package playground.wrashid.parkingChoice.freeFloatingCarSharing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.PC2.ParkingModuleWithFFCarSharing;
import org.matsim.contrib.parking.PC2.infrastructure.PPRestrictedToFacilities;
import org.matsim.contrib.parking.PC2.infrastructure.PublicParking;
import org.matsim.contrib.parking.PC2.scoring.ParkingBetas;
import org.matsim.contrib.parking.PC2.scoring.ParkingCostModel;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoreManager;
import org.matsim.contrib.parking.PC2.scoring.ParkingScoringFunctionFactory;
import org.matsim.contrib.parking.PC2.simulation.ParkingInfrastructureManager;
import org.matsim.contrib.parking.parkingChoice.carsharing.ParkingModuleWithFreeFloatingCarSharing;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

import playground.wrashid.parkingChoice.infrastructure.PrivateParking;
import playground.wrashid.parkingChoice.infrastructure.api.Parking;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ParkingLoader;
import playground.wrashid.parkingSearch.ppSim.jdepSim.zurich.ZHScenarioGlobal;

public class SetupParkingForZHScenario {

	public static void prepare(ParkingModuleWithFFCarSharing parkingModule, Config config){
		String baseDir = config.getParam("parkingChoice.ZH", "parkingDataDirectory");
		
		// TODO: perform scaling here!
		
		LinkedList<Parking> parkings = ParkingLoader.getParkingsForScenario(baseDir);
	
		// instead of reading scoring parameters inside the manager, just set the scoring model on the manager,
		// after setting it up (which can be a separate module).
		
		
		
		ParkingScoreManager parkingScoreManager = prepareParkingScoreManager(parkingModule);
		ParkingInfrastructureManager pim=new ParkingInfrastructureManager(parkingScoreManager);
		
		ParkingCostModel pcm=new ParkingCostModelZH(config,parkings);
		LinkedList<PublicParking> publicParkings=new LinkedList<PublicParking>();
		LinkedList<PPRestrictedToFacilities> ppRestrictedToFacilities=new LinkedList<PPRestrictedToFacilities>();
		for (Parking parking: parkings){
			String groupName=null;
			if (parking.getId().toString().contains("stp")){
				groupName="streetParking";
			} else if (parking.getId().toString().contains("gp")){
				groupName="garageParking";
			} else if (parking.getId().toString().contains("publicPOutsideCity")){
				groupName="publicPOutsideCity";
			}
			if (groupName!=null){
				PublicParking publicParking=new PublicParking(parking.getId(),parking.getIntCapacity(),parking.getCoord(),pcm,groupName);
				publicParkings.add(publicParking);
			} else {
				PrivateParking pp=(PrivateParking) parking;
				HashSet<Id> hs=new HashSet<Id>();
				hs.add(pp.getActInfo().getFacilityId());
				PPRestrictedToFacilities PPRestrictedToFacilitiesTmp=new PPRestrictedToFacilities(parking.getId(),parking.getIntCapacity(),parking.getCoord(),pcm,groupName,hs);
				ppRestrictedToFacilities.add(PPRestrictedToFacilitiesTmp);
			}
		}
		
		pim.setPublicParkings(publicParkings); 
		pim.setPrivateParkingRestrictedToFacilities(ppRestrictedToFacilities);
		
		parkingModule.setParkingInfrastructurManager(pim);
		appendScoringFactory(parkingModule);
	}
	
	public static void appendScoringFactory(ParkingModuleWithFFCarSharing parkingModule){
		parkingModule.getControler().setScoringFunctionFactory(new ParkingScoringFunctionFactory (parkingModule.getControler().getScoringFunctionFactory(),parkingModule.getParkingScoreManager()));
	}
	
	public static ParkingScoreManager prepareParkingScoreManager(ParkingModuleWithFFCarSharing parkingModule) {
		Controler controler=parkingModule.getControler();
		ParkingScoreManager parkingScoreManager = new ParkingScoreManager(getWalkTravelTime(parkingModule.getControler()));
		
		ParkingBetas parkingBetas=new ParkingBetas();
		parkingBetas.setParkingWalkBeta(controler.getConfig().getParam("parkingChoice.ZH", "parkingWalkBeta"));
		parkingBetas.setParkingCostBeta(controler.getConfig().getParam("parkingChoice.ZH", "parkingCostBeta"));
		parkingScoreManager.setParkingBetas(parkingBetas);
		
		double parkingScoreScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice.ZH", "parkingScoreScalingFactor"));
		parkingScoreManager.setParkingScoreScalingFactor(parkingScoreScalingFactor);
		double randomErrorTermScalingFactor= Double.parseDouble(controler.getConfig().getParam("parkingChoice.ZH", "randomErrorTermScalingFactor"));
		parkingScoreManager.setRandomErrorTermScalingFactor(randomErrorTermScalingFactor);
		return parkingScoreManager;
	}
	
	private static WalkTravelTime getWalkTravelTime(Controler controler){
		Map<Id, Double> linkSlopes=new HashMap<Id, Double>();
		String linkSlopeAttributeFile = controler.getConfig().getParam("parkingChoice.ZH", "networkLinkSlopes");
		ObjectAttributes lp = new ObjectAttributes();
		new ObjectAttributesXmlReader(lp).parse(linkSlopeAttributeFile);

		for (Id linkId : controler.getNetwork().getLinks().keySet()) {
			linkSlopes.put(linkId, (Double) lp.getAttribute(linkId.toString(), "slope"));
		}
		
		WalkTravelTime walkTravelTime = new WalkTravelTime(new PlansCalcRouteConfigGroup(), linkSlopes);
		return walkTravelTime;
	}

}
