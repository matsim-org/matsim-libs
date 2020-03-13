/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.jsprit;

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author rewert
 * 
 *         Includes all classes and methods for the distance constraint of every
 *         vehicle with an energyCapacity. The base for calculating the
 *         consumption is only the driven distance and not the transported
 *         weight or other influences. But is possible to integrate it.
 * 
 *         !! No recharging or refueling is integrated. Vehicles are totally
 *         full at the beginning.
 *
 *         Creates the distance constraint.
 */
/* pacakge-private */ class DistanceConstraint implements HardActivityConstraint {

	private final StateManager stateManager;

	private final StateId distanceStateId;

	private final CarrierVehicleTypes vehicleTypes;

	private final NetworkBasedTransportCosts netBasedCosts;

	public DistanceConstraint(StateId distanceStateId, StateManager stateManager, CarrierVehicleTypes vehicleTypes,
			NetworkBasedTransportCosts netBasedCosts) {
		this.stateManager = stateManager;
		this.distanceStateId = distanceStateId;
		this.vehicleTypes = vehicleTypes;
		this.netBasedCosts = netBasedCosts;
	}

	/**
	 * When adding a TourActivity to the tour and the new vehicle has an
	 * energyCapacity (fuel or electricity etc.) the algorithm always checks the
	 * fulfilled method if all conditions (constraints) are fulfilled or not.
	 * Because every activity is added separately and the pickup before the delivery
	 * of a shipment, it will investigate which additional distance is necessary for
	 * the pickup and which minimal additional distance of the associated Delivery
	 * is needed. This is also important for the fulfilled decision of this
	 * function. At the end the conditions checks if the consumption of the tour
	 * including the additional shipment is possible with the possible
	 * energyCapacity.
	 */

	@Override
	public ConstraintsStatus fulfilled(JobInsertionContext context, TourActivity prevAct, TourActivity newAct,
			TourActivity nextAct, double departureTime) {
		double additionalDistance;

		VehicleType vehicleTypeOfNewVehicle = vehicleTypes.getVehicleTypes()
				.get(Id.create(context.getNewVehicle().getType().getTypeId().toString(), VehicleType.class));
		if (VehicleUtils.getEnergyCapacity(vehicleTypeOfNewVehicle.getEngineInformation()) != null) {

			Vehicle newVehicle = context.getNewVehicle();

			Double energyCapacityInKWhOrLiters = VehicleUtils
					.getEnergyCapacity(vehicleTypeOfNewVehicle.getEngineInformation());
			Double consumptionPerMeter;
			if (VehicleUtils.getHbefaTechnology(vehicleTypeOfNewVehicle.getEngineInformation()).equals("electricity"))
				consumptionPerMeter = VehicleUtils
						.getEnergyConsumptionKWhPerMeter(vehicleTypeOfNewVehicle.getEngineInformation());
			else
				consumptionPerMeter = VehicleUtils.getFuelConsumption(vehicleTypeOfNewVehicle);
			Double routeConsumption = null;

			Double routeDistance = stateManager.getRouteState(context.getRoute(), distanceStateId, Double.class);

			if (routeDistance == null) {
				routeDistance = 0.;
				routeConsumption = 0.;
			} else {
				routeConsumption = routeDistance * (consumptionPerMeter);
			}
			if (newAct.getName().contains("pickupShipment")) {
				additionalDistance = getDistance(prevAct, newAct, newVehicle, departureTime)
						+ getDistance(newAct, nextAct, newVehicle, departureTime)
						- getDistance(prevAct, nextAct, newVehicle, departureTime)
						+ findMinimalAdditionalDistance(context, newAct, nextAct, departureTime);
			} else {
				additionalDistance = getDistance(prevAct, newAct, newVehicle, departureTime)
						+ getDistance(newAct, nextAct, newVehicle, departureTime)
						- getDistance(prevAct, nextAct, newVehicle, departureTime);

			}
			double additionalConsumption = additionalDistance * (consumptionPerMeter);
			double newRouteConsumption = routeConsumption + additionalConsumption;

			if (newRouteConsumption > energyCapacityInKWhOrLiters) {
				return ConstraintsStatus.NOT_FULFILLED;
			} else
				return ConstraintsStatus.FULFILLED;
		} else {
			return ConstraintsStatus.FULFILLED;
		}
	}

	/**
	 * Finds a minimal additional distance for the tour, when a pickup is added to
	 * the plan. The AssociatedActivities contains both activities of a job which
	 * should be added to the existing tour. The TourActivities which are already in
	 * the tour are found in context.getRoute().getTourActivities. In this method
	 * the position of the new pickup is fixed and three options of the location of
	 * the delivery activity will be checked: delivery between every other activity
	 * after the pickup, delivery as the last activity before the end and delivery
	 * directly behind the new pickup. This method gives back the minimal distance
	 * of this three options.
	 *
	 * @param context
	 * @param newAct
	 * @param nextAct
	 * @return minimal distance of the associated delivery
	 */
	private double findMinimalAdditionalDistance(JobInsertionContext context, TourActivity newAct, TourActivity nextAct,
			double departureTime) {
		double minimalAdditionalDistance = 0;

		if (context.getAssociatedActivities().get(1).getName().contains("deliverShipment")) {
			TourActivity assignedDelivery = context.getAssociatedActivities().get(1);
			minimalAdditionalDistance = 0;
			int indexNextActicity = nextAct.getIndex();
			int index = 0;
			int countIndex = 0;
			Vehicle newVehicle = context.getNewVehicle();
			VehicleRoute route = context.getRoute();

			// search the index of the activity behind the pickup activity which should be
			// added to the tour
			for (TourActivity tourActivity : route.getTourActivities().getActivities()) {
				if (tourActivity.getIndex() == indexNextActicity) {
					while (countIndex < route.getTourActivities().getActivities().size()) {
						if (route.getTourActivities().getActivities().get(countIndex).getIndex() == indexNextActicity) {
							index = countIndex;
							break;
						}
					}
				}
				break;
			}

			// search the minimal distance between every exiting TourAcitivity
			while ((index + 1) < route.getTourActivities().getActivities().size()) {
				TourActivity activityBefore = route.getTourActivities().getActivities().get(index);
				TourActivity activityAfter = route.getTourActivities().getActivities().get(index + 1);
				double possibleAdditionalDistance = getDistance(activityBefore, assignedDelivery, newVehicle,
						departureTime) + getDistance(assignedDelivery, activityAfter, newVehicle, departureTime)
						- getDistance(activityBefore, activityAfter, newVehicle, departureTime);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
				index++;
			}
			// checks the distance if the delivery is the last activity before the end of
			// the tour
			if (route.getTourActivities().getActivities().size() > 0) {
				TourActivity activityLastDelivery = route.getTourActivities().getActivities()
						.get(route.getTourActivities().getActivities().size() - 1);
				TourActivity activityEnd = route.getEnd();
				double possibleAdditionalDistance = getDistance(activityLastDelivery, assignedDelivery, newVehicle,
						departureTime) + getDistance(assignedDelivery, activityEnd, newVehicle, departureTime)
						- getDistance(activityLastDelivery, activityEnd, newVehicle, departureTime);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
				// Checks the distance if the delivery will added directly behind the pickup
				TourActivity activityAfter = route.getTourActivities().getActivities().get(index);
				possibleAdditionalDistance = getDistance(newAct, assignedDelivery, newVehicle, departureTime)
						+ getDistance(assignedDelivery, activityAfter, newVehicle, departureTime)
						- getDistance(newAct, activityAfter, newVehicle, departureTime);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
			}

		}
		return minimalAdditionalDistance;
	}

	/**
	 * Checks if the find possible distance is the minimal one.
	 *
	 * @param minimalAdditionalDistance
	 * @param possibleAdditionalDistance
	 * @return the minimal transport distance
	 */
	private double findMinimalDistance(double minimalAdditionalDistance, double possibleAdditionalDistance) {
		if (minimalAdditionalDistance == 0)
			minimalAdditionalDistance = possibleAdditionalDistance;
		else if (possibleAdditionalDistance < minimalAdditionalDistance)
			minimalAdditionalDistance = possibleAdditionalDistance;
		return minimalAdditionalDistance;
	}

	private double getDistance(TourActivity from, TourActivity to, Vehicle vehicle, double departureTime) {
		double distance = netBasedCosts.getTransportDistance(from.getLocation(), to.getLocation(), departureTime, null,
				vehicle);
		if (!(distance >= 0.))
			throw new AssertionError("Distance must not be negativ! From, to" + from.toString() + ", " + to.toString()
					+ " distance " + distance);
		return distance;
	}
}