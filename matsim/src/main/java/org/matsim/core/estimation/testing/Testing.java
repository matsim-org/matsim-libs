package org.matsim.core.estimation.testing;

import org.matsim.core.estimation.RequestCache;
import org.matsim.core.estimation.TripEstimator;
import org.matsim.core.estimation.modes.CarEstimator;
import org.matsim.core.estimation.modes.CarVariables;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.router.TripRouter;

public class Testing {
    static public void main(String[] args) {
        TripRouter tripRouter = null;

        CarEstimator carEstimator = new CarEstimator(tripRouter);

        TripEstimator estimator = new TripEstimator();
        estimator.addEstimator(CarVariables.class, carEstimator);
        estimator.addCache(CarVariables.class, RequestCache.create(CarVariables.class));

        RoutingRequest request = null;

        CarVariables variables = estimator.process(request, CarVariables.class);
        System.out.println(variables);
    }
}
