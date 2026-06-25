package org.matsim.core.estimation.modes;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.estimation.Estimator;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;

public class CarEstimator implements Estimator<CarVariables> {
    private final TripRouter tripRouter;

    public CarEstimator(TripRouter tripRouter) {
        this.tripRouter = tripRouter;
    }

    @Override
    public CarVariables process(RoutingRequest request) {
        // TODO: Let TripRouter directly accept a RoutingRequest
        List<? extends PlanElement> route = tripRouter.calcRoute(TransportMode.car, request.getFromFacility(),
                request.getToFacility(), request.getDepartureTime(), request.getPerson(), request.getAttributes());

        double accessEgressTime_min = 0.0;
        double inVehicleTime_min = 0.0;

        for (Leg leg : TripStructureUtils.getLegs(route)) {
            if (leg.getMode().equals(TransportMode.car)) {
                inVehicleTime_min += leg.getTravelTime().seconds() / 60.0;
            } else if (leg.getMode().equals(TransportMode.walk)) {
                accessEgressTime_min += leg.getTravelTime().seconds() / 60.0;
            } else {
                throw new IllegalAccessError("Unknwon leg mode in car trip: " + leg.getMode());
            }
        }

        return new CarVariables(accessEgressTime_min, inVehicleTime_min, route);
    }
}
