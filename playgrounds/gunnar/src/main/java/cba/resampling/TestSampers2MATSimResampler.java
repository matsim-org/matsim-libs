package cba.resampling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.math.MultinomialLogit;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class TestSampers2MATSimResampler {

	public static void main(String[] args) {

		// ---------- SET UP THE PROBLEM ----------

		final Random rnd = new Random();

		final int universalChoiceSetSize = 10 * 1000;

		final List<Double> sampersScores = new ArrayList<>(universalChoiceSetSize);
		final List<Double> sampersTimeScores = new ArrayList<>(universalChoiceSetSize);
		final List<Double> matsimTimeScores = new ArrayList<>(universalChoiceSetSize);
		for (int i = 0; i < universalChoiceSetSize; i++) {
			final double sampersNoTimeScore = rnd.nextGaussian();
			final double sampersTimeScore = rnd.nextGaussian();
			sampersScores.add(sampersNoTimeScore + sampersTimeScore);
			sampersTimeScores.add(sampersTimeScore);
			matsimTimeScores.add(2 * sampersTimeScore);
		}

		final double scale = 1.0;

		final MultinomialLogit sampersChoiceModel = new MultinomialLogit(universalChoiceSetSize, 1);
		sampersChoiceModel.setUtilityScale(scale);
		for (int i = 0; i < universalChoiceSetSize; i++) {
			sampersChoiceModel.setCoefficient(0, 1.0);
			sampersChoiceModel.setAttribute(i, 0, sampersScores.get(i));
			sampersChoiceModel.setASC(i, 0);
		}
		sampersChoiceModel.enforcedUpdate();

		final MultinomialLogit matsimChoiceModel = new MultinomialLogit(universalChoiceSetSize, 1);
		matsimChoiceModel.setUtilityScale(scale);
		for (int i = 0; i < universalChoiceSetSize; i++) {
			matsimChoiceModel.setCoefficient(0, 1.0);
			matsimChoiceModel.setAttribute(i, 0,
					sampersScores.get(i) - sampersTimeScores.get(i) + matsimTimeScores.get(i));
			matsimChoiceModel.setASC(i, 0);
		}
		matsimChoiceModel.enforcedUpdate();

		final MyGumbelDistribution logitEpsDistr = new MyGumbelDistribution(scale);
		final Map<Alternative, Double> alternative2sampersChoiceProba = new LinkedHashMap<>();
		final Map<Alternative, Double> alternative2matsimChoiceProba = new LinkedHashMap<>();
		for (int i = 0; i < universalChoiceSetSize; i++) {
			final Alternative alt = new DummySampersAlternative(sampersScores.get(i), sampersTimeScores.get(i),
					matsimTimeScores.get(i), sampersChoiceModel.getProbs().get(i), logitEpsDistr);
			alternative2sampersChoiceProba.put(alt, sampersChoiceModel.getProbs().get(i));
			alternative2matsimChoiceProba.put(alt, matsimChoiceModel.getProbs().get(i));
		}

		// ---------- CREATE SUBSAMPLES ----------

		final int totalReplications = 10 * 1000;
		final int subsampleDraws = 100;
		final boolean doResampling = true;

		final Map<Alternative, Integer> resample2cnt = new LinkedHashMap<>();
		for (int replication = 0; replication < totalReplications; replication++) {
			final Set<Alternative> matsimChoiceSet = new LinkedHashSet<>();
			for (int i = 0; i < subsampleDraws; i++) {
				matsimChoiceSet.add(MathHelpers.draw(alternative2sampersChoiceProba, rnd));
			}
			final Alternative resample;
			if (doResampling) {
				final Sampers2MATSimResampler resampler = new Sampers2MATSimResampler(rnd, matsimChoiceSet,
						subsampleDraws);
				resample = resampler.next();
			} else {
				resample = (new ArrayList<>(matsimChoiceSet)).get(rnd.nextInt(matsimChoiceSet.size()));
			}
			final Integer cnt = resample2cnt.get(resample);
			if (cnt == null) {
				resample2cnt.put(resample, 1);
			} else {
				resample2cnt.put(resample, cnt + 1);
			}
		}

		// ---------- ANALYZE ----------

		// System.out.println("value\tfrequency\tapprox.probability");
		System.out.println("frequency\tapprox.probability");
		for (Map.Entry<Alternative, Integer> alternative2cntEntry : resample2cnt.entrySet()) {
			// System.out.print(value2cntEntry.getKey());
			// System.out.print("\t");
			System.out.print(((double) alternative2cntEntry.getValue()) / totalReplications);
			System.out.print("\t");
			System.out.println(alternative2matsimChoiceProba.get(alternative2cntEntry.getKey()));
		}

		System.out.println();
		System.out.println("DONE");

	}
}
