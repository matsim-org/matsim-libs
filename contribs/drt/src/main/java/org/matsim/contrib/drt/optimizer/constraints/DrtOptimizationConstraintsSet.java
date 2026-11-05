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
    private String constraintSetName = DEFAULT_PARAMS_NAME;

    @Parameter
    @Comment("Max wait time for the bus to come (optimisation constraint).")
    @PositiveOrZero
    private double maxWaitTime = Double.NaN;// seconds

    @Parameter
    @Comment("If true, the max travel and wait times of a submitted request"
            + " are considered hard constraints (the request gets rejected if one of the constraints is violated)."
            + " If false, the max travel and wait times are considered soft constraints (insertion of a request that"
            + " violates one of the constraints is allowed, but its cost is increased by additional penalty to make"
            + " it relatively less attractive). Penalisation of insertions can be customised by injecting a customised"
            + " InsertionCostCalculator.PenaltyCalculator. Note that this only applies to the request being inserted:"
            + " the constraints of already scheduled requests are never relaxed, so a new insertion must not delay a"
            + " stop whose constraints are already violated.")
    public boolean rejectRequestIfMaxWaitOrTravelTimeViolated = true;


    //TODO consider renaming maxWalkDistance to max access/egress distance (or even have 2 separate params)
    @Parameter
    @Comment(
            "Maximum beeline distance (in meters) to next stop location in stopbased system for access/egress walk leg to/from drt."
                    + " If no stop can be found within this maximum distance will return null (in most cases caught by fallback routing module).")
    @PositiveOrZero // used only for stopbased DRT scheme
    private
    double maxWalkDistance = Double.MAX_VALUE;// [m];

    @Parameter
    @Comment(
            "Minimum beeline distance (in meters) between the access and the egress link of a drt trip, measured between"
                    + " the link coordinates. In a stopbased or serviceAreaBased system, these are the links of the chosen"
                    + " access and egress stops, not the locations of the activities. In a door2door system, these are the links of the activities."
                    + " Shorter trips are not served: routing returns null (in most cases caught by"
                    + " the fallback routing module), and requests that reach the mobsim without having been routed in this"
                    + " run are rejected. By default, this bound is disabled (i.e. set to 0).")
    @PositiveOrZero
    private double minimumTripDistance = 0;// [m]

    @Parameter
    @Comment(
            "Time before reaching a planned dropoff from which it is not allowed to insert new detours for new requests. I.e.," +
                    " if set to 180, then a vehicle will not divert to pickup or dropoff a new passenger once a boarded passenger is only " +
                    "3 minutes away from her destination, even though her time window would allow it." +
                    " Delayed detours just before arrival are usually perceived very negatively.")
    @PositiveOrZero
    private double lateDiversionThreshold = 0; // [s];

    @Parameter
    @Comment(
            "Defines the maximum delay allowed from the initial scheduled pick up time. Once the initial pickup time is offered, the latest promised"
                    + "pickup time is calculated based on initial scheduled pickup time + maxAllowedPickupDelay. "
                    + "By default, this limit is disabled. If enabled, a value between 0 and 240 is a good choice.")
    @PositiveOrZero
    private double maxAllowedPickupDelay = Double.POSITIVE_INFINITY;// [s]

    public DrtOptimizationConstraintsSet() {
        super(SET_NAME);
    }

    public @NotBlank String getConstraintSetName() {
        return constraintSetName;
    }

    public void setConstraintSetName(@NotBlank String constraintSetName) {
        this.constraintSetName = constraintSetName;
    }

    @PositiveOrZero
    public double getMaxWaitTime() {
        return maxWaitTime;
    }

    public void setMaxWaitTime(@PositiveOrZero double maxWaitTime) {
        this.maxWaitTime = maxWaitTime;
    }

    public boolean isRejectRequestIfMaxWaitOrTravelTimeViolated() {
        return rejectRequestIfMaxWaitOrTravelTimeViolated;
    }

    public void setRejectRequestIfMaxWaitOrTravelTimeViolated(boolean rejectRequestIfMaxWaitOrTravelTimeViolated) {
        this.rejectRequestIfMaxWaitOrTravelTimeViolated = rejectRequestIfMaxWaitOrTravelTimeViolated;
    }

    @PositiveOrZero
    public double getMaxWalkDistance() {
        return maxWalkDistance;
    }

    public void setMaxWalkDistance(@PositiveOrZero double maxWalkDistance) {
        this.maxWalkDistance = maxWalkDistance;
    }

    @PositiveOrZero
    public double getMinimumTripDistance() {
        return minimumTripDistance;
    }

    public void setMinimumTripDistance(@PositiveOrZero double minimumTripDistance) {
        this.minimumTripDistance = minimumTripDistance;
    }

    @PositiveOrZero
    public double getLateDiversionThreshold() {
        return lateDiversionThreshold;
    }

    public void setLateDiversionThreshold(@PositiveOrZero double lateDiversionThreshold) {
        this.lateDiversionThreshold = lateDiversionThreshold;
    }

    @PositiveOrZero
    public double getMaxAllowedPickupDelay() {
        return maxAllowedPickupDelay;
    }

    public void setMaxAllowedPickupDelay(@PositiveOrZero double maxAllowedPickupDelay) {
        this.maxAllowedPickupDelay = maxAllowedPickupDelay;
    }
}
