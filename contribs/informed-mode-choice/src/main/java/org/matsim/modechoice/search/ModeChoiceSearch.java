package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;
import it.unimi.dsi.fastutil.objects.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This class finds the best solutions by doing an exhaustive search over the best possible combinations of modes.
 */
final class ModeChoiceSearch {

	/**
	 * Stores the estimates for all modes by trip x mode
	 */
	private final double[][] estimates;

	/**
	 * Mode to index mapping.
	 */
	private final Object2ByteMap<String> mapping;
	private final Byte2ObjectMap<String> inv;

	/**
	 * Constructor
	 *
	 * @param trips number of trips
	 * @param modes max number of modes per trip
	 */
	@SuppressWarnings("unchecked")
	ModeChoiceSearch(int trips, int modes) {

		estimates = new double[trips][modes];
		mapping = new Object2ByteOpenHashMap<>();
		inv = new Byte2ObjectOpenHashMap<>();

		clear();
	}

	/**
	 * Iterate over all solutions, starting with the best possible combination.
	 * {@link #addEstimates(String, double[])} needs to be called for every search.
	 *
	 * @param result mode assignment will be written into this array, no memory is allocated during search.
	 * @return iterator returning the summed utility.
	 */
	public DoubleIterator iter(String[] result) {

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


		return new Iterator(result, maxs, new Entry(path, 0), total);
	}

	/**
	 * Copy estimates into the internal map.
	 */
	public void addEstimates(String mode, double[] values) {
		addEstimates(mode, values, null);
	}

	public void addEstimates(String mode, double[] values, boolean[] mask) {

		byte idx = mapping.computeIfAbsent(mode, k -> (byte) mapping.size());
		inv.putIfAbsent(idx, mode);

		// estimates needs to be accessed by each trip index first and then by mode
		for (int i = 0; i < values.length; i++) {
			if (mask == null || mask[i])
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
	private void convert(byte[] path, String[] modes) {
		for (int i = 0; i < path.length; i++) {
			String m = inv.get(path[i]);
			// Pre-defined entries are not touched
			if (m != null)
				modes[i] = m;
		}
	}


	/**
	 * Searches all possible combination by using a heap.
	 */
	final class Iterator implements DoubleIterator {

		// This implementation uses a heap and stores all seen combinations
		// Because the graph structure is fixed, it does not need to be as complicated as other k shortest path algorithms
		// Two major improvements may be investigated:
		// Could this be implemented with pre allocated memory and without objects ?
		// Do all seen combinations need to be stored ?
		// This is the case in Yens algorithms and other and will consume a lot of memory, when many top k paths are generated and a lot are thrown away.

		private final String[] result;
		private final double[] max;
		private final double best;
		private final ObjectHeapPriorityQueue<Entry> heap;
		private final Set<Entry> seen;


		public Iterator(String[] result, double[] max, Entry shortest, double best) {
			this.result = result;
			this.max = max;
			this.best = best;
			this.heap = new ObjectHeapPriorityQueue<>();
			this.seen = new HashSet<>();

			heap.enqueue(shortest);
			seen.add(shortest);
		}

		@Override
		public double nextDouble() {

			Entry entry = heap.dequeue();

			for (int i = 0; i < result.length; i++) {

				byte mode = -1;
				byte originalMode = entry.modes[i];

				// This mode had no options
				if (originalMode == -1)
					continue;

				double min = estimates[i][originalMode];
				double max = Double.NEGATIVE_INFINITY;

				// search for a deviation that is worse than the current mode

				for (byte j = 0; j < estimates[i].length; j++) {
					if (estimates[i][j] <= min && j != originalMode && estimates[i][j] > max) {
						max = estimates[i][j];
						mode = j;
					}
				}

				if (mode != -1) {
					byte[] path = Arrays.copyOf(entry.modes, entry.modes.length);
					path[i] = mode;

					// recompute the deviation from the maximum
					// there might be a way to store and update this, without recomputing

					double dev = 0;
					for (int j = 0; j < result.length; j++) {
						byte legMode = path[j];
						if (legMode == -1)
							continue;

						dev += this.max[j] - estimates[j][legMode];
					}

					Entry e = new Entry(path, dev);

					if (!seen.contains(e)) {
						heap.enqueue(e);
						seen.add(e);
					}
				}
			}


			convert(entry.modes, result);
			return best - entry.deviation;
		}

		@Override
		public boolean hasNext() {
			return !heap.isEmpty();
		}
	}

	private record Entry(byte[] modes, double deviation) implements Comparable<Entry> {

		@Override
		public int compareTo(Entry o) {
			return Double.compare(deviation, o.deviation);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Entry entry = (Entry) o;
			return Double.compare(entry.deviation, deviation) == 0 && Arrays.equals(modes, entry.modes);
		}

		@Override
		public int hashCode() {
			int result = Objects.hash(deviation);
			result = 31 * result + Arrays.hashCode(modes);
			return result;
		}

		@Override
		public String toString() {
			return Arrays.toString(modes) + " = " + deviation;
		}
	}

}
