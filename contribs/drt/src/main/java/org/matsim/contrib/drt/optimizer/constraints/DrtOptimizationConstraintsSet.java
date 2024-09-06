package org.matsim.contrib.drt.optimizer.constraints;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.matsim.core.config.ReflectiveConfigGroup;

public abstract class DrtOptimizationConstraintsSet extends ReflectiveConfigGroup {

    public static final String SET_NAME = "drtOptimizationConstraintsSet";

    public static final String DEFAULT_PARAMS_NAME = "default";

    @Parameter
    @Comment("name of optimization params")
    @NotBlank
    public String name = DEFAULT_PARAMS_NAME;

    @Parameter
    @Comment("Max wait time for the bus to come (optimisation constraint).")
    @PositiveOrZero
    public double maxWaitTime = Double.NaN;// seconds

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

    @Parameter
    @Comment(
            "Defines the maximum delay allowed from the initial scheduled pick up time. Once the initial pickup time is offered, the latest promised"
                    + "pickup time is calculated based on initial scheduled pickup time + maxAllowedPickupDelay. "
                    + "By default, this limit is disabled. If enabled, a value between 0 and 240 is a good choice.")
    @PositiveOrZero
    public double maxAllowedPickupDelay = Double.POSITIVE_INFINITY;// [s]

    public DrtOptimizationConstraintsSet() {
        super(SET_NAME);
    }
}
