package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
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

		if (constraintsSet instanceof DefaultDrtOptimizationConstraintsSet defaultSet) {
			double maxTravelTime = defaultSet.maxTravelTimeAlpha * unsharedRideTime + defaultSet.maxTravelTimeBeta;
			double maxDetour = Math.max(defaultSet.minimumAllowedDetour, unsharedRideTime * (defaultSet.maxDetourAlpha -1) + defaultSet.maxDetourBeta);
			double maxRideTime = unsharedRideTime + Math.min(defaultSet.maxAbsoluteDetour, maxDetour);
			double maxWaitTime = constraintsSet.maxWaitTime;

			return new DrtRouteConstraints(maxTravelTime, maxRideTime, maxWaitTime);
		} else {
			throw new IllegalArgumentException("Constraint set is not a default set");
		}

	}
}