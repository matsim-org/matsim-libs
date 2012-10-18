package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;

public class ServiceDistribTWSPFactory implements ServiceProviderAgentFactory{

	private VehicleRoutingCosts vehicleRoutingCost;
	
	private TourCost tourCost;
	
	public ServiceDistribTWSPFactory(TourCost tourCost, VehicleRoutingCosts vehicleRoutingCost) {
		super();
		this.tourCost = tourCost;
		this.vehicleRoutingCost = vehicleRoutingCost;
	}
	
	@Override
	public ServiceProviderAgent createAgent(Vehicle vehicle, Driver driver, TourImpl tour) {
		RRDriverAgent a = new RRDriverAgent(vehicle, driver, tour);
		a.setTourTimeWindowsAndCostUpdater(new TourCostAndTWProcessor(vehicleRoutingCost));
		a.setTourCost(tourCost);
		ServiceDistributionLeastCostTourCalculator bestJobInsertionFinder = new ServiceDistributionLeastCostTourCalculator();
		bestJobInsertionFinder.setMarginalCostCalculator(new LocalMCCalculator(vehicleRoutingCost));
		a.setLeastCostTourCalculator(bestJobInsertionFinder);
		return a;
	}

	@Override
	public ServiceProviderAgent createAgent(Vehicle vehicle, Driver driver) {
		VrpTourBuilder vrpTourBuilder = new VrpTourBuilder();
		vrpTourBuilder.scheduleStart(vehicle.getLocationId(), vehicle.getEarliestDeparture(), Double.MAX_VALUE);
		vrpTourBuilder.scheduleEnd(vehicle.getLocationId(), 0.0, vehicle.getLatestArrival());
		TourImpl tour = vrpTourBuilder.build();
		return createAgent(vehicle,driver,tour);
	}

	@Override
	public ServiceProviderAgent createAgent(ServiceProviderAgent agent) {
		TourImpl tourCopy = new TourImpl(agent.getTour());
		return createAgent(agent.getVehicle(),agent.getDriver(),tourCopy);
	}

}
