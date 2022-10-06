package org.matsim.contrib.freight.mobsim;

import com.google.inject.Provider;
import org.junit.Ignore;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.controler.CarrierStrategyManager;
import org.matsim.contrib.freight.controler.CarrierStrategyManagerImpl;
import org.matsim.contrib.freight.controler.ReRouteVehicles;
import org.matsim.core.replanning.*;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.Map;

@Ignore
public class StrategyManagerFactoryForTests implements Provider<CarrierStrategyManager>{

    @Inject Network network;
    @Inject Map<String, TravelTime> travelTimes;

    static class MyTravelCosts implements TravelDisutility {

        private final TravelTime travelTime;

        public MyTravelCosts(TravelTime travelTime) {
            super();
            this.travelTime = travelTime;
        }

        @Override
        public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
            return disutility(link.getLength(),  travelTime.getLinkTravelTime(link, time, null, null));
        }

        @Override
        public double getLinkMinimumTravelDisutility(Link link) {
            return disutility(link.getLength(),  link.getLength() / link.getFreespeed());
        }

        private double disutility(double distance, double time) {
            double cost_per_m = 1.0 / 1000.0;
            double cost_per_s = 50.0 / (60.0 * 60.0);
            return distance * cost_per_m + time * cost_per_s;
        }
    }

    @Override
    public CarrierStrategyManager get() {

        final LeastCostPathCalculator router = new FastDijkstraFactory().createPathCalculator(network, new MyTravelCosts(travelTimes.get(TransportMode.car)), travelTimes.get(TransportMode.car));

        GenericPlanStrategyImpl<CarrierPlan, Carrier> planStrat_reRoutePlan = new GenericPlanStrategyImpl<>( new BestPlanSelector<>() );
        planStrat_reRoutePlan.addStrategyModule(new ReRouteVehicles(router, network, travelTimes.get(TransportMode.car)));

        CarrierStrategyManager stratManager = new CarrierStrategyManagerImpl();

        stratManager.addStrategy(planStrat_reRoutePlan, null, 1.0);

        return stratManager;
    }

}
