/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.speedup;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams.WaitingTimeUpdateDuringSpeedUp;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.common.base.Preconditions;

/**
 * @author ikaddoura
 * @author michalm (Michal Maciejewski)
 */
public final class DrtSpeedUp implements IterationStartsListener, IterationEndsListener {
	private static final Logger log = LogManager.getLogger(DrtSpeedUp.class);

	public static boolean isTeleportDrtUsers(DrtSpeedUpParams drtSpeedUpParams, ControllerConfigGroup controlerConfig,
			int iteration) {
		int lastIteration = controlerConfig.getLastIteration();
		if (iteration < drtSpeedUpParams.fractionOfIterationsSwitchOn * lastIteration
				|| iteration >= drtSpeedUpParams.fractionOfIterationsSwitchOff * lastIteration) {
			return false; // full drt simulation
		}

		//full drt simulation only with a defined interval
		return iteration % drtSpeedUpParams.intervalDetailedIteration != 0;
	}

	private final String mode;
	private final DrtSpeedUpParams drtSpeedUpParams;
	private final ControllerConfigGroup controlerConfig;
	private final Network network;
	private final FleetSpecification fleetSpecification;
	private final DrtEventSequenceCollector drtEventSequenceCollector;

	private final SimpleRegression ridesPerVehicle2avgWaitingTimeRegression = new SimpleRegression();

	private final List<Double> averageWaitingTimes = new ArrayList<>();
	private final List<Double> averageInVehicleBeelineSpeeds = new ArrayList<>();

	private double currentAvgWaitingTime;
	private double currentAvgInVehicleBeelineSpeed;

	public DrtSpeedUp(String mode, DrtSpeedUpParams drtSpeedUpParams, ControllerConfigGroup controlerConfig,
			Network network, FleetSpecification fleetSpecification,
			DrtEventSequenceCollector drtEventSequenceCollector) {
		this.mode = mode;
		this.drtSpeedUpParams = drtSpeedUpParams;
		this.controlerConfig = controlerConfig;
		this.network = network;
		this.fleetSpecification = fleetSpecification;
		this.drtEventSequenceCollector = drtEventSequenceCollector;

		currentAvgWaitingTime = drtSpeedUpParams.initialWaitingTime;
		currentAvgInVehicleBeelineSpeed = drtSpeedUpParams.initialInVehicleBeelineSpeed;
	}

	public DrtTeleportedRouteCalculator createTeleportedRouteCalculator() {
		return new DrtTeleportedRouteCalculator(currentAvgWaitingTime, currentAvgInVehicleBeelineSpeed);
	}

	public DrtSpeedUpParams getParams() {
		return drtSpeedUpParams;
	}

	double getCurrentAvgWaitingTime() {
		return currentAvgWaitingTime;
	}

	double getCurrentAvgInVehicleBeelineSpeed() {
		return currentAvgInVehicleBeelineSpeed;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int iteration = event.getIteration();
		boolean teleportDrtUsers = isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, iteration);
		if (teleportDrtUsers) {
			log.info(
					"Teleporting {} users in iteration {}. Current teleported mode speed: {}. Current waiting time: {}",
					mode, iteration, currentAvgInVehicleBeelineSpeed, currentAvgWaitingTime);
		} else {
			log.info("Simulating {} in iteration {}", mode, iteration);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int iteration = event.getIteration();
		boolean teleportDrtUsers = isTeleportDrtUsers(drtSpeedUpParams, controlerConfig, iteration);
		if (iteration < drtSpeedUpParams.firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams) {
			String type = teleportDrtUsers ? "teleported" : "simulated";
			log.info("Number of {} {} trips: {}", type, mode, completedTripCount());
		} else {
			if (teleportDrtUsers) {
				postprocessTeleportedDrtTrips();
			} else {
				postprocessSimulatedDrtTrips();
			}
		}
	}

	private int completedTripCount() {
		return (int)drtEventSequenceCollector.getPerformedRequestSequences()
				.values()
				.stream()
				.filter(DrtEventSequenceCollector.EventSequence::isCompleted)
				.count();
	}

