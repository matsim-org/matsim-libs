package org.matsim.contrib.freight.mobsim;

import org.junit.Ignore;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.Map;

@Ignore
public class StrategyManagerFactoryForTests implements CarrierPlanStrategyManagerFactory {

    @Inject Network network;
    @Inject Map<String, TravelTime> travelTimes;

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
    public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {

        final LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(network, new MyTravelCosts(travelTimes.get(TransportMode.car)), travelTimes.get(TransportMode.car));

        GenericPlanStrategyImpl<CarrierPlan, Carrier> planStrat_reRoutePlan =
                new GenericPlanStrategyImpl<CarrierPlan, Carrier>(new BestPlanSelector<CarrierPlan, Carrier>());
        planStrat_reRoutePlan.addStrategyModule(new ReRouteVehicles(router, network, travelTimes.get(TransportMode.car)));


        GenericStrategyManager<CarrierPlan, Carrier> stratManager = new GenericStrategyManager<CarrierPlan, Carrier>();
        stratManager.addStrategy(planStrat_reRoutePlan, null, 1.0);

        return stratManager;
    }

}
