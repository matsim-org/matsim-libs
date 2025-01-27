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

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.state.StateId;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingActivityCosts;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Service.Builder;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.*;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
import com.graphhopper.jsprit.core.problem.vehicle.Vehicle;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Coordinate;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfig;
import com.graphhopper.jsprit.io.algorithm.AlgorithmConfigXmlReader;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.freight.carriers.*;
import org.matsim.freight.carriers.TimeWindow;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * A factory that creates matsim-object from jsprit
 * (<a href="https://github.com/jsprit/jsprit">...</a>) and vice versa.
 *
 * @author sschroeder
 *         <p>
 *         Extend the functionality for the use of shipments
 * @author kturner
 */
public final class MatsimJspritFactory {

	@SuppressWarnings("unused")
	private static final  Logger log = LogManager.getLogger(MatsimJspritFactory.class);


	/**
	 * Creates (MATSim) {@link CarrierShipment} from a (jsprit) {@link Shipment}
	 *
	 * @param jspritShipment to be transformed to MATSim
	 * @return CarrierShipment
	 * @see CarrierShipment , Shipment
	 */
	static CarrierShipment createCarrierShipment(Shipment jspritShipment) {
		CarrierShipment carrierShipment = CarrierShipment.Builder
				.newInstance(Id.create(jspritShipment.getId(), CarrierShipment.class),
						Id.createLinkId(jspritShipment.getPickupLocation().getId()),
						Id.createLinkId(jspritShipment.getDeliveryLocation().getId()), jspritShipment.getSize().get(0))
				.setDeliveryDuration(jspritShipment.getDeliveryServiceTime())
				.setDeliveryStartingTimeWindow(org.matsim.freight.carriers.TimeWindow.newInstance(jspritShipment.getDeliveryTimeWindow().getStart(),
						jspritShipment.getDeliveryTimeWindow().getEnd()))
				.setPickupDuration(jspritShipment.getPickupServiceTime())
				.setPickupStartingTimeWindow(org.matsim.freight.carriers.TimeWindow.newInstance(jspritShipment.getPickupTimeWindow().getStart(),
						jspritShipment.getPickupTimeWindow().getEnd()))
				.build();
		CarriersUtils.setSkills(carrierShipment, jspritShipment.getRequiredSkills().values());
		return carrierShipment;
	}

