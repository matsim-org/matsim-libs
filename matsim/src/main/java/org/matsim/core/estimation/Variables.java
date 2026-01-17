package org.matsim.core.estimation;

import java.util.List;

import javax.annotation.Nullable;

import org.matsim.api.core.v01.population.PlanElement;

public interface Variables {
    @Nullable
    public List<? extends PlanElement> getRoute();
}
