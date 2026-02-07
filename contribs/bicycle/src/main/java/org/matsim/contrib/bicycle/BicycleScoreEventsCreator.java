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
 * <p>
 * True overtaking definition on a link L:
 * enter_car > enter_bike  AND  leave_car < leave_bike
 */
class BicycleScoreEventsCreator implements
	VehicleEntersTrafficEventHandler,
	LinkEnterEventHandler,
	LinkLeaveEventHandler,
	VehicleLeavesTrafficEventHandler {
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

	/**
	 * per vehicle -> (link -> enterTime) for currently occupied link. We only need current link, but store by link for safety.
	 */
	private final Map<Id<Vehicle>, LinkEnterInfo> currentLinkEnter = new LinkedHashMap<>();

	/**
	 * per link: finished car traversals for counting overtakes
	 */
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
			// -> but in the current setup overtakes are conceptually "motorized interaction".
			// If overtakes ALWAYS wanted, remove this if-else and always call registerEnter using modeFromVehicle.
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

		FinishedCarStore store = TransportMode.car.equals(mode)
			? finishedCarsByLink.computeIfAbsent(linkId, k -> new FinishedCarStore())
			: finishedCarsByLink.get(linkId);

		if (store == null) {
			currentLinkEnter.remove(vehicleId);
			return;
		}

		// HARD PRUNE (choose conservative horizon)
		store.pruneCarsThatLeftBefore(time - computeOvertakeHorizonSeconds(linkId));

		if (TransportMode.car.equals(mode)) {
			store.addFinishedCarTraversal(enterTime, time);
		}

		if (bicycleMode.equals(mode)) {
			store.pruneCarsThatLeftBefore(enterTime);

			int overtakes = store.countCarsWithEnterAfter(enterTime);
			if (overtakes > 0) {
				final Id<Person> driver = vehicle2driver.getDriverOfVehicle(vehicleId);
				if (driver != null) {
					eventsManager.processEvent(
						new PersonScoreEvent(time, driver, marginalUtilityPerOvertake * overtakes, "bicycleOvertakeScore"));
				}
			}
		}

		if (store.size() == 0) {
			finishedCarsByLink.remove(linkId);
		}

		currentLinkEnter.remove(vehicleId);


	}

	private double computeOvertakeHorizonSeconds(Id<Link> linkId) {
		Link l = network.getLinks().get(linkId);
		if (l == null) return 300.0;
		double free = l.getLength() / Math.max(0.1, l.getFreespeed());
		//return Math.min(600.0, Math.max(60.0, 5.0 * free));
		return Math.max(600.0, 20.0 * free); // TODO: needs to be adjusted, but the equil we need less prune
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
		private final TreapMultiset enters = new TreapMultiset();
		private final ArrayDeque<CarTraversal> finishedCarsByLeaveTime = new ArrayDeque<>();

		void addFinishedCarTraversal(double carEnter, double carLeave) {
			enters.add(carEnter);
			finishedCarsByLeaveTime.addLast(new CarTraversal(carLeave, carEnter));
		}

		void pruneCarsThatLeftBefore(double thresholdTime) {
			while (!finishedCarsByLeaveTime.isEmpty()
				&& finishedCarsByLeaveTime.peekFirst().leaveTime < thresholdTime) {
				CarTraversal old = finishedCarsByLeaveTime.removeFirst();
				enters.remove(old.enterTime);
			}
		}

		int countCarsWithEnterAfter(double bikeEnterTime) {
			return enters.countGreaterThan(bikeEnterTime);
		}

		int size() {
			return enters.size();
		}
	}

	private static final class TreapMultiset {
		private static final class Node {
			final double key;
			final int prio;
			int cnt;
			int size;
			Node left, right;

			Node(double key, int prio) {
				this.key = key;
				this.prio = prio;
				this.cnt = 1;
				this.size = 1;
			}
		}

		private final Random rnd = new Random(1);
		private Node root;

		int size() {
			return size(root);
		}

		void add(double key) {
			root = insert(root, key);
		}

		void remove(double key) {
			root = erase(root, key);
		}

		int countGreaterThan(double key) {
			return size(root) - countLessEqual(root, key);
		}

		private static int size(Node n) {
			return n == null ? 0 : n.size;
		}

		private static void pull(Node n) {
			if (n != null) n.size = n.cnt + size(n.left) + size(n.right);
		}

		private static Node rotateRight(Node y) {
			Node x = y.left;
			y.left = x.right;
			x.right = y;
			pull(y);
			pull(x);
			return x;
		}

		private static Node rotateLeft(Node x) {
			Node y = x.right;
			x.right = y.left;
			y.left = x;
			pull(x);
			pull(y);
			return y;
		}

		private Node insert(Node n, double key) {
			if (n == null) return new Node(key, rnd.nextInt());
			if (key == n.key) {
				n.cnt++;
			} else if (key < n.key) {
				n.left = insert(n.left, key);
				if (n.left.prio > n.prio) n = rotateRight(n);
			} else {
				n.right = insert(n.right, key);
				if (n.right.prio > n.prio) n = rotateLeft(n);
			}
			pull(n);
			return n;
		}

		private Node erase(Node n, double key) {
			if (n == null) return null;

			if (key == n.key) {
				if (n.cnt > 1) {
					n.cnt--;
				} else {
					if (n.left == null) return n.right;
					if (n.right == null) return n.left;

					if (n.left.prio > n.right.prio) {
						n = rotateRight(n);
						n.right = erase(n.right, key);
					} else {
						n = rotateLeft(n);
						n.left = erase(n.left, key);
					}
				}
			} else if (key < n.key) {
				n.left = erase(n.left, key);
			} else {
				n.right = erase(n.right, key);
			}

			pull(n);
			return n;
		}

		private int countLessEqual(Node n, double key) {
			if (n == null) return 0;
			if (key < n.key) {
				return countLessEqual(n.left, key);
			} else {
				return size(n.left) + n.cnt + countLessEqual(n.right, key);
			}
		}
	}

}
