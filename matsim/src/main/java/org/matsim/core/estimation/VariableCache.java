package org.matsim.core.estimation;

import org.matsim.core.router.RoutingRequest;

public interface VariableCache<T extends Variables> {
    Integer calculateKey(RoutingRequest request);

    Integer calculateKey(RoutingRequest request, T variables);
}
