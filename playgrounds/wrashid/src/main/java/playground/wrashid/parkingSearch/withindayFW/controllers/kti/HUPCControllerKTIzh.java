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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
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
import playground.wrashid.parkingSearch.withindayFW.interfaces.ParkingCostCalculator;
import playground.wrashid.parkingSearch.withindayFW.util.GlobalParkingSearchParams;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;
import playground.wrashid.parkingSearch.withindayFW.zhCity.*;
import playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC.HUPCIdentifier;
import playground.wrashid.parkingSearch.withindayFW.zhCity.HUPC.HUPCReplannerFactory;

import java.util.*;

public class HUPCControllerKTIzh extends KTIWithinDayControler  {
	private LinkedList<PParking> parkings;

	protected static final Logger log = Logger.getLogger(HUPCControllerKTIzh.class);
	







	public HUPCControllerKTIzh(String[] args) {
		super(args);
		throw new RuntimeException(Gbl.CREATE_ROUTING_ALGORITHM_WARNING_MESSAGE) ;
	}
	
	@Override
	protected void startUpBegin() {
		//TODO: read all parameters from module parking at one place!
		String scenarioIdString = this.getConfig().findParam("parking", "scenarioId");
		GlobalParkingSearchParams.setScenarioId(Integer.parseInt(scenarioIdString));
		
		String tmpString = this.getConfig().findParam("parking", "populationPercentage");
		GlobalParkingSearchParams.setPopulationPercentage(Double.parseDouble(tmpString));
		
		tmpString = this.getConfig().findParam("parking", "parkingScoreWeight");
		GlobalParkingSearchParams.setParkingScoreWeight(Double.parseDouble(tmpString));
		
		tmpString = this.getConfig().findParam("parking", "detailedOutputAfterIteration");
		GlobalParkingSearchParams.setDetailedOutputAfterIteration(Integer.parseInt(tmpString));
		
		
		
		
		HashMap<String, HashSet<Id>> parkingTypes=new HashMap<String, HashSet<Id>>();
		initParkingInfrastructure(this,parkingTypes);
		
		
		String isRunningOnServer = this.getConfig().findParam("parking", "isRunningOnServer");
		String cityZonesFilePath=null;
		if (Boolean.parseBoolean(isRunningOnServer)) {
			cityZonesFilePath = "/Network/Servers/kosrae.ethz.ch/Volumes/ivt-home/wrashid/data/experiments/TRBAug2012/parkings/zones.csv";
			String numReplanningThreadsString = this.getConfig().findParam("parking", "numReplanningThreads");
			numReplanningThreads=Integer.parseInt(numReplanningThreadsString);
		} else {
			cityZonesFilePath = "H:/data/experiments/TRBAug2012/parkings/zones.csv";
		}
		
		ParkingCostCalculator parkingCostCalculator=null;
		if (GlobalParkingSearchParams.getScenarioId()==1){
			parkingCostCalculator=new ParkingCostCalculatorZH(new CityZones(cityZonesFilePath), scenarioData,parkings);
		} else if (GlobalParkingSearchParams.getScenarioId()==2) {
			ParkingCostCalculatorZH parkingCostCalculatorZH = new ParkingCostCalculatorZH(new CityZones(cityZonesFilePath), scenarioData,parkings);
			parkingCostCalculator=new ParkingCostOptimizerZH(parkingCostCalculatorZH,this);
		} else {
			DebugLib.stopSystemAndReportInconsistency("sceanrio unknown");
		}
		
		parkingInfrastructure=new ParkingInfrastructureZH(this.scenarioData,parkingTypes, parkingCostCalculator,parkings);
		
	}

	private HashMap<String, HashSet<Id>> initParkingTypes() {
		
		return null;
	}

