package gunnar.ihop2.roadpricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TollLevelsRandomizer implements DecisionVariableRandomizer<TollLevels> {

	// -------------------- MEMBERS --------------------

	private final TollLevels initialTollLevels;

	private final OrderedValuesRandomizer timeRandomizer;

	private final OrderedValuesRandomizer costRandomizer;

	// -------------------- CONSTRUCTION --------------------

	TollLevelsRandomizer(final TollLevels initialTollLevels,
			final double changeTimeProba, final double changeCostProba,
			final double deltaTime_s, final double deltaCost_money) {
		this.initialTollLevels = initialTollLevels;

		this.timeRandomizer = new OrderedValuesRandomizer(
				new OrderedValuesRandomizer.ValueRandomizer() {

					@Override
					public double newUnconstrained(final double oldValue) {
						if (MatsimRandom.getRandom().nextDouble() <= changeTimeProba) {
							if (MatsimRandom.getRandom().nextBoolean()) {
								return (oldValue + deltaTime_s);
							} else {
								return (oldValue - deltaTime_s);
							}
						} else {
							return oldValue;
						}
					}

					@Override
					public double constrain(double originalValue) {
						// TODO Could also re-center this at the value grid.
						return Math.max(0, Math.min(24 * 3600, originalValue));
					}
				});

		this.costRandomizer = new OrderedValuesRandomizer(
				new OrderedValuesRandomizer.ValueRandomizer() {

					@Override
					public double newUnconstrained(double oldValue) {
						if (MatsimRandom.getRandom().nextDouble() <= changeCostProba) {
							if (MatsimRandom.getRandom().nextBoolean()) {
								return (oldValue + deltaCost_money);
							} else {
								return (oldValue - deltaCost_money);
							}
						} else {
							return oldValue;
						}
					}

					@Override
					public double constrain(double originalValue) {
						// TODO Could also re-center this at the value grid.
						return originalValue;
						// return Math.max(0, originalValue);
					}
				});
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	// @Override
	// public TollLevels newRandomDecisionVariable() {
	// // TODO This is not (and should not need not be) random.
	// return this.initialTollLevels;
	// }

	@Override
	public Collection<TollLevels> newRandomVariations(
			final TollLevels decisionVariable) {
		final List<List<Double>> newTimeStructurePair = this.timeRandomizer
				.newRandomizedPair(decisionVariable.getTimeStructure());
		final List<List<Double>> newCostStructurePair = this.costRandomizer
				.newRandomizedPair(decisionVariable.getCostStructure());
		final List<TollLevels> result = new ArrayList<>(2);
		result.add(new TollLevels(newTimeStructurePair.get(0),
				newCostStructurePair.get(0), this.initialTollLevels.scenario));
		result.add(new TollLevels(newTimeStructurePair.get(1),
				newCostStructurePair.get(1), this.initialTollLevels.scenario));
		return result;
	}
}
