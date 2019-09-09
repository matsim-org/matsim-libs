/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.vsp.avparking.optimizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelDataImpl;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.search.ParkingSearchLogic;
import org.matsim.contrib.parking.parkingsearch.search.RandomParkingSearchLogic;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizer;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.schedule.TaxiTask;
import org.matsim.contrib.taxi.schedule.TaxiTask.TaxiTaskType;
import org.matsim.contrib.taxi.scheduler.TaxiScheduler;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import playground.vsp.avparking.AvParkingContext;

public class PrivateAVTaxiDispatcher extends DefaultTaxiOptimizer {
	public static PrivateAVTaxiDispatcher create(EventsManager eventsManager, TaxiConfigGroup taxiCfg, Fleet fleet,
			Network network, MobsimTimer timer, TravelTime travelTime, TravelDisutility travelDisutility,
			TaxiScheduler scheduler, ParkingSearchManager parkingManger,
			AvParkingContext context) {
		LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, travelDisutility,
				travelTime);
		PrivateAVRequestInserter requestInserter = new PrivateAVRequestInserter(fleet, scheduler, timer, travelTime,
				parkingManger, router);
		return new PrivateAVTaxiDispatcher(taxiCfg, fleet, network, timer, travelTime, scheduler, parkingManger,
				context, router, requestInserter, eventsManager);
	}

	public enum AVParkBehavior {
		findfreeSlot, garage, cruise, randombehavior
	}

	private final String mode;
	private final EventsManager eventsManager;
	private final Fleet fleet;
	private final TaxiScheduler scheduler;
	private final Network network;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final LeastCostPathCalculator router;

	private AVParkBehavior parkBehavior;
	private Random random = MatsimRandom.getRandom();
	private final ParkingSearchManager manager;
	private final ParkingSearchLogic parkingLogic;
	private final List<Link> avParkings;

	public PrivateAVTaxiDispatcher(TaxiConfigGroup taxiCfg, Fleet fleet, Network network, MobsimTimer timer,
			TravelTime travelTime, TaxiScheduler scheduler,
			ParkingSearchManager parkingManger, AvParkingContext context, LeastCostPathCalculator router,
			PrivateAVRequestInserter requestInserter, EventsManager eventsManager) {
		super(eventsManager, taxiCfg, fleet, scheduler, requestInserter);
		this.fleet = fleet;
		this.scheduler = scheduler;
		this.network = network;
		this.timer = timer;
		this.travelTime = travelTime;
		this.router = router;

		parkBehavior = context.getBehavior();
		manager = parkingManger;
		parkingLogic = new RandomParkingSearchLogic(network);
		this.eventsManager = eventsManager;
		this.avParkings = NetworkUtils.getLinks(network, context.getAvParkings());

		this.mode = taxiCfg.getMode();
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if (isNewDecisionEpoch(e, 60)) {
			for (DvrpVehicle veh : fleet.getVehicles().values()) {
				if (veh.getSchedule().getStatus().equals(ScheduleStatus.STARTED)) {

					if (isWaitStay((TaxiTask)veh.getSchedule().getCurrentTask())
							&& manager.getVehicleParkingLocation(Id.createVehicleId(veh.getId())) == null) {

						// check if current location allows for parking and park here
						Link lastLink = Schedules.getLastLinkInSchedule(veh);
						if (manager.reserveSpaceIfVehicleCanParkHere(Id.createVehicleId(veh.getId()),
								lastLink.getId())) {
							manager.parkVehicleHere(Id.createVehicleId(Id.createVehicleId(veh.getId())),
									lastLink.getId(), e.getSimulationTime());
						} else {

							AVParkBehavior vehParkBehavior = parkBehavior;
							if (vehParkBehavior == AVParkBehavior.randombehavior) {
								int i = random.nextInt(3);
								if (i == 0)
									vehParkBehavior = AVParkBehavior.cruise;
								else if (i == 1)
									vehParkBehavior = AVParkBehavior.findfreeSlot;
								else if (i == 2)
									vehParkBehavior = AVParkBehavior.garage;
							}
							switch (vehParkBehavior) {
								case cruise:
									// Logger.getLogger(getClass()).info(veh.getId()+" --> cruise");
									sendVehicleToCruise(veh);
									break;
								case findfreeSlot:
									// Logger.getLogger(getClass()).info(veh.getId()+" --> parking");
									findFreeSlotAndParkVehicle(veh);
									break;
								case garage:
									// Logger.getLogger(getClass()).info(veh.getId()+" --> garage");
									sendVehicleToGarage(veh);
									break;
								case randombehavior:
									throw new IllegalStateException();
							}

						}
					}

				}
			}
		}

		super.notifyMobsimBeforeSimStep(e);
	}

	/**
	 * @param veh
	 */
	private void findFreeSlotAndParkVehicle(DvrpVehicle veh) {
		if (isWaitStay((TaxiTask)veh.getSchedule().getCurrentTask())) {
			Link lastLink = Schedules.getLastLinkInSchedule(veh);
			// AV is not parked

			Id<org.matsim.vehicles.Vehicle> vehicleId = Id.createVehicleId(veh.getId());
			Id<Link> parkingLinkId = lastLink.getId();
			while (!this.manager.reserveSpaceIfVehicleCanParkHere(vehicleId, parkingLinkId)) {
				parkingLinkId = parkingLogic.getNextLink(parkingLinkId, vehicleId, mode);
			}
			VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink, network.getLinks().get(parkingLinkId),
					timer.getTimeOfDay(), router, travelTime);

			((PrivateAVScheduler)scheduler).moveIdleVehicle(veh, path);
			manager.parkVehicleHere(vehicleId, parkingLinkId, timer.getTimeOfDay());

		}

	}

	private void sendVehicleToGarage(DvrpVehicle veh) {
		if (isWaitStay((TaxiTask)veh.getSchedule().getCurrentTask())) {
			Link lastLink = Schedules.getLastLinkInSchedule(veh);
			Link garageLink = findClosestAVParking(lastLink);
			if (!lastLink.getId().equals(garageLink.getId())) {
				VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(lastLink, garageLink, timer.getTimeOfDay(),
						router, travelTime);

				((PrivateAVScheduler)scheduler).moveIdleVehicle(veh, path);
				manager.parkVehicleHere(Id.createVehicleId(veh.getId()), garageLink.getId(), path.getArrivalTime());
			}
		}

	}

	/**
	 * @param lastLink
	 * @return
	 */
	private Link findClosestAVParking(Link lastLink) {
		double closestDistance = Double.MAX_VALUE;
		Link closestLink = null;
		for (Link p : avParkings) {
			double distance = DistanceUtils.calculateSquaredDistance(lastLink.getCoord(), p.getCoord());
			if (distance < closestDistance) {
				closestDistance = distance;
				closestLink = p;
			}
		}
		return closestLink;
	}

	private void sendVehicleToCruise(DvrpVehicle veh) {
		if (isWaitStay((TaxiTask)veh.getSchedule().getCurrentTask())) {
			Link lastLink = Schedules.getLastLinkInSchedule(veh);
			Coord firstCircleCoord = new Coord(lastLink.getCoord().getX() - 500 - random.nextInt(1500),
					lastLink.getCoord().getY() - 1000 + random.nextInt(2000));
			Link firstCircleLink = NetworkUtils.getNearestLink(network, firstCircleCoord);
			Coord secondCircleCoord = new Coord(lastLink.getCoord().getX() + 500 + random.nextInt(1500),
					lastLink.getCoord().getY() - 1000 + random.nextInt(2000));
			Link secondCircleLink = NetworkUtils.getNearestLink(network, secondCircleCoord);
			double lastDepartureTime = timer.getTimeOfDay();
			List<Double> linkTTs = new ArrayList<>();
			List<Link> links = new ArrayList<>();
			for (int i = 0; i < 1000; i++) {
				VrpPathWithTravelData outpath = VrpPaths.calcAndCreatePath(lastLink, firstCircleLink, lastDepartureTime,
						router, travelTime);
				lastDepartureTime = outpath.getArrivalTime();
				for (int z = 0; z < outpath.getLinkCount() - 1; z++) {
					linkTTs.add(outpath.getLinkTravelTime(z));
					links.add(outpath.getLink(z));
				}

				VrpPathWithTravelData nextpath = VrpPaths.calcAndCreatePath(firstCircleLink, secondCircleLink,
						lastDepartureTime, router, travelTime);

				for (int z = 0; z < nextpath.getLinkCount() - 1; z++) {
					linkTTs.add(nextpath.getLinkTravelTime(z));
					links.add(nextpath.getLink(z));
				}

				lastDepartureTime = nextpath.getArrivalTime();
				VrpPathWithTravelData lastPath = VrpPaths.calcAndCreatePath(secondCircleLink, lastLink,
						lastDepartureTime, router, travelTime);
				for (int z = 0; z < lastPath.getLinkCount() - 1; z++) {
					linkTTs.add(lastPath.getLinkTravelTime(z));
					links.add(lastPath.getLink(z));
				}

				lastDepartureTime = lastPath.getArrivalTime();

			}
			double[] linkTTA = new double[linkTTs.size()];
			int i = 0;
			for (Double d : linkTTs) {
				linkTTA[i] = d;
				i++;
			}

			VrpPathWithTravelData path = new VrpPathWithTravelDataImpl(timer.getTimeOfDay(),
					lastDepartureTime - timer.getTimeOfDay(), links.toArray(new Link[links.size()]), linkTTA);
			((PrivateAVScheduler)scheduler).moveIdleVehicle(veh, path);
		}
	}

	@Override
	protected boolean doReoptimizeAfterNextTask(TaxiTask newCurrentTask) {
		return isWaitStay(newCurrentTask);
	}

	private boolean isWaitStay(TaxiTask task) {
		return task.getTaxiTaskType() == TaxiTaskType.STAY;
	}
}
