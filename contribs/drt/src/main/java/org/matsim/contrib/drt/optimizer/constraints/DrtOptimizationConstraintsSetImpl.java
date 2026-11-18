package org.matsim.contrib.drt.optimizer.constraints;

import com.google.common.base.Verify;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.Config;

public class DrtOptimizationConstraintsSetImpl extends DrtOptimizationConstraintsSet {

    @Parameter
    @Comment("Defines the slope of the maxTravelTime function (optimisation constraint), i.e. "
            + "maxTravelTime = maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta. "
            + "maxTravelTime is measured from the earliest departure time to the dropoff, so it includes the wait time. "
            + "The in-vehicle time alone is limited separately by maxDetourAlpha, maxDetourBeta and maxAbsoluteDetour. "
            + "Alpha should not be smaller than 1.")
    @DecimalMin("1.0")
    private double maxTravelTimeAlpha = Double.NaN;// [-]

    @Parameter
    @Comment("Defines the shift of the maxTravelTime function (optimisation constraint), i.e. "
            + "maxTravelTime = maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta. "
            + "Beta should not be smaller than 0.")
    @PositiveOrZero
    private double maxTravelTimeBeta = Double.NaN;// [s]

    @Parameter
    @Comment(
            "Defines the maximum allowed absolute detour in seconds. The detour limits the ride duration (in-vehicle time) "
                    + "independently of the wait time: maxRideDuration = unsharedRideTime + min(maxAbsoluteDetour, "
                    + "max(minimumAllowedDetour, (maxDetourAlpha - 1) * unsharedRideTime + maxDetourBeta)). "
                    + "The ride duration is measured from the end of the pickup to the dropoff time of the stop time calculator. "
                    + "It therefore includes intermediate stops and, if the passenger stop duration provider assigns one, "
                    + "the passenger's own dropoff duration (0 in the default setup), so the detour should exceed that dropoff duration. "
                    + "maxAbsoluteDetour should not be smaller than 0, "
                    + "and should be higher than the offset maxDetourBeta. By default, this limit is disabled (i.e. set to Inf)")
    @PositiveOrZero
    private double maxAbsoluteDetour = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment(
            "Defines the maximum allowed detour based on the unsharedRideTime, i.e. "
                    + "(maxDetourAlpha - 1) * unsharedRideTime + maxDetourBeta. This is the ratio part. "
                    + "By default, this limit is disabled (i.e. set to Inf, together with maxDetourBeta).")
    @DecimalMin("1.0")
    private double maxDetourAlpha = Double.POSITIVE_INFINITY;

    @Parameter
    @Comment(
            "Defines the maximum allowed detour based on the unsharedRideTime, i.e. "
                    + "(maxDetourAlpha - 1) * unsharedRideTime + maxDetourBeta. This is the constant part. "
                    + "By default, this limit is disabled (i.e. set to Inf, together with maxDetourAlpha).")
    @PositiveOrZero
    private double maxDetourBeta = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment(
            "Defines the minimum allowed absolute detour in seconds. It only applies if maxDetourAlpha and maxDetourBeta "
                    + "are set, and it is capped by maxAbsoluteDetour. By default, this bound is disabled (i.e. set to 0.)")
    @PositiveOrZero
    private double minimumAllowedDetour = 0;

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        Verify.verify(getMaxAbsoluteDetour() > getMinimumAllowedDetour(), "The minimum allowed detour must" +
                "be lower than the maximum allowed detour.");
    }

    @DecimalMin("1.0")
    public double getMaxTravelTimeAlpha() {
        return maxTravelTimeAlpha;
    }

    public void setMaxTravelTimeAlpha(@DecimalMin("1.0") double maxTravelTimeAlpha) {
        this.maxTravelTimeAlpha = maxTravelTimeAlpha;
    }

    @PositiveOrZero
    public double getMaxTravelTimeBeta() {
        return maxTravelTimeBeta;
    }

    public void setMaxTravelTimeBeta(@PositiveOrZero double maxTravelTimeBeta) {
        this.maxTravelTimeBeta = maxTravelTimeBeta;
    }

    @PositiveOrZero
    public double getMaxAbsoluteDetour() {
        return maxAbsoluteDetour;
    }

    public void setMaxAbsoluteDetour(@PositiveOrZero double maxAbsoluteDetour) {
        this.maxAbsoluteDetour = maxAbsoluteDetour;
    }

    @DecimalMin("1.0")
    public double getMaxDetourAlpha() {
        return maxDetourAlpha;
    }

    public void setMaxDetourAlpha(@DecimalMin("1.0") double maxDetourAlpha) {
        this.maxDetourAlpha = maxDetourAlpha;
    }

    @PositiveOrZero
    public double getMaxDetourBeta() {
        return maxDetourBeta;
    }

    public void setMaxDetourBeta(@PositiveOrZero double maxDetourBeta) {
        this.maxDetourBeta = maxDetourBeta;
    }

    @PositiveOrZero
    public double getMinimumAllowedDetour() {
        return minimumAllowedDetour;
    }

    public void setMinimumAllowedDetour(@PositiveOrZero double minimumAllowedDetour) {
        this.minimumAllowedDetour = minimumAllowedDetour;
    }
}
