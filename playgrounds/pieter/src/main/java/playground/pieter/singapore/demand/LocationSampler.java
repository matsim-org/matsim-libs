package playground.pieter.singapore.demand;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

import org.matsim.core.utils.collections.Tuple;


public class LocationSampler implements Serializable {
	private final String activityName;
	private final String[] facilityIds;
	private final double[] facilityCaps;
	private final double[] probabilities;

	// double[] cumulativeProbs;
	public LocationSampler(String activityName, String[] facilityIds,
			double[] facilityCaps) {
		super();
		this.activityName = activityName;
		this.facilityIds = facilityIds;
		this.facilityCaps = facilityCaps;
		this.probabilities = new double[facilityCaps.length];
		// this.cumulativeProbs = new double[facilityCaps.length];
		removeZeroes();
	}

	private void removeZeroes() {
		double min = 100000000000000000000d;
		// double cumTotal[] = new double[facilityCaps.length];
		double total = 0d;
        for (double facilityCap : facilityCaps) {
            if (facilityCap < min && facilityCap > 0)
                min = facilityCap;
            total += facilityCap;
        }
//		min = 0.1*min;
		for (int i = 0; i < facilityCaps.length; i++) {
			if (facilityCaps[i] < min)
				facilityCaps[i] = min;
			probabilities[i] = facilityCaps[i] / total;
		}
	}

	Tuple<String[], double[]> sampleLocations(int noLocations) {
		// if there are too many requested, return the full list of faciltiies
		if (noLocations >= this.facilityIds.length)
			return new Tuple<>(facilityIds, facilityCaps);

		WeightedRandPerm wrp = new WeightedRandPerm(new Random(), facilityCaps);
		wrp.reset(facilityCaps.length);
		String[] ids = new String[noLocations];
		double[] caps = new double[noLocations];
		for (int i = 0; i < noLocations; i++) {
			int idx = wrp.next();
			ids[i] = facilityIds[idx];
			caps[i] = facilityCaps[idx];
		}
		return new Tuple<>(ids, caps);

	}

//	Tuple<String[], double[]> sampleLocationsWithoutReplacement(int noLocations) {
//		// if there are too many requested, return the full list of faciltiies
//		if ((double) noLocations >= (double) 0.5 * this.facilityIds.length)
//			return new Tuple<String[], double[]>(facilityIds, facilityCaps);
//
//		EmpiricalWalker empW = new EmpiricalWalker(this.probabilities,
//				Empirical.LINEAR_INTERPOLATION, new DRand());
//
//		HashSet<Integer> uniqueIndexes = new HashSet<Integer>();
//		while (uniqueIndexes.size() < noLocations) {
//			uniqueIndexes.add(empW.nextInt());
//
//		}
//
//		String[] ids = new String[noLocations];
//		double[] caps = new double[noLocations];
//		int i = 0;
//		for (int idx : uniqueIndexes) {
//			ids[i] = facilityIds[idx];
//			caps[i] = facilityCaps[idx];
//			i++;
//		}
//		Tuple<String[], double[]> t = new Tuple<String[], double[]>(ids, caps);
//		return t;
//
//	}
//
//	String weightedSampleSingleLocation() {
//		// if there are too many requested, return the full list of faciltiies
//
//		EmpiricalWalker empW = new EmpiricalWalker(this.probabilities,
//				Empirical.NO_INTERPOLATION, new DRand());
//		return facilityIds[empW.nextInt()];
//	
//
//	}

	Tuple<String[], double[]> sampleLocationsNoWeight(int numberOfLocations) {
		// if there are too many requested, return the full list of faciltiies
		if (numberOfLocations >= this.facilityIds.length)
			return new Tuple<>(facilityIds, facilityCaps);
		int N = this.facilityCaps.length; // from 0, 1, ..., N-1

		// create permutation 0, 1, ..., N-1
		int[] perm = new int[N];
		for (int i = 0; i < N; i++)
			perm[i] = i;

		// create random sample in perm[0], perm[1], ..., perm[M-1]
		for (int i = 0; i < numberOfLocations; i++) {

			// random integer between i and N-1
			int r = i + (int) (Math.random() * (N - i));

			// swap elements at indices i and r
			int t = perm[r];
			perm[r] = perm[i];
			perm[i] = t;
		}

		// print results
		String[] ids = new String[numberOfLocations];
		double[] caps = new double[numberOfLocations];
		for (int i = 0; i < numberOfLocations; i++) {
			ids[i] = facilityIds[perm[i]];
			caps[i] = facilityCaps[perm[i]];
		}

		return new Tuple<>(ids, caps);

	}

	public static void main(String args[]) {
		String[] ids = { "0", "1", "2", "3", "4" };
		double weights[] = { 0.001, 0.001, 2, 0.001, 0.001 };
		LocationSampler ls = new LocationSampler("test", ids, weights);
		int[] sampleCountsSet = { 0, 0, 0, 0, 0 };
		int[] sampleCountsNormal = { 0, 0, 0, 0, 0 };
		int[] sampleSingle = { 0, 0, 0, 0, 0 };
		int[] sampleNoweight = { 0, 0, 0, 0, 0 };
		for (int i = 0; i < 100000; i++) {
			String[] selected;
//			selected = ls.sampleLocationsWithoutReplacement(2)
//					.getFirst();
//			for (String r : selected) {
//				sampleCountsSet[Integer.parseInt(r)]++;
//			}
			selected = ls.sampleLocations(2).getFirst();
			for (String r : selected) {
				sampleCountsNormal[Integer.parseInt(r)]++;
			}
			selected = ls.sampleLocationsNoWeight(2).getFirst();
			for (String r : selected) {
				sampleNoweight[Integer.parseInt(r)]++;
			}
			
			String single = ls.sampleLocations(1).getFirst()[0];

			sampleSingle[Integer.parseInt(single)]++;

		}
		System.out.println("proper: "+Arrays.toString(sampleCountsNormal));
		System.out.println("setwisewalker: "+Arrays.toString(sampleCountsSet));
		System.out.println("singlewalker: "+Arrays.toString(sampleSingle));
		System.out.println("noweight: "+Arrays.toString(sampleNoweight));

	}
}