	@Override
	protected void startUpFinishing() {
		
		
		HashMap<Id, Double> houseHoldIncome = getHouseHoldIncomeCantonZH(this.scenarioData);
		
		writeoutHouseholdIncome(houseHoldIncome);
		
		ParkingPersonalBetas parkingPersonalBetas = new ParkingPersonalBetas(this.scenarioData, houseHoldIncome);

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

		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), this.getTravelTimeCollector(), this.getConfig().planCalcScore());
				
		// adding hight utility parking choice algo
		HUPCReplannerFactory hupcReplannerFactory = new HUPCReplannerFactory(this.getWithinDayEngine(),
				this.scenarioData, parkingAgentsTracker, this.getWithinDayTripRouterFactory(), routingContext);
		
		HUPCIdentifier hupcSearchIdentifier = new HUPCIdentifier(parkingAgentsTracker, (ParkingInfrastructureZH) parkingInfrastructure, this);
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
		
		parkingAgentsTracker.setParkingAnalysisHandler(new ParkingAnalysisHandlerZH(this,parkingInfrastructure));
	}

	private void writeoutHouseholdIncome(HashMap<Id, Double> houseHoldIncome) {
		String fileName = this.getControlerIO().getOutputFilename("houseHoldIncome.txt");
		GeneralLib.writeHashMapToFile(houseHoldIncome,"personId\tIncome",fileName);
	}

	public static HashMap<Id, Double> getHouseHoldIncomeCantonZH(Scenario scenario) {
		HashMap<Id, Double> houseHoldIncome=new HashMap<Id, Double>();
		
		for (Id personId : scenario.getPopulation().getPersons().keySet()) {
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
	
	
	
	private void initParkingInfrastructure(Controler controler, HashMap<String, HashSet<Id>> parkingTypes) {
		parkings = getParkingsForScenario(controler);
		
		int i=0;
		while (i<parkings.size()){
			if (parkings.get(i).getCapacity()<1){
				parkings.remove(i);
				continue;
			}
			i++;
		}
		
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
		String parkingsFile = null;
		//if (isKTIMode) {
			parkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0_dilZh30km_10pct.xml";
		//} else {
		//	streetParkingsFile = parkingDataBase + "publicParkingsOutsideZHCity_v0.xml";
		//}

		readParkings(parkingsOutsideZHCityScaling, parkingsFile, parkingCollection);

		int numberOfStreetParking=0;
		int numberOfGarageParking=0;
		int numberOfPrivateParking=0;
		
		for (PParking parking:parkingCollection){
			if (parking.getId().toString().contains("stp")){
				numberOfStreetParking+=parking.getCapacity();
			} else if (parking.getId().toString().contains("gp")){
				numberOfGarageParking+=parking.getCapacity();
			} else if (parking.getId().toString().contains("private")){
				numberOfPrivateParking+=parking.getCapacity();
			}
		}
		
		double totalNumberOfParkingZH=numberOfStreetParking+numberOfGarageParking+numberOfPrivateParking;
		
		log.info("streetParking to garageParking (%): " + numberOfStreetParking/1.0/numberOfGarageParking + " - ref: 3.03");
		log.info("numberOfStreetParking (%): " + numberOfStreetParking/totalNumberOfParkingZH*100 + " - ref: 18.5 - [" + numberOfStreetParking + "]");
		log.info("numberOfGarageParking (%):" + numberOfGarageParking/totalNumberOfParkingZH*100 + " - ref: 6.1 - [" + numberOfGarageParking + "]");
		log.info("numberOfPrivateParking (%):" + numberOfPrivateParking/totalNumberOfParkingZH*100 + " - ref: 75.4 - [" + numberOfPrivateParking + "]");
		
		double populationScalingFactor = GlobalParkingSearchParams.getPopulationPercentage();
		
		log.info("totalNumberOfParkingZH: " + Math.round(totalNumberOfParkingZH/1000) + "k - ref: "+267000*populationScalingFactor/1000 + "k");
		
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
			args = new String[] { "H:/data/experiments/TRBAug2012/runs/run1/configRunLocal.xml" };
			
		
		}
		final HUPCControllerKTIzh controller = new HUPCControllerKTIzh(args);

		controller.setOverwriteFiles(true);

		controller.run();

		
		System.exit(0);
	}

}
