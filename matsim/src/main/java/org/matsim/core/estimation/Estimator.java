package org.matsim.core.estimation;

import org.matsim.core.router.RoutingRequest;

public interface Estimator<T extends Variables> {
    T process(RoutingRequest request);
}
