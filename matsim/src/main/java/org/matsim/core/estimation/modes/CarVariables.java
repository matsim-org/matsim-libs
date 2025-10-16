package org.matsim.core.estimation.modes;

import java.util.List;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.estimation.AbstractVariablesWithRoute;

public class CarVariables extends AbstractVariablesWithRoute {
    protected double accessEgressTime_min;
    protected double inVehicleTime_min;

    public CarVariables(double accessEgressTime_min, double inVehicleTime_min, List<? extends PlanElement> route) {
        super(route);

        this.accessEgressTime_min = accessEgressTime_min;
        this.inVehicleTime_min = inVehicleTime_min;
    }

    public double getInVehicleTime_min() {
        return inVehicleTime_min;
    }

    public double getAccessEgressTime_min() {
        return accessEgressTime_min;
    }
}
