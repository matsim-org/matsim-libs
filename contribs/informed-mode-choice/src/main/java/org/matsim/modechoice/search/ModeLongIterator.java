package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

import java.util.Objects;

/**
 * Searches all possible combination by using a heap.
 */
final class ModeLongIterator implements ModeIterator {

	// This implementation uses a heap and stores all seen combinations
	// Because the graph structure is fixed, it does not need to be as complicated as other k shortest path algorithms
	// Two major improvements may be investigated:
	// Could this be implemented with pre allocated memory and without objects ?
	// Do all seen combinations need to be stored ?
	// This is the case in Yens algorithms and other and will consume a lot of memory, when many top k paths are generated and a lot are thrown away.

	private final String[] result;
	private final byte[] modes;
	private final double[] max;
	private final double best;
	private final double[][] estimates;
	private final long base;
	private final int depth;
	private final ModeChoiceSearch search;
	private final ObjectHeapPriorityQueue<Entry> heap;
	private final LongSet seen;


	ModeLongIterator(String[] result, double[] max, Entry shortest, double best, long base, ModeChoiceSearch search) {

		this.result = result;
		this.modes = new byte[result.length];
		this.max = max;
		this.best = best;
		this.estimates = search.estimates;
		this.base = base;
		this.depth = search.depth;
		this.search = search;
		this.heap = new ObjectHeapPriorityQueue<>();
		this.seen = new LongOpenHashSet();

		heap.enqueue(shortest);
		seen.add(shortest.index);
	}

	@Override
	public double nextDouble() {

		Entry entry = heap.dequeue();
		// Create the array of indices from the entry
		entry.toArray(modes, base, depth);

		for (int i = 0; i < result.length; i++) {

			byte mode = -1;
			byte originalMode = modes[i];

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
				long newIdx = Entry.toIndex(modes, depth, i, mode);

				// recompute the deviation from the maximum
				// there might be a way to store and update this, without recomputing
				double dev = 0;
				for (int j = 0; j < result.length; j++) {
					// Use either the replaced mode or the original mode
					byte legMode = j == i ? mode : modes[j];

					if (legMode == -1)
						continue;

					dev += this.max[j] - estimates[j][legMode];
				}

				Entry e = new Entry(newIdx, dev);

				if (!seen.contains(e.index)) {
					heap.enqueue(e);
					seen.add(e.index);
				}
			}
		}

		search.convert(modes, result);
		return best - entry.deviation;
	}

	@Override
	public boolean hasNext() {
		return !heap.isEmpty();
	}

	static final class Entry implements Comparable<Entry> {
		private final long index;
		private final double deviation;

		Entry(byte[] modes, int depth, double deviation) {
			this.index = toIndex(modes, depth);
			this.deviation = deviation;
		}

		Entry(long index, double deviation) {
			this.index = index;
			this.deviation = deviation;
		}

		static long toIndex(byte[] modes, int depth) {
			long result = modes[0] + 1;
			long base = depth;
			for (int i = 1; i < modes.length; i++) {
				result += (modes[i] + 1) * base;
				base *= depth;
			}

			return result;
		}

		/**
		 * Convert the mode array to an index, where one mode at index {@code idx} is replaced.
		 */
		static long toIndex(byte[] modes, int depth, int idx, byte mode) {
			long result = (idx == 0 ? mode : modes[0]) + 1;
			long base = depth;
			for (int i = 1; i < modes.length; i++) {
				result += ((idx == i ? mode : modes[i]) + 1) * base;
				base *= depth;
			}

			return result;
		}

		@Override
		public int compareTo(Entry o) {
			return Double.compare(deviation, o.deviation);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Entry entry = (Entry) o;
			return Double.compare(entry.deviation, deviation) == 0 && index == entry.index;
		}

		@Override
		public int hashCode() {
			int result = Objects.hash(deviation);
			result = 31 * result + Long.hashCode(index);
			return result;
		}

		@Override
		public String toString() {
			return "idx: " + index + " = " + deviation;
		}

		public long getIndex() {
			return index;
		}

		void toArray(byte[] array, long base, int depth) {
			long idx = index;
			for (int i = array.length - 1; i > 0; i--) {

				long v = idx / base;
				array[i] = (byte) (v - 1);

				idx -= v * base;
				base /= depth;
			}

			array[0] = (byte) (idx - 1);
		}
	}
}
