package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import it.unimi.dsi.fastutil.objects.Object2ByteOpenHashMap;

import java.util.Arrays;

/**
 * This class finds the best solutions by doing an exhaustive search over the best possible combinations of modes.
 */
final class ModeChoiceSearch {

	/**
	 * Stores the estimates for all modes by trip x mode
	 */
	final double[][] estimates;

	/**
	 * Mode to index mapping.
	 */
	final Byte2ObjectMap<String> inv;
	final int depth;

	private final Object2ByteMap<String> mapping;
	private final double maxSize;

	/**
	 * Constructor
	 *
	 * @param trips number of trips
	 * @param modes max number of modes per trip
	 */
	ModeChoiceSearch(int trips, int modes) {

		this.estimates = new double[trips][modes];
		this.mapping = new Object2ByteOpenHashMap<>();
		this.inv = new Byte2ObjectOpenHashMap<>();
		// Number of options per trip, +1 to encode null value
		this.depth = modes + 1;

		maxSize = Math.pow(depth, trips);
		if (!Double.isFinite(maxSize))
			throw new IllegalArgumentException("Too many mode combinations: %s ^ %s".formatted(modes, trips));

		clear();
	}

	/**
	 * Iterate over all solutions, starting with the best possible combination.
	 * {@link #addEstimates(String, double[])} needs to be called for every search.
	 *
	 * @param result mode assignment will be written into this array, no memory is allocated during search.
	 * @return iterator returning the summed utility.
	 */
	public ModeIterator iter(String[] result) {

		assert result.length == estimates.length;

		byte[] path = new byte[result.length];

		// minimum estimates for each trip
		double[] maxs = new double[result.length];

		double total = 0;
		for (int i = 0; i < result.length; i++) {

			double max = Double.NEGATIVE_INFINITY;
			path[i] = -1;

			for (byte j = 0; j < estimates[i].length; j++) {

				if (estimates[i][j] > max) {
					max = estimates[i][j];
					path[i] = j;
				}

			}

			maxs[i] = max;

			if (max != Double.NEGATIVE_INFINITY)
				total += max;
		}

		long b = 1;
		for (int i = 0; i < this.estimates.length - 1; i++) {
			b *= depth;
		}

		if (maxSize < Integer.MAX_VALUE) {
			return new ModeIntIterator(result, maxs, new ModeIntIterator.Entry(path, depth, 0), total, (int) b, this);
		}

		if (maxSize < Long.MAX_VALUE) {
			return new ModeLongIterator(result, maxs, new ModeLongIterator.Entry(path, depth, 0), total, b, this);
		}

		return new ModeArrayIterator(result, maxs, new ModeArrayIterator.Entry(path, 0), total, this);
	}

	/**
	 * Copy estimates into the internal map.
	 */
	public void addEstimates(String mode, double[] values) {
		addEstimates(mode, values, null, null);
	}

	/**
	 * Copy estimates into the internal map.
	 * @param mode added mode
	 * @param values utility estimates
	 * @param mask only use estimates where the mask is true
	 * @param filter only use estimates where the filter is false
	 */
	public void addEstimates(String mode, double[] values, boolean[] mask, boolean[] filter) {

		byte idx = mapping.computeIfAbsent(mode, k -> (byte) mapping.size());
		inv.putIfAbsent(idx, mode);

		// estimates needs to be accessed by each trip index first and then by mode
		for (int i = 0; i < values.length; i++) {
			if ( (mask == null || mask[i]) && (filter == null || !filter[i]) )
				estimates[i][idx] = values[i];
		}
	}

	public void clear() {

		for (double[] estimate : estimates) {
			Arrays.fill(estimate, Double.NEGATIVE_INFINITY);
		}

		mapping.clear();
		inv.clear();
	}

	/**
	 * Check if any estimates are present-
	 */
	public boolean isEmpty() {
		return mapping.isEmpty();
	}

	@Override
	public String toString() {

		StringBuilder b = new StringBuilder();

		for (int i = 0; i < estimates.length; i++) {
			double[] estimate = estimates[i];

			for (byte j = 0; j < estimate.length; j++) {

				b.append(inv.get(j));
				b.append("=>");
				b.append(estimate[j]);

				if (j != estimate.length - 1)
					b.append(", ");
			}

			if (i != estimate.length - 1)
				b.append(" || ");
		}

		return b.toString();
	}

	/**
	 * Byte representation as string array.
	 */
	void convert(byte[] path, String[] modes) {
		for (int i = 0; i < path.length; i++) {
			String m = inv.get(path[i]);
			// Pre-defined entries are not touched
			if (m != null)
				modes[i] = m;
		}
	}

}