	/**
	 * Creates (jsprit) {@link Shipment} from a (MATSim) {@link CarrierShipment}
	 *
	 * @param carrierShipment to be transformed to jsprit
	 * @return Shipment
	 * @see CarrierShipment , Shipment
	 */
	static Shipment createJspritShipment(CarrierShipment carrierShipment) {
		Shipment.Builder shipmentBuilder = Shipment.Builder.newInstance(carrierShipment.getId().toString())
				.setDeliveryLocation(Location.newInstance(carrierShipment.getDeliveryLinkId().toString()))
				.setDeliveryServiceTime(carrierShipment.getDeliveryDuration())
				.setDeliveryTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow
						.newInstance(carrierShipment.getDeliveryStartingTimeWindow().getStart(),
								carrierShipment.getDeliveryStartingTimeWindow().getEnd()))
				.setPickupServiceTime(carrierShipment.getPickupDuration())
				.setPickupLocation(Location.newInstance(carrierShipment.getPickupLinkId().toString()))
				.setPickupTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(
						carrierShipment.getPickupStartingTimeWindow().getStart(),
						carrierShipment.getPickupStartingTimeWindow().getEnd()))
				.addSizeDimension(0, carrierShipment.getCapacityDemand());
		for (String skill : CarriersUtils.getSkills(carrierShipment)) {
			shipmentBuilder.addRequiredSkill(skill);
		}
		return shipmentBuilder.build();
	}

	static Shipment createJspritShipment(CarrierShipment carrierShipment, Coord fromCoord, Coord toCoord) {
		Location.Builder fromLocationBuilder = Location.Builder.newInstance();
		fromLocationBuilder.setId(carrierShipment.getPickupLinkId().toString());
		if (fromCoord != null) {
			fromLocationBuilder.setCoordinate(Coordinate.newInstance(fromCoord.getX(), fromCoord.getY()));
		}
		Location fromLocation = fromLocationBuilder.build();

		Location.Builder toLocationBuilder = Location.Builder.newInstance();
		toLocationBuilder.setId(carrierShipment.getDeliveryLinkId().toString());
		if (toCoord != null) {
			toLocationBuilder.setCoordinate(Coordinate.newInstance(toCoord.getX(), toCoord.getY()));
		}
		Location toLocation = toLocationBuilder.build();

		Shipment.Builder shipmentBuilder = Shipment.Builder.newInstance(carrierShipment.getId().toString())
				.setDeliveryLocation(toLocation).setDeliveryServiceTime(carrierShipment.getDeliveryDuration())
				.setDeliveryTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow
						.newInstance(carrierShipment.getDeliveryStartingTimeWindow().getStart(),
								carrierShipment.getDeliveryStartingTimeWindow().getEnd()))
				.setPickupServiceTime(carrierShipment.getPickupDuration()).setPickupLocation(fromLocation)
				.setPickupTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(
						carrierShipment.getPickupStartingTimeWindow().getStart(),
						carrierShipment.getPickupStartingTimeWindow().getEnd()))
				.addSizeDimension(0, carrierShipment.getCapacityDemand());
		for (String skill : CarriersUtils.getSkills(carrierShipment)) {
			shipmentBuilder.addRequiredSkill(skill);
		}
		return shipmentBuilder.build();
	}

	static Service createJspritService(CarrierService carrierService, Coord locationCoord) {
		Location.Builder locationBuilder = Location.Builder.newInstance();
		locationBuilder.setId(carrierService.getServiceLinkId().toString());
		if (locationCoord != null) {
			locationBuilder.setCoordinate(Coordinate.newInstance(locationCoord.getX(), locationCoord.getY()));
		}
		Location location = locationBuilder.build();

		Builder serviceBuilder = Builder.newInstance(carrierService.getId().toString());
		serviceBuilder.addSizeDimension(0, carrierService.getCapacityDemand());
		serviceBuilder.setLocation(location).setServiceTime(carrierService.getServiceDuration())
				.setTimeWindow(com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow.newInstance(
						carrierService.getServiceStaringTimeWindow().getStart(),
						carrierService.getServiceStaringTimeWindow().getEnd()));
		for (String skill : CarriersUtils.getSkills(carrierService)) {
			serviceBuilder.addRequiredSkill(skill);
		}
		return serviceBuilder.build();
	}

	/**
	 * Creates a (MATSim) {@link CarrierService} from a (jsprit) Service
	 * @param jspritService service from jsprit
	 * @return CarrierService
	 */
	static CarrierService createCarrierService(Service jspritService) {
		CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(
				Id.create(jspritService.getId(), CarrierService.class), Id.create(jspritService.getLocation().getId(), Link.class));
		serviceBuilder.setCapacityDemand(jspritService.getSize().get(0));
		serviceBuilder.setServiceDuration(jspritService.getServiceDuration());
		serviceBuilder.setServiceStartingTimeWindow(TimeWindow.newInstance(jspritService.getTimeWindow().getStart(), jspritService.getTimeWindow().getEnd()));
		CarrierService carrierService = serviceBuilder.build();
		CarriersUtils.setSkills(carrierService, jspritService.getRequiredSkills().values());
		return carrierService;
	}

	/**
	 * Creates jsprit-vehicle from a matsim-carrier-vehicle.
	 *
	 * @param carrierVehicle to be transformed to jsprit
	 * @param locationCoord  to use as start location
	 * @return jsprit vehicle
	 * @see Vehicle, CarrierVehicle
	 */
	static com.graphhopper.jsprit.core.problem.vehicle.Vehicle createJspritVehicle(CarrierVehicle carrierVehicle, Coord locationCoord) {
		Location.Builder vehicleLocationBuilder = Location.Builder.newInstance();
		vehicleLocationBuilder.setId(carrierVehicle.getLinkId().toString() );
		if (locationCoord != null) {
			vehicleLocationBuilder.setCoordinate(Coordinate.newInstance(locationCoord.getX(), locationCoord.getY()));
		}
		Location vehicleLocation = vehicleLocationBuilder.build();
		com.graphhopper.jsprit.core.problem.vehicle.VehicleType vehicleType = createJspritVehicleType(
				carrierVehicle.getType());
		VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(carrierVehicle.getId().toString());
		vehicleBuilder.setEarliestStart(carrierVehicle.getEarliestStartTime())
				.setLatestArrival(carrierVehicle.getLatestEndTime()).setStartLocation(vehicleLocation)
				.setType(vehicleType);
		for (String skill : CarriersUtils.getSkills(carrierVehicle.getType())) {
			vehicleBuilder.addSkill(skill);
		}

		VehicleImpl vehicle = vehicleBuilder.build();

		if (carrierVehicle.getEarliestStartTime() != vehicle.getEarliestDeparture())
			throw new AssertionError("carrierVeh must have the same earliestDep as vrpVeh");
		if (carrierVehicle.getLatestEndTime() != vehicle.getLatestArrival())
			throw new AssertionError("carrierVeh must have the same latestArr as vrpVeh");
		if (!carrierVehicle.getLinkId().toString().equals(vehicle.getStartLocation().getId() ))
			throw new AssertionError("locations must be equal");
		return vehicle;
	}

	/**
	 * Creates {@link CarrierVehicle} from a {basics.route.Vehicle}
	 *
	 * @param jspritVehicle to be transformed to CarrierVehicle
	 * @return carrierVehicle
	 * @see CarrierVehicle , Vehicle
	 */
	static CarrierVehicle createCarrierVehicle(com.graphhopper.jsprit.core.problem.vehicle.Vehicle jspritVehicle) {
		VehicleType matsimVehicleType;
		if (jspritVehicle.getType().getUserData() != null){
			log.info("Use the (MATSim) vehicleType that was stored inside the (jsprit) vehicleType during the jsprit run but never interacted with jsprit. ");
			matsimVehicleType = (VehicleType) jspritVehicle.getType().getUserData(); //Read in store MATSimVehicleType... Attention: This will not take care for any changes during the jsprit run
		} else {
			log.info("There was no (MATSim) vehicleType stored inside the (jsprit) vehicleType. -> create one from the available data of the (jsprit) vehicle type");
			matsimVehicleType = createMatsimVehicleType(jspritVehicle.getType());
		}
		// yyyy

		String vehicleId = jspritVehicle.getId();
		CarrierVehicle.Builder carrierVehicleBuilder = CarrierVehicle.Builder.newInstance(
				Id.create(vehicleId, org.matsim.vehicles.Vehicle.class),
				Id.create(jspritVehicle.getStartLocation().getId(), Link.class),
				matsimVehicleType );

//		carrierVehicleBuilder.setType(matsimVehicleType); //Not needed any more, because is now part of the constructor. KMT jan22
		carrierVehicleBuilder.setEarliestStart(jspritVehicle.getEarliestDeparture());
		carrierVehicleBuilder.setLatestEnd(jspritVehicle.getLatestArrival());
		CarrierVehicle carrierVehicle = carrierVehicleBuilder.build();

		for (String skill : jspritVehicle.getSkills().values()) {
			CarriersUtils.addSkill(carrierVehicle.getType(), skill);
		}

		if (jspritVehicle.getEarliestDeparture() != carrierVehicle.getEarliestStartTime())
			throw new AssertionError("vehicles must have the same earliestStartTime");
		if (jspritVehicle.getLatestArrival() != carrierVehicle.getLatestEndTime())
			throw new AssertionError("vehicles must have the same latestEndTime");
		if (!jspritVehicle.getStartLocation().getId().equals(carrierVehicle.getLinkId().toString() ))
			throw new AssertionError("locs must be the same");
		return carrierVehicle;
	}

	/**
	 * Creates a MATSim {@link VehicleType} from a jsprit
	 * {@link com.graphhopper.jsprit.core.problem.vehicle.VehicleType}.
	 *
	 * <p>
	 * No description and engineInformation can be set here. Do it by calling
	 * setEngineInformation(engineInfo) from the returned object.
	 *
	 * @param jspritVehType to be transformed
	 * @return MatsimVehicleType
	 */
	static VehicleType createMatsimVehicleType(com.graphhopper.jsprit.core.problem.vehicle.VehicleType jspritVehType) {
		VehicleType matsimVehicleType = VehicleUtils.getFactory().createVehicleType(Id.create(jspritVehType.getTypeId(), VehicleType.class));
		matsimVehicleType.getCapacity().setWeightInTons(jspritVehType.getCapacityDimensions().get(0));
		matsimVehicleType.getCostInformation().setCostsPerMeter(jspritVehType.getVehicleCostParams().perDistanceUnit);
		matsimVehicleType.getCostInformation().setCostsPerSecond(jspritVehType.getVehicleCostParams().perTransportTimeUnit);
		matsimVehicleType.getCostInformation().setFixedCost(jspritVehType.getVehicleCostParams().fix);
		VehicleUtils.setCostsPerSecondInService(matsimVehicleType.getCostInformation(), jspritVehType.getVehicleCostParams().perServiceTimeUnit);
		VehicleUtils.setCostsPerSecondWaiting(matsimVehicleType.getCostInformation(), jspritVehType.getVehicleCostParams().perWaitingTimeUnit);
		matsimVehicleType.setMaximumVelocity(jspritVehType.getMaxVelocity());
		return matsimVehicleType;
	}

	/**
	 * Creates a jsprit {@link com.graphhopper.jsprit.core.problem.vehicle.VehicleType} from a MATSin
	 * {@link VehicleType}.
	 * @param matsimVehicleType The (MATSim) {@link VehicleType} that should be converted
	 * @return {@link com.graphhopper.jsprit.core.problem.vehicle.VehicleType} The jsprit VehicleType
	 */
	static com.graphhopper.jsprit.core.problem.vehicle.VehicleType createJspritVehicleType(VehicleType matsimVehicleType) {
		if (matsimVehicleType == null)
			throw new IllegalStateException("carrierVehicleType is null");
		VehicleTypeImpl.Builder jspritVehTypeBuilder = VehicleTypeImpl.Builder
				.newInstance(matsimVehicleType.getId().toString());
		if (matsimVehicleType.getCapacity().getVolumeInCubicMeters() < Double.MAX_VALUE) {
			throw new RuntimeException(
					"restrictions can currently only be set for \"other\".  not a big problem, but needs to be implemented.  "
							+ "kai/kai, sep'19");
		}
		if (matsimVehicleType.getCapacity().getWeightInTons() < Double.MAX_VALUE) {
			throw new RuntimeException(
					"restrictions can currently only be set for \"other\".  not a big problem, but needs to be implemented.  "
							+ "kai/kai, sep'19");
		}
		final double vehicleCapacity = matsimVehicleType.getCapacity().getOther();
		final int vehicleCapacityInt = (int) vehicleCapacity;
		if (vehicleCapacity - vehicleCapacityInt > 0) {
			log.warn("vehicle capacity truncated to int: before={}; after={}", vehicleCapacity, vehicleCapacityInt);
			// yyyyyy this implies that we would have fewer problems if we set vehicle
			// capacity in kg instead of in tons in our data model. kai, aug'19
		}
		jspritVehTypeBuilder.addCapacityDimension(0, vehicleCapacityInt);
		jspritVehTypeBuilder.setCostPerDistance(matsimVehicleType.getCostInformation().getCostsPerMeter());

		jspritVehTypeBuilder.setCostPerTransportTime(matsimVehicleType.getCostInformation().getCostsPerSecond());
		if (VehicleUtils.getCostsPerSecondInService(matsimVehicleType.getCostInformation()) != null) {
			jspritVehTypeBuilder.setCostPerServiceTime(VehicleUtils.getCostsPerSecondInService(matsimVehicleType.getCostInformation()));
		} else {
			log.info("'costsPerSecondInService' is not set in VehicleType attributes. Will use the value of 'costsPerSecond' instead. VehicleTypeId: {}", matsimVehicleType.getId());
			jspritVehTypeBuilder.setCostPerServiceTime(matsimVehicleType.getCostInformation().getCostsPerSecond());
		}
		if (VehicleUtils.getCostsPerSecondWaiting(matsimVehicleType.getCostInformation()) != null) {
			jspritVehTypeBuilder.setCostPerWaitingTime(VehicleUtils.getCostsPerSecondWaiting(matsimVehicleType.getCostInformation()));
		} else {
			log.info("'costsPerSecondWaiting' is not set in VehicleType attributes. Will use the value of 'costsPerSecond' instead. VehicleTypeId: {}", matsimVehicleType.getId());
			jspritVehTypeBuilder.setCostPerWaitingTime(matsimVehicleType.getCostInformation().getCostsPerSecond());
		}
		jspritVehTypeBuilder.setFixedCost(matsimVehicleType.getCostInformation().getFixedCosts());
		jspritVehTypeBuilder.setMaxVelocity(matsimVehicleType.getMaximumVelocity());

		//KMT Jan22 Store MATSimVehType here
		jspritVehTypeBuilder.setUserData(matsimVehicleType);

		return jspritVehTypeBuilder.build();
	}

	/**
	 * Creates a MATSim {@link ScheduledTour} from jsprit {@link VehicleRoute}.
	 *
	 * @param jspritRoute to be transformed
	 * @return ScheduledTour
	 * @throws IllegalStateException if tourActivity is NOT {@link ServiceActivity}.
	 */
	static ScheduledTour createScheduledTour(VehicleRoute jspritRoute, Id<Tour> tourId) {
		// have made this non-public for the time being since it is nowhere used within the freight contrib, and we might want to add the
		// vehicle types as argument.  If this is publicly used, please move back to public.  kai, jan'22

		if (jspritRoute.getDepartureTime() != jspritRoute.getStart().getEndTime())
			throw new AssertionError("at this point route.getDepartureTime and route.getStart().getEndTime() must be equal");

		TourActivities tour = jspritRoute.getTourActivities();
		CarrierVehicle carrierVehicle = createCarrierVehicle(jspritRoute.getVehicle());
		double depTime = jspritRoute.getStart().getEndTime();

		Tour.Builder matsimFreightTourBuilder = Tour.Builder.newInstance(tourId);
		matsimFreightTourBuilder.scheduleStart(Id.create(jspritRoute.getStart().getLocation().getId(), Link.class));
		for (TourActivity act : tour.getActivities()) {
			if (act instanceof ServiceActivity || act instanceof PickupService) {
				log.debug("Found ServiceActivity or PickupService : {} at location {} : {}", act.getName(), act.getLocation().getId(), act.getLocation().getCoordinate());
				Service job = (Service) ((JobActivity) act).getJob();
				CarrierService carrierService = createCarrierService(job);
				matsimFreightTourBuilder.addLeg(new Tour.Leg());
				matsimFreightTourBuilder.scheduleService(carrierService);
			} else if (act instanceof DeliverShipment) {
				log.debug("Found DeliveryShipment: {} at location {} : {}", act.getName(), act.getLocation().getId(), act.getLocation().getCoordinate());
				Shipment job = (Shipment) ((JobActivity) act).getJob();
				CarrierShipment carrierShipment = createCarrierShipment(job);
				matsimFreightTourBuilder.addLeg(new Tour.Leg());
				matsimFreightTourBuilder.scheduleDelivery(carrierShipment);
			} else if (act instanceof PickupShipment) {
				log.debug("Found PickupShipment: {} at location {} : {}", act.getName(), act.getLocation().getId(), act.getLocation().getCoordinate());
				Shipment job = (Shipment) ((JobActivity) act).getJob();
				CarrierShipment carrierShipment = createCarrierShipment(job);
				matsimFreightTourBuilder.addLeg(new Tour.Leg());
				matsimFreightTourBuilder.schedulePickup(carrierShipment);
			} else
				throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
		}
		matsimFreightTourBuilder.addLeg(new Tour.Leg());
		matsimFreightTourBuilder.scheduleEnd(Id.create(jspritRoute.getEnd().getLocation().getId(), Link.class));
		Tour matsimVehicleTour = matsimFreightTourBuilder.build();
		ScheduledTour sTour = ScheduledTour.newInstance(matsimVehicleTour, carrierVehicle, depTime);

		if (jspritRoute.getDepartureTime() != sTour.getDeparture())
			throw new AssertionError("departureTime of both route and scheduledTour must be equal");

		return sTour;
	}

	/**
	 * Creates a jsprit {@link VehicleRoute} from a MATSim {@link ScheduledTour}.
	 *
	 * <p>
	 * The {@link Network} is required to retrieve coordinates.
	 *
	 * @param scheduledTour         to be transformed
	 * @param vehicleRoutingProblem the routing problem
	 * @return VehicleRoute
	 */
	public static VehicleRoute createRoute(ScheduledTour scheduledTour, VehicleRoutingProblem vehicleRoutingProblem) {
		CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
		double depTime = scheduledTour.getDeparture();
		Tour tour = scheduledTour.getTour();
		Id<org.matsim.vehicles.Vehicle> vehicleId = carrierVehicle.getId();
		Vehicle jspritVehicle = null;
		for (Vehicle vehicle : vehicleRoutingProblem.getVehicles()) {
			if (vehicle.getId().equals(vehicleId.toString())) {
				jspritVehicle = vehicle;
				break;
			}
		}
		if (jspritVehicle == null)
			throw new IllegalStateException("jsprit-vehicle to id=" + vehicleId + " is missing");

		VehicleRoute.Builder jspritRouteBuilder = VehicleRoute.Builder.newInstance(jspritVehicle);
		jspritRouteBuilder.setJobActivityFactory(vehicleRoutingProblem.getJobActivityFactory());
		jspritRouteBuilder.setDepartureTime(depTime);

		for (Tour.TourElement e : tour.getTourElements()) {
			if (e instanceof Tour.TourActivity) {
				if (e instanceof Tour.ServiceActivity) {
					CarrierService carrierService = ((Tour.ServiceActivity) e)
							.getService();
					Service service = (Service) vehicleRoutingProblem.getJobs().get(carrierService.getId().toString());
					if (service == null)
						throw new IllegalStateException("service to id=" + carrierService.getId() + " is missing");
					jspritRouteBuilder.addService(service);
				}
			}
		}
		VehicleRoute jspritRoute = jspritRouteBuilder.build();
		log.debug("jsprit route: {}", jspritRoute);
		log.debug("start-location: {} endTime: {}({})", jspritRoute.getStart().getLocation(), jspritRoute.getDepartureTime(), jspritRoute.getStart().getEndTime());
		for (TourActivity act : jspritRoute.getActivities()) {
			log.debug("act: {}", act);
		}
		log.debug("end: {}", jspritRoute.getEnd());
		if (jspritRoute.getDepartureTime() != scheduledTour.getDeparture())
			throw new AssertionError("departureTimes of both routes must be equal");
		return jspritRoute;
	}

	/**
	 * Creates an immutable {@link VehicleRoutingProblem} from {@link Carrier}.
	 *
	 * <p>
	 * For creation, it takes only the information needed to set up the problem (not
	 * the solution, i.e. predefined plans are ignored).
	 * <p>
	 * The network is required to retrieve coordinates of locations.
	 * <p>
	 * Note that currently only services ({@link Service}) and Shipments
	 * ({@link Shipment}) are supported.
	 * <p>
	 * Pickups and deliveries can be defined as shipments with only one location
	 * (toLocation for delivery and fromLocation for pickup). Implementation follows
	 */
	public static VehicleRoutingProblem createRoutingProblem(Carrier carrier, Network network,
			VehicleRoutingTransportCosts transportCosts, VehicleRoutingActivityCosts activityCosts) {
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		boolean serviceInVrp = !carrier.getServices().isEmpty();
		boolean shipmentInVrp = !carrier.getShipments().isEmpty();

		if (shipmentInVrp && serviceInVrp) {
			throw new UnsupportedOperationException(
					"VRP with mixed Services and Shipments may lead to invalid solutions because of vehicle capacity handling are different");
		}

		FleetSize fleetSize;
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		if (carrierCapabilities.getFleetSize()
				.equals(CarrierCapabilities.FleetSize.INFINITE)) {
			fleetSize = FleetSize.INFINITE;
			vrpBuilder.setFleetSize(fleetSize);
		} else {
			fleetSize = FleetSize.FINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		for (CarrierVehicle carrierVehicle : carrierCapabilities.getCarrierVehicles().values()) {
			Coord coordinate = null;
			if (network != null) {
				Link link = network.getLinks().get(carrierVehicle.getLinkId() );
				if (link == null)
					throw new IllegalStateException("vehicle.locationId cannot be found in network [vehicleId="
							+ carrierVehicle.getId() + "][locationId=" + carrierVehicle.getLinkId() + "]");
				coordinate = link.getCoord();
			} else
				log.warn("cannot find linkId {}", carrierVehicle.getId());
			Vehicle veh = createJspritVehicle(carrierVehicle, coordinate);

			if (veh.getEarliestDeparture() != carrierVehicle.getEarliestStartTime())
				throw new AssertionError("earliestDeparture of both vehicles must be equal");
			if (veh.getLatestArrival() != carrierVehicle.getLatestEndTime())
				throw new AssertionError("latestArrTime of both vehicles must be equal");

			vrpBuilder.addVehicle(veh);
		}


		if (serviceInVrp) {
			for (CarrierService service : carrier.getServices().values()) {

				Coord coordinate = null;
				if (network != null) {
					Link link = network.getLinks().get(service.getServiceLinkId());
					if (link != null) {
						coordinate = link.getCoord();
					} else
						log.warn("cannot find linkId {}", service.getServiceLinkId());
				}
				vrpBuilder.addJob(createJspritService(service, coordinate));
			}
		}

		if (shipmentInVrp) {
			for (CarrierShipment carrierShipment : carrier.getShipments().values()) {
				Coord fromCoordinate = null;
				Coord toCoordinate = null;
				if (network != null) {
					Link fromLink = network.getLinks().get(carrierShipment.getPickupLinkId());
					Link toLink = network.getLinks().get(carrierShipment.getDeliveryLinkId());

					if (fromLink != null && toLink != null) { // Shipment to be delivered from specified location to
						// specified location
						fromCoordinate = fromLink.getCoord();
						toCoordinate = toLink.getCoord();
						vrpBuilder.addJob(createJspritShipment(carrierShipment, fromCoordinate, toCoordinate));
					} else
						throw new IllegalStateException(
								"cannot create shipment since neither fromLinkId " + carrierShipment.getDeliveryLinkId()
										+ " nor toLinkId " + carrierShipment.getDeliveryLinkId() + " exists in network.");

				}
				vrpBuilder.addJob(createJspritShipment(carrierShipment, fromCoordinate, toCoordinate));
			}
		}

		if (transportCosts != null)
			vrpBuilder.setRoutingCost(transportCosts);
		if (activityCosts != null)
			vrpBuilder.setActivityCosts(activityCosts);
		return vrpBuilder.build();
	}

	/**
	 * Creates {@link VehicleRoutingProblem.Builder} from {@link Carrier} for later
	 * building of the {@link VehicleRoutingProblem}. This is required if you need
	 * to add stuff to the problem later, e.g. because it cannot solely be retrieved
	 * from network and carrier such as {@link NetworkBasedTransportCosts}.
	 *
	 * <p>
	 * For creation, it takes only the information needed to set up the problem (not
	 * the solution, i.e. predefined plans are ignored).
	 * <p>
	 * The network is required to retrieve coordinates of locations.
	 * <p>
	 * Note that currently only services ({@link Service}) and Shipments
	 * ({@link Shipment}) are supported.
	 * <p>
	 * Pickups and deliveries can be defined as shipments with only one location
	 * (toLocation for delivery and fromLocation for pickup). Implementation follows
	 */
	public static VehicleRoutingProblem.Builder createRoutingProblemBuilder(Carrier carrier, Network network) {
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		boolean serviceInVrp = !carrier.getServices().isEmpty();
		boolean shipmentInVrp = !carrier.getShipments().isEmpty();

		if (shipmentInVrp && serviceInVrp) {
			throw new UnsupportedOperationException(
					"VRP with mixed Services and Shipments may lead to invalid solutions because of vehicle capacity handling are different");
		}

		FleetSize fleetSize;
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		if (carrierCapabilities.getFleetSize()
				.equals(CarrierCapabilities.FleetSize.INFINITE)) {
			fleetSize = FleetSize.INFINITE;
			vrpBuilder.setFleetSize(fleetSize);
		} else {
			fleetSize = FleetSize.FINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		for (CarrierVehicle carrierVehicle : carrierCapabilities.getCarrierVehicles().values()) {
			Coord coordinate = null;
			if (network != null) {
				Link link = network.getLinks().get(carrierVehicle.getLinkId() );
				if (link == null)
					throw new IllegalStateException("vehicle.locationId cannot be found in network [vehicleId="
							+ carrierVehicle.getId() + "][locationId=" + carrierVehicle.getLinkId() + "]");
				coordinate = link.getCoord();
			} else
				log.warn("cannot find linkId {}", carrierVehicle.getId());
			vrpBuilder.addVehicle(createJspritVehicle(carrierVehicle, coordinate));
		}

		if (serviceInVrp) {
			for (CarrierService service : carrier.getServices().values()) {
				log.debug("Handle CarrierService: {}", service.toString());
				Coord coordinate = null;
				if (network != null) {
					Link link = network.getLinks().get(service.getServiceLinkId());
					if (link == null) {
						throw new IllegalStateException("cannot create service since linkId " + service.getServiceLinkId()
								+ " does not exists in network.");
					} else
						coordinate = link.getCoord();
				}
				vrpBuilder.addJob(createJspritService(service, coordinate));
			}
		}

		if (shipmentInVrp) {
			for (CarrierShipment carrierShipment : carrier.getShipments().values()) {
				log.debug("Handle CarrierShipment: {}", carrierShipment.toString());
				Coord fromCoordinate = null;
				Coord toCoordinate = null;
				if (network != null) {
					Link fromLink = network.getLinks().get(carrierShipment.getPickupLinkId());
					Link toLink = network.getLinks().get(carrierShipment.getDeliveryLinkId());

					if (fromLink != null && toLink != null) { // Shipment to be delivered from specified location to
						// specified location
						log.debug("Shipment identified as Shipment: {}", carrierShipment.getId().toString());
						fromCoordinate = fromLink.getCoord();
						toCoordinate = toLink.getCoord();
					} else
						throw new IllegalStateException("cannot create shipment " + carrierShipment.getId().toString()
								+ " since either fromLinkId " + carrierShipment.getPickupLinkId() + " or toLinkId "
								+ carrierShipment.getDeliveryLinkId() + " exists in network.");

				}
				vrpBuilder.addJob(createJspritShipment(carrierShipment, fromCoordinate, toCoordinate));
			}
		}

		return vrpBuilder;
	}

	/**
	 * Creates a {@link VehicleRoutingProblemSolution} from {@link CarrierPlan}.
	 *
	 * <p>
	 * To retrieve coordinates the {@link Network} is required. </br>
	 */
	public static VehicleRoutingProblemSolution createSolution(CarrierPlan plan,
			VehicleRoutingProblem vehicleRoutingProblem) {
		List<VehicleRoute> routes = new ArrayList<>();
		for (ScheduledTour tour : plan.getScheduledTours()) {
			VehicleRoute route = createRoute(tour, vehicleRoutingProblem);
			routes.add(route);
		}
		double costs;
		if (plan.getScore() == null)
			costs = -9999.0;
		else
			costs = plan.getScore() * -1.0;
		return new VehicleRoutingProblemSolution(routes, costs);
	}

	/**
	 * Creates a {@link Carrier} from {@link VehicleRoutingProblem}. The carrier is
	 * initialized with the carrierId, i.e. <code>carrier.getId()</code> returns
	 * carrierId. </br>
	 */
	public static Carrier createCarrier(String carrierId, VehicleRoutingProblem vrp) {
		Id<Carrier> id = Id.create(carrierId, Carrier.class);
		Carrier carrier = CarriersUtils.createCarrier(id);
		CarrierCapabilities.Builder capabilityBuilder = CarrierCapabilities.Builder.newInstance();

		// fleet and vehicles
		if (vrp.getFleetSize().equals(FleetSize.FINITE)) {
			capabilityBuilder.setFleetSize(CarrierCapabilities.FleetSize.FINITE);
		} else
			capabilityBuilder.setFleetSize(CarrierCapabilities.FleetSize.INFINITE);
		for (Vehicle vehicle : vrp.getVehicles()) {
			capabilityBuilder.addVehicle(createCarrierVehicle(vehicle));
		}
		carrier.setCarrierCapabilities(capabilityBuilder.build());

		return carrier;
	}

	/**
	 * Creates a {@link CarrierPlan} from {@link VehicleRoutingProblemSolution}.
	 *
	 * <p>
	 * Note that the costs of the solution are multiplied by -1 to represent the
	 * corresponding score of a carrierPlan.
	 * <p>
	 * The input parameter {@link Carrier} is just required to initialize the plan.
	 * </br>
	 */
	public static CarrierPlan createPlan(Carrier carrier, VehicleRoutingProblemSolution solution) {
		Collection<ScheduledTour> tours = new ArrayList<>();
		int tourIdIndex = 1;
		for (VehicleRoute route : solution.getRoutes()) {
			ScheduledTour scheduledTour = createScheduledTour(route, Id.create(tourIdIndex, Tour.class));
			tourIdIndex++;
			tours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(carrier, tours);
		carrierPlan.setJspritScore(solution.getCost() * (-1));
		return carrierPlan;
	}

	/**
	 * Creates or load the VehicleRoutingAlgorithm. If an algorithmFile is given the
	 * algorithm will be load. If there is no algorithm given a new algorithm will
	 * be created. In both situation the distanceConstraint will be used if he is
	 * set in the freightConfigGroup.
	 */
	public static VehicleRoutingAlgorithm loadOrCreateVehicleRoutingAlgorithm(Scenario scenario,
																			  FreightCarriersConfigGroup freightConfig, NetworkBasedTransportCosts netBasedCosts, VehicleRoutingProblem problem) {
		VehicleRoutingAlgorithm algorithm;
		final String vehicleRoutingAlgorithmFile = freightConfig.getVehicleRoutingAlgorithmFile();

		if (vehicleRoutingAlgorithmFile != null && !vehicleRoutingAlgorithmFile.isEmpty()) {
			log.info("Will read in VehicleRoutingAlgorithm from {}", vehicleRoutingAlgorithmFile);
			URL vraURL;
			try {
				vraURL = IOUtils.extendUrl(scenario.getConfig().getContext(), vehicleRoutingAlgorithmFile);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			switch (freightConfig.getUseDistanceConstraintForTourPlanning()) {
				case noDistanceConstraint -> algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, vraURL);
				case basedOnEnergyConsumption -> {
					log.info("Use the distanceConstraint based on energy consumption.");
					StateManager stateManager = new StateManager(problem);
					StateId distanceStateId = stateManager.createStateId("distance");
					stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, netBasedCosts));
					ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
					constraintManager.addConstraint(
							new DistanceConstraint(
									CarriersUtils.getCarrierVehicleTypes(scenario), netBasedCosts),
							ConstraintManager.Priority.CRITICAL);
					AlgorithmConfig algorithmConfig = new AlgorithmConfig();
					AlgorithmConfigXmlReader xmlReader = new AlgorithmConfigXmlReader(algorithmConfig);
					xmlReader.read(vraURL);
					algorithm = VehicleRoutingAlgorithms.readAndCreateAlgorithm(problem, algorithmConfig, 0, null,
							stateManager, constraintManager, true);
				}
				default -> throw new IllegalStateException(
						"Unexpected value: " + freightConfig.getUseDistanceConstraintForTourPlanning());
			}

		} else {
			log.info("Use a VehicleRoutingAlgorithm out of the box.");
			switch (freightConfig.getUseDistanceConstraintForTourPlanning()) {
				case noDistanceConstraint -> algorithm = new SchrimpfFactory().createAlgorithm(problem);
				case basedOnEnergyConsumption -> {
					log.info("Use the distanceConstraint based on energy consumption.");
					StateManager stateManager = new StateManager(problem);
					StateId distanceStateId = stateManager.createStateId("distance");
					stateManager.addStateUpdater(new DistanceUpdater(distanceStateId, stateManager, netBasedCosts));
					ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
					constraintManager.addConstraint(
							new DistanceConstraint(
									CarriersUtils.getCarrierVehicleTypes(scenario), netBasedCosts),
							ConstraintManager.Priority.CRITICAL);
					algorithm = Jsprit.Builder.newInstance(problem)
							.setStateAndConstraintManager(stateManager, constraintManager).buildAlgorithm();
				}
				default -> throw new IllegalStateException(
						"Unexpected value: " + freightConfig.getUseDistanceConstraintForTourPlanning());
			}
		}
		return algorithm;
	}
}
