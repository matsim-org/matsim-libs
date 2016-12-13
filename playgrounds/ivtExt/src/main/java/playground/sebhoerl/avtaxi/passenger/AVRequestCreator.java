package playground.sebhoerl.avtaxi.passenger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import playground.sebhoerl.avtaxi.data.AVData;
import playground.sebhoerl.avtaxi.data.AVOperator;
import playground.sebhoerl.avtaxi.dispatcher.AVDispatcher;
import playground.sebhoerl.avtaxi.routing.AVRoute;

import java.util.Map;

@Singleton
public class AVRequestCreator implements PassengerRequestCreator {
    @Inject
    AVData data;

    @Inject
    Map<Id<AVOperator>, AVOperator> operators;

    @Inject
    Map<Id<AVOperator>, AVDispatcher> dispatchers;

    @Override
    public PassengerRequest createRequest(Id<Request> id, MobsimPassengerAgent passenger, Link pickupLink, Link dropoffLink, double t0, double t1, double now) {
        if (!(passenger instanceof PlanAgent)) {
            throw new RuntimeException("Need PlanAgent in order to figure out the operator");
        }

        PlanAgent agent = (PlanAgent) passenger;
        Leg leg = (Leg) agent.getCurrentPlanElement();

        AVRoute route = (AVRoute) leg.getRoute();
        route.setDistance(Double.NaN);

        AVOperator operator = operators.get(route.getOperatorId());

        if (operator == null) {
            throw new IllegalStateException("Operator '" + route.getOperatorId().toString() + "' does not exist.");
        }

        return new AVRequest(id, passenger, pickupLink, dropoffLink, t1, now, route, operator, dispatchers.get(route.getOperatorId()));
    }
}
