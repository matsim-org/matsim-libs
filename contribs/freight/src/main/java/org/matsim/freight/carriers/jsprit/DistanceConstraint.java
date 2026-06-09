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

import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.route.activity.*;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

	// Recalculating every candidate route is expensive, so only do it close to the usable range limit.
	private static final double FULL_ROUTE_RECALCULATION_THRESHOLD = 0.9;

	private static final Logger log = LogManager.getLogger(DistanceConstraint.class);

	private final CarrierVehicleTypes vehicleTypes;

	private final NetworkBasedTransportCosts netBasedCosts;

	private final double usableEnergyCapacityFactor;

	private final Set<Id<VehicleType>> loggedVehicleTypes = ConcurrentHashMap.newKeySet();

	public DistanceConstraint(CarrierVehicleTypes vehicleTypes, NetworkBasedTransportCosts netBasedCosts,
			double usableRangeInPercent) {
		if (!Double.isFinite(usableRangeInPercent) || usableRangeInPercent <= 0. || usableRangeInPercent > 100.) {
			throw new IllegalArgumentException("Distance constraint usable range must be in the range (0, 100].");
		}
		this.vehicleTypes = vehicleTypes;
		this.netBasedCosts = netBasedCosts;
		this.usableEnergyCapacityFactor = usableRangeInPercent / 100.;
		log.info("Distance constraint initialized with a usable range of {} percent of the vehicle range during tour planning.",
				usableRangeInPercent);
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
		Id<VehicleType> vehicleTypeId = Id.create(newVehicle.getType().getTypeId(), VehicleType.class);
		VehicleType vehicleTypeOfNewVehicle = vehicleTypes.getVehicleTypes()
				.get(vehicleTypeId);
		Double vehicleEnergyCapacity = VehicleUtils.getEnergyCapacity(vehicleTypeOfNewVehicle.getEngineInformation());
		if (vehicleEnergyCapacity == null)  return ConstraintsStatus.FULFILLED;

		double energyCapacityInKWhOrLiters = vehicleEnergyCapacity * usableEnergyCapacityFactor;
		if (loggedVehicleTypes.add(vehicleTypeId)) {
			log.info("Distance constraint checks vehicle type {} with energy capacity {} and usable energy capacity {} ({} percent usable range).",
				vehicleTypeId, vehicleEnergyCapacity, energyCapacityInKWhOrLiters, (usableEnergyCapacityFactor * 100.));
		}
		Double consumptionPerMeter;
		if (VehicleUtils.getHbefaTechnology(vehicleTypeOfNewVehicle.getEngineInformation()).equals("electricity"))
			consumptionPerMeter = VehicleUtils
				.getEnergyConsumptionKWhPerMeter(vehicleTypeOfNewVehicle.getEngineInformation());
		else
			consumptionPerMeter = VehicleUtils.getFuelConsumptionLitersPerMeter(vehicleTypeOfNewVehicle.getEngineInformation());

		double currentDistance = calculateRouteDistance(context, newVehicle);
		double currentRouteConsumption = currentDistance * (consumptionPerMeter);
		if (currentRouteConsumption > energyCapacityInKWhOrLiters && newAct instanceof PickupShipment)
			return ConstraintsStatus.NOT_FULFILLED_BREAK;

		double distancePrevAct2NewAct = getDistance(prevAct, newAct, newVehicle, prevActDepTime);
		double newActDepTime = calculateDepartureTimeAtNewActivity(context, prevAct, newAct, newVehicle, prevActDepTime);
		double distanceNewAct2nextAct = getDistance(newAct, nextAct, newVehicle, newActDepTime);
		double distancePrevAct2NextAct = getDistance(prevAct, nextAct, newVehicle, prevActDepTime);
		if (prevAct instanceof Start && nextAct instanceof End) distancePrevAct2NextAct = 0;
		if (nextAct instanceof End && !context.getNewVehicle().isReturnToDepot()) {
			distanceNewAct2nextAct = 0;
			distancePrevAct2NextAct = 0;
		}
		double additionalDistance = distancePrevAct2NewAct + distanceNewAct2nextAct - distancePrevAct2NextAct;
		double additionalConsumption = additionalDistance * (consumptionPerMeter);

		double additionalDistanceOfPickup;
		double additionalConsumptionOfPickup = 0;
		if (newAct instanceof DeliverShipment) {
			int iIndexOfPickup = context.getRelatedActivityContext().getInsertionIndex();
			TourActivity pickup = context.getAssociatedActivities().getFirst();
			Location beforePickupLocation;
			double beforePickupDepTime;
			if (iIndexOfPickup > 0) {
				TourActivity actBeforePickup = context.getRoute().getActivities().get(iIndexOfPickup - 1);
				beforePickupLocation = actBeforePickup.getLocation();
				beforePickupDepTime = actBeforePickup.getEndTime();
			} else {
				beforePickupLocation = context.getNewVehicle().getStartLocation();
				beforePickupDepTime = context.getNewDepTime();
			}
			TourActivity actAfterPickup;
			if (iIndexOfPickup < context.getRoute().getActivities().size())
				actAfterPickup = context.getRoute().getActivities().get(iIndexOfPickup);
			else
				actAfterPickup = nextAct;
			double distanceActBeforePickup2Pickup = getDistance(beforePickupLocation, pickup.getLocation(), newVehicle, beforePickupDepTime);
			double distancePickup2ActAfterPickup = getDistance(pickup, actAfterPickup, newVehicle, context.getRelatedActivityContext().getEndTime());
			double distanceBeforePickup2AfterPickup = getDistance(beforePickupLocation, actAfterPickup.getLocation(), newVehicle, beforePickupDepTime);
			if (context.getRoute().isEmpty()) distanceBeforePickup2AfterPickup = 0;
			if (actAfterPickup instanceof End && !context.getNewVehicle().isReturnToDepot()) {
				distancePickup2ActAfterPickup = 0;
				distanceBeforePickup2AfterPickup = 0;
			}
			additionalDistanceOfPickup = distanceActBeforePickup2Pickup + distancePickup2ActAfterPickup - distanceBeforePickup2AfterPickup;
			additionalConsumptionOfPickup = additionalDistanceOfPickup * (consumptionPerMeter);
		}

		double estimatedConsumption = currentRouteConsumption + additionalConsumption + additionalConsumptionOfPickup;
		if (estimatedConsumption < energyCapacityInKWhOrLiters * FULL_ROUTE_RECALCULATION_THRESHOLD)
			return ConstraintsStatus.FULFILLED;

		// Static routing cannot change the distance of subsequent legs. During pickup evaluation, jsprit has also not
		// selected the delivery position yet; the complete shipment is recalculated with its delivery candidate.
		// recalculation is only done if the network is time-dependent
		if (!netBasedCosts.usesTimeDependentRouting() || newAct instanceof PickupShipment)
			return estimatedConsumption > energyCapacityInKWhOrLiters
				? ConstraintsStatus.NOT_FULFILLED
				: ConstraintsStatus.FULFILLED;

		double recalculatedConsumption = calculateRouteDistanceWithInsertion(context, newAct, newVehicle) * consumptionPerMeter;
		if (recalculatedConsumption > energyCapacityInKWhOrLiters)
			return ConstraintsStatus.NOT_FULFILLED;

		return ConstraintsStatus.FULFILLED;

	}

	/**
	 * Calculates the distance of the current route using its stored activity end times. This is the cheap baseline
	 * for the local insertion estimate.
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

	/**
	 * Recalculates the complete candidate route while propagating its departure times. Thus, an insertion can also
	 * change the time-dependent distance of all subsequent legs.
	 */
	private double calculateRouteDistanceWithInsertion(JobInsertionContext context, TourActivity newAct, Vehicle vehicle) {
		List<TourActivity> activities = createActivitySequenceWithInsertion(context, newAct);
		Location previousLocation = vehicle.getStartLocation();
		double departureTime = context.getNewDepTime();
		double distance = 0.;

		for (TourActivity activity : activities) {
			distance += getDistance(previousLocation, activity.getLocation(), vehicle, departureTime);
			double arrivalTime = departureTime + netBasedCosts.getTransportTime(
				previousLocation, activity.getLocation(), departureTime, context.getNewDriver(), vehicle);
			double operationStartTime = Math.max(arrivalTime, activity.getTheoreticalEarliestOperationStartTime());
			departureTime = operationStartTime + activity.getOperationTime(); // + activityCosts.getActivityDuration(activity, arrivalTime, context.getNewDriver(), vehicle);
			previousLocation = activity.getLocation();
		}

		if (vehicle.isReturnToDepot()) {
			distance += getDistance(previousLocation, vehicle.getEndLocation(), vehicle, departureTime);
		}
		return distance;
	}

	/**
	 * Creates the complete activity sequence represented by the current insertion context. Shipment pickup and
	 * delivery indices both refer to positions in the original route.
	 */
	private List<TourActivity> createActivitySequenceWithInsertion(JobInsertionContext context, TourActivity newAct) {
		List<TourActivity> currentActivities = context.getRoute().getActivities();
		List<TourActivity> activitiesWithInsertion = new ArrayList<>(currentActivities.size() + 2);
		int insertionIndex = context.getActivityContext().getInsertionIndex();

		if (newAct instanceof DeliverShipment) {
			int pickupInsertionIndex = context.getRelatedActivityContext().getInsertionIndex();
			TourActivity pickup = context.getAssociatedActivities().getFirst();
			for (int index = 0; index <= currentActivities.size(); index++) {
				if (index == pickupInsertionIndex) activitiesWithInsertion.add(pickup);
				if (index == insertionIndex) activitiesWithInsertion.add(newAct);
				if (index < currentActivities.size()) activitiesWithInsertion.add(currentActivities.get(index));
			}
		} else {
			activitiesWithInsertion.addAll(currentActivities.subList(0, insertionIndex));
			activitiesWithInsertion.add(newAct);
			activitiesWithInsertion.addAll(currentActivities.subList(insertionIndex, currentActivities.size()));
		}
		return activitiesWithInsertion;
	}

	private double calculateDepartureTimeAtNewActivity(JobInsertionContext context, TourActivity prevAct,
			TourActivity newAct, Vehicle vehicle, double prevActDepTime) {
			double arrivalTime = prevActDepTime + netBasedCosts.getTransportTime(
			prevAct.getLocation(), newAct.getLocation(), prevActDepTime, context.getNewDriver(), vehicle);
		double operationStartTime = Math.max(arrivalTime, newAct.getTheoreticalEarliestOperationStartTime());
		return operationStartTime + newAct.getOperationTime(); //.activityCosts.getActivityDuration(newAct, arrivalTime, context.getNewDriver(), vehicle);
	}

	private double getDistance(TourActivity from, TourActivity to, Vehicle vehicle, double departureTime) {
		return getDistance(from.getLocation(), to.getLocation(), vehicle, departureTime);
	}

	private double getDistance(Location from, Location to, Vehicle vehicle, double departureTime) {
		double distance = netBasedCosts.getDistance(from, to, departureTime, vehicle);
		if (!(distance >= 0.))
			throw new AssertionError("Distance must not be negative! From, to" + from + ", " + to
					+ " distance " + distance);
		return distance;
	}
}
