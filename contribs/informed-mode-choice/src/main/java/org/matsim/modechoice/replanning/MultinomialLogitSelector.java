package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.ModeChoiceWeightScheduler;
import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

;

/**
 * The MultinomialLogitSelector collects a set of candidates with given
 * utilities and then selects on according to the multinomial logit model.
 * All utilities are normalized beforehand, so that they are scale and location invariant.
 * For each candidate (i) a utility is calculated as:
 *
 * <code>P(i) = exp( Ui ) / Sum( U1 + U2 + ... + Un )</code>
 * <p>
 * With a scale parameter of zero this class is equivalent to the best choice.
 *
 * @author sebhoerl
 * @author rakow
 */
public class MultinomialLogitSelector implements PlanSelector {


	private final static Logger log = LogManager.getLogger(MultinomialLogitSelector.class);
	private final double scale;
	private final Random rnd;

	/**
	 * @param scale if scale is 0, always the best option will be picked.
	 */
	public MultinomialLogitSelector(double scale, Random rnd) {
		this.scale = scale;
		this.rnd = rnd;
	}

	@Nullable
	@Override
	public PlanCandidate select(Collection<PlanCandidate> candidates) {

		if (candidates.isEmpty())
			return null;

		if (candidates.size() == 1)
			return candidates.iterator().next();

		// Short-path to do completely random selection
		if (scale == Double.POSITIVE_INFINITY) {
			return candidates.stream().skip(rnd.nextInt(candidates.size())).findFirst().orElse(null);
		}

		// if two option are exactly the same this will be incorrect
		// for very small scales, exp overflows, this function needs to return best solution at this point
		if (scale <= 1 / 700d) {
			return candidates.stream().sorted().findFirst().orElse(null);
		}

		// III) Create a probability distribution over candidates
		DoubleList density = new DoubleArrayList(candidates.size());
		List<PlanCandidate> pcs = new ArrayList<>(candidates);

		double min = candidates.stream().mapToDouble(PlanCandidate::getUtility).min().orElseThrow();
		double scale = candidates.stream().mapToDouble(PlanCandidate::getUtility).max().orElseThrow() - min;

		// For very small differences the small is ignored
		if (scale < 1e-6)
			scale = 1;

		for (PlanCandidate candidate : pcs) {
			double utility = (candidate.getUtility() - min) / scale;
			density.add(Math.exp(utility / this.scale));
		}

		// IV) Build a cumulative density of the distribution
		DoubleList cumulativeDensity = new DoubleArrayList(density.size());
		double totalDensity = 0.0;

		for (int i = 0; i < density.size(); i++) {
			totalDensity += density.getDouble(i);
			cumulativeDensity.add(totalDensity);
		}

		// V) Perform a selection using the CDF
		double pointer = rnd.nextDouble() * totalDensity;

		int selection = (int) cumulativeDensity.doubleStream().filter(f -> f < pointer).count();
		return pcs.get(selection);
	}


	/**
	 * Write debug information regarding choice probability.
	 */
	public static void main(String[] args) throws IOException {

		if (args.length < 2) {
			System.out.println("Provide at least two choices.");
			return;
		}

		for (InformedModeChoiceConfigGroup.Schedule schedule : List.of(InformedModeChoiceConfigGroup.Schedule.linear, InformedModeChoiceConfigGroup.Schedule.quadratic, InformedModeChoiceConfigGroup.Schedule.cubic,
				InformedModeChoiceConfigGroup.Schedule.exponential, InformedModeChoiceConfigGroup.Schedule.trigonometric)) {

			List<PlanCandidate> candidates = new ArrayList<>();

			for (String arg : args) {
				candidates.add(new PlanCandidate(new String[]{arg}, Double.parseDouble(arg)));
			}

			Path p = Path.of("choices-" + args.length + "-" + schedule + ".tsv");

			System.out.println("Writing to " + p);

			CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(p), CSVFormat.MONGODB_TSV);

			printer.print("x");
			printer.print("weight");
			for (int i = 0; i < candidates.size(); i++)
				printer.print(String.format(Locale.US, "choice %d = %.2f", i, candidates.get(i).getUtility()));

			printer.println();

			double start = 2.5;
			int n = 100;

			for (int i = 0; i <= n; i++) {

				double weight = ModeChoiceWeightScheduler.anneal(schedule, start, n, i);

				System.out.println("Sampling weight " + weight + " @ iteration " + i);

				MultinomialLogitSelector selector = new MultinomialLogitSelector(weight, new Random());
				double[] prob = selector.sample(100_000, candidates);

				printer.print(i);
				printer.print(weight);
				for (double v : prob) {
					printer.print(v);
				}
				printer.println();
			}

			printer.close();
		}
	}

}
