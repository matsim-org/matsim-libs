/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.drt.taxibus.algorithm.optimizer.prebooked.jsprit;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.DrtRequest;
import org.matsim.contrib.drt.taxibus.algorithm.optimizer.TaxibusOptimizer;
import org.matsim.contrib.drt.taxibus.algorithm.optimizer.prebooked.PrebookedTaxibusOptimizerContext;
import org.matsim.contrib.drt.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.Dijkstra;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.*;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.job.*;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.*;
import com.graphhopper.jsprit.core.problem.vehicle.*;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.util.*;

/**
 * @author  jbischoff
 *
 */
/**
 * A taxibus optimizer relying on the Jsprit package for both pickup/delivery planning and the actual planning of tours.
 * Includes the dispatching to the scheduler.
 */
public class JspritTaxibusOptimizer implements TaxibusOptimizer {

	final PrebookedTaxibusOptimizerContext context;
	final Collection<DrtRequest> unplannedRequests;
	final Random r = MatsimRandom.getLocalInstance();
	private final Dijkstra router;

	/**
	 * 
	 */
	public JspritTaxibusOptimizer(PrebookedTaxibusOptimizerContext context) {
		this.context = context;
		this.unplannedRequests = new HashSet<>();
		this.router = new Dijkstra(context.scenario.getNetwork(), context.travelDisutility, context.travelTime);

	}

	@Override
	public void vehicleEnteredNextLink(Vehicle vehicle, Link nextLink) {

	}

	@Override
	public void requestSubmitted(Request request) {
		if (context.requestDeterminator.isRequestServable(request)) {
			// Logger.getLogger(getClass()).info("Submitting " + request);
			this.unplannedRequests.add((DrtRequest)request);
		}
	}

	@Override
	public void nextTask(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		context.scheduler.updateBeforeNextTask(schedule);
		schedule.nextTask();
	}

	@Override
	public void notifyMobsimBeforeSimStep(@SuppressWarnings("rawtypes") MobsimBeforeSimStepEvent e) {
		if ((e.getSimulationTime() % (context.clustering_period_min * 60)) == 0) {
			Set<DrtRequest> dueRequests = new HashSet<>();
			for (DrtRequest r : unplannedRequests) {
				if (e.getSimulationTime() >= r.getEarliestStartTime() - context.prebook_period_min * 60) {
					dueRequests.add(r);
				}
			}
			unplannedRequests.removeAll(dueRequests);
			if (dueRequests.size() > 0) {

				VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
				vrpBuilder.setFleetSize(FleetSize.FINITE);
				Map<String, Vehicle> vehicles = new HashMap<>();
				Map<String, DrtRequest> requests = new HashMap<>();
				VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType")
						.addCapacityDimension(0, context.capacity);
				VehicleType vehicleType = vehicleTypeBuilder.build();
				for (Vehicle veh : this.context.vrpData.getVehicles().values()) {
					if (context.scheduler.isIdle(veh)) {
						Coord startCoord = veh.getStartLink().getCoord();
						String vId = veh.getId().toString();
						vehicles.put(vId, veh);
						Builder vehicleBuilder = VehicleImpl.Builder.newInstance(vId);
						vehicleBuilder.setStartLocation(Location.newInstance(startCoord.getX(), startCoord.getY()));
						vehicleBuilder.setType(vehicleType);
						vehicleBuilder.setEarliestStart(e.getSimulationTime());
						VehicleImpl vehicle = vehicleBuilder.build();
						vrpBuilder.addVehicle(vehicle);

					}

				}

				if (vrpBuilder.getAddedVehicles().isEmpty()) {
					this.unplannedRequests.addAll(dueRequests);
				} else {

					for (DrtRequest req : dueRequests) {

						String rId = req.getId().toString();
						requests.put(rId, req);
						Location fromLoc = Location.Builder.newInstance().setId(req.getFromLink().getId().toString())
								.setCoordinate(Coordinate.newInstance(req.getFromLink().getCoord().getX(),
										req.getFromLink().getCoord().getY()))
								.build();
						Location toLoc = Location.Builder.newInstance().setId(req.getToLink().getId().toString())
								.setCoordinate(Coordinate.newInstance(req.getToLink().getCoord().getX(),
										req.getToLink().getCoord().getY()))
								.build();

						double pickupOffSet = Math.max(e.getSimulationTime(), req.getEarliestStartTime());
						Shipment shipment = Shipment.Builder.newInstance(rId).addSizeDimension(0, 1)
								.setPickupLocation(fromLoc).setDeliveryLocation(toLoc)
								.setPickupTimeWindow(TimeWindow.newInstance(pickupOffSet, pickupOffSet + 3600)).build();

						vrpBuilder.addJob(shipment);
					}
					VehicleRoutingProblem problem = vrpBuilder.build();
					VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
					Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
					VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

					for (VehicleRoute vr : bestSolution.getRoutes()) {
						Vehicle veh = vehicles.get(vr.getVehicle().getId());

						Iterator<TourActivity> it = vr.getActivities().iterator();
						TourActivity lastAct = it.next();
						Link lastDestination = context.scenario.getNetwork().getLinks()
								.get(Id.createLinkId(lastAct.getLocation().getId()));
						VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(veh.getStartLink(), lastDestination,
								context.timer.getTimeOfDay(), router, context.travelTime);
						TaxibusDispatch dispatch = new TaxibusDispatch(veh, path);
						double d = context.timer.getTimeOfDay();
						while (it.hasNext()) {
							d += 60;
							TourActivity current = it.next();
							Link currentDestination = context.scenario.getNetwork().getLinks()
									.get(Id.createLinkId(current.getLocation().getId()));
							dispatch.addPath(VrpPaths.calcAndCreatePath(lastDestination, currentDestination, d, router,
									context.travelTime));
							lastAct = current;
							lastDestination = currentDestination;
						}
						dispatch.addPath(VrpPaths.calcAndCreatePath(lastDestination, veh.getStartLink(), d + 60, router,
								context.travelTime));
						for (Job j : vr.getTourActivities().getJobs()) {
							if (requests.containsKey(j.getId())) {
								DrtRequest r = requests.remove(j.getId());
								dispatch.addRequest(r);
							} else {
								Logger.getLogger(getClass())
										.error(j.getId() + " is not a part of request list? " + requests.toString());
							}
						}
						context.scheduler.scheduleRequest(dispatch);

					}

					for (Job j : bestSolution.getUnassignedJobs()) {
						DrtRequest r = requests.remove(j.getId());
						unplannedRequests.add(r);
					}

					/*
					 * print nRoutes and totalCosts of bestSolution
					 */
					// SolutionPrinter.print(bestSolution);

				}
			}
		}
	}

}
