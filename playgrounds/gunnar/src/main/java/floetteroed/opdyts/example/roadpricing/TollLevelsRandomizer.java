package floetteroed.opdyts.example.roadpricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.utilities.math.MathHelpers;

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
						originalValue = round(originalValue, deltaTime_s);
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
						originalValue = round(originalValue, deltaCost_money);
						return Math.max(0.0, originalValue);
					}
				});
	}

	private static double round(final double val, final double discr) {
		return MathHelpers.round(val / discr) * discr;

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

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		final Random rnd = new Random();
		for (int i = 0; i < 25; i++) {
			final double x = rnd.nextGaussian();
			System.out.println(x + "\t" + round(x, 0.25));
		}

	}

}
