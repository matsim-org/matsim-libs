package org.matsim.contrib.accessibility;

import org.matsim.contrib.matrixbasedptrouter.PtMatrix;
import org.matsim.core.config.Config;

/**
 * @author thibautd
 * @author michaz
 */
public final class PtMatrixAccessibilityUtils {
	private PtMatrixAccessibilityUtils(){} ; // do not instantiate

	public static AccessibilityContributionCalculator createPtMatrixAccessibilityCalculator(
			final PtMatrix ptMatrix,
			final Config config) {
		return new LeastCostPathCalculatorAccessibilityContributionCalculator(
				config.planCalcScore(),
				ptMatrix.asPathCalculator(config.planCalcScore()));
	}

}
