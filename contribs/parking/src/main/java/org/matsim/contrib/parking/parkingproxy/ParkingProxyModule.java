/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.contrib.parking.parkingproxy.config.ParkingProxyConfigGroup;

/**
 * <p>
 * Module to estimate additional time needed by agents due to high parking pressure in an area. For configuration options
 * see {@linkplain ParkingProxyConfigGroup}.
 * </p>
 * <p>
 * The module generates a (random) initial car distribution based on the statistical number of cars per 1000 persons. Then
 * these cars are tracked through the day and for each cell in a space-time-grid counted how many cars are in it. A time
 * penalty is calculated based on that value and added to every agent's egress walk from the car if they are arriving by
 * car in that specific space-time-gridcell.
 * <p>
 * 
 * @author tkohl / Senozon
 *
 */
public /*deliberately non-final*/ class ParkingProxyModule extends AbstractModule {
	
	private final static int GRIDSIZE = 500;
	private final Scenario scenario;
	
	public ParkingProxyModule(Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void install() {
		ParkingProxyConfigGroup parkingConfig = ConfigUtils.addOrGetModule(getConfig(), ParkingProxyConfigGroup.class );
		
		Collection<Tuple<Coord, Integer>> initialLoad = calculateInitialLoad(parkingConfig);
		int qsimEndTime = (int) getConfig().qsim().getEndTime().orElse(30*3600);
		MovingEntityCounter carCounter = new MovingEntityCounter(
				initialLoad, 
				parkingConfig.getTimeBinSize(), 
				qsimEndTime,
				GRIDSIZE
				);
		PenaltyFunction penaltyFunction = new LinearPenaltyFunctionWithCap(parkingConfig.getDelayPerCar(), parkingConfig.getMaxDelay());
		//PenaltyFunction penaltyFunction = new ExponentialPenaltyFunctionWithCap(10, parkingConfig.getGridSize(), parkingConfig.getMaxDelay(), 360);
		
		ParkingVehiclesCountEventHandler parkingHandler = new ParkingVehiclesCountEventHandler(carCounter, scenario.getNetwork(), parkingConfig.getScenarioScaleFactor());
		super.addEventHandlerBinding().toInstance(parkingHandler);
		
		CarEgressWalkObserver walkObserver;
		switch (parkingConfig.getIter0Method()) {
		case hourPenalty:
			walkObserver = new CarEgressWalkObserver(parkingHandler, penaltyFunction, PenaltyCalculator.getDummyHourCalculator());
			break;
		case noPenalty:
			walkObserver = new CarEgressWalkObserver(parkingHandler, penaltyFunction, PenaltyCalculator.getDummyZeroCalculator());
			break;
		case takeFromAttributes:
			// CarEgressWalkChanger will handle this, we don't want to also change egress walks. Note that if it is observeOnly, the first iteration will put out zeros.
			walkObserver = new CarEgressWalkObserver(parkingHandler, penaltyFunction, PenaltyCalculator.getDummyZeroCalculator());
			break;
		case estimateFromPlans:
			ParkingCounterByPlans plansCounter = new ParkingCounterByPlans(carCounter, parkingConfig.getScenarioScaleFactor());
			plansCounter.calculateByPopulation(scenario.getPopulation(), scenario.getNetwork());
			walkObserver = new CarEgressWalkObserver(parkingHandler, penaltyFunction, plansCounter.generatePenaltyCalculator());
			break;
		default:
			throw new RuntimeException("Unknown iter0 mode");
		}
		if (parkingConfig.getObserveOnly()) {
			super.addControlerListenerBinding().toInstance(walkObserver);
		} else {
			CarEgressWalkChanger walkChanger = new CarEgressWalkChanger(parkingHandler, penaltyFunction, walkObserver, parkingConfig.getIter0Method());
			super.addControlerListenerBinding().toInstance(walkChanger);
			super.addControlerListenerBinding().toInstance(walkChanger.getBackChanger());
		}
	}
	
	protected Collection<Tuple<Coord, Integer>> calculateInitialLoad(ParkingProxyConfigGroup parkingConfig) {
		InitialLoadGenerator loadGenerator = new InitialLoadGeneratorWithConstantShare(scenario.getPopulation().getPersons().values(), parkingConfig.getScenarioScaleFactor(), parkingConfig.getCarsPer1000Persons());
//		bind( InitialLoadGenerator.class ).toInstance( loadGenerator );
		return loadGenerator.calculateInitialCarPositions();
	}

}
