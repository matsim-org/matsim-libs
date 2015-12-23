package gunnar.ihop2.roadpricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

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
			final double deltaTime_s, final double deltaCost_money,
			final Random rnd) {
		this.initialTollLevels = initialTollLevels;

		this.timeRandomizer = new OrderedValuesRandomizer(
				new OrderedValuesRandomizer.ValueRandomizer() {
					// @Override
					// public double newConstrained() {
					// return rnd.nextDouble() * 24 * 3600;
					// }

					@Override
					public double newUnconstrained(final double oldValue) {
						if (rnd.nextDouble() <= changeTimeProba) {
							if (rnd.nextBoolean()) {
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
						return Math.max(0, Math.min(24 * 3600, originalValue));
					}
				});

		this.costRandomizer = new OrderedValuesRandomizer(
				new OrderedValuesRandomizer.ValueRandomizer() {
					// @Override
					// public double newConstrained() {
					// return rnd.nextDouble() * 50;
					// }

					@Override
					public double newUnconstrained(double oldValue) {
						if (rnd.nextDouble() <= changeCostProba) {
							if (rnd.nextBoolean()) {
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
						return Math.max(0, originalValue);
					}
				});
	}

	// -------------------- INTERNALS --------------------
	//
	// private TollLevels newTollLevels(final List<Double> timeStructure,
	// final List<Double> costStructure) {
	// return new TollLevels(timeStructure.get(0), timeStructure.get(1),
	// timeStructure.get(2), timeStructure.get(3),
	// timeStructure.get(4), timeStructure.get(5),
	// timeStructure.get(6), timeStructure.get(7),
	// timeStructure.get(8), timeStructure.get(9),
	// costStructure.get(0), costStructure.get(1),
	// costStructure.get(2), this.scenario);
	// }

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	@Override
	public TollLevels newRandomDecisionVariable() {
		// TODO This is not (and need not be) random.
		return this.initialTollLevels;
	}

	@Override
	public Collection<TollLevels> newRandomVariations(
			final TollLevels decisionVariable) {

		// final List<Double> oldTimeStructure = new ArrayList<>(10);
		// oldTimeStructure.add(decisionVariable.level1start_s);
		// oldTimeStructure.add(decisionVariable.morningLevel2start_s);
		// oldTimeStructure.add(decisionVariable.morningLevel3start_s);
		// oldTimeStructure.add(decisionVariable.morningLevel3end_s);
		// oldTimeStructure.add(decisionVariable.morningLevel2end_s);
		// oldTimeStructure.add(decisionVariable.eveningLevel2start_s);
		// oldTimeStructure.add(decisionVariable.eveningLevel3start_s);
		// oldTimeStructure.add(decisionVariable.eveningLevel3end_s);
		// oldTimeStructure.add(decisionVariable.eveningLevel2end_s);
		// oldTimeStructure.add(decisionVariable.level1end_s);
		final List<List<Double>> newTimeStructurePair = this.timeRandomizer
				.newRandomizedPair(decisionVariable.getTimeStructure());

		// final List<Double> oldCostStructure = new ArrayList<>(3);
		// oldCostStructure.add(decisionVariable.level1cost_money);
		// oldCostStructure.add(decisionVariable.level2cost_money);
		// oldCostStructure.add(decisionVariable.level3cost_money);
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
