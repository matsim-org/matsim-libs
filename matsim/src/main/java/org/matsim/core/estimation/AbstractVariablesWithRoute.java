package org.matsim.core.estimation;

import java.util.List;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.population.PlanElement;

public class AbstractVariablesWithRoute implements Variables {
    private final List<? extends PlanElement> route;

    public AbstractVariablesWithRoute(List<? extends PlanElement> route) {
        this.route = route;
    }

    @Override
    @Nullable
    public List<? extends PlanElement> getRoute() {
        return route;
    }
}
