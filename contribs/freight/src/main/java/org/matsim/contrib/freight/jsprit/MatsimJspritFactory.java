/*******************************************************************************
 * Copyright (c) 2011 Stefan Schroeder.
 * eMail: stefan.schroeder@kit.edu
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan Schroeder - initial API and implementation
 ******************************************************************************/
package org.matsim.contrib.freight.jsprit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierShipment;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.carrier.Tour;
import org.matsim.contrib.freight.carrier.Tour.Leg;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.core.basic.v01.IdImpl;

import util.Coordinate;
import basics.Service;
import basics.Service.Builder;
import basics.VehicleRoutingProblem;
import basics.VehicleRoutingProblem.FleetSize;
import basics.VehicleRoutingProblemSolution;
import basics.costs.VehicleRoutingActivityCosts;
import basics.costs.VehicleRoutingTransportCosts;
import basics.route.End;
import basics.route.ServiceActivity;
import basics.route.Start;
import basics.route.TourActivity;
import basics.route.Vehicle;
import basics.route.VehicleImpl;
import basics.route.VehicleRoute;
import basics.route.VehicleType;
import basics.route.VehicleTypeImpl;


/**
 * A factory that creates matsim-object from jsprit (https://github.com/jsprit/jsprit) and vice versa.
 * 
 * @author sschroeder
 *
 */
public class MatsimJspritFactory {

	private static Logger log = Logger.getLogger(MatsimJspritFactory.class);
	
	private static Id makeId(String id) {
		return new IdImpl(id);
	}
	
	static CarrierShipment createCarrierShipment(Service service, String depotLink){
		CarrierShipment carrierShipment = CarrierShipment.Builder.newInstance(makeId(depotLink), makeId(service.getLocationId()), service.getCapacityDemand()).
				setDeliveryServiceTime(service.getServiceDuration()).
				setDeliveryTimeWindow(TimeWindow.newInstance(service.getTimeWindow().getStart(),service.getTimeWindow().getEnd())).build();
		return carrierShipment;
	}
	
	static Service createService(CarrierService carrierService, Coord locationCoord) {
		Builder serviceBuilder = Service.Builder.newInstance(carrierService.getId().toString(), carrierService.getCapacityDemand());
		serviceBuilder.setLocationId(carrierService.getLocationLinkId().toString()).setServiceTime(carrierService.getServiceDuration())
			.setTimeWindow(basics.route.TimeWindow.newInstance(carrierService.getServiceStartTimeWindow().getStart(), carrierService.getServiceStartTimeWindow().getEnd()));
		if(locationCoord != null){
			serviceBuilder.setCoord(Coordinate.newInstance(locationCoord.getX(), locationCoord.getY()));
		}
		Service service = serviceBuilder.build();
		return service;
	}
	
	static CarrierService createCarrierService(Service service) {
		CarrierService.Builder serviceBuilder = CarrierService.Builder.newInstance(makeId(service.getId()), makeId(service.getLocationId()));
		serviceBuilder.setCapacityDemand(service.getCapacityDemand());
		serviceBuilder.setServiceDuration(service.getServiceDuration());
		serviceBuilder.setServiceStartTimeWindow(TimeWindow.newInstance(service.getTimeWindow().getStart(), service.getTimeWindow().getEnd()));
		CarrierService carrierService = serviceBuilder.build();
		return carrierService;

	}
	
	/**
	 * Creates jsprit-vehicle from a matsim-carrier-vehicle.
	 * 
	 * @param carrierVehicle
	 * @param locationCoord
	 * @return jsprit vehicle
	 * @see Vehicle, CarrierVehicle
	 */
	static Vehicle createVehicle(CarrierVehicle carrierVehicle, Coord locationCoord){
		VehicleType vehicleType = createVehicleType(carrierVehicle.getVehicleType());
		VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(carrierVehicle.getVehicleId().toString());
		vehicleBuilder.setEarliestStart(carrierVehicle.getEarliestStartTime())
		.setLatestArrival(carrierVehicle.getLatestEndTime())
		.setLocationId(carrierVehicle.getLocation().toString())
		.setType(vehicleType);
		if(locationCoord != null){
			vehicleBuilder.setLocationCoord(Coordinate.newInstance(locationCoord.getX(), locationCoord.getY()));
		}
		VehicleImpl vehicle = vehicleBuilder.build();
		return vehicle;
	}
	
