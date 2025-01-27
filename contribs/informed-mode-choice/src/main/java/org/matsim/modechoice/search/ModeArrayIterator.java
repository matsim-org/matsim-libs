package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Searches all possible combination by using a heap. Uses arrays to store combinations of unlimited size.
 */
final class ModeArrayIterator implements ModeIterator {

	private final String[] result;
	private final double[] max;
	private final double best;
	private final double[][] estimates;
	private final ModeChoiceSearch search;
	private final ObjectHeapPriorityQueue<Entry> heap;
	private final Set<Entry> seen;


	public ModeArrayIterator(String[] result, double[] max, Entry shortest, double best, ModeChoiceSearch search) {
		this.result = result;
		this.max = max;
		this.best = best;
		this.estimates = search.estimates;
		this.search = search;
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


		search.convert(entry.modes, result);
		return best - entry.deviation;
	}

	@Override
	public boolean hasNext() {
		return !heap.isEmpty();
	}

	@Override
	public int maxIters() {
		return 1_000_000;
	}

	record Entry(byte[] modes, double deviation) implements Comparable<Entry> {

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
