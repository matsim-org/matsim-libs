package org.matsim.contrib.freight.algorithms;

import java.util.Collection;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.vrp.DTWSolverFactory;
import org.matsim.contrib.freight.vrp.MatsimVrpSolver;
import org.matsim.contrib.freight.vrp.MatsimVrpSolverFactory;
import org.matsim.contrib.freight.vrp.TransportCostCalculator;
import org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators.TourCost;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class VehicleRouter implements CarrierAlgorithm{
	
	public static int TIMESLICE = 24*3600;
	
	private TravelDisutility travelDisutility;
	
	private TravelTime travelTime;
	
	private LeastCostPathCalculator leastCostPathCalculator;
	
	private VehicleRoutingCosts transportCostCalculator;
	
	private TourCost tourCost;
	
	private MatsimVrpSolverFactory matsimVrpSolverFactory;

	public void setMatsimVrpSolverFactory(MatsimVrpSolverFactory matsimVrpSolverFactory) {
		this.matsimVrpSolverFactory = matsimVrpSolverFactory;
	}

	public void setTourCost(TourCost tourCost) {
		this.tourCost = tourCost;
	}

	public void setTransportCostCalculator(TransportCostCalculator transportCostCalculator) {
		this.transportCostCalculator = transportCostCalculator;
	}

	private Network network;

	public VehicleRouter(Network network, TravelDisutility travelDisutility, TravelTime travelTime) {
		super();
		this.travelDisutility = travelDisutility;
		this.travelTime = travelTime;
		this.network = network;
	}

	public void setLeastCostPathCalculator(LeastCostPathCalculator leastCostPathCalculator) {
		this.leastCostPathCalculator = leastCostPathCalculator;
	}

	/**
	 * 1) 
	 * 1.1) solves vehicle routing problem which is determined by shipments and vehicles (uses the DTWSolver)
	 * 1.2) routes the tour through time and space 
	 * 2) creates a new carrier plan
	 * 3) adds the plan to carriers plans
	 * 4) sets this plan to selected plan  
	 */
	@Override
	public void run(Carrier carrier) {
		init(); 
		MatsimVrpSolver matsimVrpSolver = matsimVrpSolverFactory.createSolver(carrier, network, getTourCost(), transportCostCalculator); 
		Collection<ScheduledTour> scheduledTours = matsimVrpSolver.solve();
		route(scheduledTours);
		CarrierPlan carrierPlan = new CarrierPlan(carrier, scheduledTours);
		carrier.getPlans().add(carrierPlan);
		carrier.setSelectedPlan(carrierPlan);
	}

	private void route(Collection<ScheduledTour> scheduledTours) {
		for(ScheduledTour scheduledTour : scheduledTours){
			new TimeAndSpaceTourRouter(leastCostPathCalculator, network, travelTime).route(scheduledTour);
		}
	}

	private TourCost getTourCost() {
		if(tourCost == null){
			return new TourCost(){

				@Override
				public double getTourCost(TourImpl tour, Driver driver, Vehicle vehicle) {
					return tour.tourData.transportCosts;
				}

			};
		}
		else{
			return tourCost;
		}
	}

	private void init() {
		if(leastCostPathCalculator == null){
			leastCostPathCalculator = new FastDijkstraFactory().createPathCalculator(network, travelDisutility, travelTime); 
		}
		if(transportCostCalculator == null){
			transportCostCalculator = new TransportCostCalculator(leastCostPathCalculator, network, TIMESLICE);
		}
		if(matsimVrpSolverFactory == null){
			matsimVrpSolverFactory = new DTWSolverFactory();
		}
		
	}
	
	

}