	/**
	 * Creates {@link CarrierVehicle} from a {basics.route.Vehicle}
	 * 
	 * @param vehicle
	 * @return carrierVehicle
	 * @see CarrierVehicle, Vehicle
	 */
	static CarrierVehicle createCarrierVehicle(Vehicle vehicle){
		String vehicleId = vehicle.getId();
		CarrierVehicle.Builder vehicleBuilder = CarrierVehicle.Builder.newInstance(makeId(vehicleId), makeId(vehicle.getLocationId()));
		CarrierVehicleType carrierVehicleType = createCarrierVehicleType(vehicle.getType());
		vehicleBuilder.setType(carrierVehicleType);
		vehicleBuilder.setEarliestStart(vehicle.getEarliestDeparture());
		vehicleBuilder.setLatestEnd(vehicle.getLatestArrival());
		CarrierVehicle carrierVehicle = vehicleBuilder.build();
		return carrierVehicle;
	}
	
	/**
	 * Creates {@link CarrierVehicleType} from {@link VehicleType}.
	 * 
	 * <p>No description and engineInformation can be set here. Do it by calling setEngineInforation(engineInfo) from the returned 
	 * object. 
	 * 
	 * @param type
	 * @return CarrierVehicleType
	 */
	static CarrierVehicleType createCarrierVehicleType(VehicleType type){
		CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(makeId(type.getTypeId()));
		typeBuilder.setCapacity(type.getCapacity());
		typeBuilder.setCostPerDistanceUnit(type.getVehicleCostParams().perDistanceUnit).setCostPerTimeUnit(type.getVehicleCostParams().perTimeUnit)
		.setFixCost(type.getVehicleCostParams().fix);
		typeBuilder.setMaxVelocity(type.getMaxVelocity());
		CarrierVehicleType carrierVehicleType = typeBuilder.build();
		return carrierVehicleType;
	}
	
	/**
	 * Creates {@link VehicleType} from {@link CarrierVehicleType}.

	 */
	static VehicleType createVehicleType(CarrierVehicleType carrierVehicleType){
		if(carrierVehicleType == null) throw new IllegalStateException("carrierVehicleType is null");
		VehicleTypeImpl.Builder typeBuilder = VehicleTypeImpl.Builder.newInstance(carrierVehicleType.getId().toString(), carrierVehicleType.getCarrierVehicleCapacity());
		typeBuilder.setCostPerDistance(carrierVehicleType.getVehicleCostInformation().perDistanceUnit);
		typeBuilder.setCostPerTime(carrierVehicleType.getVehicleCostInformation().perTimeUnit);
		typeBuilder.setFixedCost(carrierVehicleType.getVehicleCostInformation().fix);
		typeBuilder.setMaxVelocity(carrierVehicleType.getMaximumVelocity());
		VehicleType type = typeBuilder.build();
		return type;
	}

	/**
	 * Creates {@link ScheduledTour} from {@link VehicleRoute}.
	 * 
	 * @param route
	 * @return ScheduledTour
	 * @throws IllegalStateException if tourActivity is NOT {@link ServiceActivity}.
	 */
	public static ScheduledTour createTour(VehicleRoute route) {
		basics.route.TourActivities tour = route.getTourActivities();
		CarrierVehicle carrierVehicle = createCarrierVehicle(route.getVehicle());
		double depTime = route.getStart().getEndTime();
		Tour.Builder tourBuilder = Tour.Builder.newInstance();
		tourBuilder.scheduleStart(makeId(route.getStart().getLocationId()));
		for (TourActivity act : tour.getActivities()) {
			if(act instanceof ServiceActivity){
				Service job = ((ServiceActivity) act).getJob();				 
				CarrierService carrierService = createCarrierService(job);
				tourBuilder.addLeg(new Leg());
				tourBuilder.scheduleService(carrierService);
			}
			else {
				throw new IllegalStateException("unknown tourActivity occurred. this cannot be");
			}
		}
		tourBuilder.addLeg(new Leg());
		tourBuilder.scheduleEnd(makeId(route.getEnd().getLocationId()));
		org.matsim.contrib.freight.carrier.Tour vehicleTour = tourBuilder.build();
		ScheduledTour sTour = ScheduledTour.newInstance(vehicleTour, carrierVehicle, depTime);
		return sTour;
	}
	
