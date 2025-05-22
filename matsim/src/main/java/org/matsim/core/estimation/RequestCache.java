package org.matsim.core.estimation;

import org.matsim.core.router.RoutingRequest;

public class RequestCache<T extends Variables> implements VariableCache<T> {
    private RequestCache() {
    }

    @Override
    public Integer calculateKey(RoutingRequest request) {
        return request.hashCode();
    }

    @Override
    public Integer calculateKey(RoutingRequest request, T variables) {
        return request.hashCode();
    }

    static public <T extends Variables> RequestCache<T> create(Class<T> type) {
        return new RequestCache<>();
    }
}
