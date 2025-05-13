/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
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

package org.matsim.freight.carriers.jsprit;

import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.*;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.freight.carriers.CarrierVehicleTypes;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author rewert
 *
 *         Includes all classes and methods for the distance constraint of every
 *         vehicle with an energyCapacity. The base for calculating the
 *         consumption is only the driven distance and not the transported
 *         weight or other influences. But is possible to integrate it.
 * <p>
 *         !! No recharging or refueling is integrated. Vehicles are totally
 *         full at the beginning.
 *
 *         Creates the distance constraint.
 */
/* package-private */ class DistanceConstraint implements HardActivityConstraint {

	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(DistanceConstraint.class);

	private final CarrierVehicleTypes vehicleTypes;

	private final NetworkBasedTransportCosts netBasedCosts;

	public DistanceConstraint(CarrierVehicleTypes vehicleTypes,
							  NetworkBasedTransportCosts netBasedCosts) {
		this.vehicleTypes = vehicleTypes;
		this.netBasedCosts = netBasedCosts;
	}

	/**
	 * When adding a TourActivity to the tour and the new vehicle has an energyCapacity (fuel or electricity etc.) the algorithm always checks the fulfilled method if all conditions (constraints) are fulfilled or not.
	 * If a delivery is added, it also accounts for the necessary consumption if the pickup is added at the given position of the tour. This is also important for the fulfilled decision of this
	 * function. In the end, the conditions check if the consumption of the tour including the additional shipment is possible with the possible energyCapacity.
	 */
	//TODO add the time dependencies of the distance calculation because the route choice can be different for different times
	@Override
	public ConstraintsStatus fulfilled(JobInsertionContext context, TourActivity prevAct, TourActivity newAct,
			TourActivity nextAct, double prevActDepTime) {
//		double additionalDistance;

		Vehicle newVehicle = context.getNewVehicle();
		VehicleType vehicleTypeOfNewVehicle = vehicleTypes.getVehicleTypes()
				.get(Id.create(newVehicle.getType().getTypeId(), VehicleType.class));
		if (VehicleUtils.getEnergyCapacity(vehicleTypeOfNewVehicle.getEngineInformation()) == null)  return ConstraintsStatus.FULFILLED;

		Double energyCapacityInKWhOrLiters = VehicleUtils
			.getEnergyCapacity(vehicleTypeOfNewVehicle.getEngineInformation());
		Double consumptionPerMeter;
		if (VehicleUtils.getHbefaTechnology(vehicleTypeOfNewVehicle.getEngineInformation()).equals("electricity"))
			consumptionPerMeter = VehicleUtils
				.getEnergyConsumptionKWhPerMeter(vehicleTypeOfNewVehicle.getEngineInformation());
		else
			consumptionPerMeter = VehicleUtils.getFuelConsumptionLitersPerMeter(vehicleTypeOfNewVehicle.getEngineInformation());

		double currentDistance = calculateRouteDistance(context, newVehicle);
		double currentRouteConsumption = currentDistance * (consumptionPerMeter);
		if (currentRouteConsumption > energyCapacityInKWhOrLiters) return ConstraintsStatus.NOT_FULFILLED_BREAK;

		double distancePrevAct2NewAct = getDistance(prevAct, newAct, newVehicle, prevActDepTime);
		double distanceNewAct2nextAct = getDistance(newAct, nextAct, newVehicle, prevActDepTime);
		double distancePrevAct2NextAct = getDistance(prevAct, nextAct, newVehicle, prevActDepTime);
		if (prevAct instanceof Start && nextAct instanceof End) distancePrevAct2NextAct = 0;
		if (nextAct instanceof End && !context.getNewVehicle().isReturnToDepot()) {
			distanceNewAct2nextAct = 0;
			distancePrevAct2NextAct = 0;
		}
		double additionalDistance = distancePrevAct2NewAct + distanceNewAct2nextAct - distancePrevAct2NextAct;
		double additionalConsumption = additionalDistance * (consumptionPerMeter);
		if (currentRouteConsumption + additionalConsumption > energyCapacityInKWhOrLiters) return ConstraintsStatus.NOT_FULFILLED;


		double additionalDistanceOfPickup;
		double additionalConsumptionOfPickup = 0;
		if (newAct instanceof DeliverShipment) {
			int iIndexOfPickup = context.getRelatedActivityContext().getInsertionIndex();
			TourActivity pickup = context.getAssociatedActivities().getFirst();
			TourActivity actBeforePickup;
			if (iIndexOfPickup > 0) actBeforePickup = context.getRoute().getActivities().get(iIndexOfPickup - 1);
			else actBeforePickup = new Start(context.getNewVehicle().getStartLocation(), 0, Double.MAX_VALUE);
			TourActivity actAfterPickup;
			if (iIndexOfPickup < context.getRoute().getActivities().size())
				actAfterPickup = context.getRoute().getActivities().get(iIndexOfPickup);
			else
				actAfterPickup = nextAct;
			double distanceActBeforePickup2Pickup = getDistance(actBeforePickup, pickup, newVehicle, actBeforePickup.getEndTime());
			double distancePickup2ActAfterPickup = getDistance(pickup, actAfterPickup, newVehicle, context.getRelatedActivityContext().getEndTime());
			double distanceBeforePickup2AfterPickup = getDistance(actBeforePickup, actAfterPickup,newVehicle, actBeforePickup.getEndTime());
			if (context.getRoute().isEmpty()) distanceBeforePickup2AfterPickup = 0;
			if (actAfterPickup instanceof End && !context.getNewVehicle().isReturnToDepot()) {
				distancePickup2ActAfterPickup = 0;
				distanceBeforePickup2AfterPickup = 0;
			}
			additionalDistanceOfPickup = distanceActBeforePickup2Pickup + distancePickup2ActAfterPickup - distanceBeforePickup2AfterPickup;
			additionalConsumptionOfPickup = additionalDistanceOfPickup * (consumptionPerMeter);
		}

		if (currentRouteConsumption + additionalConsumption + additionalConsumptionOfPickup > energyCapacityInKWhOrLiters)
			return ConstraintsStatus.NOT_FULFILLED;

		return ConstraintsStatus.FULFILLED;

	}

	/**
	 * Calculates the distance based on the route-based distances between every tour activity.
	 */
	private double calculateRouteDistance(JobInsertionContext context, Vehicle newVehicle) {
		double realRouteDistance = 0;
		if (context.getRoute().getTourActivities().getActivities().isEmpty())
			return realRouteDistance;
		int n = 0;
		realRouteDistance = getDistance(context.getRoute().getStart(),
				context.getRoute().getTourActivities().getActivities().getFirst(), newVehicle, context.getRoute().getStart().getEndTime());
		while (context.getRoute().getTourActivities().getActivities().size() > (n + 1)) {

			TourActivity from = context.getRoute().getTourActivities().getActivities().get(n);
			realRouteDistance = realRouteDistance
					+ getDistance(from,	context.getRoute().getTourActivities().getActivities().get(n + 1), newVehicle, from.getEndTime());
			n++;
		}
		TourActivity from = context.getRoute().getTourActivities().getActivities().get(n);
		realRouteDistance = realRouteDistance + getDistance(
				context.getRoute().getTourActivities().getActivities().get(n), context.getRoute().getEnd(), newVehicle, from.getEndTime());
		return realRouteDistance;
	}

	private double getDistance(TourActivity from, TourActivity to, Vehicle vehicle, double departureTime) {
		double distance = netBasedCosts.getDistance(from.getLocation(), to.getLocation(), departureTime,
				vehicle);
		if (!(distance >= 0.))
			throw new AssertionError("Distance must not be negative! From, to" + from + ", " + to
					+ " distance " + distance);
		return distance;
	}
}