	private void postprocessSimulatedDrtTrips() {
		SimulatedTripStats tripStats = computeSimulatedTripStats();
		log.info("Number of simulated " + mode + " trips: " + tripStats.count);

		// store additional information
		averageWaitingTimes.add(tripStats.averageWaitTime);
		averageInVehicleBeelineSpeeds.add(tripStats.averageInVehicleBeelineSpeed);

		double movingAverageWaitingTime = computeMovingAverage(drtSpeedUpParams.movingAverageSize, averageWaitingTimes);
		log.info("Setting waiting time for {} to: {} (previous value: {})", mode, movingAverageWaitingTime,
				currentAvgWaitingTime);
		currentAvgWaitingTime = movingAverageWaitingTime;

		double movingAverageInVehicleBeelineSpeed = computeMovingAverage(drtSpeedUpParams.movingAverageSize,
				averageInVehicleBeelineSpeeds);
		log.info("Setting in-vehicle beeline speed for {} to: {} (previous value: {})", mode,
				movingAverageInVehicleBeelineSpeed, currentAvgInVehicleBeelineSpeed);
		currentAvgInVehicleBeelineSpeed = movingAverageInVehicleBeelineSpeed;

		if (drtSpeedUpParams.waitingTimeUpdateDuringSpeedUp == WaitingTimeUpdateDuringSpeedUp.LinearRegression) {
			// update regression model
			double fleetSize = fleetSpecification.getVehicleSpecifications().size();
			Preconditions.checkState(fleetSize >= 1, "No vehicles for drt mode %s. Aborting...", mode);
			double ridesPerVehicle = tripStats.count / fleetSize;
			ridesPerVehicle2avgWaitingTimeRegression.addData(ridesPerVehicle, currentAvgWaitingTime);
		}
	}

	private static class SimulatedTripStats {
		private final int count;
		private final double averageInVehicleBeelineSpeed;
		private final double averageWaitTime;

		private SimulatedTripStats(int count, double averageInVehicleBeelineSpeed, double averageWaitTime) {
			this.count = count;
			this.averageInVehicleBeelineSpeed = averageInVehicleBeelineSpeed;
			this.averageWaitTime = averageWaitTime;
		}
	}

	private SimulatedTripStats computeSimulatedTripStats() {
		Mean meanInVehicleBeelineSpeed = new Mean();
		Mean meanWaitTime = new Mean();

		for (var sequence : drtEventSequenceCollector.getPerformedRequestSequences().values()) {
			if (!sequence.isCompleted()) {
				continue;//skip incomplete sequences
			}
			DrtRequestSubmittedEvent submittedEvent = sequence.getSubmitted();
			Link depLink = network.getLinks().get(submittedEvent.getFromLinkId());
			Link arrLink = network.getLinks().get(submittedEvent.getToLinkId());
			double beelineDistance = DistanceUtils.calculateDistance(depLink.getToNode(), arrLink.getToNode());

			for (Id<Person> personId : submittedEvent.getPersonIds()) {
				if(sequence.getPersonEvents().containsKey(personId)) {
					DrtEventSequenceCollector.EventSequence.PersonEvents personEvents = sequence.getPersonEvents().get(personId);
					if(personEvents.getPickedUp().isPresent() && personEvents.getDroppedOff().isPresent()) {
						double pickupTime = personEvents.getPickedUp().get().getTime();
						double waitTime = pickupTime - sequence.getSubmitted().getTime();
						double rideTime = personEvents.getDroppedOff().get().getTime() - pickupTime;

						//TODO I would map unshared_ride_time to rideTime -- should be more precise
						meanInVehicleBeelineSpeed.increment(beelineDistance / rideTime);
						meanWaitTime.increment(waitTime);
					}
				}
			}
		}

		int count = (int)meanWaitTime.getN();
		return new SimulatedTripStats(count,
				count == 0 ? drtSpeedUpParams.initialInVehicleBeelineSpeed : meanInVehicleBeelineSpeed.getResult(),
				count == 0 ? drtSpeedUpParams.initialWaitingTime : meanWaitTime.getResult());
	}

	static double computeMovingAverage(int movingAverageSize, List<Double> values) {
		int startIndex = Math.max(0, values.size() - movingAverageSize);
		return values.subList(startIndex, values.size()).stream().mapToDouble(v -> v).average().orElseThrow();
	}

	private void postprocessTeleportedDrtTrips() {
		if (drtSpeedUpParams.waitingTimeUpdateDuringSpeedUp == WaitingTimeUpdateDuringSpeedUp.LinearRegression) {
			//FIXME potential race condition: fleet may be modified by opt-drt!!
			// I suggest modifying them when an iteration starts
			// (like modifying population plans happens at the beginning of an iteration)
			double fleetSize = fleetSpecification.getVehicleSpecifications().size();
			Preconditions.checkState(fleetSize >= 1, "No vehicles for drt mode %s. Aborting...", mode);
			log.info("Current fleet size for {}: {}", mode, fleetSize);
			double predictedWaitingTime = ridesPerVehicle2avgWaitingTimeRegression.predict(
					completedTripCount() / fleetSize);
			log.info("Predicted average waiting time for {}: {}", mode, predictedWaitingTime);

			if (Double.isNaN(predictedWaitingTime)) {
				log.info("Not enough data points for linear regression. Not updating the average waiting time!");
			} else if (predictedWaitingTime <= 0) {
				//TODO why not setting it to 0 instead?
				log.info(
						"Predicted average waiting from linear regression is negative. Not updating the average waiting time!");
			} else {
				log.info("Setting waiting time for {} to: {} (previous value: {})", mode, predictedWaitingTime,
						currentAvgWaitingTime);
				currentAvgWaitingTime = predictedWaitingTime; //TODO maybe combine with the moving average
			}
		}
	}
}
