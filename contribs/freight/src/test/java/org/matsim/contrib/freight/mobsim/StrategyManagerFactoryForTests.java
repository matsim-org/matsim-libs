package org.matsim.contrib.freight.mobsim;

import org.junit.Ignore;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

@Ignore
public class StrategyManagerFactoryForTests implements CarrierPlanStrategyManagerFactory {

    private Controler controler;

    public StrategyManagerFactoryForTests(Controler controler) {
        this.controler = controler;
    }

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
                    + travelTime.getLinkTravelTime(link, time, null, null) * cost_per_s;
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

    @Override
    public GenericStrategyManager<CarrierPlan> createStrategyManager() {

        final LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(controler.getScenario()
                .getNetwork(), new MyTravelCosts(controler
                .getLinkTravelTimes()), controler
                .getLinkTravelTimes());

        GenericPlanStrategyImpl<CarrierPlan> planStrat_reRoutePlan =
                new GenericPlanStrategyImpl<CarrierPlan>(new BestPlanSelector<CarrierPlan>());
        planStrat_reRoutePlan.addStrategyModule(new ReRouteVehicles(router, controler.getNetwork(), controler.getLinkTravelTimes()));


        GenericStrategyManager<CarrierPlan> stratManager = new GenericStrategyManager<CarrierPlan>();
        stratManager.addStrategy(planStrat_reRoutePlan, null, 1.0);

        return stratManager;
    }

}
