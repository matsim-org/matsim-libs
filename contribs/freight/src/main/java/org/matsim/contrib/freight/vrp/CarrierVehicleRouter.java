package org.matsim.contrib.freight.vrp;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.replanning.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.ScheduleVehicles;
import org.matsim.contrib.freight.vrp.DTWSolverFactory;
import org.matsim.contrib.freight.vrp.NetworkTransportCosts;
import org.matsim.contrib.freight.vrp.VRPSolverFactory;
import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.DriverCostParams;
import org.matsim.core.config.Config;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.Vehicle;

public class CarrierVehicleRouter {
	
	static class MyTravelCosts implements TravelDisutility{

		private final TravelTime travelTime;
		
		private final double cost_per_m;
		
		private final double cost_per_s;
		
		public MyTravelCosts(TravelTime travelTime, RouterConfig routerConfig) {
			super();
			this.travelTime = travelTime;
			this.cost_per_m = routerConfig.getTransportCostPerMeter();
			this.cost_per_s = routerConfig.getTransportCostPerSecond();
		}


		@Override
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			double genCosts = link.getLength()*cost_per_m + travelTime.getLinkTravelTime(link, time)*cost_per_s;
			return genCosts;
		}


		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	private Scenario scen;
	private Config config;
	private ScheduleVehicles scheduler;
	private ReRouteVehicles reRoute;
	private VRPSolverFactory solverFac;
	private RouterConfig routerConfig;

	public CarrierVehicleRouter(Scenario scen, Config config, RouterConfig routerConfig) {
		this.scen = scen;
		this.config = config;
		solverFac = new DTWSolverFactory(routerConfig.getIterations(),routerConfig.getWarmupIterations());
		this.routerConfig = routerConfig;
		ini();
	}
	
	public void setSolverFac(VRPSolverFactory solverFac) {
		this.solverFac = solverFac;
	}

	private void ini(){
		TravelTimeCalculator ttCalcForPlanning = new TravelTimeCalculator(scen.getNetwork(), config.travelTimeCalculator());
		LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(scen.getNetwork(), new MyTravelCosts(ttCalcForPlanning, routerConfig) , ttCalcForPlanning);
		
		DriverCostParams driverCostParams = new DriverCostParams();
		driverCostParams.fixCost_per_vehicleService = routerConfig.getFixCostPerActiveVehicle();
		driverCostParams.transportCost_per_meter = routerConfig.getTransportCostPerMeter();
		driverCostParams.transportCost_per_second = routerConfig.getTransportCostPerSecond();
		
		Costs costs = new NetworkTransportCosts(router, driverCostParams, scen.getNetwork(), routerConfig.getTransportTimeSlice());

		scheduler = new ScheduleVehicles(scen.getNetwork(), costs, solverFac);
		
		reRoute = new ReRouteVehicles(router, scen.getNetwork());
	}

	public void run(Carrier carrier) {
		scheduler.handleActor(carrier);
		reRoute.handleActor(carrier);
	}

}
