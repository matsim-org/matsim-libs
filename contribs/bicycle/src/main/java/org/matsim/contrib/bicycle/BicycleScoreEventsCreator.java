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
 * Adds link-based bicycle score events and (optionally) "true overtaking" interaction score events.
 *
 * True overtaking definition on a link L:
 *   enter_car > enter_bike  AND  leave_car < leave_bike
 */
class BicycleScoreEventsCreator implements
	VehicleEntersTrafficEventHandler,
	LinkEnterEventHandler,
	LinkLeaveEventHandler,
	VehicleLeavesTrafficEventHandler
{
	private static final Logger log = LogManager.getLogger(BicycleScoreEventsCreator.class);

	private final Network network;
	private final EventsManager eventsManager;
	private final AdditionalBicycleLinkScore additionalBicycleLinkScore;
	private final String bicycleMode;
	private final BicycleConfigGroup bicycleConfig;

	private final Vehicle2DriverEventHandler vehicle2driver = new Vehicle2DriverEventHandler();

	// existing state
	private final Map<Id<Vehicle>, Id<Link>> firstLinkIdMap = new LinkedHashMap<>();
	private final Map<Id<Vehicle>, String> modeFromVehicle = new LinkedHashMap<>();

	// === "primitive" car-count interaction (kept as-is) ===
	private final Map<String, Map<Id<Link>, Double>> numberOfVehiclesOnLinkByMode = new LinkedHashMap<>();

	// === TRUE OVERTAKING bookkeeping ===

	/** per vehicle -> (link -> enterTime) for currently occupied link. We only need current link, but store by link for safety. */
	private final Map<Id<Vehicle>, LinkEnterInfo> currentLinkEnter = new LinkedHashMap<>();

	/** per link: finished car traversals for counting overtakes */
	private final Map<Id<Link>, FinishedCarStore> finishedCarsByLink = new LinkedHashMap<>();

	/**
	 * Score per overtake (negative). Choose something small; tune later.
	 * Units are "utils" like other scoring components.
	 */
	private final double marginalUtilityPerOvertake = -0.004; // you can tune

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

		currentLinkEnter.clear();
		finishedCarsByLink.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		vehicle2driver.handleEvent(event);
		this.firstLinkIdMap.put(event.getVehicleId(), event.getLinkId());

		if (this.bicycleConfig.isMotorizedInteraction()) {
			modeFromVehicle.put(event.getVehicleId(), event.getNetworkMode());

			// primitive counts:
			numberOfVehiclesOnLinkByMode.putIfAbsent(event.getNetworkMode(), new LinkedHashMap<>());
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(event.getNetworkMode());
			map.merge(event.getLinkId(), 1., Double::sum);
		}

		// Treat "enters traffic" as being on the first link from this time.
		// This is important, otherwise the very first link would miss enterTime.
		registerEnter(event.getVehicleId(), event.getLinkId(), event.getTime(), event.getNetworkMode());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (this.bicycleConfig.isMotorizedInteraction()) {
			// primitive counts:
			String mode = this.modeFromVehicle.get(event.getVehicleId());
			numberOfVehiclesOnLinkByMode.putIfAbsent(mode, new LinkedHashMap<>());
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(mode);
			map.merge(event.getLinkId(), 1., Double::sum);

			registerEnter(event.getVehicleId(), event.getLinkId(), event.getTime(), mode);
		} else {
			// even if motorizedInteraction is off, you might still want overtakes
			// -> but in your current setup overtakes are conceptually "motorized interaction".
			// If you want overtakes ALWAYS, remove this if-else and always call registerEnter using modeFromVehicle.
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.bicycleConfig.isMotorizedInteraction()) {
			// primitive counts:
			String mode = this.modeFromVehicle.get(event.getVehicleId());
			numberOfVehiclesOnLinkByMode.putIfAbsent(mode, new LinkedHashMap<>());
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(mode);
			Gbl.assertIf(map.merge(event.getLinkId(), -1., Double::sum) >= 0);

			registerLeave(event.getVehicleId(), event.getLinkId(), event.getTime(), mode);
		}

		// existing link-based bicycle additional score (unchanged)
		if (vehicle2driver.getDriverOfVehicle(event.getVehicleId()) != null) {
			double amount = additionalBicycleLinkScore.computeLinkBasedScore(
				network.getLinks().get(event.getLinkId()),
				event.getVehicleId(),
				this.bicycleMode
			);

			// only throw PersonScoreEvent if amount != NaN = mode of vehicle equals bicycleMode.
//			it would be more straight forward to do the mode check here,
//			but we do not have the Vehicles Object in this class, which we need to retrieve the mode
//			of the current vehicle. -sm0925
			if (!Double.isNaN(amount)) {
				if (this.bicycleConfig.isMotorizedInteraction()) {
					// KEEP old behaviour (optional):
					var carCounts = this.numberOfVehiclesOnLinkByMode.get(TransportMode.car);
					if (carCounts != null) {
						amount -= 0.004 * carCounts.getOrDefault(event.getLinkId(), 0.);
					}
				}

				final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
				Gbl.assertNotNull(driverOfVehicle);
				this.eventsManager.processEvent(
					new PersonScoreEvent(event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore")
				);
			}
		} else {
			log.warn("no driver found for vehicleId=" + event.getVehicleId() + "; not clear why this could happen");
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		if (this.bicycleConfig.isMotorizedInteraction()) {
			// primitive counts:
			String mode = this.modeFromVehicle.get(event.getVehicleId());
			numberOfVehiclesOnLinkByMode.putIfAbsent(mode, new LinkedHashMap<>());
			Map<Id<Link>, Double> map = numberOfVehiclesOnLinkByMode.get(mode);
			Gbl.assertIf(map.merge(event.getLinkId(), -1., Double::sum) >= 0.);

			// treat leaves-traffic as leaving the current link
			registerLeave(event.getVehicleId(), event.getLinkId(), event.getTime(), mode);
		}

		// existing end-of-leg link-based bicycle additional score (unchanged)
		if (vehicle2driver.getDriverOfVehicle(event.getVehicleId()) != null) {
			if (!Objects.equals(this.firstLinkIdMap.get(event.getVehicleId()), event.getLinkId())) {
				double amount = additionalBicycleLinkScore.computeLinkBasedScore(
					network.getLinks().get(event.getLinkId()),
					event.getVehicleId(),
					this.bicycleMode
				);

				// only throw PersonScoreEvent if amount != NaN = mode of vehicle equals bicycleMode.
//				it would be more straight forward to do the mode check here,
//				but we do not have the Vehicles Object in this class, which we need to retrieve the mode
//				of the current vehicle. -sm0925
				if (!Double.isNaN(amount)) {
					final Id<Person> driverOfVehicle = vehicle2driver.getDriverOfVehicle(event.getVehicleId());
					Gbl.assertNotNull(driverOfVehicle);
					this.eventsManager.processEvent(
						new PersonScoreEvent(event.getTime(), driverOfVehicle, amount, "bicycleAdditionalLinkScore")
					);
				}
			}
		} else {
			log.warn("no driver found for vehicleId=" + event.getVehicleId() + "; not clear why this could happen");
		}

		// Needs to be called last, because it will remove driver information
		vehicle2driver.handleEvent(event);

		// cleanup mode mapping to avoid growth
		modeFromVehicle.remove(event.getVehicleId());
		currentLinkEnter.remove(event.getVehicleId());
	}

	// ======================================================================
	// TRUE OVERTAKE implementation
	// ======================================================================

	private void registerEnter(Id<Vehicle> vehicleId, Id<Link> linkId, double time, String mode) {
		if (mode == null) return;

		// store enter time for current link
		currentLinkEnter.put(vehicleId, new LinkEnterInfo(linkId, time, mode));
	}

	private void registerLeave(Id<Vehicle> vehicleId, Id<Link> linkId, double time, String mode) {
		if (mode == null) return;

		LinkEnterInfo enterInfo = currentLinkEnter.get(vehicleId);
		if (enterInfo == null) {
			// can happen if something is inconsistent; skip
			return;
		}
		if (!Objects.equals(enterInfo.linkId, linkId)) {
			// out-of-sync; skip to be safe
			return;
		}

		final double enterTime = enterInfo.enterTime;

		// Only cars contribute to "finished cars" store
		if (TransportMode.car.equals(mode)) {
			FinishedCarStore store = finishedCarsByLink.computeIfAbsent(linkId, k -> new FinishedCarStore());
			store.addFinishedCarTraversal(enterTime, time);
		}

		// Only bikes are "victims" that get overtaking counted
		if (bicycleMode.equals(mode)) {
			FinishedCarStore store = finishedCarsByLink.get(linkId);
			if (store != null) {
				// prune cars that left before bike entered (cannot overtake this bike)
				store.pruneCarsThatLeftBefore(enterTime);

				// count cars that entered after bike entered
				int overtakes = store.countCarsWithEnterAfter(enterTime);

				if (overtakes > 0) {
					// score
					final Id<Person> driver = vehicle2driver.getDriverOfVehicle(vehicleId);
					if (driver != null) {
						double amount = marginalUtilityPerOvertake * overtakes;
						eventsManager.processEvent(new PersonScoreEvent(time, driver, amount, "bicycleOvertakeScore"));
					}
				}
			}
		}

		// remove enter info for this vehicle (it will be overwritten on next enter anyway)
		currentLinkEnter.remove(vehicleId);
	}

	private static final class LinkEnterInfo {
		final Id<Link> linkId;
		final double enterTime;
		final String mode;

		LinkEnterInfo(Id<Link> linkId, double enterTime, String mode) {
			this.linkId = linkId;
			this.enterTime = enterTime;
			this.mode = mode;
		}
	}

	private static final class CarTraversal {
		final double leaveTime;
		final double enterTime;

		CarTraversal(double leaveTime, double enterTime) {
			this.leaveTime = leaveTime;
			this.enterTime = enterTime;
		}
	}

	/**
	 * Stores car traversals that already finished the link.
	 * Used to count "cars that entered after bike entered and left before bike left".
	 */
	private static final class FinishedCarStore {
		// multiset keyed by enterTime
		private final NavigableMap<Double, Integer> finishedCarsByEnterTime = new TreeMap<>();
		// queue by leaveTime for pruning
		private final ArrayDeque<CarTraversal> finishedCarsByLeaveTime = new ArrayDeque<>();

		void addFinishedCarTraversal(double carEnter, double carLeave) {
			finishedCarsByEnterTime.merge(carEnter, 1, Integer::sum);
			finishedCarsByLeaveTime.addLast(new CarTraversal(carLeave, carEnter));
		}

		void pruneCarsThatLeftBefore(double bikeEnterTime) {
			// remove all cars with leaveTime < bikeEnterTime (too early)
			while (!finishedCarsByLeaveTime.isEmpty() && finishedCarsByLeaveTime.peekFirst().leaveTime < bikeEnterTime) {
				CarTraversal old = finishedCarsByLeaveTime.removeFirst();
				decrementEnterTime(old.enterTime);
			}
		}

		int countCarsWithEnterAfter(double bikeEnterTime) {
			int sum = 0;
			for (Map.Entry<Double, Integer> e : finishedCarsByEnterTime.tailMap(bikeEnterTime, false).entrySet()) {
				sum += e.getValue();
			}
			return sum;
		}

		private void decrementEnterTime(double enterTime) {
			Integer cnt = finishedCarsByEnterTime.get(enterTime);
			if (cnt == null) return;
			if (cnt <= 1) finishedCarsByEnterTime.remove(enterTime);
			else finishedCarsByEnterTime.put(enterTime, cnt - 1);
		}
	}
}
