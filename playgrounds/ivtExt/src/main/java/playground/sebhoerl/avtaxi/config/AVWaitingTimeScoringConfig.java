package playground.sebhoerl.avtaxi.config;

import org.matsim.core.config.ReflectiveConfigGroup.*;

public class AVWaitingTimeScoringConfig {
    static final String PLANNED_WAITING = "marginalUtiltiyOfPlannedWaitingTime";
    static final String UNPLANNED_WAITING = "marginalUtilityOfUnplannedWaitingTime";

    private double marginalUtilityOfPlannedWaitingTime = Double.NEGATIVE_INFINITY;
    private double marginalUtilityOfUnplannedWaitingTime = Double.NEGATIVE_INFINITY;

    @StringGetter(PLANNED_WAITING)
    double getMarginalUtilityOfPlannedWaitingTime() {
        return marginalUtilityOfPlannedWaitingTime;
    }

    @StringSetter(PLANNED_WAITING)
    void setMarginalUtiltiyOfPlannedWaitingTime(double marginalUtilityOfPlannedWaitingTime) {
        this.marginalUtilityOfPlannedWaitingTime = marginalUtilityOfPlannedWaitingTime;
    }

    @StringGetter(UNPLANNED_WAITING)
    double getMarginalUtilityOfUnplannedWaitingTime() {
        return marginalUtilityOfUnplannedWaitingTime;
    }

    @StringSetter(UNPLANNED_WAITING)
    void setMarginalUtiltiyOfUnplannedWaitingTime(double marginalUtilityOfPlannedWaitingTime) {
        this.marginalUtilityOfUnplannedWaitingTime = marginalUtilityOfUnplannedWaitingTime;
    }
}
