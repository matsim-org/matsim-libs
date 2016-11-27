package cba.resampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.Vector;

/**
 * Consider a population of of n=1...N travelers and equip each traveler with a
 * choice set Cn and a choice distribution Pn(i). This class tests if a set of
 * independent choice realizations {j1,...,jN}, one per traveler, is overall
 * consistent with the individual specific choice distributions.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ResamplingTest {

	// -------------------- MEMBERS --------------------

	private final List<Double> logsOfRealizedChoiceProbas = new ArrayList<>();

	private final List<Double> expOfLogsOfChoiceProbas = new ArrayList<>();

	private final List<Double> varOfLogsOfChoiceProbas = new ArrayList<>();

	// -------------------- CONSTRUCTION --------------------

	public ResamplingTest() {
	}

	// -------------------- DATA REGISTRATION --------------------

	public void registerChoiceAndDistribution(final int choiceIndex, final List<Double> probabilities) {
		if (probabilities.get(choiceIndex) == 0) {
			throw new RuntimeException("Cannot register an impossible event.");
		}
		double expLnChoice = 0;
		double expLnChoice2 = 0;
		for (double proba : probabilities) {
			if (proba > 0) {
				final double logProba = Math.log(proba);
				expLnChoice += proba * logProba;
				expLnChoice2 += proba * logProba * logProba;
			}
		}
		this.logsOfRealizedChoiceProbas.add(Math.log(probabilities.get(choiceIndex)));
		this.expOfLogsOfChoiceProbas.add(expLnChoice);
		this.varOfLogsOfChoiceProbas.add(expLnChoice2 - expLnChoice * expLnChoice);
	}

	// -------------------- TEST RESULTS --------------------

	public double getStatistic() {

		final double _N = this.logsOfRealizedChoiceProbas.size();

		double meanOfLogsOfRealizedChoiceProbas = 0;
		for (double real : this.logsOfRealizedChoiceProbas) {
			meanOfLogsOfRealizedChoiceProbas += real;
		}
		meanOfLogsOfRealizedChoiceProbas /= _N;

		double meanOfExpOfLogsOfChoiceProbas = 0;
		for (double exp : this.expOfLogsOfChoiceProbas) {
			meanOfExpOfLogsOfChoiceProbas += exp;
		}
		meanOfExpOfLogsOfChoiceProbas /= _N;

		double meanOfVarOfLogsOfChoiceProbas = 0;
		for (double var : this.varOfLogsOfChoiceProbas) {
			meanOfVarOfLogsOfChoiceProbas += var;
		}
		meanOfVarOfLogsOfChoiceProbas /= (_N * _N);

		final double statistic = Math.abs(meanOfLogsOfRealizedChoiceProbas - meanOfExpOfLogsOfChoiceProbas)
				/ Math.sqrt(meanOfVarOfLogsOfChoiceProbas);

		return statistic;
	}

	static List<Double> newDistr(final int size, final Random rnd) {
		final List<Double> result = new ArrayList<>(size);
		double cumProba = 0;
		for (int i = 0; i < (size - 1); i++) {
			final double proba = (1.0 - cumProba) * rnd.nextDouble();
			cumProba += proba;
			result.add(proba);
		}
		result.add(1.0 - cumProba);
		return result;
	}

	public static void main(String[] args) {

		final Random rnd = new Random();
		final int choiceSetSize = 10;
		final int popSize = 1000;

		final ResamplingTest trueTest = new ResamplingTest();
		final ResamplingTest falseTest = new ResamplingTest();

		for (int n = 0; n < popSize; n++) {
			final List<Double> trueDistr = newDistr(choiceSetSize, rnd);
			final List<Double> falseDistr = newDistr(choiceSetSize, rnd);
			final int trueChoice = MathHelpers.draw(new Vector(trueDistr), rnd);
			final int falseChoice = MathHelpers.draw(new Vector(falseDistr), rnd);
			trueTest.registerChoiceAndDistribution(trueChoice, trueDistr);
			falseTest.registerChoiceAndDistribution(falseChoice, trueDistr);
			System.out.println(trueTest.getStatistic() + "\t" + falseTest.getStatistic());
		}
	}
}
