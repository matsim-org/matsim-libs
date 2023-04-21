package org.matsim.modechoice.pruning;

import it.unimi.dsi.fastutil.objects.Reference2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2DoubleMap;
import org.matsim.modechoice.PlanModel;

import java.util.Map;

/**
 * Calculate the allowed threshold based on a constant plus distance factor depending on the current mode.
 */
public class ModeDistanceBasedPruner implements CandidatePruner {

	private final double c;
	private final Reference2DoubleMap<String> dist;

	/**
	 * Constructor.
	 *
	 * @param c       constant
	 * @param factors dist factors for all modes
	 */
	public ModeDistanceBasedPruner(double c, Map<String, Double> factors) {
		this.c = c;
		dist = new Reference2DoubleArrayMap<>();
		for (Map.Entry<String, Double> e : factors.entrySet()) {
			dist.put(e.getKey().intern(), e.getValue());
		}
	}

	@Override
	public double planThreshold(PlanModel planModel) {
		double t = c;

		for (int i = 0; i < planModel.trips(); i++) {
			t += dist.getDouble(planModel.getCurrentModesMutable()[i]) * planModel.distance(i) / 1000;
		}

		return t;
	}

	@Override
	public double tripThreshold(PlanModel planModel, int idx) {
		return c + dist.getDouble(planModel.getCurrentModesMutable()[idx]) * planModel.distance(idx) / 1000;
	}
}
