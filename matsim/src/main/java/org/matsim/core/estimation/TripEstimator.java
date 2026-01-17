package org.matsim.core.estimation;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.router.RoutingRequest;

public class TripEstimator {
    private final Map<Class<? extends Variables>, Estimator<? extends Variables>> estimators = new HashMap<>();

    private final Map<Class<? extends Variables>, VariableCache<? extends Variables>> caches = new HashMap<>();
    private final Map<Class<? extends Variables>, Map<Integer, ? super Variables>> cacheData = new HashMap<>();

    public <T extends Variables> void addEstimator(Class<T> type, Estimator<T> estimator) {
        estimators.put(type, estimator);
    }

    public <T extends Variables> void addCache(Class<T> type, VariableCache<T> cache) {
        caches.put(type, cache);
        cacheData.put(type, new HashMap<>());
    }

    public <T extends Variables> T process(RoutingRequest request, Class<T> type) {
        VariableCache<T> cache = (VariableCache<T>) caches.get(type);

        if (cache != null) {
            Integer key = cache.calculateKey(request);
            T variables = (T) cacheData.get(type).get(key);

            if (variables != null) {
                return variables;
            }
        }

        Estimator<T> estimator = (Estimator<T>) estimators.get(type);
        T variables = estimator.process(request);

        if (cache != null) {
            Integer key = cache.calculateKey(request, variables);
            cacheData.get(type).put(key, variables);
        }

        return variables;
    }
}
