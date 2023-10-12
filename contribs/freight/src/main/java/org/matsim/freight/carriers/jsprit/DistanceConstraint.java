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
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.DeliverShipment;
import com.graphhopper.jsprit.core.problem.solution.route.activity.PickupShipment;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
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
	//TODO add the time dependencies of the distance calculation because the route choice can be different for different times
	@Override
	public ConstraintsStatus fulfilled(JobInsertionContext context, TourActivity prevAct, TourActivity newAct,
			TourActivity nextAct, double departureTime) {
		double additionalDistance;

		VehicleType vehicleTypeOfNewVehicle = vehicleTypes.getVehicleTypes()
				.get(Id.create(context.getNewVehicle().getType().getTypeId(), VehicleType.class));
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

			double routeDistance = calculateRouteDistance(context, newVehicle);
			double routeConsumption = routeDistance * (consumptionPerMeter);

			if (newAct instanceof PickupShipment) {
				// calculates the additional distance for adding a pickup and checks the shortest possibility for adding the associated delivery
				additionalDistance = getDistance(prevAct, newAct, newVehicle) + getDistance(newAct, nextAct, newVehicle)
						- getDistance(prevAct, nextAct, newVehicle)+ findMinimalAdditionalDistance(context, newAct, newAct);

			} else if (newAct instanceof DeliverShipment) {
				// calculates the distance new for integrating the associated pickup
				routeDistance = calculateRouteDistanceWithAssociatedPickup(context);
				routeConsumption = routeDistance * (consumptionPerMeter);

				additionalDistance = getDistance(prevAct, newAct, newVehicle) + getDistance(newAct, nextAct, newVehicle)
						- getDistance(prevAct, nextAct, newVehicle);

			} else {
				additionalDistance = getDistance(prevAct, newAct, newVehicle) + getDistance(newAct, nextAct, newVehicle)
						- getDistance(prevAct, nextAct, newVehicle);
			}
			double additionalConsumption = additionalDistance * (consumptionPerMeter);
			double newRouteConsumption = routeConsumption + additionalConsumption;

			if (newRouteConsumption > energyCapacityInKWhOrLiters) {
				return ConstraintsStatus.NOT_FULFILLED_BREAK;
			} else {
				return ConstraintsStatus.FULFILLED;
			}
		} else {
			return ConstraintsStatus.FULFILLED;
		}
	}

	/**
	 * Calculates the distance based on the route-based distances between every tour
	 * activities. The method also integrates the associated pickup in the tour.
	 */
	private double calculateRouteDistanceWithAssociatedPickup(JobInsertionContext context) {
		double routeDistance;
		int positionOfRelatedPickup = context.getRelatedActivityContext().getInsertionIndex();
		int nextRouteActivity = 0;

		// checks if the associated pickup is on first position
		if (positionOfRelatedPickup == 0 && context.getRoute().getActivities().isEmpty()) {
			context.getRoute().getStart().setLocation(context.getNewVehicle().getStartLocation());
			context.getRoute().getEnd().setLocation(context.getNewVehicle().getEndLocation());
			routeDistance = getDistance(context.getAssociatedActivities().get(0), context.getRoute().getEnd(),
					context.getNewVehicle(), context.getNewDepTime());
			return routeDistance;
		} else if (positionOfRelatedPickup == 0 && !context.getRoute().getActivities().isEmpty()) {
			routeDistance = getDistance(context.getAssociatedActivities().get(0),
					context.getRoute().getActivities().get(0), context.getNewVehicle(), context.getNewDepTime());
		} else {
			routeDistance = getDistance(context.getRoute().getStart(), context.getRoute().getActivities().get(0),
					context.getNewVehicle(), context.getNewDepTime());
		}
		// adds distances between every tour activity and adds the associated pickup on
		// the correct position of the tour
		while (context.getRoute().getTourActivities().getActivities().size() > (nextRouteActivity + 1)) {
			if (positionOfRelatedPickup == (nextRouteActivity + 1) && positionOfRelatedPickup != 0) {
				routeDistance = routeDistance + getDistance(context.getRoute().getActivities().get(nextRouteActivity),
						context.getAssociatedActivities().get(0), context.getNewVehicle());
				routeDistance = routeDistance + getDistance(context.getAssociatedActivities().get(0),
						context.getRoute().getActivities().get(nextRouteActivity), context.getNewVehicle());
			} else {
				routeDistance = routeDistance + getDistance(context.getRoute().getActivities().get(nextRouteActivity),
						context.getRoute().getActivities().get(nextRouteActivity + 1), context.getNewVehicle());
			}
			nextRouteActivity++;
		}
		if (positionOfRelatedPickup == context.getRoute().getActivities().size()) {
			routeDistance = routeDistance + getDistance(context.getRoute().getActivities().get(nextRouteActivity),
					context.getAssociatedActivities().get(0), context.getNewVehicle());
			routeDistance = routeDistance + getDistance(context.getAssociatedActivities().get(0),
					context.getRoute().getEnd(), context.getNewVehicle());
		} else
			routeDistance = routeDistance + getDistance(context.getRoute().getActivities().get(nextRouteActivity),
					context.getRoute().getEnd(), context.getNewVehicle());

		return routeDistance;
	}

	/**
	 * Calculates the distance based on the route-based distances between every tour
	 * activities.
	 *
	 */
	private double calculateRouteDistance(JobInsertionContext context, Vehicle newVehicle) {
		double realRouteDistance = 0;
		if (context.getRoute().getTourActivities().getActivities().isEmpty())
			return realRouteDistance;
		int n = 0;
		realRouteDistance = getDistance(context.getRoute().getStart(),
				context.getRoute().getTourActivities().getActivities().get(0), newVehicle);
		while (context.getRoute().getTourActivities().getActivities().size() > (n + 1)) {
			realRouteDistance = realRouteDistance
					+ getDistance(context.getRoute().getTourActivities().getActivities().get(n),
							context.getRoute().getTourActivities().getActivities().get(n + 1), newVehicle);
			n++;
		}
		realRouteDistance = realRouteDistance + getDistance(
				context.getRoute().getTourActivities().getActivities().get(n), context.getRoute().getEnd(), newVehicle);
		return realRouteDistance;
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
	 * @param newInvestigatedPickup
	 * @param nextAct
	 * @return minimal distance of the associated delivery
	 */
	private double findMinimalAdditionalDistance(JobInsertionContext context, TourActivity newInvestigatedPickup,
			TourActivity nextAct) {
		double minimalAdditionalDistance = 0;

		if (context.getAssociatedActivities().get(1) instanceof DeliverShipment) {
			TourActivity assignedDelivery = context.getAssociatedActivities().get(1);
			minimalAdditionalDistance = 0;
			int indexNextActivity = nextAct.getIndex();
			int tourPositionOfActivityBehindNewPickup = 0;
			int countIndex = 0;
			Vehicle newVehicle = context.getNewVehicle();
			VehicleRoute route = context.getRoute();

			// search the index of the activity behind the pickup activity which should be
			// added to the tour
			a: for (TourActivity tourActivity : route.getTourActivities().getActivities()) {
				if (tourActivity.getIndex() == indexNextActivity) {
					while (countIndex < route.getTourActivities().getActivities().size()) {
						if (route.getTourActivities().getActivities().get(countIndex).getIndex() == indexNextActivity) {
							tourPositionOfActivityBehindNewPickup = countIndex;
							break a;
						}
						countIndex++;
					}
				}
			}

			// search the minimal distance between every exiting TourActivity
			while ((tourPositionOfActivityBehindNewPickup + 1) < route.getTourActivities().getActivities().size()) {
				TourActivity activityBefore = route.getTourActivities().getActivities()
						.get(tourPositionOfActivityBehindNewPickup);
				TourActivity activityAfter = route.getTourActivities().getActivities()
						.get(tourPositionOfActivityBehindNewPickup + 1);
				double possibleAdditionalDistance = getDistance(activityBefore, assignedDelivery, newVehicle)
						+ getDistance(assignedDelivery, activityAfter, newVehicle)
						- getDistance(activityBefore, activityAfter, newVehicle);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);
				tourPositionOfActivityBehindNewPickup++;
			}
			// checks the distance if the delivery is the last activity before the end of
			// the tour
			if (route.getTourActivities().getActivities().size() > 0) {
				TourActivity activityLastDelivery = route.getTourActivities().getActivities()
						.get(route.getTourActivities().getActivities().size() - 1);
				TourActivity activityEnd = route.getEnd();
				double possibleAdditionalDistance = getDistance(activityLastDelivery, assignedDelivery, newVehicle)
						+ getDistance(assignedDelivery, activityEnd, newVehicle)
						- getDistance(activityLastDelivery, activityEnd, newVehicle);
				minimalAdditionalDistance = findMinimalDistance(minimalAdditionalDistance, possibleAdditionalDistance);

				// Checks the distance if the delivery will be added directly behind the pickup
				TourActivity activityAfter = route.getTourActivities().getActivities()
						.get(tourPositionOfActivityBehindNewPickup);
				possibleAdditionalDistance = getDistance(newInvestigatedPickup, assignedDelivery, newVehicle)
						+ getDistance(assignedDelivery, activityAfter, newVehicle)
						- getDistance(newInvestigatedPickup, activityAfter, newVehicle);
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

	private double getDistance(TourActivity from, TourActivity to, Vehicle vehicle) {
		double distance = netBasedCosts.getDistance(from.getLocation(), to.getLocation(), from.getEndTime(),
				vehicle);
		if (!(distance >= 0.))
			throw new AssertionError("Distance must not be negative! From, to" + from + ", " + to
					+ " distance " + distance);
		return distance;
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
