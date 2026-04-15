/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 * BicycleLegScoring.java                                                  *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.bicycle;

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * Creates score events for bicycle legs.
 * <p>
 * The class combines:
 * <ul>
 *     <li>bicycleAdditionalLinkScore for static bicycle link attributes such as infrastructure, comfort and gradient</li>
 *     <li>bicycleMotorizedInteractionScore for motorized interaction effects</li>
 * </ul>
 * Motorized interaction can currently be modeled in three ways:
 * <ul>
 *     <li>carCountOnBicycleLeaveLink</li>
 *     <li>carsPassedBicycleOnLink</li>
 *     <li>avgCarOccupancyDuringBicycleTraversal: average car occupancy on the same link during the bicycle traversal</li>
 * </ul>
 *
 * @author dziemke, vizsim
 */
class BicycleScoreEventsCreator implements
//		SumScoringFunction.LegScoring, SumScoringFunction.ArbitraryEventScoring
	VehicleEntersTrafficEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {
// -----------------------------------------------------------------------------
// Historical note
// -----------------------------------------------------------------------------
// yyyy The car interaction is still somewhat primitive -- a vehicle leaving a link gets a penalty that is multiplied with the number of cars that
// are on the link at that moment.  Evidently, this could be improved, by counting the cars that actually overtake the bicycle.  Not very difficult
// ...

	private static final Logger log = LogManager.getLogger(BicycleScoreEventsCreator.class);
	private static final String BICYCLE_ADDITIONAL_LINK_SCORE = "bicycleAdditionalLinkScore";
	private static final String BICYCLE_MOTORIZED_INTERACTION_SCORE = "bicycleMotorizedInteractionScore";
	private static final double MARGINAL_UTILITY_OF_CAR_COUNT_ON_BICYCLE_LEAVE_LINK = -0.004;
	private static final double MARGINAL_UTILITY_OF_CARS_PASSED_BICYCLE_ON_LINK = -0.004;
	private static final double MARGINAL_UTILITY_OF_AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL = -0.004;

	private final Network network;
	private final EventsManager eventsManager;
	private final AdditionalBicycleLinkScore additionalBicycleLinkScore;
	private final String bicycleMode;
	private final BicycleConfigGroup bicycleConfig;

	// -----------------------------------------------------------------------------
	// State for bicycleAdditionalLinkScore
	// -----------------------------------------------------------------------------
	private final Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();
	private final Map<Id<Vehicle>, Id<Link>> firstLinkIdMap = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, String> modeFromVehicle = new LinkedHashMap<>();

	// -----------------------------------------------------------------------------
	// State for bicycleMotorizedInteractionScore: carCountOnBicycleLeaveLink
	// -----------------------------------------------------------------------------
	private final Map<String, Map<Id<Link>, Double>> numberOfVehiclesOnLinkByMode = new LinkedHashMap<>();

	// -----------------------------------------------------------------------------
	// State for bicycleMotorizedInteractionScore: avgCarOccupancyDuringBicycleTraversal
	// -----------------------------------------------------------------------------
	private final Map<Id<Vehicle>, BikeExposureInfo> bikeExposureInfoByVehicle = new LinkedHashMap<>();
	private final Map<Id<Link>, Set<Id<Vehicle>>> activeBicyclesByLink = new LinkedHashMap<>();

	// -----------------------------------------------------------------------------
	// State for bicycleMotorizedInteractionScore: carsPassedBicycleOnLink
	// -----------------------------------------------------------------------------
	private final Map<Id<Vehicle>, LinkEnterInfo> currentLinkEnterInfoByVehicle = new LinkedHashMap<>();
	private final Map<Id<Link>, FinishedCarStore> finishedCarsByLink = new LinkedHashMap<>();

	@Inject
	BicycleScoreEventsCreator(Scenario scenario, EventsManager eventsManager, AdditionalBicycleLinkScore additionalBicycleLinkScore) {
		this.eventsManager = eventsManager;
		this.network = scenario.getNetwork();
		this.additionalBicycleLinkScore = additionalBicycleLinkScore;
		this.bicycleConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), BicycleConfigGroup.class);
		this.bicycleMode = bicycleConfig.getBicycleMode();
	}

	@Override
	public void reset(int iteration) {
		vehicle2driver.reset(iteration);
		firstLinkIdMap.clear();
		modeFromVehicle.clear();
		numberOfVehiclesOnLinkByMode.clear();
		bikeExposureInfoByVehicle.clear();
		activeBicyclesByLink.clear();
		currentLinkEnterInfoByVehicle.clear();
		finishedCarsByLink.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		// 1. Track vehicle and first link for bicycleAdditionalLinkScore.
		vehicle2driver.handleEvent(event);

		this.firstLinkIdMap.put(event.getVehicleId(), event.getLinkId());

		if (this.bicycleConfig.isMotorizedInteraction()) {
			// 2. Motorized interaction uses the network mode later in both interaction models.
			this.modeFromVehicle.put(event.getVehicleId(), event.getNetworkMode());

			if (this.bicycleConfig.isCarCountOnBicycleLeaveLink()) {
				// 3a. carCountOnBicycleLeaveLink: initialize link occupancy.
				// inc count by one:
				numberOfVehiclesOnLinkByMode.putIfAbsent(event.getNetworkMode(), new LinkedHashMap<>());
				Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(event.getNetworkMode());
				map.merge(event.getLinkId(), 1., Double::sum);
			}

			if (this.bicycleConfig.isCarsPassedBicycleOnLink()) {
				// 3b. carsPassedBicycleOnLink: remember the enter time on the current link.
				// Treat "enters traffic" as being on the first link from this time.
				// This is important, otherwise the very first link would miss enterTime.
				registerEnter(event.getVehicleId(), event.getLinkId(), event.getTime(), event.getNetworkMode());
			}

			if (this.bicycleConfig.isAvgCarOccupancyDuringBicycleTraversal()) {
				// 3c. avgCarOccupancyDuringBicycleTraversal: update the old car occupancy interval before mutating it.
				if (TransportMode.car.equals(event.getNetworkMode())) {
					updateBikeExposureOnLink(event.getLinkId(), event.getTime());
					incrementVehicleCountOnLink(TransportMode.car, event.getLinkId());
				} else if (bicycleMode.equals(event.getNetworkMode())) {
					registerBikeExposureEnter(event.getVehicleId(), event.getLinkId(), event.getTime());
				}
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.bicycleConfig.isMotorizedInteraction()) {
			String mode = this.modeFromVehicle.get(event.getVehicleId());
			if (mode == null) {
				return;
			}

			if (this.bicycleConfig.isCarCountOnBicycleLeaveLink()) {
				// 1. carCountOnBicycleLeaveLink: increment link occupancy.
				// inc count by one:
				numberOfVehiclesOnLinkByMode.putIfAbsent(mode, new LinkedHashMap<>());
				Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(mode);
				map.merge(event.getLinkId(), 1., Double::sum);
			}

			if (this.bicycleConfig.isCarsPassedBicycleOnLink()) {
				// 2. carsPassedBicycleOnLink: update current link enter information.
				registerEnter(event.getVehicleId(), event.getLinkId(), event.getTime(), mode);
			}

			if (this.bicycleConfig.isAvgCarOccupancyDuringBicycleTraversal()) {
				// 3. avgCarOccupancyDuringBicycleTraversal: update exposure before changing car occupancy.
				if (TransportMode.car.equals(mode)) {
					updateBikeExposureOnLink(event.getLinkId(), event.getTime());
					incrementVehicleCountOnLink(TransportMode.car, event.getLinkId());
				} else if (bicycleMode.equals(mode)) {
					registerBikeExposureEnter(event.getVehicleId(), event.getLinkId(), event.getTime());
				}
			}
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		String mode = this.modeFromVehicle.get(event.getVehicleId());

		if (this.bicycleConfig.isCarCountOnBicycleLeaveLink() && mode != null) {
			// 1. carCountOnBicycleLeaveLink: decrement link occupancy.
			// dec count by one:
			numberOfVehiclesOnLinkByMode.putIfAbsent(mode, new LinkedHashMap<>());
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(mode);
			Gbl.assertIf(map.merge(event.getLinkId(), -1., Double::sum) >= 0.);
		}

		if (this.bicycleConfig.isAvgCarOccupancyDuringBicycleTraversal() && TransportMode.car.equals(mode)) {
			// 1b. avgCarOccupancyDuringBicycleTraversal: update exposure before changing car occupancy.
			updateBikeExposureOnLink(event.getLinkId(), event.getTime());
			decrementVehicleCountOnLink(TransportMode.car, event.getLinkId());
		}

		if (vehicle2driver.getDriverOfVehicle(event.getVehicleId()) != null) {
			// 2. bicycleAdditionalLinkScore: emit score for static bicycle link attributes.
			double amount = additionalBicycleLinkScore.computeLinkBasedScore(network.getLinks().get(event.getLinkId()),
				event.getVehicleId(), this.bicycleMode);

			// only throw PersonScoreEvent if amount != NaN = mode of vehicle equals bicycleMode.
//			it would be more straight forward to do the mode check here,
//			but we do not have the Vehicles Object in this class, which we need to retrieve the mode
//			of the current vehicle. -sm0925
			if (!Double.isNaN(amount)) {
				final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
				Gbl.assertNotNull(driverOfVehicle);
				this.eventsManager.processEvent(new PersonScoreEvent(event.getTime(), driverOfVehicle, amount, BICYCLE_ADDITIONAL_LINK_SCORE));

				if (this.bicycleConfig.isCarCountOnBicycleLeaveLink()) {
					// 3. bicycleMotorizedInteractionScore: emit carCountOnBicycleLeaveLink.
					// yyyy this is the place where instead a data structure would need to be build that counts interaction with every car
					// that entered the link after the bicycle, and left it before.  kai, jul'23
					var carCounts = this.numberOfVehiclesOnLinkByMode.get(TransportMode.car);
					if (carCounts != null) {
						double interactionAmount =
							MARGINAL_UTILITY_OF_CAR_COUNT_ON_BICYCLE_LEAVE_LINK * carCounts.getOrDefault(event.getLinkId(), 0.);
						if (interactionAmount != 0.) {
							this.eventsManager.processEvent(
								new PersonScoreEvent(event.getTime(), driverOfVehicle, interactionAmount, BICYCLE_MOTORIZED_INTERACTION_SCORE));
						}
					}
				}
			}
		} else {
			log.warn("no driver found for vehicleId=" + event.getVehicleId() + "; not clear why this could happen");
		}

		if (this.bicycleConfig.isCarsPassedBicycleOnLink()) {
			// 4. bicycleMotorizedInteractionScore: evaluate carsPassedBicycleOnLink.
			registerLeave(event.getVehicleId(), event.getLinkId(), event.getTime(), mode);
		}

		if (this.bicycleConfig.isAvgCarOccupancyDuringBicycleTraversal() && bicycleMode.equals(mode)) {
			// 5. bicycleMotorizedInteractionScore: evaluate avgCarOccupancyDuringBicycleTraversal.
			registerBikeExposureLeave(event.getVehicleId(), event.getLinkId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		String mode = this.modeFromVehicle.get(event.getVehicleId());

		if (this.bicycleConfig.isCarCountOnBicycleLeaveLink() && mode != null) {
			// 1. carCountOnBicycleLeaveLink: decrement link occupancy.
			// dec count by one:
			numberOfVehiclesOnLinkByMode.putIfAbsent(mode, new LinkedHashMap<>());
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(mode);
			Gbl.assertIf(map.merge(event.getLinkId(), -1., Double::sum) >= 0.);
		}

		if (this.bicycleConfig.isAvgCarOccupancyDuringBicycleTraversal() && TransportMode.car.equals(mode)) {
			// 1b. avgCarOccupancyDuringBicycleTraversal: update exposure before changing car occupancy.
			updateBikeExposureOnLink(event.getLinkId(), event.getTime());
			decrementVehicleCountOnLink(TransportMode.car, event.getLinkId());
		}

		if (vehicle2driver.getDriverOfVehicle(event.getVehicleId()) != null) {
			if (!Objects.equals(this.firstLinkIdMap.get(event.getVehicleId()), event.getLinkId())) {
				// 2. bicycleAdditionalLinkScore: emit score for the final link if needed.
				// what is this good for?  maybe that bicycles that enter and leave on the same link should not receive the additional score?  kai, jul'23

				// yyyy in the link based scoring, it actually uses event.getReleativePositionOnLink.  Good idea!  kai, jul'23

//				I am pretty sure that here the last link is scored twice. It is already scored for LinkLeaveEvent, so why are we doing it again here? -sm0325
//				because when an agent gets to the final link of a route the sequence is NOT LinkEnter - LinkLeave
//				but LinkEnter - VehicleLeavesTraffic. If we didn't throw the score here, the last link would be omitted. -sm0825

//				yyyy still, the last link is not counted in for e.g. trip distance in output_trips nor trip distance in experienced_plans??

				double amount = additionalBicycleLinkScore.computeLinkBasedScore( network.getLinks().get( event.getLinkId() ),
					event.getVehicleId(), this.bicycleMode);

				// only throw PersonScoreEvent if amount != NaN = mode of vehicle equals bicycleMode.
//				it would be more straight forward to do the mode check here,
//				but we do not have the Vehicles Object in this class, which we need to retrieve the mode
//				of the current vehicle. -sm0925
				if (!Double.isNaN(amount)) {
					final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
					Gbl.assertNotNull(driverOfVehicle);
					this.eventsManager.processEvent(new PersonScoreEvent(event.getTime(), driverOfVehicle, amount, BICYCLE_ADDITIONAL_LINK_SCORE));
				}
			}
		} else {
			log.warn("no driver found for vehicleId=" + event.getVehicleId() + "; not clear why this could happen");
		}

		if (this.bicycleConfig.isCarsPassedBicycleOnLink()) {
			// 3. carsPassedBicycleOnLink: treat VehicleLeavesTraffic like leaving the current link.
			// treat leaves-traffic as leaving the current link
			registerLeave(event.getVehicleId(), event.getLinkId(), event.getTime(), mode);
		}

		if (this.bicycleConfig.isAvgCarOccupancyDuringBicycleTraversal() && bicycleMode.equals(mode)) {
			// 4. avgCarOccupancyDuringBicycleTraversal: treat VehicleLeavesTraffic like leaving the current link.
			registerBikeExposureLeave(event.getVehicleId(), event.getLinkId(), event.getTime());
		}

		// 5. Cleanup.
		// Needs to be called last, because it will remove driver information
		vehicle2driver.handleEvent(event);
		this.firstLinkIdMap.remove(event.getVehicleId());
		this.modeFromVehicle.remove(event.getVehicleId());
		cleanupBikeExposureState(event.getVehicleId());
		this.currentLinkEnterInfoByVehicle.remove(event.getVehicleId());
	}

	private void registerBikeExposureEnter(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		cleanupBikeExposureState(vehicleId);
		this.bikeExposureInfoByVehicle.put(vehicleId, new BikeExposureInfo(linkId, time, time, 0.));
		this.activeBicyclesByLink.computeIfAbsent(linkId, ignored -> new LinkedHashSet<>()).add(vehicleId);
	}

	private void updateBikeExposureOnLink(Id<Link> linkId, double time) {
		Set<Id<Vehicle>> activeBicycles = this.activeBicyclesByLink.get(linkId);
		if (activeBicycles == null || activeBicycles.isEmpty()) {
			return;
		}

		double currentCarCount = getVehicleCountOnLink(TransportMode.car, linkId);
		for (Iterator<Id<Vehicle>> iterator = activeBicycles.iterator(); iterator.hasNext(); ) {
			Id<Vehicle> vehicleId = iterator.next();
			BikeExposureInfo bikeExposureInfo = this.bikeExposureInfoByVehicle.get(vehicleId);
			if (bikeExposureInfo == null || !Objects.equals(bikeExposureInfo.linkId, linkId)) {
				iterator.remove();
				continue;
			}

			double dt = time - bikeExposureInfo.lastUpdateTime;
			if (dt > 0.) {
				bikeExposureInfo.accumulatedCarSeconds += currentCarCount * dt;
				bikeExposureInfo.lastUpdateTime = time;
			}
		}

		if (activeBicycles.isEmpty()) {
			this.activeBicyclesByLink.remove(linkId);
		}
	}

	private void registerBikeExposureLeave(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		BikeExposureInfo bikeExposureInfo = this.bikeExposureInfoByVehicle.get(vehicleId);
		if (bikeExposureInfo == null) {
			return;
		}
		if (!Objects.equals(bikeExposureInfo.linkId, linkId)) {
			cleanupBikeExposureState(vehicleId);
			return;
		}

		double currentCarCount = getVehicleCountOnLink(TransportMode.car, linkId);
		double dt = time - bikeExposureInfo.lastUpdateTime;
		if (dt > 0.) {
			bikeExposureInfo.accumulatedCarSeconds += currentCarCount * dt;
			bikeExposureInfo.lastUpdateTime = time;
		}

		double travelTime = time - bikeExposureInfo.enterTime;
		if (travelTime <= 0.) {
			cleanupBikeExposureState(vehicleId);
			return;
		}

		double avgCarOccupancy = bikeExposureInfo.accumulatedCarSeconds / travelTime;
		if (avgCarOccupancy > 0.) {
			Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(vehicleId);
			if (driverOfVehicle != null) {
				this.eventsManager.processEvent(new PersonScoreEvent(
					time,
					driverOfVehicle,
					MARGINAL_UTILITY_OF_AVG_CAR_OCCUPANCY_DURING_BICYCLE_TRAVERSAL * avgCarOccupancy,
					BICYCLE_MOTORIZED_INTERACTION_SCORE
				));
			}
		}

		cleanupBikeExposureState(vehicleId);
	}

	private void cleanupBikeExposureState(Id<Vehicle> vehicleId) {
		BikeExposureInfo bikeExposureInfo = this.bikeExposureInfoByVehicle.remove(vehicleId);
		if (bikeExposureInfo == null) {
			return;
		}

		Set<Id<Vehicle>> activeBicycles = this.activeBicyclesByLink.get(bikeExposureInfo.linkId);
		if (activeBicycles == null) {
			return;
		}

		activeBicycles.remove(vehicleId);
		if (activeBicycles.isEmpty()) {
			this.activeBicyclesByLink.remove(bikeExposureInfo.linkId);
		}
	}

	private void incrementVehicleCountOnLink(String mode, Id<Link> linkId) {
		this.numberOfVehiclesOnLinkByMode.computeIfAbsent(mode, ignored -> new LinkedHashMap<>())
			.merge(linkId, 1., Double::sum);
	}

	private void decrementVehicleCountOnLink(String mode, Id<Link> linkId) {
		Map<Id<Link>, Double> vehicleCounts = this.numberOfVehiclesOnLinkByMode.get(mode);
		if (vehicleCounts == null) {
			return;
		}

		double currentCount = vehicleCounts.getOrDefault(linkId, 0.);
		if (currentCount <= 1.) {
			vehicleCounts.remove(linkId);
		} else {
			vehicleCounts.put(linkId, currentCount - 1.);
		}

		if (vehicleCounts.isEmpty()) {
			this.numberOfVehiclesOnLinkByMode.remove(mode);
		}
	}

	private double getVehicleCountOnLink(String mode, Id<Link> linkId) {
		Map<Id<Link>, Double> vehicleCounts = this.numberOfVehiclesOnLinkByMode.get(mode);
		if (vehicleCounts == null) {
			return 0.;
		}
		return vehicleCounts.getOrDefault(linkId, 0.);
	}

	// -----------------------------------------------------------------------------
	// Support for bicycleMotorizedInteractionScore: avgCarOccupancyDuringBicycleTraversal
	// -----------------------------------------------------------------------------

	// TODO test avgCarOccupancyDuringBicycleTraversal:
	// - no cars on link
	// - constant one-car occupancy
	// - occupancy change during bike traversal
	// - bike travel time zero
	// - multiple bikes on same link
	// - car enters/leaves at same second as bike events
	// - VehicleLeavesTraffic on final link

	private static final class BikeExposureInfo {
		final Id<Link> linkId;
		final double enterTime;
		double lastUpdateTime;
		double accumulatedCarSeconds;

		private BikeExposureInfo(Id<Link> linkId, double enterTime, double lastUpdateTime, double accumulatedCarSeconds) {
			this.linkId = linkId;
			this.enterTime = enterTime;
			this.lastUpdateTime = lastUpdateTime;
			this.accumulatedCarSeconds = accumulatedCarSeconds;
		}
	}

	// -----------------------------------------------------------------------------
	// Support for bicycleMotorizedInteractionScore: carsPassedBicycleOnLink
	// -----------------------------------------------------------------------------

	private void registerEnter(Id<Vehicle> vehicleId, Id<Link> linkId, double time, String mode) {
		if (mode == null) {
			return;
		}
		this.currentLinkEnterInfoByVehicle.put(vehicleId, new LinkEnterInfo(linkId, time, mode));
	}

	private void registerLeave(Id<Vehicle> vehicleId, Id<Link> linkId, double time, String mode) {
		if (mode == null) {
			return;
		}

		LinkEnterInfo enterInfo = this.currentLinkEnterInfoByVehicle.get(vehicleId);
		if (enterInfo == null) {
			return;
		}
		if (!Objects.equals(enterInfo.linkId, linkId)) {
			return;
		}

		final double enterTime = enterInfo.enterTime;

		FinishedCarStore store = TransportMode.car.equals(mode)
			? this.finishedCarsByLink.computeIfAbsent(linkId, ignored -> new FinishedCarStore())
			: this.finishedCarsByLink.get(linkId);

		if (store == null) {
			this.currentLinkEnterInfoByVehicle.remove(vehicleId);
			return;
		}

		// This implements carsPassedBicycleOnLink.
		// True overtaking definition on a link L:
		// enter_car > enter_bike  AND  leave_car < leave_bike
		// TODO: How are events handled if they enter at the same time?
		store.pruneCarsThatLeftBefore(time - computeOvertakeHorizonSeconds(linkId));

		if (TransportMode.car.equals(mode)) {
			store.addFinishedCarTraversal(enterTime, time);
		}

		if (bicycleMode.equals(mode)) {
			store.pruneCarsThatLeftBefore(enterTime);
			int carsPassedBicycleOnLink = store.countCarsWithEnterAfter(enterTime);
			if (carsPassedBicycleOnLink > 0) {
				final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(vehicleId);
				if (driverOfVehicle != null) {
					this.eventsManager.processEvent(new PersonScoreEvent(
						time,
						driverOfVehicle,
						MARGINAL_UTILITY_OF_CARS_PASSED_BICYCLE_ON_LINK * carsPassedBicycleOnLink,
						BICYCLE_MOTORIZED_INTERACTION_SCORE
					));
				}
			}
		}

		if (store.size() == 0) {
			this.finishedCarsByLink.remove(linkId);
		}

		this.currentLinkEnterInfoByVehicle.remove(vehicleId);
	}

	private double computeOvertakeHorizonSeconds(Id<Link> linkId) {
		// Conservative pruning horizon for finished car traversals on a link.
		Link link = network.getLinks().get(linkId);
		if (link == null) {
			return 300.0;
		}
		double free = link.getLength() / Math.max(0.1, link.getFreespeed());
		//return Math.min(600.0, Math.max(60.0, 5.0 * free));  // this should be better, but wont work if links are super long like equil
		return Math.max(600.0, 20.0 * free); // TODO: needs to be adjusted, but the equil we need less prune
	}

	// -----------------------------------------------------------------------------
	// Data structures for carsPassedBicycleOnLink
	// -----------------------------------------------------------------------------

	private static final class LinkEnterInfo {
		final Id<Link> linkId;
		final double enterTime;
		final String mode;

		private LinkEnterInfo(Id<Link> linkId, double enterTime, String mode) {
			this.linkId = linkId;
			this.enterTime = enterTime;
			this.mode = mode;
		}
	}

	private static final class CarTraversal {
		final double leaveTime;
		final double enterTime;

		private CarTraversal(double leaveTime, double enterTime) {
			this.leaveTime = leaveTime;
			this.enterTime = enterTime;
		}
	}

	/**
	 * Stores car traversals that already finished the link.
	 * Used to count cars that entered after the bicycle entered and left before the bicycle left.
	 */
	private static final class FinishedCarStore {
		private final TreeMap<Double, Integer> enterCounts = new TreeMap<>();
		private final ArrayDeque<CarTraversal> finishedCarsByLeaveTime = new ArrayDeque<>();
		private int totalCount = 0;

		void addFinishedCarTraversal(double carEnter, double carLeave) {
			this.enterCounts.merge(carEnter, 1, Integer::sum);
			this.totalCount++;
			this.finishedCarsByLeaveTime.addLast(new CarTraversal(carLeave, carEnter));
		}

		void pruneCarsThatLeftBefore(double thresholdTime) {
			while (!this.finishedCarsByLeaveTime.isEmpty()
				&& this.finishedCarsByLeaveTime.peekFirst().leaveTime < thresholdTime) {
				CarTraversal old = this.finishedCarsByLeaveTime.removeFirst();
				Integer count = this.enterCounts.get(old.enterTime);
				if (count != null) {
					if (count <= 1) {
						this.enterCounts.remove(old.enterTime);
					} else {
						this.enterCounts.put(old.enterTime, count - 1);
					}
					this.totalCount--;
				}
			}
		}

		int countCarsWithEnterAfter(double bikeEnterTime) {
			int lessOrEqual = 0;
			for (int count : this.enterCounts.headMap(bikeEnterTime, true).values()) {
				lessOrEqual += count;
			}
			return this.totalCount - lessOrEqual;
		}

		int size() {
			return this.totalCount;
		}
	}
}
