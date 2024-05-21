package org.matsim.contrib.drt.optimizer;

import com.google.common.base.Verify;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Optional;

/**
 * @author nkuehnel / MOIA
 */
public class DrtOptimizationConstraintsParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "drtOptimizationConstraints";

	public static String defaultConstraintSet = DrtOptimizationConstraintsSet.DEFAULT_PARAMS_NAME;


    public DrtOptimizationConstraintsParams() {
        super(SET_NAME);
    }

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        List<DrtOptimizationConstraintsSet> drtOptimizationConstraintsSets = getDrtOptimizationConstraintsSets();

        Verify.verify(!drtOptimizationConstraintsSets.isEmpty(),
                "At least one DrtOptimizationConstraintsParams is required.");
        Verify.verify(drtOptimizationConstraintsSets.stream()
                        .anyMatch(params -> params.name.equals(defaultConstraintSet)),
                "Default DrtOptimizationConstraintsParams is required.");
        Verify.verify(drtOptimizationConstraintsSets.stream()
                        .map(params -> params.name)
                        .distinct()
                        .count() == drtOptimizationConstraintsSets.size(),
                "Cannot have DrtOptimizationConstraintsParams with identical names.");
    }

    public List<DrtOptimizationConstraintsSet> getDrtOptimizationConstraintsSets() {
        return getParameterSets(DrtOptimizationConstraintsSet.SET_NAME).stream()
                .filter(DrtOptimizationConstraintsSet.class::isInstance)
                .map(DrtOptimizationConstraintsSet.class::cast)
                .toList();
    }

    public DrtOptimizationConstraintsSet addOrGetDefaultDrtOptimizationConstraintsSet() {
        Optional<DrtOptimizationConstraintsSet> drtOptParams = getDrtOptimizationConstraintsSets().stream()
                .filter(params -> params.name.equals(defaultConstraintSet))
                .findAny();
        if (drtOptParams.isEmpty()) {
            addParameterSet(new DrtOptimizationConstraintsSet());
        }
        return getDrtOptimizationConstraintsSets().stream()
                .filter(params -> params.name.equals(defaultConstraintSet))
                .findAny().orElseThrow();
    }

    /**
     * for backwards compatibility with old drt config groups
     */
    public void handleAddUnknownParam(final String paramName, final String value) {
        switch (paramName) {
            case "maxWaitTime":
            case "maxTravelTimeAlpha":
            case "maxTravelTimeBeta":
            case "maxAbsoluteDetour":
            case "maxDetourAlpha":
            case "maxDetourBeta":
            case "maxAllowedPickupDelay":
            case "rejectRequestIfMaxWaitOrTravelTimeViolated":
            case "maxWalkDistance":
                addOrGetDefaultDrtOptimizationConstraintsSet().addParam(paramName, value);
                break;
            default:
                super.handleAddUnknownParam(paramName, value);
        }
    }
}