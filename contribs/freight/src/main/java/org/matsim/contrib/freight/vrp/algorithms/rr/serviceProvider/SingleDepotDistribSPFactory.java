package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;

class SingleDepotDistribSPFactory implements ServiceProviderAgentFactory{
	
	private VehicleRoutingCosts vehicleRoutingCost;
	
	private TourCost tourCost;
	
	SingleDepotDistribSPFactory(TourCost tourCost, VehicleRoutingCosts vehicleRoutingCost) {
		super();
		this.tourCost = tourCost;
		this.vehicleRoutingCost = vehicleRoutingCost;
	}

	@Override
	public ServiceProviderAgent createAgent(Vehicle vehicle, Driver driver, Tour tour) {
		RRDriverAgent a = new RRDriverAgent(vehicle, driver, tour);
		a.setTourTimeWindowsAndCostUpdater(new TourCostProcessor(vehicleRoutingCost));
		a.setTourCost(tourCost);
		SingleDepotDistributionLeastCostTourCalculator bestJobInsertionFinder = new SingleDepotDistributionLeastCostTourCalculator(vehicleRoutingCost);
		bestJobInsertionFinder.setMarginalCostCalculator(new LocalMCCalculator(vehicleRoutingCost));
		a.setBestJobInsertionFinder(bestJobInsertionFinder);
		return a;
	}

	@Override
	public ServiceProviderAgent createAgent(Vehicle vehicle, Driver driver) {
		VrpTourBuilder vrpTourBuilder = new VrpTourBuilder();
		vrpTourBuilder.scheduleStart(vehicle.getLocationId(), vehicle.getEarliestDeparture(), Double.MAX_VALUE);
		vrpTourBuilder.scheduleEnd(vehicle.getLocationId(), 0.0, vehicle.getLatestArrival());
		Tour tour = vrpTourBuilder.build();
		return createAgent(vehicle,driver,tour);
	}

	@Override
	public ServiceProviderAgent createAgent(ServiceProviderAgent agent) {
		Tour tourCopy = new Tour(agent.getTour());
		return createAgent(agent.getVehicle(),agent.getDriver(),tourCopy);
	}

}
