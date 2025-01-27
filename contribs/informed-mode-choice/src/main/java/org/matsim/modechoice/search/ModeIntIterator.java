package org.matsim.modechoice.search;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectHeapPriorityQueue;

import java.util.Objects;

/**
 * Copy of the {@link ModeLongIterator} that uses an int instead of a long to store the index.
 * @see ModeLongIterator
 */
final class ModeIntIterator implements ModeIterator {


    private final String[] result;
    private final byte[] modes;
    private final double[] max;
    private final double best;
    private final double[][] estimates;
    private final int base;
    private final int depth;
    private final ModeChoiceSearch search;
    private final ObjectHeapPriorityQueue<Entry> heap;
    private final IntSet seen;


    ModeIntIterator(String[] result, double[] max, Entry shortest, double best, int base, ModeChoiceSearch search) {

        this.result = result;
        this.modes = new byte[result.length];
        this.max = max;
        this.best = best;
        this.estimates = search.estimates;
        this.base = base;
		this.depth = search.depth;
        this.search = search;
        this.heap = new ObjectHeapPriorityQueue<>();
        this.seen = new IntOpenHashSet();

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
                int newIdx = Entry.toIndex(modes, depth, i, mode);

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
		private final int index;
		private final double deviation;

		Entry(byte[] modes, int depth, double deviation) {
			this.index = toIndex(modes, depth);
			this.deviation = deviation;
		}

		Entry(int index, double deviation) {
			this.index = index;
			this.deviation = deviation;
		}

		static int toIndex(byte[] modes, int depth) {
			int result = modes[0] + 1;
			int base = depth;
			for (int i = 1; i < modes.length; i++) {
				result += (modes[i] + 1) * base;
				base *= depth;
			}

			return result;
		}

		/**
		 * Convert the mode array to an index, where one mode at index {@code idx} is replaced.
		 */
		static int toIndex(byte[] modes, int depth, int idx, byte mode) {
			int result = (idx == 0 ? mode : modes[0]) + 1;
			int base = depth;
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

		void toArray(byte[] array, int base, int depth) {
			int idx = index;
			for (int i = array.length - 1; i > 0; i--) {

				int v = idx / base;
				array[i] = (byte) (v - 1);

				idx -= v * base;
				base /= depth;
			}

			array[0] = (byte) (idx - 1);
		}
	}
}
