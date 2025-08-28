package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtRouteConstraints;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * @author nkuehnel / MOIA
 */
public class DefaultDrtRouteConstraintsCalculator implements DrtRouteConstraintsCalculator {

	private final DrtConfigGroup drtCfg;
	private final ConstraintSetChooser constraintSetChooser;

	public DefaultDrtRouteConstraintsCalculator(DrtConfigGroup drtCfg, ConstraintSetChooser constraintSetChooser) {
		this.drtCfg = drtCfg;
		this.constraintSetChooser = constraintSetChooser;
	}

	/**
	 * Calculates the maximum travel time defined as: drtCfg.getMaxTravelTimeAlpha()
	 * unsharedRideTime + drtCfg.getMaxTravelTimeBeta()
	 *
	 * Calculates the maximum ride time defined as:
	 * unsharedRideTime + min(
	 * maxAbsoluteDetour,
	 * max(minimumAllowedDetour, unsharedRideTime * (1-drtCfg.maxDetourAlpha) + drtCfg.maxDetourBeta)
	 * )
	 *
	 * @return DrtRouteConstraints constraints
	 */
	@Override
	public DrtRouteConstraints calculateRouteConstraints(double departureTime, Link accessActLink, Link egressActLink,
			Person person, Attributes tripAttributes, double unsharedRideTime, double unsharedDistance) {
		DrtOptimizationConstraintsSet constraintsSet = constraintSetChooser
				.chooseConstraintSet(departureTime, accessActLink, egressActLink, person, tripAttributes).orElse(drtCfg
						.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet());

		if (constraintsSet instanceof DrtOptimizationConstraintsSetImpl defaultSet) {
			double maxTravelTime = defaultSet.getMaxTravelTimeAlpha() * unsharedRideTime + defaultSet.getMaxTravelTimeBeta();
			double maxDetour = Math.max(defaultSet.getMinimumAllowedDetour(), unsharedRideTime * (defaultSet.getMaxDetourAlpha() -1) + defaultSet.getMaxDetourBeta());
			double maxRideTime = unsharedRideTime + Math.min(defaultSet.getMaxAbsoluteDetour(), maxDetour);
			double maxWaitTime = constraintsSet.getMaxWaitTime();
			return new DrtRouteConstraints(
					departureTime,
					departureTime + maxWaitTime,
					departureTime + maxTravelTime,
					maxRideTime,
					constraintsSet.getMaxAllowedPickupDelay(),
					constraintsSet.getLateDiversionthreshold(),
					constraintsSet.rejectRequestIfMaxWaitOrTravelTimeViolated
			);
		} else {
			throw new IllegalArgumentException("Constraint set is not a default set");
		}

	}
}