package gunnar.ihop2.roadpricing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TollLevelsRandomizer implements DecisionVariableRandomizer<TollLevels> {

	// -------------------- MEMBERS --------------------

	private final Scenario scenario;

	private final OrderedValuesRandomizer timeRandomizer;

	private final OrderedValuesRandomizer costRandomizer;

	// -------------------- CONSTRUCTION --------------------

	TollLevelsRandomizer(final double changeTimeProba,
			final double changeCostProba, final double deltaTime_s,
			final double deltaCost_money, final Random rnd,
			final Scenario scenario) {
		this.scenario = scenario;
		this.timeRandomizer = new OrderedValuesRandomizer(
				new OrderedValuesRandomizer.ValueRandomizer() {
					@Override
					public double newValue() {
						return rnd.nextDouble() * 24 * 3600;
					}

					@Override
					public double newValue(double oldValue) {
						if (rnd.nextDouble() <= changeTimeProba) {
							if (rnd.nextBoolean()) {
								return Math.min(oldValue + deltaTime_s,
										24 * 3600);
							} else {
								return Math.max(oldValue - deltaTime_s, 0);
							}
						} else {
							return 0;
						}
					}
				});
		this.costRandomizer = new OrderedValuesRandomizer(
				new OrderedValuesRandomizer.ValueRandomizer() {
					@Override
					public double newValue() {
						return rnd.nextDouble() * 50;
					}

					@Override
					public double newValue(double oldValue) {
						if (rnd.nextDouble() <= changeCostProba) {
							if (rnd.nextBoolean()) {
								return oldValue + deltaCost_money;
							} else {
								return Math.max(oldValue - deltaCost_money, 0);
							}
						} else {
							return 0;
						}
					}
				});
	}

	// -------------------- INTERNALS --------------------

	private TollLevels newTollLevels(final List<Double> timeStructure,
			final List<Double> costStructure) {
		return new TollLevels(timeStructure.get(0), timeStructure.get(1),
				timeStructure.get(2), timeStructure.get(3),
				timeStructure.get(4), timeStructure.get(5),
				timeStructure.get(6), timeStructure.get(7),
				timeStructure.get(8), timeStructure.get(9),
				costStructure.get(0), costStructure.get(1),
				costStructure.get(2), this.scenario);
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	@Override
	public TollLevels newRandomDecisionVariable() {
		final List<Double> timeStructure = this.timeRandomizer
				.newRandomized(10);
		final List<Double> costStructure = this.costRandomizer.newRandomized(3);
		return this.newTollLevels(timeStructure, costStructure);
	}

	@Override
	public Collection<TollLevels> newRandomVariations(
			TollLevels decisionVariable) {

		final List<Double> oldTimeStructure = new ArrayList<>(10);
		oldTimeStructure.add(decisionVariable.level1start_s);
		oldTimeStructure.add(decisionVariable.morningLevel2start_s);
		oldTimeStructure.add(decisionVariable.morningLevel3start_s);
		oldTimeStructure.add(decisionVariable.morningLevel3end_s);
		oldTimeStructure.add(decisionVariable.morningLevel2end_s);
		oldTimeStructure.add(decisionVariable.eveningLevel2start_s);
		oldTimeStructure.add(decisionVariable.eveningLevel3start_s);
		oldTimeStructure.add(decisionVariable.eveningLevel3end_s);
		oldTimeStructure.add(decisionVariable.eveningLevel2end_s);
		oldTimeStructure.add(decisionVariable.level1end_s);
		final List<Double> newTimeStructure = this.timeRandomizer
				.newRandomized(oldTimeStructure);

		final List<Double> oldCostStructure = new ArrayList<>(3);
		oldCostStructure.add(decisionVariable.level1cost_money);
		oldCostStructure.add(decisionVariable.level2cost_money);
		oldCostStructure.add(decisionVariable.level3cost_money);
		final List<Double> newCostStructure = this.costRandomizer
				.newRandomized(oldCostStructure);

		final List<TollLevels> result = new ArrayList<>(1);
		result.add(this.newTollLevels(newTimeStructure, newCostStructure));
		return result;
	}
}
