package playground.vsp.drt.accessibilityOrientedDrt.optimizer;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PersonAttributeBasedConstraintSelector implements ConstraintSetChooser {
    private final Map<String, DrtOptimizationConstraintsSet> constraintsMap;

    public PersonAttributeBasedConstraintSelector(DrtConfigGroup drtConfigGroup) {
        this.constraintsMap = new HashMap<>();
        drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().getDrtOptimizationConstraintsSets().
                forEach(constraintSet -> constraintsMap.put(constraintSet.getConstraintSetName(), constraintSet));
    }

    @Override
    public Optional<DrtOptimizationConstraintsSet> chooseConstraintSet(double time, Link from, Link to, Person person, Attributes attributes) {
        if (person.getAttributes().getAttribute(PassengerAttribute.ATTRIBUTE_NAME).toString().equals(PassengerAttribute.PREMIUM)) {
			// for premium users, return premium service constraint (elevated service level)
            return Optional.of(constraintsMap.get(PassengerAttribute.PREMIUM));
        }
		// otherwise, return default service constraint (normal service level)
        return Optional.of(constraintsMap.get(DrtOptimizationConstraintsSet.DEFAULT_PARAMS_NAME));
    }

    public static void prepareDrtConstraint(DrtConfigGroup drtConfigGroup,
                                            double defaultMaxTravelTimeAlpha, double defaultMaxTravelTimeBeta, double defaultMaxWaitTime,
                                            double premiumMaxTravelTimeAlpha, double premiumMaxTravelTimeBeta, double premiumMaxWaitTime) {
        DrtOptimizationConstraintsParams params = drtConfigGroup.addOrGetDrtOptimizationConstraintsParams();
		DrtOptimizationConstraintsSetImpl defaultConstraintsSet = params.addOrGetDefaultDrtOptimizationConstraintsSet();
        defaultConstraintsSet.setMaxTravelTimeAlpha(defaultMaxTravelTimeAlpha);
		defaultConstraintsSet.setMaxTravelTimeBeta(defaultMaxTravelTimeBeta);
		defaultConstraintsSet.setMaxWaitTime(defaultMaxWaitTime);
        defaultConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated = false;

        DrtOptimizationConstraintsSetImpl premiumConstraintsSet = new DrtOptimizationConstraintsSetImpl();
        premiumConstraintsSet.setConstraintSetName(PassengerAttribute.PREMIUM);
		premiumConstraintsSet.setMaxTravelTimeAlpha(premiumMaxTravelTimeAlpha);
		premiumConstraintsSet.setMaxTravelTimeBeta(premiumMaxTravelTimeBeta);
		premiumConstraintsSet.setMaxWaitTime(premiumMaxWaitTime);
        premiumConstraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated = false;
        params.addParameterSet(premiumConstraintsSet);
    }
}
