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
public class Sampers2MATSimResampler {

	// -------------------- CONSTANTS --------------------

	// -------------------- MEMBERS --------------------

	private final Random rnd;

	private final List<Alternative> alternatives;

	private final List<Double> acceptanceProbas;

	private final int maxResamples = 1000 * 1000;

	// -------------------- CONSTRUCTION --------------------

	public Sampers2MATSimResampler(final Random rnd, final Set<? extends Alternative> choiceSet,
			final int drawsWithReplacement) {

		this.rnd = rnd;
		this.alternatives = new ArrayList<>(choiceSet.size());
		this.acceptanceProbas = new ArrayList<>(choiceSet.size());

		final List<Double> acceptanceWeights = new ArrayList<>(choiceSet.size());
		double maxAcceptanceWeight = Double.NEGATIVE_INFINITY;
		for (Alternative alt : choiceSet) {
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

		// System.out.println(acceptanceWeights);
		// System.out.println(this.acceptanceProbas);
		// System.exit(0);
	}

	// -------------------- IMPLEMENTATION --------------------

	private int drawMATSimRUChoiceIndex() {
		final List<Double> randomUtilityRealizations = new ArrayList<>(this.alternatives.size());
		for (Alternative alt : this.alternatives) {
			final double targetScore = alt.getSampersOnlyScore() + alt.getMATSimTimeScore();
			randomUtilityRealizations.add(targetScore + alt.getEpsilonDistribution().nextEpsilon());
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

	public Alternative next() {
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

}
