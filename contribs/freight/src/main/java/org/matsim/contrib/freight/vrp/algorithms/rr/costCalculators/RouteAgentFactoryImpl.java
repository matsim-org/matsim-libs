package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.utils.VrpTourBuilder;

public class RouteAgentFactoryImpl implements RouteAgentFactory{
	
	private TourCost tourCost;
	
	private JobInsertionCalculator jobInsertionCalculator;
	
	private TourStateCalculator tourStateCalculator;
	
	public RouteAgentFactoryImpl(TourCost tourCost, JobInsertionCalculator jobInsertionCalculator, TourStateCalculator tourStateCalculator) {
		super();
		this.tourCost = tourCost;
		this.jobInsertionCalculator = jobInsertionCalculator;
		this.tourStateCalculator = tourStateCalculator;
	}
	
	@Override
	public RouteAgent createAgent(Vehicle vehicle, Driver driver, TourImpl tour) {
		RouteAgentImpl a = new RouteAgentImpl(vehicle, driver, tour);
		a.setTourStateCalculator(tourStateCalculator);
		a.setTourCost(tourCost);
		a.setJobInsertionCalculator(jobInsertionCalculator);
		return a;
	}

	@Override
	public RouteAgent createAgent(Vehicle vehicle, Driver driver) {
		VrpTourBuilder vrpTourBuilder = new VrpTourBuilder();
		vrpTourBuilder.scheduleStart(vehicle.getLocationId(), vehicle.getEarliestDeparture(), Double.MAX_VALUE);
		vrpTourBuilder.scheduleEnd(vehicle.getLocationId(), 0.0, vehicle.getLatestArrival());
		TourImpl tour = vrpTourBuilder.build();
		return createAgent(vehicle,driver,tour);
	}

	@Override
	public RouteAgent createAgent(RouteAgent agent) {
		TourImpl tourCopy = agent.getRoute().getTour().duplicate();
		return createAgent(agent.getRoute().getVehicle(),agent.getRoute().getDriver(),tourCopy);
	}

}
