package org.matsim.contrib.drt.optimizer.constraints;

import com.google.common.base.Verify;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.Config;

public class DefaultDrtOptimizationConstraintsSet extends DrtOptimizationConstraintsSet {

    @Parameter
    @Comment("Defines the slope of the maxTravelTime estimation function (optimisation constraint), i.e. "
            + "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
            + "Alpha should not be smaller than 1.")
    @DecimalMin("1.0")
    public double maxTravelTimeAlpha = Double.NaN;// [-]

    @Parameter
    @Comment("Defines the shift of the maxTravelTime estimation function (optimisation constraint), i.e. "
            + "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
            + "Beta should not be smaller than 0.")
    @PositiveOrZero
    public double maxTravelTimeBeta = Double.NaN;// [s]

    @Parameter
    @Comment(
            "Defines the maximum allowed absolute detour in seconds. maxAbsoluteDetour should not be smaller than 0, "
                    + "and should be higher than the offset maxDetourBeta. By default, this limit is disabled (i.e. set to Inf)")
    @PositiveOrZero
    public double maxAbsoluteDetour = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment(
            "Defines the maximum allowed absolute detour based on the unsharedRideTime. A linear combination similar to travel "
                    + "time constrain is used. This is the ratio part. By default, this limit is disabled (i.e. set to Inf, together with maxDetourBeta).")
    @DecimalMin("1.0")
    public double maxDetourAlpha = Double.POSITIVE_INFINITY;

    @Parameter
    @Comment(
            "Defines the maximum allowed absolute detour based on the unsharedRideTime. A linear combination similar to travel "
                    + "time constrain is used. This is the constant part. By default, this limit is disabled (i.e. set to Inf, together with maxDetourAlpha).")
    @PositiveOrZero
    public double maxDetourBeta = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment(
            "Defines the minimum allowed absolute detour in seconds. By default, this bound is disabled (i.e. set to 0.)")
    @PositiveOrZero
    public double minimumAllowedDetour = 0;

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        Verify.verify(maxAbsoluteDetour > minimumAllowedDetour, "The minimum allowed detour must" +
                "be lower than the maximum allowed detour.");
    }
}
