package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * What something that was built actually costs in heap, <b>measured</b> rather than estimated: the used
 * heap before and after building it, plus the high-water mark the build itself touched.
 * <p>
 * It exists because {@link MatrixFootprint} is a prediction, and a prediction that is never compared
 * against the thing it predicts is a number nobody should put in a paper. The estimate decides which
 * configurations a sweep attempts, so the direction and the size of its error are results in their own
 * right: if it is pessimistic, the sweep refuses matrices that would have fit; if it is optimistic, the
 * budget check passes and the sweep dies of an {@link OutOfMemoryError} half way through.
 * <p>
 * <b>Two numbers, and they answer different questions.</b> {@link Result#retainedBytes()} is what the
 * built object goes on occupying - the quantity {@link MatrixFootprint} estimates, and the one that says
 * how many matrices of this size a machine could hold. {@link Result#peakBytes()} is how much heap the
 * build needed at its worst moment, garbage included, which is the quantity that decides whether it runs
 * at all: a sparse row is assembled as a list of boxed records and sorted before it is packed into the
 * two arrays that survive, and that happens on every thread at once.
 * <p>
 * <b>How much to trust them.</b> The retained size is a difference of two used-heap readings taken after
 * {@link System#gc()}, which is a request and not an instruction, so anything the collector has not got
 * round to is counted as part of the matrix. It turns out not to matter at this scale: checked against an
 * exact walk of the built {@code SparseMatrix} - every bucket, array and bit set, sized by hand - it came
 * out at 367.3 MB against 367.4 MB on the 5 km Dresden disc and 5,469.9 MB against 5,450.8 MB on the 20 km
 * one, so under half a percent.
 * <p>
 * The peak is weaker, and it is an <em>over</em>-estimate of what a build needs. It is the sum over the
 * heap pools of each pool's own maximum, and those maxima need not occur at the same moment; worse, it
 * counts garbage the collector merely had no reason to collect yet, which is a function of how much heap
 * the JVM was given rather than of the matrix. The 20 km disc peaks at 10.4 GB in a 24 GB heap while
 * retaining 5.3 GB - but it builds fine under {@code -Xmx7500m}, and only fails under
 * {@code -Xmx6500m}. So read it as what the build was allowed to use, not as what it required.
 *
 * @see MatrixFootprint the estimate these numbers are there to check
 */
public final class HeapProbe {

	private static final Logger log = LogManager.getLogger(HeapProbe.class);

	/**
	 * How many times to ask for a collection before reading the used heap. A concurrent collector returns
	 * from {@code System.gc()} before it has finished, so the lowest of several readings is closer to the
	 * truth than the first one.
	 */
	private static final int COLLECTIONS_PER_READING = 4;

	/**
	 * How long to let a collection run before reading the heap again [ms]. Four of these per reading and
	 * two readings per matrix is under a second, against builds measured in minutes.
	 */
	private static final long SETTLE_MILLISECONDS = 100;

	private HeapProbe() {
	}

	/**
	 * What one build cost.
	 *
	 * @param value what was built
	 * @param retainedBytes the heap it still occupies once the build's garbage is collected
	 * @param peakBytes the most heap in use at any point of the build, garbage included
	 * @param seconds how long the build took, excluding the collections around it
	 */
	public record Result<T>(T value, long retainedBytes, long peakBytes, double seconds) {

		/**
		 * @return how the measurement compares to an estimate, as {@code measured / estimated}. Above 1
		 * means the estimate was optimistic, which is the dangerous direction.
		 */
		public double overEstimate(long estimatedBytes) {
			return estimatedBytes == 0 ? Double.NaN : (double) this.retainedBytes / estimatedBytes;
		}
	}

	/**
	 * Builds something and measures what it cost.
	 * <p>
	 * The collection before the build is not only there to take a reading: the previous matrix of a sweep
	 * is unreachable by now but may well not have been collected, and the next one is the largest
	 * allocation this process makes.
	 */
	public static <T> Result<T> measure(Supplier<T> build) {

		List<MemoryPoolMXBean> heapPools = ManagementFactory.getMemoryPoolMXBeans().stream()
			.filter(pool -> pool.getType() == MemoryType.HEAP)
			.toList();

		long before = usedHeapAfterCollection();
		heapPools.forEach(MemoryPoolMXBean::resetPeakUsage);

		long startedAt = System.nanoTime();
		T value = build.get();
		double seconds = (System.nanoTime() - startedAt) / 1e9;

		long peak = heapPools.stream().mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
		// the value is still strongly reachable here, so what survives this collection is the value and
		// whatever it holds on to
		long after = usedHeapAfterCollection();

		long retained = Math.max(0, after - before);
		log.info("Measured: {} retained ({} used before the build, {} after), a peak of {} during it, in {} s.",
			MatrixFootprint.megabytes(retained), MatrixFootprint.megabytes(before),
			MatrixFootprint.megabytes(after), MatrixFootprint.megabytes(peak),
			String.format(Locale.ROOT, "%.1f", seconds));

		return new Result<>(value, retained, peak, seconds);
	}

	/**
	 * How much heap is free once the collector has been asked to settle.
	 * <p>
	 * Asked rather than read, because the matrix of the previous configuration is unreachable by the time
	 * the next one is sized up but need not have been collected - and a free-heap reading that still counts
	 * a 9 GB matrix nobody holds would refuse the configuration after it.
	 */
	public static long freeHeapAfterCollection() {
		return Runtime.getRuntime().maxMemory() - usedHeapAfterCollection();
	}

	/**
	 * The used heap once the collector has been asked, repeatedly, to settle. The lowest reading wins:
	 * every reading is the truth plus some amount of uncollected garbage, so the smallest one carries the
	 * least of it.
	 */
	private static long usedHeapAfterCollection() {

		long lowest = Long.MAX_VALUE;
		for (int attempt = 0; attempt < COLLECTIONS_PER_READING; attempt++) {
			System.gc();
			try {
				Thread.sleep(SETTLE_MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
			Runtime runtime = Runtime.getRuntime();
			lowest = Math.min(lowest, runtime.totalMemory() - runtime.freeMemory());
		}
		return lowest;
	}
}
