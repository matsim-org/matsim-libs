package playground.sebhoerl.avtaxi.routing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;
import playground.sebhoerl.avtaxi.data.AVData;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.framework.AVModule;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
public class AVRoutingModule implements RoutingModule {
    @Inject private Map<Id<AVOperator>, AVOperator> operators;
    @Inject private AVRouteFactory routeFactory;

    Id<AVOperator> chooseRandomOperator() {
        int draw = MatsimRandom.getRandom().nextInt(operators.size());
        Iterator<Id<AVOperator>> iterator = operators.keySet().iterator();

        for (int i = 0; i < draw; i++) {
            iterator.next();
        }

        return iterator.next();
    }

    @Override
    public List<? extends PlanElement> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
        Id<AVOperator> operator = chooseRandomOperator();

        AVRoute route = routeFactory.createRoute(fromFacility.getLinkId(), toFacility.getLinkId(), operator);
        route.setDistance(Double.NaN);
        route.setTravelTime(Double.NaN);

        Leg leg = PopulationUtils.createLeg(AVModule.AV_MODE);
        leg.setDepartureTime(departureTime);
        leg.setTravelTime(Double.NaN);
        leg.setRoute(route);

        return Collections.singletonList(leg);
    }

    @Override
    public StageActivityTypes getStageActivityTypes() {
        return EmptyStageActivityTypes.INSTANCE;
    }
}
