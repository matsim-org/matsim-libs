package org.matsim.contrib.accessibility;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.accessibility.utils.AggregationObject;
import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.ActivityFacility;

/**
 * @author thibautd
 * @author michaz
 */
class PtMatrixAccessibilityContributionCalculator {

	public static AccessibilityContributionCalculator create(
			final PtMatrix ptMatrix,
			final Config config) {
		return new LeastCostPathCalculatorAccessibilityContributionCalculator(
				config.planCalcScore(),
				ptMatrix.asPathCalculator(config.planCalcScore()));
	}

}
