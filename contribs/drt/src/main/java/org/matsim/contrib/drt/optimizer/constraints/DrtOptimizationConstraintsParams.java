package org.matsim.contrib.drt.optimizer.constraints;

import com.google.common.base.Verify;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author nkuehnel / MOIA
 */
public class DrtOptimizationConstraintsParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "drtOptimizationConstraints";

    private final Supplier<DrtOptimizationConstraintsSet> optimizationConstraintsSetSupplier;

    public static String defaultConstraintSet = DrtOptimizationConstraintsSet.DEFAULT_PARAMS_NAME;


    public DrtOptimizationConstraintsParams() {
        this(DefaultDrtOptimizationConstraintsSet::new);
    }

    public DrtOptimizationConstraintsParams(Supplier<DrtOptimizationConstraintsSet> supplier) {
        super(SET_NAME);
        this.optimizationConstraintsSetSupplier = supplier;
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
            DrtOptimizationConstraintsSet defaultSet = optimizationConstraintsSetSupplier.get();
            addParameterSet(defaultSet);
            return defaultSet;
        }
        return drtOptParams.get();
    }

    @Override
    public ConfigGroup createParameterSet(final String type) {
        switch ( type ) {
            case DrtOptimizationConstraintsSet.SET_NAME:
                return optimizationConstraintsSetSupplier.get();
            default:
                throw new IllegalArgumentException( "unknown set type "+type );
        }
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