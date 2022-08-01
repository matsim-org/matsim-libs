package org.matsim.modechoice.replanning;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

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

		// if two option are exactly the same this will be incorrect
		if (scale <= 0) {
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

		List<PlanCandidate> candidates = new ArrayList<>();

		for (String arg : args) {
			candidates.add(new PlanCandidate(null, Double.parseDouble(arg)));
		}

		CSVPrinter printer = new CSVPrinter(Files.newBufferedWriter(Path.of("choices-sample.tsv")), CSVFormat.MONGODB_TSV);

		printer.print("weight");
		for (int i = 0; i < candidates.size(); i++)
			printer.print(String.format("choice %d = %.2f", i, candidates.get(i).getUtility()));

		printer.println();

		double start = 100;

		for (int i = 0; i <= 100; i++) {

			double weight = start - i * start / 100;

			System.out.println("Sampling weight " + weight + " ...");

			MultinomialLogitSelector selector = new MultinomialLogitSelector(weight, new Random());
			double[] prob = selector.sample(100_000, candidates);

			printer.print(weight);
			for (double v : prob) {
				printer.print(v);
			}
			printer.println();
		}

		printer.close();
	}

}
