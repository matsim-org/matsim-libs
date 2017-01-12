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
package playground.jbischoff.taxibus.algorithm.optimizer.clustered;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.router.Dijkstra;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import playground.jbischoff.drt.scheduler.tasks.DrtTask;
import playground.jbischoff.taxibus.algorithm.optimizer.TaxibusOptimizer;
import playground.jbischoff.taxibus.algorithm.passenger.TaxibusRequest;
import playground.jbischoff.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class JspritTaxibusOptimizer implements TaxibusOptimizer {

	final ClusteringTaxibusOptimizerContext context;
	final Collection<TaxibusRequest> unplannedRequests;
	final Random r = MatsimRandom.getLocalInstance();
	private final Dijkstra router;

	/**
	 * 
	 */
	public JspritTaxibusOptimizer(ClusteringTaxibusOptimizerContext context) {
		this.context = context;
		this.unplannedRequests = new HashSet<>();
		this.router = new Dijkstra(context.scenario.getNetwork(), context.travelDisutility, context.travelTime);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking#
	 * nextLinkEntered(org.matsim.contrib.dvrp.schedule.DriveTask)
	 */
	@Override
	public void nextLinkEntered(DriveTask driveTask) {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.dvrp.optimizer.VrpOptimizer#requestSubmitted(org.
	 * matsim.contrib.dvrp.data.Request)
	 */
	@Override
	public void requestSubmitted(Request request) {
		if (context.requestDeterminator.isRequestServable(request)) {
			// Logger.getLogger(getClass()).info("Submitting " + request);
			this.unplannedRequests.add((TaxibusRequest) request);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.contrib.dvrp.optimizer.VrpOptimizer#nextTask(org.matsim.
	 * contrib.dvrp.schedule.Schedule)
	 */
	@Override
	public void nextTask(Schedule<? extends Task> schedule) {
		@SuppressWarnings("unchecked")
		Schedule<DrtTask> taxibusSchedule = (Schedule<DrtTask>) schedule;
		context.scheduler.updateBeforeNextTask(taxibusSchedule);

		DrtTask newCurrentTask = taxibusSchedule.nextTask();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener#
	 * notifyMobsimBeforeSimStep(org.matsim.core.mobsim.framework.events.
	 * MobsimBeforeSimStepEvent)
	 */
	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		if ((e.getSimulationTime() % (context.clustering_period_min * 60)) == 0) {
			Set<TaxibusRequest> dueRequests = new HashSet<>();
			for (TaxibusRequest r : unplannedRequests) {
				if (e.getSimulationTime() >= r.getT0() - context.prebook_period_min * 60) {
					dueRequests.add(r);
				}
			}
			unplannedRequests.removeAll(dueRequests);
			if (dueRequests.size() > 0) {
				VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
				vrpBuilder.setFleetSize(FleetSize.FINITE);
				Map<String, Vehicle> vehicles = new HashMap<>();
				Map<String, TaxibusRequest> requests = new HashMap<>();

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
					for (TaxibusRequest req : dueRequests) {

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
						
						double pickupOffSet = Math.max(e.getSimulationTime(), req.getT0());
						Shipment shipment = Shipment.Builder.newInstance(rId).addSizeDimension(0, 1)
								.setPickupLocation(fromLoc).setDeliveryLocation(toLoc)
								.setPickupTimeWindow(TimeWindow.newInstance(pickupOffSet, pickupOffSet+ 1800)).build();

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
							TaxibusRequest r = requests.remove(j.getId());
							dispatch.addRequest(r);
						}
						context.scheduler.scheduleRequest(dispatch);

					}

					for (Job j : bestSolution.getUnassignedJobs()) {
						TaxibusRequest r = requests.remove(j.getId());
						unplannedRequests.add(r);
					}

					/*
					 * print nRoutes and totalCosts of bestSolution
					 */
//					SolutionPrinter.print(bestSolution);

				}
			}
		}
	}

}
