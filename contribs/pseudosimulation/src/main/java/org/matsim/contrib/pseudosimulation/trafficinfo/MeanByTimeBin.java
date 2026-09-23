package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.matsim.core.trafficmonitoring.TimeBinUtils;

/**
 * Running mean of observed durations per time-of-day bin.
 *
 * <p>
 * The mean is kept incrementally rather than as a sum and a count so that a bin observed many
 * times cannot overflow, and is read back deterministically: two lookups of the same bin always
 * agree. That is the property pseudo-simulation needs, and the one a sampled measure lacks.
 */
final class MeanByTimeBin {

	private final double[] means;
	private final int[] counts;

	MeanByTimeBin(int binCount) {
		this.means = new double[binCount];
		this.counts = new int[binCount];
	}

	int binCount() {
		return means.length;
	}

	synchronized void add(int bin, double value) {
		counts[bin]++;
		means[bin] += (value - means[bin]) / counts[bin];
	}

	synchronized double mean(int bin) {
		return means[bin];
	}

	synchronized int count(int bin) {
		return counts[bin];
	}

	synchronized void reset() {
		java.util.Arrays.fill(means, 0.0);
		java.util.Arrays.fill(counts, 0);
	}

	int binOf(double time, double binSize) {
		return TimeBinUtils.getTimeBinIndex(time, binSize, means.length);
	}
}
