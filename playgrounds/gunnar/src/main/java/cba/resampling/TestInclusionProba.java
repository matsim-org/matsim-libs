package cba.resampling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import floetteroed.utilities.math.MathHelpers;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TestInclusionProba {

	public static void main(String[] args) {

		final Random rnd = new Random();

		// ----- CREATE UNIVERSAL CHOICE SET -----

		final int universalChoiceSetSize = 10 * 1000;
		final List<Integer> values = new ArrayList<>(universalChoiceSetSize);
		for (Integer i = 1; i <= universalChoiceSetSize; i++) {
			values.add(i);
		}

		System.out.println("UNIVERSAL CHOICE SET");
		System.out.println(values);
		System.out.println();

		// ----- CREATE PROPOSAL DISTRIBUTION -----

		final Map<Integer, Double> values2proposalProba = new LinkedHashMap<>();
		{
			final List<Double> proposalProbas = new ArrayList<>(values.size());
			double xSum = 0;
			for (int i = 0; i < values.size(); i++) {
				final double x = -Math.log(rnd.nextDouble() + 1e-8);
				proposalProbas.add(x);
				xSum += x;
			}
			for (int i = 0; i < values.size(); i++) {
				values2proposalProba.put(values.get(i), proposalProbas.get(i) / xSum);
			}
		}

		System.out.println("PROPOSAL PROBABILITIES");
		for (int i = 0; i < values.size(); i++) {
			System.out.println(values.get(i) + "\t" + values2proposalProba.get(values.get(i)));
		}
		System.out.println();

		// ----- CREATE MANY SUBSAMPLES -----

		final int subsampleDraws = 100;
		final int subsampleCnt = 10 * 1000;

		final Map<Integer, Integer> value2cnt = new LinkedHashMap<>();
		for (int replication = 0; replication < subsampleCnt; replication++) {
			final Set<Integer> subsample = new LinkedHashSet<>();
			for (int i = 0; i < subsampleDraws; i++) {
				subsample.add(MathHelpers.draw(values2proposalProba, rnd));
			}
			for (Integer value : subsample) {
				final Integer cnt = value2cnt.get(value);
				if (cnt == null) {
					value2cnt.put(value, 1);
				} else {
					value2cnt.put(value, cnt + 1);
				}
			}
		}

		System.out.println("SUBSAMPLES");
		System.out.println("Created " + subsampleCnt + " subsamples by performing " + subsampleDraws
				+ " independent draws and removing duplicates.");
		System.out.println();

		// ----- APPROXIMATE SUBSAMPLE PROBABILITIES -----

		System.out.println("value\tfrequency\tapprox.probability");
		for (Map.Entry<Integer, Integer> value2cntEntry : value2cnt.entrySet()) {
			System.out.print(value2cntEntry.getKey());
			System.out.print("\t");
			System.out.print(((double) value2cntEntry.getValue()) / subsampleCnt);
			System.out.print("\t");
			System.out.println(1.0 - Math.pow(1.0 - values2proposalProba.get(value2cntEntry.getKey()), subsampleDraws));
		}

		System.out.println();
		System.out.println("DONE");
	}
}
