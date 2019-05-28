/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.zonal;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.UnplannedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.IdleTaxiZonalRegistry;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedRequestInserter;
import org.matsim.contrib.taxi.optimizer.rules.UnplannedRequestZonalRegistry;
import org.matsim.contrib.taxi.passenger.TaxiRequest;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.Zones;
import org.matsim.contrib.zone.util.NetworkWithZonesUtils;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author michalm
 */
public class ZonalRequestInserter implements UnplannedRequestInserter {
	private static final Comparator<DvrpVehicle> LONGEST_WAITING_FIRST = Comparator.comparingDouble(
			v -> v.getSchedule().getCurrentTask().getBeginTime());

	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final BestDispatchFinder dispatchFinder;
	private final RuleBasedRequestInserter requestInserter;

	private final Map<Id<Zone>, Zone> zones;
	private Map<Id<Zone>, PriorityQueue<DvrpVehicle>> zoneToIdleVehicleQueue;
	private final Map<Id<Link>, Zone> linkToZone;

	public ZonalRequestInserter(Fleet fleet, TaxiScheduler scheduler, MobsimTimer timer, Network network,
			TravelTime travelTime, TravelDisutility travelDisutility, ZonalTaxiOptimizerParams params,
			IdleTaxiZonalRegistry idleTaxiRegistry, UnplannedRequestZonalRegistry unplannedRequestRegistry) {
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.dispatchFinder = new BestDispatchFinder(scheduler, network, timer, travelTime, travelDisutility);
		this.requestInserter = new RuleBasedRequestInserter(scheduler, timer, dispatchFinder, params, idleTaxiRegistry,
				unplannedRequestRegistry);

		zones = Zones.readZones(params.zonesXmlFile, params.zonesShpFile);
		System.err.println("No conversion of SRS is done");

		this.linkToZone = NetworkWithZonesUtils.createLinkToZoneMap(network,
				new ZoneFinderImpl(zones, params.expansionDistance));

		// FIXME zonal system used in RuleBasedTaxiOptim (for registers) should be equivalent to
		// the zones used in ZonalTaxiOptim (for dispatching)
	}

	@Override
	public void scheduleUnplannedRequests(Collection<TaxiRequest> unplannedRequests) {
		initIdleVehiclesInZones();
		scheduleUnplannedRequestsWithinZones(unplannedRequests);

		if (!unplannedRequests.isEmpty()) {
			requestInserter.scheduleUnplannedRequests(unplannedRequests);
		}
	}

	private void initIdleVehiclesInZones() {
		// TODO use idle vehicle register instead...

		zoneToIdleVehicleQueue = new HashMap<>();
		for (Id<Zone> zoneId : zones.keySet()) {
			zoneToIdleVehicleQueue.put(zoneId, new PriorityQueue<DvrpVehicle>(10, LONGEST_WAITING_FIRST));
		}

		for (DvrpVehicle veh : fleet.getVehicles().values()) {
			if (scheduler.isIdle(veh)) {
				Link link = ((StayTask)veh.getSchedule().getCurrentTask()).getLink();
				Zone zone = linkToZone.get(link.getId());
				if (zone != null) {
					PriorityQueue<DvrpVehicle> queue = zoneToIdleVehicleQueue.get(zone.getId());
					queue.add(veh);
				}
			}
		}
	}

	private void scheduleUnplannedRequestsWithinZones(Collection<TaxiRequest> unplannedRequests) {
		Iterator<TaxiRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			TaxiRequest req = reqIter.next();

			Zone zone = linkToZone.get(req.getFromLink().getId());
			if (zone == null) {
				continue;
			}

			PriorityQueue<DvrpVehicle> idleVehsInZone = zoneToIdleVehicleQueue.get(zone.getId());
			if (idleVehsInZone.isEmpty()) {
				continue;
			}

			Stream<DvrpVehicle> filteredVehs = Stream.of(idleVehsInZone.peek());
			BestDispatchFinder.Dispatch<TaxiRequest> best = dispatchFinder.findBestVehicleForRequest(req, filteredVehs);

			if (best != null) {
				scheduler.scheduleRequest(best.vehicle, best.destination, best.path);
				reqIter.remove();
				idleVehsInZone.remove(best.vehicle);
			}
		}
	}
}
