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
package org.matsim.contrib.taxibus.algorithm.optimizer.prebooked.clustered;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.taxibus.TaxibusRequest;
import org.matsim.contrib.taxibus.algorithm.optimizer.prebooked.PrebookedTaxibusOptimizerContext;
import org.matsim.contrib.taxibus.algorithm.scheduler.vehreqpath.TaxibusDispatch;
import org.matsim.contrib.taxibus.tasks.TaxibusStayTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.router.Dijkstra;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.*;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.*;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.util.*;

/**
 * @author  jbischoff
 * A dispatch creator based on the Jsprit package
 */
/**
 *
 */
public class JspritDispatchCreator implements RequestDispatcher {
	private final PrebookedTaxibusOptimizerContext context;
	private final Dijkstra router;

	/**
	 * 
	 */
	public JspritDispatchCreator(PrebookedTaxibusOptimizerContext context) {
		this.context = context;
		this.router = new Dijkstra(context.scenario.getNetwork(), context.travelDisutility, context.travelTime);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see playground.jbischoff.taxibus.algorithm.optimizer.clustered. RequestDispatcher#createDispatch(java.util.Set)
	 */
	@Override
	public TaxibusDispatch createDispatch(Set<TaxibusRequest> commonRequests) {
		Coord requestCentroid = calcRequestCentroid(commonRequests);
		Vehicle veh = findClosestIdleVehicle(requestCentroid);
		if (veh != null) {
			return createDispatchForVehicle(commonRequests, veh);
		} else
			return null;
	}

	/**
	 * @param commonRequests
	 * @param veh
	 * @return
	 */
	private TaxibusDispatch createDispatchForVehicle(Set<TaxibusRequest> commonRequests, Vehicle veh) {
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		vrpBuilder.setFleetSize(FleetSize.FINITE);
		Map<String, TaxibusRequest> requests = new HashMap<>();
		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType")
				.addCapacityDimension(0, commonRequests.size());
		VehicleType vehicleType = vehicleTypeBuilder.build();

		TaxibusStayTask ct = (TaxibusStayTask)veh.getSchedule().getCurrentTask();

		Coord startCoord = ct.getLink().getCoord();
		String vId = veh.getId().toString();
		Builder vehicleBuilder = VehicleImpl.Builder.newInstance(vId);
		vehicleBuilder.setStartLocation(Location.newInstance(startCoord.getX(), startCoord.getY()));
		vehicleBuilder.setType(vehicleType);
		vehicleBuilder.setReturnToDepot(false);
		VehicleImpl vehicle = vehicleBuilder.build();

		vrpBuilder.addVehicle(vehicle);

		for (TaxibusRequest req : commonRequests) {
			String rId = req.getId().toString();
			requests.put(rId, req);
			Location fromLoc = Location.Builder.newInstance()
					.setId(req.getFromLink().getId().toString()).setCoordinate(Coordinate
							.newInstance(req.getFromLink().getCoord().getX(), req.getFromLink().getCoord().getY()))
					.build();
			Location toLoc = Location.Builder.newInstance().setId(req.getToLink().getId().toString()).setCoordinate(
					Coordinate.newInstance(req.getToLink().getCoord().getX(), req.getToLink().getCoord().getY()))
					.build();
			Shipment shipment = Shipment.Builder.newInstance(rId).addSizeDimension(0, 1).setPickupLocation(fromLoc)
					.setDeliveryLocation(toLoc).build();
			vrpBuilder.addJob(shipment);
		}
		VehicleRoutingProblem problem = vrpBuilder.build();
		VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
		if (bestSolution.getRoutes().size() > 1) {
			Logger.getLogger(getClass()).error("Solution has more than one vehicle on dispatch???");
			throw new RuntimeException();
		}
		VehicleRoute vr = (VehicleRoute)bestSolution.getRoutes().toArray()[0];
		Iterator<TourActivity> it = vr.getActivities().iterator();
		TourActivity lastAct = it.next();
		Link lastDestination = context.scenario.getNetwork().getLinks()
				.get(Id.createLinkId(lastAct.getLocation().getId()));
		VrpPathWithTravelData path = VrpPaths.calcAndCreatePath(ct.getLink(), lastDestination,
				context.timer.getTimeOfDay(), router, context.travelTime);
		TaxibusDispatch dispatch = new TaxibusDispatch(veh, path);
		double d = context.timer.getTimeOfDay();
		while (it.hasNext()) {
			d += 60;
			TourActivity current = it.next();
			Link currentDestination = context.scenario.getNetwork().getLinks()
					.get(Id.createLinkId(current.getLocation().getId()));
			dispatch.addPath(
					VrpPaths.calcAndCreatePath(lastDestination, currentDestination, d, router, context.travelTime));
			lastAct = current;
			lastDestination = currentDestination;
		}
		if (context.returnToDepot) {
			dispatch.addPath(VrpPaths.calcAndCreatePath(lastDestination, veh.getStartLink(), d + 60, router,
					context.travelTime));
		}
		dispatch.addRequests(commonRequests);

		return dispatch;
	}

	/**
	 * @param coord
	 * @return
	 */
	private Vehicle findClosestIdleVehicle(Coord coord) {
		double bestDistance = Double.MAX_VALUE;
		Vehicle bestVehicle = null;
		for (Vehicle veh : this.context.vrpData.getVehicles().values()) {
			if (context.scheduler.isIdle(veh)) {
				TaxibusStayTask ct = (TaxibusStayTask)veh.getSchedule().getCurrentTask();
				Coord startCoord = ct.getLink().getCoord();
				double distance = DistanceUtils.calculateSquaredDistance(startCoord, coord);
				if (distance < bestDistance) {
					bestDistance = distance;
					bestVehicle = veh;
				}

			}

		}

		return bestVehicle;
	}

	/**
	 * @param commonRequests
	 * @return
	 */
	private Coord calcRequestCentroid(Set<TaxibusRequest> commonRequests) {
		Set<Coord> coords = new HashSet<>();
		for (TaxibusRequest r : commonRequests) {
			coords.add(r.getFromLink().getCoord());
		}
		return getCoordCentroid(coords);
	}

	private Coord getCoordCentroid(Set<Coord> coords) {
		double x = 0;
		double y = 0;
		for (Coord c : coords) {
			x += c.getX();
			y += c.getY();
		}
		x = x / coords.size();
		y = y / coords.size();
		return new Coord(x, y);

	}

}
