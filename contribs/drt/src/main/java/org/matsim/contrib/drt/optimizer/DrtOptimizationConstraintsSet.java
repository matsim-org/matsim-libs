package org.matsim.contrib.drt.optimizer;

import com.google.common.base.Verify;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class DrtOptimizationConstraintsSet extends ReflectiveConfigGroup {

    public static final String SET_NAME = "drtOptimizationConstraintsSet";
    public static final String DEFAULT_PARAMS_NAME = "default";

    public DrtOptimizationConstraintsSet() {
        super(SET_NAME);
    }

    @Parameter
    @Comment("name of optimization params")
    @NotBlank
    public String name = DEFAULT_PARAMS_NAME;

    @Parameter
    @Comment("Max wait time for the bus to come (optimisation constraint).")
    @PositiveOrZero
    public double maxWaitTime = Double.NaN;// seconds

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
            "Defines the maximum allowed absolute detour in seconds. Note that the detour is computed from the latest promised pickup time. " +
                    "To enable the max detour constraint, maxAllowedPickupDelay has to be specified. maxAbsoluteDetour should not be smaller than 0, "
                    + "and should be higher than the offset maxDetourBeta. By default, this limit is disabled (i.e. set to Inf)")
    @PositiveOrZero
    public double maxAbsoluteDetour = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment(
            "Defines the maximum allowed absolute detour based on the unsharedRideTime. Note that the detour is computed from the latest promised "
                    + "pickup time. To enable the max detour constraint, maxAllowedPickupDelay has to be specified. A linear combination similar to travel "
                    + "time constrain is used. This is the ratio part. By default, this limit is disabled (i.e. set to Inf, together with maxDetourBeta).")
    @DecimalMin("1.0")
    public double maxDetourAlpha = Double.POSITIVE_INFINITY;

    @Parameter
    @Comment(
            "Defines the maximum allowed absolute detour based on the unsharedRideTime. Note that the detour is computed from the latest promised "
                    + "pickup time. To enable the max detour constraint, maxAllowedPickupDelay has to be specified. A linear combination similar to travel "
                    + "time constrain is used. This is the constant part. By default, this limit is disabled (i.e. set to Inf, together with maxDetourAlpha).")
    @PositiveOrZero
    public double maxDetourBeta = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment(
            "Defines the maximum delay allowed from the initial scheduled pick up time. Once the initial pickup time is offered, the latest promised"
                    + "pickup time is calculated based on initial scheduled pickup time + maxAllowedPickupDelay. "
                    + "By default, this limit is disabled. If enabled, a value between 0 and 240 is a good choice.")
    @PositiveOrZero
    public double maxAllowedPickupDelay = Double.POSITIVE_INFINITY;// [s]

    @Parameter
    @Comment("If true, the max travel and wait times of a submitted request"
            + " are considered hard constraints (the request gets rejected if one of the constraints is violated)."
            + " If false, the max travel and wait times are considered soft constraints (insertion of a request that"
            + " violates one of the constraints is allowed, but its cost is increased by additional penalty to make"
            + " it relatively less attractive). Penalisation of insertions can be customised by injecting a customised"
            + " InsertionCostCalculator.PenaltyCalculator")
    public boolean rejectRequestIfMaxWaitOrTravelTimeViolated = true;//TODO consider renaming maxWalkDistance to max access/egress distance (or even have 2 separate params)

    @Parameter
    @Comment(
            "Maximum beeline distance (in meters) to next stop location in stopbased system for access/egress walk leg to/from drt."
                    + " If no stop can be found within this maximum distance will return null (in most cases caught by fallback routing module).")
    @PositiveOrZero // used only for stopbased DRT scheme
    public double maxWalkDistance = Double.MAX_VALUE;// [m];

    @Parameter
    @Comment(
            "Time before reaching a planned dropoff from which it is not allowed to insert new detours for new requests. I.e.," +
                    " if set to 180, then a vehicle will not divert to pickup or dropoff a new passenger once a boarded passenger is only " +
                    "3 minutes away from her destination, even though her time window would allow it." +
                    " Delayed detours just before arrival are usually perceived very negatively.")
    @PositiveOrZero
    public double lateDiversionthreshold = 0; // [s];

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        if ((maxDetourAlpha != Double.POSITIVE_INFINITY && maxDetourBeta != Double.POSITIVE_INFINITY) || maxAbsoluteDetour != Double.POSITIVE_INFINITY) {
            Verify.verify(maxAllowedPickupDelay != Double.POSITIVE_INFINITY, "Detour constraints are activated, " +
                    "maxAllowedPickupDelay must be specified! A value between 0 and 240 seconds can be a good choice for maxAllowedPickupDelay.");
        }
    }
}