	private static Coord findCoord(Id linkId, Network network) {
		if(network == null) return null;
		Link l = network.getLinks().get(linkId);
		if(l == null) throw new IllegalStateException("link to linkId " + linkId + " is missing.");
		return l.getCoord();
	}
	
	
	/**
	 * Creates {@link VehicleRoute} from {@link ScheduledTour}.
	 * 
	 * <p>The {@link Network} is required to retrieve coordinates.
	 * 
	 * @param scheduledTour
	 * @param network
	 * @return VehicleRoute
	 */
	public static VehicleRoute createRoute(ScheduledTour scheduledTour, Network network){
		CarrierVehicle carrierVehicle = scheduledTour.getVehicle();
		double depTime = scheduledTour.getDeparture();
		Tour tour = scheduledTour.getTour();
			
		Vehicle jspritVehicle = createVehicle(carrierVehicle, findCoord(carrierVehicle.getLocation(), network));
		
		Start start = Start.newInstance(tour.getStartLinkId().toString(), carrierVehicle.getEarliestStartTime(), carrierVehicle.getLatestEndTime());
		start.setEndTime(depTime);
		End end = End.newInstance(tour.getEndLinkId().toString(), carrierVehicle.getEarliestStartTime(), carrierVehicle.getLatestEndTime());
		
		basics.route.VehicleRoute.Builder routeBuilder = VehicleRoute.Builder.newInstance(start, end);
		routeBuilder.setVehicle(jspritVehicle);
		
		for(TourElement e : tour.getTourElements()){
			if(e instanceof org.matsim.contrib.freight.carrier.Tour.TourActivity){
				if(e instanceof org.matsim.contrib.freight.carrier.Tour.ServiceActivity){
					CarrierService carrierService = ((org.matsim.contrib.freight.carrier.Tour.ServiceActivity) e).getService();
					Service service = createService(carrierService, findCoord(carrierService.getLocationLinkId(), network));
					ServiceActivity serviceAct = ServiceActivity.newInstance(service);
					routeBuilder.addActivity(serviceAct);
				}
			}
		}
		return routeBuilder.build();
	}

