package cba.resampling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import floetteroed.utilities.Tuple;
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

	private void registerChoiceData(final double logOfRealizedChoiceProba, final double expOfLogOfChoiceProba,
			final double varOfLogOfChoiceProba) {
		this.logsOfRealizedChoiceProbas.add(logOfRealizedChoiceProba);
		this.expOfLogsOfChoiceProbas.add(expOfLogOfChoiceProba);
		this.varOfLogsOfChoiceProbas.add(varOfLogOfChoiceProba);
	}

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
		this.registerChoiceData(Math.log(probabilities.get(choiceIndex)), expLnChoice,
				expLnChoice2 - expLnChoice * expLnChoice);
		// this.logsOfRealizedChoiceProbas.add(Math.log(probabilities.get(choiceIndex)));
		// this.expOfLogsOfChoiceProbas.add(expLnChoice);
		// this.varOfLogsOfChoiceProbas.add(expLnChoice2 - expLnChoice *
		// expLnChoice);
	}

	// -------------------- TEST RESULTS --------------------

	public double getStatistic() {

		double sumOfLogsOfRealizedChoiceProbas = 0;
		for (double real : this.logsOfRealizedChoiceProbas) {
			sumOfLogsOfRealizedChoiceProbas += real;
		}

		double sumOfExpOfLogsOfChoiceProbas = 0;
		for (double exp : this.expOfLogsOfChoiceProbas) {
			sumOfExpOfLogsOfChoiceProbas += exp;
		}

		double sumOfVarOfLogsOfChoiceProbas = 0;
		for (double var : this.varOfLogsOfChoiceProbas) {
			sumOfVarOfLogsOfChoiceProbas += var;
		}

		final double statistic = (sumOfLogsOfRealizedChoiceProbas - sumOfExpOfLogsOfChoiceProbas)
				/ Math.sqrt(sumOfVarOfLogsOfChoiceProbas);

		return statistic;
	}

	public Tuple<Double, Double> getBootstrap95Percent(final int bootstrapDraws, final Random rnd) {

		final LinkedList<Double> resampledStatisticList = new LinkedList<>();
		for (int bootstrapDraw = 0; bootstrapDraw < bootstrapDraws; bootstrapDraw++) {
			final ResamplingTest bootstrapTest = new ResamplingTest();
			for (int i = 0; i < this.logsOfRealizedChoiceProbas.size(); i++) {
				final int resampleIndex = rnd.nextInt(this.logsOfRealizedChoiceProbas.size());
				bootstrapTest.registerChoiceData(this.logsOfRealizedChoiceProbas.get(resampleIndex),
						this.expOfLogsOfChoiceProbas.get(resampleIndex),
						this.varOfLogsOfChoiceProbas.get(resampleIndex));
			}
			resampledStatisticList.add(bootstrapTest.getStatistic());
		}

		Collections.sort(resampledStatisticList);

		// remove 2.5 percent on either end
		for (int remove = 0; remove < (0.025 * bootstrapDraws); remove++) {
			resampledStatisticList.removeFirst();
			resampledStatisticList.removeLast();
		}

		return new Tuple<Double, Double>(resampledStatisticList.getFirst(), resampledStatisticList.getLast());
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
