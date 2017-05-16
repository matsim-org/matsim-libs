package cba.resampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class Sampers2MATSimResampler<A extends Alternative> implements Runnable {

	// -------------------- CONSTANTS --------------------

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final List<A> alternatives;

	private final List<Double> acceptanceProbas;

	private final int maxResamples = 1000 * 1000;

	// -------------------- CONSTRUCTION --------------------

	public Sampers2MATSimResampler(final Random rnd, final Set<A> choiceSet, final int drawsWithReplacement) {

		this.rnd = new Random(rnd.nextLong());
		this.alternatives = new ArrayList<>(choiceSet.size());
		this.acceptanceProbas = new ArrayList<>(choiceSet.size());

		final List<Double> acceptanceWeights = new ArrayList<>(choiceSet.size());
		double maxAcceptanceWeight = Double.NEGATIVE_INFINITY;
		for (A alt : choiceSet) {
			final double acceptanceWeight = 1.0
					/ (1.0 - Math.pow(1.0 - alt.getSampersChoiceProbability(), drawsWithReplacement));
			maxAcceptanceWeight = Math.max(maxAcceptanceWeight, acceptanceWeight);
			acceptanceWeights.add(acceptanceWeight);
			this.alternatives.add(alt);
		}

		if (maxAcceptanceWeight < 1e-8) {
			throw new RuntimeException("maxAcceptanceWeight = " + maxAcceptanceWeight + " < 1e-8");
		}

		for (Double acceptanceWeight : acceptanceWeights) {
			this.acceptanceProbas.add(acceptanceWeight / maxAcceptanceWeight);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	private int drawMATSimRUChoiceIndex() {
		final List<Double> randomUtilityRealizations = new ArrayList<>(this.alternatives.size());
		for (Alternative alt : this.alternatives) {
			final double targetScore = alt.getSampersOnlyScore() + alt.getMATSimTimeScore();
			final double epsilonRealization = alt.getEpsilonDistribution().nextEpsilon();
			randomUtilityRealizations.add(targetScore + epsilonRealization);
			alt.setSampersEpsilonRealization(epsilonRealization);
		}
		int result = 0;
		double resultRU = randomUtilityRealizations.get(0);
		for (int i = 1; i < randomUtilityRealizations.size(); i++) {
			if (randomUtilityRealizations.get(i) > resultRU) {
				result = i;
				resultRU = randomUtilityRealizations.get(i);
			}
		}
		return result;
	}

	public A next() {
		int resample = 0;
		while (true) {
			final int candidateIndex = this.drawMATSimRUChoiceIndex();
			final double x = this.rnd.nextDouble();
			final double accProba = this.acceptanceProbas.get(candidateIndex);
			if ((x < accProba) || (++resample >= this.maxResamples)) {
				return this.alternatives.get(candidateIndex);
			}
		}
	}

	// -------------------- IMPLEMENTATION OF Runnable --------------------

	private A result = null;

	public A getResult() {
		return this.result;
	}

	@Override
	public void run() {
		System.out.println("Simulating a choice");
		this.result = this.next();
	}
}
