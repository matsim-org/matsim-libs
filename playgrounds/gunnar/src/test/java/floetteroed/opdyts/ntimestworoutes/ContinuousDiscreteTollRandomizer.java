package floetteroed.opdyts.ntimestworoutes;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import floetteroed.opdyts.DecisionVariableRandomizer;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ContinuousDiscreteTollRandomizer
		implements
		DecisionVariableRandomizer<NTimesTwoRoutesDecisionVariableMixedDiscrCont> {

	// -------------------- CONSTANTS --------------------

	private final NTimesTwoRoutesSimulator simulator;

	private final int roadCnt;

	private final int tollCnt;

	final double sigmaToll;

	final double maxToll;

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	ContinuousDiscreteTollRandomizer(final NTimesTwoRoutesSimulator simulator,
			final int roadCnt, final int tollCnt, final double sigmaToll,
			final double maxToll, final Random rnd) {
		this.simulator = simulator;
		this.roadCnt = roadCnt;
		this.tollCnt = tollCnt;
		this.sigmaToll = sigmaToll;
		this.maxToll = maxToll;
		this.rnd = rnd;
	}

	// ---------- IMPLEMENTATION OF DecisionVariableRandomizer ----------

	public NTimesTwoRoutesDecisionVariableMixedDiscrCont newRandomDecisionVariable() {

		// where to put the tolls
		final List<Integer> allRoadIndices = new ArrayList<Integer>(
				this.roadCnt);
		for (int i = 0; i < this.roadCnt; i++) {
			allRoadIndices.add(i);
		}
		Collections.shuffle(allRoadIndices);
		final List<Integer> tollIndices = allRoadIndices.subList(0,
				this.tollCnt);

		// at what levels to set the tolls
		final List<Double> tollValues = new ArrayList<Double>(this.tollCnt);
		for (int i = 0; i < this.tollCnt; i++) {
			tollValues.add(this.maxToll * this.rnd.nextDouble());
		}

		return new NTimesTwoRoutesDecisionVariableMixedDiscrCont(
				this.simulator, this.roadCnt, tollIndices, tollValues);
	}

	private NTimesTwoRoutesDecisionVariableMixedDiscrCont newVariation(
			final NTimesTwoRoutesDecisionVariableMixedDiscrCont parent,
			final List<Double> randomNumbers, final double sign) {

		// randomize all toll values; the may become negative
		final List<Integer> newTollIndices = new ArrayList<Integer>();
		final List<Double> newTollValues = new ArrayList<Double>();
		for (int i = 0; i < parent.getTollValues().size(); i++) {
			newTollIndices.add(parent.getTollIndices().get(i));
			newTollValues.add(Math.min(this.maxToll, parent.getTollValues()
					.get(i) + this.sigmaToll * sign * randomNumbers.get(i)));
		}

		// remove and memorize all negativeTolls
		final List<Double> additionalTollValues = new ArrayList<Double>();
		for (int i = 0; i < newTollValues.size();) {
			if (newTollValues.get(i) < 0.0) {
				additionalTollValues.add(-newTollValues.get(i));
				newTollIndices.remove(i);
				newTollValues.remove(i);
			} else {
				i++;
			}
		}

		// create new toll indices
		final List<Integer> candidateIndices = new ArrayList<Integer>();
		for (int i = 0; i < this.roadCnt; i++) {
			if (!newTollIndices.contains(i)) {
				candidateIndices.add(i);
			}
		}
		Collections.shuffle(candidateIndices);

		// add new toll indices and corresponding values
		for (int i = 0; i < additionalTollValues.size(); i++) {
			newTollValues.add(additionalTollValues.get(i));
			newTollIndices.add(candidateIndices.get(i));
		}

		return new NTimesTwoRoutesDecisionVariableMixedDiscrCont(
				this.simulator, this.roadCnt, newTollIndices, newTollValues);
	}

	@Override
	public List<NTimesTwoRoutesDecisionVariableMixedDiscrCont> newRandomVariations(
			final NTimesTwoRoutesDecisionVariableMixedDiscrCont parent) {
		final List<Double> randomNumbers = new ArrayList<>(parent
				.getTollValues().size());
		for (int i = 0; i < parent.getTollValues().size(); i++) {
			randomNumbers.add(this.rnd.nextGaussian());
		}
		final List<NTimesTwoRoutesDecisionVariableMixedDiscrCont> result = new ArrayList<>(
				2);
		result.add(this.newVariation(parent, unmodifiableList(randomNumbers),
				+1.0));
		result.add(this.newVariation(parent, unmodifiableList(randomNumbers),
				-1.0));
		return result;
	}
}
