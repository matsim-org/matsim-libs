package org.matsim.contrib.freight.mobsim;

import org.junit.Ignore;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategy;
import org.matsim.contrib.freight.replanning.CarrierReplanningStrategyManager;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.selectors.SelectBestPlan;
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
	
	public CarrierReplanningStrategyManager createStrategyManager(Controler controler){

		final LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(controler.getScenario()
						.getNetwork(), new MyTravelCosts(controler
						.getLinkTravelTimes()), controler
						.getLinkTravelTimes());



		CarrierReplanningStrategy planStrat_reRoutePlan = new CarrierReplanningStrategy(new SelectBestPlan());
		planStrat_reRoutePlan.addModule(new ReRouteVehicles(router, controler.getNetwork(),controler.getLinkTravelTimes()));

		CarrierReplanningStrategyManager stratManager = new CarrierReplanningStrategyManager();
		stratManager.addStrategy(planStrat_reRoutePlan, 1.0);

		return stratManager;
	}
	

}
