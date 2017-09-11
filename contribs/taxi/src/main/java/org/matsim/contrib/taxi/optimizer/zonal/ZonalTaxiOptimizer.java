/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.taxi.data.TaxiRequest;
import org.matsim.contrib.taxi.optimizer.BestDispatchFinder;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.Zones;
import org.matsim.contrib.zone.util.NetworkWithZonesUtils;
import org.matsim.contrib.zone.util.ZoneFinderImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class ZonalTaxiOptimizer extends RuleBasedTaxiOptimizer {
	private static final Comparator<Vehicle> LONGEST_WAITING_FIRST = new Comparator<Vehicle>() {
		public int compare(Vehicle v1, Vehicle v2) {
			double beginTime1 = v1.getSchedule().getCurrentTask().getBeginTime();
			double beginTime2 = v2.getSchedule().getCurrentTask().getBeginTime();
			return Double.compare(beginTime1, beginTime2);
		}
	};

	private final Map<Id<Zone>, Zone> zones;
	private Map<Id<Zone>, PriorityQueue<Vehicle>> zoneToIdleVehicleQueue;
	private final Map<Id<Link>, Zone> linkToZone;

	public ZonalTaxiOptimizer(TaxiConfigGroup taxiCfg, Fleet fleet, Network network,
			MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility, TaxiScheduler scheduler,
			ZonalTaxiOptimizerParams params) {
		super(taxiCfg, fleet, network, timer, travelTime, travelDisutility, scheduler, params);

		zones = Zones.readZones(params.zonesXmlFile, params.zonesShpFile);
		System.err.println("No conversion of SRS is done");

		this.linkToZone = NetworkWithZonesUtils.createLinkToZoneMap(network,
				new ZoneFinderImpl(zones, params.expansionDistance));

		// FIXME zonal system used in RuleBasedTaxiOptim (for registers) should be equivalent to
		// the zones used in ZonalTaxiOptim (for dispatching)
	}

	@Override
	protected void scheduleUnplannedRequests() {
		initIdleVehiclesInZones();
		scheduleUnplannedRequestsWithinZones();

		if (!getUnplannedRequests().isEmpty()) {
			super.scheduleUnplannedRequests();
		}
	}

	private void initIdleVehiclesInZones() {
		// TODO use idle vehicle register instead...

		zoneToIdleVehicleQueue = new HashMap<>();
		for (Id<Zone> zoneId : zones.keySet()) {
			zoneToIdleVehicleQueue.put(zoneId, new PriorityQueue<Vehicle>(10, LONGEST_WAITING_FIRST));
		}

		for (Vehicle veh : getFleet().getVehicles().values()) {
			if (getScheduler().isIdle(veh)) {
				Link link = ((StayTask)veh.getSchedule().getCurrentTask()).getLink();
				Zone zone = linkToZone.get(link.getId());
				if (zone != null) {
					PriorityQueue<Vehicle> queue = zoneToIdleVehicleQueue.get(zone.getId());
					queue.add(veh);
				}
			}
		}
	}

	private void scheduleUnplannedRequestsWithinZones() {
		Iterator<TaxiRequest> reqIter = getUnplannedRequests().iterator();
		while (reqIter.hasNext()) {
			TaxiRequest req = reqIter.next();

			Zone zone = linkToZone.get(req.getFromLink().getId());
			if (zone == null) {
				continue;
			}

			PriorityQueue<Vehicle> idleVehsInZone = zoneToIdleVehicleQueue.get(zone.getId());
			if (idleVehsInZone.isEmpty()) {
				continue;
			}

			Iterable<Vehicle> filteredVehs = Collections.singleton(idleVehsInZone.peek());
			BestDispatchFinder.Dispatch<TaxiRequest> best = getDispatchFinder().findBestVehicleForRequest(req,
					filteredVehs);

			if (best != null) {
				getScheduler().scheduleRequest(best.vehicle, best.destination, best.path);
				reqIter.remove();
				idleVehsInZone.remove(best.vehicle);
			}
		}
	}
}
