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

package playground.wrashid.parkingSearch.withindayFW.controllers.kti;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;

import playground.wrashid.parkingChoice.infrastructure.api.PParking;
import playground.wrashid.parkingChoice.trb2011.ParkingHerbieControler;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategyManager;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyActivityMapperFW;
import playground.wrashid.parkingSearch.withindayFW.psHighestUtilityParkingChoice.HUPCIdentifier;
import playground.wrashid.parkingSearch.withindayFW.psHighestUtilityParkingChoice.HUPCReplannerFactory;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class HUPCBasicControllerKTI extends KTIWithinDayControler  {

	private LinkedList<PParking> parkings;

	public HUPCBasicControllerKTI(String[] args) {
		super(args);
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}
	
	@Override
	protected void startUpBegin() {
		initParkingInfrastructure(this);
	}

	@Override
	protected void startUpFinishing() {
		
		ParkingPersonalBetas parkingPersonalBetas = new ParkingPersonalBetas(this.scenarioData, null);

		ParkingStrategyActivityMapperFW parkingStrategyActivityMapperFW = new ParkingStrategyActivityMapperFW();
		Collection<ParkingStrategy> parkingStrategies = new LinkedList<ParkingStrategy>();
		ParkingStrategyManager parkingStrategyManager = new ParkingStrategyManager(parkingStrategyActivityMapperFW,
				parkingStrategies, parkingPersonalBetas);
		parkingAgentsTracker.setParkingStrategyManager(parkingStrategyManager);

		/*
		 * Initialize TravelTimeCollector and create a FactoryWrapper which will act as
		 * factory but returns always the same travel time object, which is possible since
		 * the TravelTimeCollector is not personalized.
		 */
		Set<String> analyzedModes = new HashSet<String>();
		analyzedModes.add(TransportMode.car);
		super.createAndInitTravelTimeCollector(analyzedModes);
		
		this.setTravelDisutilityFactory(new OnlyTimeDependentTravelDisutilityFactory());
		this.initWithinDayTripRouterFactory();
		
		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), this.getTravelTimeCollector(), this.config.planCalcScore());
		
		// adding hight utility parking choice algo
		HUPCReplannerFactory hupcReplannerFactory = new HUPCReplannerFactory(this.getWithinDayEngine(),
				this.scenarioData, parkingAgentsTracker, this. getWithinDayTripRouterFactory(), routingContext);
		HUPCIdentifier hupcSearchIdentifier = new HUPCIdentifier(parkingAgentsTracker, parkingInfrastructure, this.scenarioData );
		this.getFixedOrderSimulationListener().addSimulationListener(hupcSearchIdentifier);
		hupcReplannerFactory.addIdentifier(hupcSearchIdentifier);
		ParkingStrategy parkingStrategy = new ParkingStrategy(hupcSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getWithinDayEngine().addDuringLegReplannerFactory(hupcReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "home", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work_sector2", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work_sector3", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "shop", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "leisure", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_other", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_kindergarten", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_primary", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_secondary", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "education_higher", parkingStrategy);	
		
		this.addControlerListener(parkingStrategyManager);
		this.getFixedOrderSimulationListener().addSimulationListener(parkingStrategyManager);

		initParkingFacilityCapacities();
		
		cleanNetwork();
	}

	private void cleanNetwork() {
		//network cleaning
		// set min length of link to 10m (else replanning does not function, because vehicle arrives
		// before it can be replanned).
		// case where this happend: although p1 and p2 are on different links, if both have length zero
		// then replanning does not happen and walk leg, etc. is not set of link.
		
		//TODO: alternative to handle this (make check where getting alternative parking (not on same link)
		// there check, if link length is zero of current and next link (and if this is the case), exclude both links
		// until link with length non-zero in the set.
		
		int minLinkLength = 40;
        for (Link link: getScenario().getNetwork().getLinks().values()){
			if (link.getLength()<minLinkLength){
				link.setLength(minLinkLength);
			}
		}
	}

	private void initParkingFacilityCapacities() {
		IntegerValueHashMap<Id> facilityCapacities=new IntegerValueHashMap<Id>();
		parkingInfrastructure.setFacilityCapacities(facilityCapacities);
		
		for (PParking parking:parkings){
			facilityCapacities.incrementBy(parking.getId(),(int) Math.round(parking.getCapacity()));
		}
	}
	
	
	
	private void initParkingInfrastructure(Controler controler) {
		parkings = getParkingsForScenario(controler);
		
		ActivityFacilities facilities = this.scenarioData.getActivityFacilities();
		ActivityFacilitiesFactory factory = facilities.getFactory();

		for (PParking parking:parkings){
			
			ActivityFacility parkingFacility = factory.createActivityFacility(Id.create(parking.getId(), ActivityFacility.class), parking.getCoord());
			facilities.addActivityFacility(parkingFacility);
			Link nearestLink = NetworkUtils.getNearestLink(((NetworkImpl) this.scenarioData.getNetwork()), parking.getCoord());
			
			((ActivityFacilityImpl)parkingFacility).setLinkId(nearestLink.getId());
			
			ActivityOption activityOption = factory.createActivityOption("parking");
			parkingFacility.addActivityOption(activityOption);
			activityOption.addOpeningTime(new OpeningTimeImpl(0*3600, 24*3600));
			activityOption.setCapacity(Double.MAX_VALUE);
		}

	}

	public static LinkedList<PParking> getParkingsForScenario(Controler controler) {
		String parkingDataBase;
		String isRunningOnServer = controler.getConfig().findParam("parking", "isRunningOnServer");
		if (Boolean.parseBoolean(isRunningOnServer)) {
			parkingDataBase = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2011/parkings/flat/";
			ParkingHerbieControler.isRunningOnServer = true;
		} else {
			parkingDataBase = "H:/data/experiments/TRBAug2011/parkings/flat/";
			ParkingHerbieControler.isRunningOnServer = false;
		}
		
		
		double parkingsOutsideZHCityScaling = Double.parseDouble(controler.getConfig().findParam("parking",
				"publicParkingsCalibrationFactorOutsideZHCity"));

		LinkedList<PParking> parkingCollection = getParkingCollectionZHCity(controler,parkingDataBase);
		String streetParkingsFile = null;
		//if (isKTIMode) {
			streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0_dilZh30km_10pct.xml";
		//} else {
		//	streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0.xml";
		//}

		readParkings(parkingsOutsideZHCityScaling, streetParkingsFile, parkingCollection);

		return parkingCollection;
	}
	
	public static LinkedList<PParking> getParkingCollectionZHCity(Controler controler,String parkingDataBase) {
		double streetParkingCalibrationFactor = Double.parseDouble(controler.getConfig().findParam("parking",
				"streetParkingCalibrationFactorZHCity"));
		double garageParkingCalibrationFactor = Double.parseDouble(controler.getConfig().findParam("parking",
				"garageParkingCalibrationFactorZHCity"));
		double privateParkingCalibrationFactorZHCity = Double.parseDouble(controler.getConfig().findParam("parking",
				"privateParkingCalibrationFactorZHCity"));
		// double
		// privateParkingsOutdoorCalibrationFactor=Double.parseDouble(controler.getConfig().findParam("parking",
		// "privateParkingsOutdoorCalibrationFactorZHCity"));

		LinkedList<PParking> parkingCollection = new LinkedList<PParking>();

		String streetParkingsFile = parkingDataBase + "streetParkings.xml";
		readParkings(streetParkingCalibrationFactor, streetParkingsFile, parkingCollection);

		String garageParkingsFile = parkingDataBase + "garageParkings.xml";
		readParkings(garageParkingCalibrationFactor, garageParkingsFile, parkingCollection);

		String privateIndoorParkingsFile = null;
		//if (isKTIMode) {
			privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1_kti.xml";
		//} else {
		//	privateIndoorParkingsFile = parkingDataBase + "privateParkings_v1.xml";
		//}

		readParkings(privateParkingCalibrationFactorZHCity, privateIndoorParkingsFile, parkingCollection);

		return parkingCollection;
	}
	
	
	public static void readParkings(double parkingCalibrationFactor, String parkingsFile, LinkedList<PParking> parkingCollection) {
		ParkingHerbieControler.readParkings(parkingCalibrationFactor, parkingsFile, parkingCollection);
	}
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("using default config");
		//	args = new String[] { "test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml" };
			args = new String[] { "H:/data/experiments/TRBAug2011/runs/ktiRun1/configRunLocal2.xml" };
			
		
		}
		final HUPCBasicControllerKTI controller = new HUPCBasicControllerKTI(args);

		controller.setOverwriteFiles(true);

		controller.run();

		
		System.exit(0);
	}

}
