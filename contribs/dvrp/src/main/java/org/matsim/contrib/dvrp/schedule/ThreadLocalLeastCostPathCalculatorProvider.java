package org.matsim.contrib.dvrp.schedule;

import com.google.inject.Provider;
import org.matsim.core.router.util.LeastCostPathCalculator;

public class ThreadLocalLeastCostPathCalculatorProvider {
    private final ThreadLocal<LeastCostPathCalculator> threadLocal;

    public ThreadLocalLeastCostPathCalculatorProvider(Provider<LeastCostPathCalculator> provider) {
        this.threadLocal = ThreadLocal.withInitial(provider::get);
    }

    public LeastCostPathCalculator get() {
        return threadLocal.get();
    }
}
