package org.matsim.contrib.freight.mobsim;

import org.junit.Ignore;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategy;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManager;
import org.matsim.contrib.freight.replanning.modules.MemorizeSelectedPlan;
import org.matsim.contrib.freight.replanning.modules.RouteVehicles;
import org.matsim.contrib.freight.replanning.modules.SelectBestPlan;
import org.matsim.contrib.freight.vrp.TransportCostCalculator;
import org.matsim.contrib.freight.vrp.algorithms.rr.RuinAndRecreateChartListener;
import org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider.TourCost;
import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.VehicleRoutingCosts;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

@Ignore
public class StrategyManagerFactoryForTests implements CarrierPlanStrategyManagerFactory{
	
	static class MyTravelCosts implements TravelDisutility {

		private TravelTime travelTime;

		private double cost_per_m = 1.0 / 1000.0;

		private double cost_per_s = 50.0 / (60.0 * 60.0);

		public MyTravelCosts(TravelTime travelTime) {
			super();
			this.travelTime = travelTime;
		}

		@Override
		public double getLinkTravelDisutility(final Link link,
				final double time, final Person person, final Vehicle vehicle) {
			double genCosts = link.getLength() * cost_per_m
					+ travelTime.getLinkTravelTime(link, time,null,null) * cost_per_s;
			// double genCosts = travelTime.getLinkTravelTime(link,
			// time)*cost_per_s;
			return genCosts;
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return getLinkTravelDisutility(link, Time.UNDEFINED_TIME, null,
					null);
		}
	}
	
	public CarrierPlanStrategyManager createStrategyManager(Controler controler){
		CarrierPlanStrategy planStrat_reSchedule = new CarrierPlanStrategy();

		final LeastCostPathCalculator router = new FastDijkstraFactory()
				.createPathCalculator(controler.getScenario()
						.getNetwork(), new MyTravelCosts(controler
						.getTravelTimeCalculator()), controler
						.getTravelTimeCalculator());

		// final LeastCostPathCalculator router =
		// event.getControler().getLeastCostPathCalculatorFactory().createPathCalculator(event.getControler().getScenario().getNetwork(),
		// event.getControler().createTravelCostCalculator(),
		// event.getControler().getTravelTimeCalculator());

		VehicleRoutingCosts costs = new TransportCostCalculator(router, controler.getNetwork(), controler.getConfig()
				.travelTimeCalculator().getTraveltimeBinSize());

		String filename = controler.getControlerIO()
				.getIterationPath(controler.getIterationNumber())
				+ "/" + controler.getIterationNumber() + ".vrp.png";
		RuinAndRecreateChartListener chartListener = new RuinAndRecreateChartListener();
		chartListener.setFilename(filename);

		TourCost tourCost = new TourCost() {

			@Override
			public double getTourCost(TourImpl tour, Driver driver,
					org.matsim.contrib.freight.vrp.basics.Vehicle vehicle) {
				return vehicle.getType().vehicleCostParams.fix
						+ tour.tourData.transportCosts;
			}

		};

//		ReScheduleVehicles vehicleReRouter = new ReScheduleVehicles(controler.getNetwork(), costs, tourCost);
//		// vehicleReRouter.listeners.add(chartListener);
//
//		planStrat_reSchedule.addModule(new MemorizeSelectedPlan());
//		planStrat_reSchedule.addModule(vehicleReRouter);
//		planStrat_reSchedule.addModule(new ReRouteVehicles(router, controler.getNetwork(), controler
//				.getTravelTimeCalculator()));
//
//		ScheduleVehicles vehicleRouter = new ScheduleVehicles(controler.getNetwork(), tourCost, costs,
//				new DTWSolverFactory());
//
//		CarrierPlanStrategy planStrat_schedule = new CarrierPlanStrategy();
//		planStrat_schedule.addModule(new MemorizeSelectedPlan());
//		planStrat_schedule.addModule(vehicleRouter);
//		planStrat_schedule.addModule(new ReRouteVehicles(router, controler.getNetwork(),controler
//				.getTravelTimeCalculator()));
//
//		CarrierPlanStrategy planStrat_bestPlan = new CarrierPlanStrategy();
//		planStrat_bestPlan.addModule(new MemorizeSelectedPlan());
//		planStrat_bestPlan.addModule(new SelectBestPlan());

		CarrierPlanStrategy planStrat_reRoutePlan = new CarrierPlanStrategy();
		planStrat_reRoutePlan.addModule(new MemorizeSelectedPlan());
		planStrat_reRoutePlan.addModule(new SelectBestPlan());
		planStrat_reRoutePlan.addModule(new RouteVehicles(router, controler.getNetwork(),controler.getTravelTimeCalculator()));

		CarrierPlanStrategyManager stratManager = new CarrierPlanStrategyManager();
//		stratManager.addStrategy(planStrat_reSchedule, 0.0);
		// stratManager.addStrategy(planStrat_schedule, 0.1);
//		stratManager.addStrategy(planStrat_bestPlan, 0.0);
		stratManager.addStrategy(planStrat_reRoutePlan, 1.0);

		return stratManager;
	}
	

}
