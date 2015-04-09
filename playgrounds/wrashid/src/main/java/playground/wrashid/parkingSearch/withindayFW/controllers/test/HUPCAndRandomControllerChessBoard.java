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

package playground.wrashid.parkingSearch.withindayFW.controllers.test;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.multimodal.router.MultimodalTripRouterFactory;
import org.matsim.contrib.multimodal.router.util.BikeTravelTime;
import org.matsim.contrib.multimodal.router.util.UnknownTravelTime;
import org.matsim.contrib.multimodal.router.util.WalkTravelTime;
import org.matsim.contrib.parking.lib.GeneralLib;
import org.matsim.contrib.parking.lib.obj.IntegerValueHashMap;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.facilities.ActivityFacility;

import playground.wrashid.parkingSearch.withindayFW.controllers.WithinDayParkingController;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategy;
import playground.wrashid.parkingSearch.withindayFW.core.ParkingStrategyManager;
import playground.wrashid.parkingSearch.withindayFW.impl.ParkingStrategyActivityMapperFW;
import playground.wrashid.parkingSearch.withindayFW.psHighestUtilityParkingChoice.HUPCIdentifier;
import playground.wrashid.parkingSearch.withindayFW.psHighestUtilityParkingChoice.HUPCReplannerFactory;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategy.RandomSearchIdentifier;
import playground.wrashid.parkingSearch.withindayFW.randomTestStrategy.RandomSearchReplannerFactory;
import playground.wrashid.parkingSearch.withindayFW.utility.ParkingPersonalBetas;

public class HUPCAndRandomControllerChessBoard extends WithinDayParkingController  {
	public HUPCAndRandomControllerChessBoard(String[] args) {
		super(args);
	}

	@Override
	protected void startUpFinishing() {
		
		ParkingPersonalBetas parkingPersonalBetas = new ParkingPersonalBetas(this.scenarioData, null);

		ParkingStrategyActivityMapperFW parkingStrategyActivityMapperFW = new ParkingStrategyActivityMapperFW();
		Collection<ParkingStrategy> parkingStrategies = new LinkedList<ParkingStrategy>();
		ParkingStrategyManager parkingStrategyManager = new ParkingStrategyManager(parkingStrategyActivityMapperFW,
				parkingStrategies, parkingPersonalBetas);
		parkingAgentsTracker.setParkingStrategyManager(parkingStrategyManager);

		// create a copy of the MultiModalTravelTimeWrapperFactory and set the
		// TravelTimeCollector for car mode
		
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.walk, new WalkTravelTime(this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.bike, new BikeTravelTime(this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.ride, new UnknownTravelTime(TransportMode.ride, this.config.plansCalcRoute()));
		travelTimes.put(TransportMode.pt, new UnknownTravelTime(TransportMode.pt, this.config.plansCalcRoute()));

		// travelTimes.put(TransportMode.car, super.getTravelTimeCollector());
		// Only the "non-simulated" modes handled by the multimodal extension should go in there.
		
		this.setTravelDisutilityFactory(new OnlyTimeDependentTravelDisutilityFactory());
		this.initWithinDayTripRouterFactory();
		
		RoutingContext routingContext = new RoutingContextImpl(this.getTravelDisutilityFactory(), this.getTravelTimeCollector(), this.config.planCalcScore());
		
		TripRouterFactory tripRouterFactory = new MultimodalTripRouterFactory(this.scenarioData, travelTimes,
				this.getTravelDisutilityFactory());
		this.setWithinDayTripRouterFactory(tripRouterFactory);

		// adding hight utility parking choice algo
		HUPCReplannerFactory hupcReplannerFactory = new HUPCReplannerFactory(this.getWithinDayEngine(),
				this.scenarioData, parkingAgentsTracker, tripRouterFactory, routingContext);
		HUPCIdentifier hupcSearchIdentifier = new HUPCIdentifier(parkingAgentsTracker, parkingInfrastructure, this.scenarioData );
		this.getFixedOrderSimulationListener().addSimulationListener(hupcSearchIdentifier);
		hupcReplannerFactory.addIdentifier(hupcSearchIdentifier);
		ParkingStrategy parkingStrategy = new ParkingStrategy(hupcSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getWithinDayEngine().addDuringLegReplannerFactory(hupcReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "home", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "shopping", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "leisure", parkingStrategy);

		
		// adding random test strategy
		RandomSearchReplannerFactory randomReplannerFactory = new RandomSearchReplannerFactory(this.getWithinDayEngine(),
				this.scenarioData, parkingAgentsTracker, this.getWithinDayTripRouterFactory(), routingContext);
		RandomSearchIdentifier randomSearchIdentifier = new RandomSearchIdentifier(parkingAgentsTracker, parkingInfrastructure);
		this.getFixedOrderSimulationListener().addSimulationListener(randomSearchIdentifier);
		randomReplannerFactory.addIdentifier(randomSearchIdentifier);
		parkingStrategy = new ParkingStrategy(randomSearchIdentifier);
		parkingStrategies.add(parkingStrategy);
		this.getWithinDayEngine().addDuringLegReplannerFactory(randomReplannerFactory);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "home", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "work", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "shopping", parkingStrategy);
		parkingStrategyActivityMapperFW.addSearchStrategy(null, "leisure", parkingStrategy);
		
		this.addControlerListener(parkingStrategyManager);
		this.getFixedOrderSimulationListener().addSimulationListener(parkingStrategyManager);
	
		initParkingFacilityCapacities();
	}
	
	private void initParkingFacilityCapacities() {
		IntegerValueHashMap<Id> facilityCapacities=new IntegerValueHashMap<Id>();
		parkingInfrastructure.setFacilityCapacities(facilityCapacities);
		
		for (ActivityFacility parkingFacility:parkingInfrastructure.getParkingFacilities()){
			facilityCapacities.incrementBy(parkingFacility.getId(),1000);
		}
	}
	
	
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println("using default config");
			args = new String[] { "test/input/playground/wrashid/parkingSearch/withinday/chessboard/config.xml" };
			
		
		}
		final HUPCAndRandomControllerChessBoard controller = new HUPCAndRandomControllerChessBoard(args);

		controller.setOverwriteFiles(true);

		controller.run();

		
		System.exit(0);
	}

	@Override
	protected void initReplanners(QSim sim) {
		// TODO Auto-generated method stub
		
	}

}