	/**
	 * Creates an immutable {@link VehicleRoutingProblem} from {@link Carrier}.
	 * 
	 * <p>For creation it takes only the information needed to setup the problem (not the solution, i.e. predefined plans are ignored). 
	 * <p>The network is required to retrieve coordinates of locations.
	 * <p>Note that currently only services ({@link Service}) are supported.
	 * 
	 * @param {@link Carrier}
	 * @param {@link Network}
	 * @param {@link VehicleRoutingTransportCosts}
	 * @param {@link VehicleRoutingActivityCosts}
	 * @return {@link VehicleRoutingProblem}
	 * @throws IllegalStateException if shipments are involved.
	 */
	public static VehicleRoutingProblem createRoutingProblem(Carrier carrier, Network network, VehicleRoutingTransportCosts transportCosts, VehicleRoutingActivityCosts activityCosts){
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		FleetSize fleetSize; 
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		if(carrierCapabilities.getFleetSize().equals(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.INFINITE)){
			fleetSize = FleetSize.INFINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		else{
			fleetSize = FleetSize.FINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		for(CarrierVehicleType vehicleType : carrierCapabilities.getVehicleTypes()){
			vrpBuilder.addVehicleType(createVehicleType(vehicleType));
		}
		for(CarrierVehicle v : carrierCapabilities.getCarrierVehicles()){
			Coord coordinate = null;
			if(network != null) {
				Link link = network.getLinks().get(v.getLocation());
				if(link == null) throw new IllegalStateException("vehicle.locationId cannot be found in network [vehicleId=" + v.getVehicleId() + "][locationId=" + v.getLocation() + "]");
				coordinate = link.getCoord();
			} else log.warn("cannot find linkId " + v.getVehicleId());
			vrpBuilder.addVehicle(createVehicle(v, coordinate));
		}
		
		for(CarrierService service : carrier.getServices()){
			Coord coordinate = null;
			if(network != null){
				Link link = network.getLinks().get(service.getLocationLinkId());
				if(link != null) {
					coordinate = link.getCoord();
				}
				else log.warn("cannot find linkId " + service.getLocationLinkId());
			}
			vrpBuilder.addService(createService(service, coordinate));
		}
		
		for(CarrierShipment s : carrier.getShipments()) throw new IllegalStateException("this is not supported yet.");
		
		if(transportCosts != null) vrpBuilder.setRoutingCost(transportCosts);
		if(activityCosts != null) vrpBuilder.setActivityCosts(activityCosts);
		VehicleRoutingProblem vrp = vrpBuilder.build();
		return vrp;
	}
	
	/**
	 * Creates {@link VehicleRoutingProblem.Builder} from {@link Carrier} for later building of the {@link VehicleRoutingProblem}. This is required if you need to
	 * add stuff to the problem later, e.g. because it cannot solely be retrieved from network and carrier such as {@link NetworkBasedTransportCosts}.
	 * 
	 * <p>For creation it takes only the information needed to setup the problem (not the solution, i.e. predefined plans are ignored). 
	 * <p>The network is required to retrieve coordinates of locations.
	 * <p>Note that currently only services ({@link Service}) are supported.
	 * 
	 * @param {@link Carrier}
	 * @param {@link Network}
	 * @return {@link VehicleRoutingProblem.Builder}
	 * @throws IllegalStateException if shipments are involved.
	 */
	public static VehicleRoutingProblem.Builder createRoutingProblemBuilder(Carrier carrier, Network network){
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		FleetSize fleetSize; 
		CarrierCapabilities carrierCapabilities = carrier.getCarrierCapabilities();
		if(carrierCapabilities.getFleetSize().equals(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.INFINITE)){
			fleetSize = FleetSize.INFINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		else{
			fleetSize = FleetSize.FINITE;
			vrpBuilder.setFleetSize(fleetSize);
		}
		for(CarrierVehicleType vehicleType : carrierCapabilities.getVehicleTypes()){
			vrpBuilder.addVehicleType(createVehicleType(vehicleType));
		}
		for(CarrierVehicle v : carrierCapabilities.getCarrierVehicles()){
			Coord coordinate = null;
			if(network != null) {
				Link link = network.getLinks().get(v.getLocation());
				if(link == null) throw new IllegalStateException("vehicle.locationId cannot be found in network [vehicleId=" + v.getVehicleId() + "][locationId=" + v.getLocation() + "]");
				coordinate = link.getCoord();
			} else log.warn("cannot find linkId " + v.getVehicleId());
			vrpBuilder.addVehicle(createVehicle(v, coordinate));
		}
		
		for(CarrierService service : carrier.getServices()){
			Coord coordinate = null;
			if(network != null){
				Link link = network.getLinks().get(service.getLocationLinkId());
				if(link == null) {
					throw new IllegalStateException("cannot create service since linkId " + service.getLocationLinkId() + " does not exists in network.");
				}
				else coordinate = link.getCoord();
			}
			vrpBuilder.addService(createService(service, coordinate));
		}
		
		for(CarrierShipment s : carrier.getShipments()) throw new IllegalStateException("this is not supported yet.");
		
		return vrpBuilder;
	}
	
	/**
	 * Creates a {@link VehicleRoutingProblemSolution} from {@link CarrierPlan}.
	 * 
	 * <p>To retrieve coordinates the {@link Network} is required.
	 * </br>
	 * @param {@link CarrierPlan}
	 * @param {@link Network}
	 * @return {@link VehicleRoutingProblemSolution}
	 */
	public static VehicleRoutingProblemSolution createSolution(CarrierPlan plan, Network network) {
		List<VehicleRoute> routes = new ArrayList<VehicleRoute>();
		for(ScheduledTour tour : plan.getScheduledTours()){
			VehicleRoute route = createRoute(tour,network);
			routes.add(route);
		}
		double costs;
		if(plan.getScore() == null) costs = VehicleRoutingProblemSolution.NO_COST_YET;
		else costs = plan.getScore() * -1.0;
		return new VehicleRoutingProblemSolution(routes, costs);
	}

	/**
	 * Creates a {@link Carrier} from {@link VehicleRoutingProblem}. The carrier is initialized with the carrierId, i.e. <code>carrier.getId()</code> returns carrierId.
	 * </br>
	 * @param carrierId
	 * @param {@link VehicleRoutingProblem}
	 * @return {@link Carrier}
	 */
	public static Carrier createCarrier(String carrierId, VehicleRoutingProblem vrp){
		Id id = makeId(carrierId);
		Carrier carrier = CarrierImpl.newInstance(id);
		CarrierCapabilities.Builder capabilityBuilder = CarrierCapabilities.Builder.newInstance();

		//fleet and vehicles
		if(vrp.getFleetSize().equals(FleetSize.FINITE)){
			capabilityBuilder.setFleetSize(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.FINITE);
		}
		else capabilityBuilder.setFleetSize(org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize.INFINITE);
		for(VehicleType type : vrp.getTypes()){
			capabilityBuilder.addType(createCarrierVehicleType(type));
		}
		for(Vehicle vehicle : vrp.getVehicles()){
			capabilityBuilder.addVehicle(createCarrierVehicle(vehicle));
		}
		carrier.setCarrierCapabilities(capabilityBuilder.build());
		
		return carrier;
	}
	
	/**
	 * Creates a {@link CarrierPlan} from {@link VehicleRoutingProblemSolution}.
	 * 
	 * <p>Note that the costs of the solution are multiplied by -1 to represent the corresponding score of a carrierPlan.
	 * <p>The input parameter {@link Carrier} is just required to initialize the plan. 
	 * </br>
	 * @param {@link Carrier}
	 * @param {@link VehicleRoutingProblemSolution}
	 * @return {@link CarrierPlan}
	 */
	public static CarrierPlan createPlan(Carrier carrier, VehicleRoutingProblemSolution solution){
		Collection<ScheduledTour> tours = new ArrayList<ScheduledTour>();
		for(VehicleRoute route : solution.getRoutes()){
			ScheduledTour scheduledTour = createTour(route);
			tours.add(scheduledTour);
		}
		CarrierPlan carrierPlan = new CarrierPlan(carrier, tours);
		carrierPlan.setScore(solution.getCost()*(-1));
		return carrierPlan;
	}
	
}
