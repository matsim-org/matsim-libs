package org.matsim.contrib.msp.traveltimematrix.experiments;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * What {@link MatrixFootprint} has to be multiplied by to say what a matrix really costs, learned from the
 * matrices this sweep has already built.
 * <p>
 * It exists because the two numbers a sweep reports are not the same kind of number. A matrix that was
 * built has been <em>weighed</em> by {@link HeapProbe}, and that measurement is the answer - nothing is
 * gained by reporting a prediction next to it. A matrix that was refused over {@code --memory-budget} was
 * never built, so a prediction is all there can ever be for it; but it does not have to be the raw
 * prediction, because the sweep that refused it has by then weighed several smaller matrices of the same
 * family and knows which way and by how much the raw one is wrong.
 * <p>
 * <b>The factor applies to the sparse half only</b>, and that is not a simplification but where the error
 * is. {@code Matrix}, the zonal half, is a {@code short[zones][zones]}, so the only thing missing from
 * {@code zones^2 * 2} is one array header per zone row - 0.8 % at Dresden's 1,183 zones, and less as the
 * cells get smaller. The sparse half is a few hundred thousand small objects whose headers are not counted
 * and a per-row {@code BitSet} that allocates by doubling, which is a third of the figure. So the
 * expectation is {@code zonalBytes + factor * sparseBytes}, and a sweep over a resolution grid can carry
 * one factor across configurations whose halves are mixed in quite different proportions.
 * <p>
 * <b>The factor is a median and not a mean</b>, because this is calibration from a handful of points and
 * one of them may be the first matrix of a run, measured while the JVM was still settling.
 * <p>
 * <b>One caveat, and it is visible in the {@code memory_factor} column.</b> The factor of a <em>bounded</em>
 * configuration mixes two errors that point in opposite directions: the bytes per neighbour are
 * under-counted (the headers and the doubling above), while the number of neighbours is deliberately
 * over-counted, being sampled by straight-line distance where the matrix fills it by network distance. On
 * a written-out Dresden network the bounded rows come out between 0.6 and 1.3 while the exact ones - whose
 * neighbour count is not sampled at all, every node being a neighbour - sit near 1.2. So the per-row
 * factor is a diagnostic worth reading, and the median across a resolution grid is a correction for rows
 * nobody built rather than a property of the matrix. Where a sweep's refused configurations are exact ones,
 * which is what part 1 is made of, the exact rows are the ones to calibrate from.
 * <p>
 * <b>What it is not.</b> It is not a model of the layout - {@link MatrixFootprint} documents what is
 * actually missing, and someone who wants a correct estimate rather than a corrected one should fix the
 * terms there. And it extrapolates: part 1 refuses matrices an order of magnitude larger than anything it
 * managed to weigh, so a row calibrated from 30,000 nodes and reporting 300,000 is honest only because the
 * measured factor does not trend with size (1.27 to 1.38 over a tenfold range of nodes on Dresden). If it
 * ever starts to, {@code memory_source} is the column that says which rows to stop trusting.
 *
 * @see HeapProbe where the measurements come from
 * @see MatrixFootprint what is being calibrated
 */
public final class MemoryCalibration {

	private static final Logger log = LogManager.getLogger(MemoryCalibration.class);

	/**
	 * What the sparse half costs over the six bytes per pair plus bit sets that {@link MatrixFootprint}
	 * counts, as measured on the Dresden study area: the median of 1.31, 1.38, 1.34 and 1.27 over discs of
	 * 5 to 30 km (3,349 to 30,315 nodes, a Germany-wide node index space, Java 25 with compressed oops).
	 * <p>
	 * Used only until this sweep has weighed a matrix of its own, which is why it is a documented constant
	 * and not a tuned one: a single-radius JVM that refuses its only configuration has nothing to learn
	 * from, and reporting the raw estimate there - knowing it to be a third low - would be worse than
	 * reporting someone else's measurement.
	 */
	static final double DEFAULT_SPARSE_FACTOR = 1.33;

	/**
	 * Where a reported memory figure came from. The point of the column is that a reader can tell a
	 * weighed matrix from an extrapolated one without comparing columns.
	 */
	public enum Source {
		/** Weighed by {@link HeapProbe} after the build. */
		MEASURED,
		/** The raw estimate, corrected by what this sweep measured on smaller matrices. */
		CALIBRATED_FROM_SWEEP,
		/** The raw estimate, corrected by {@link #DEFAULT_SPARSE_FACTOR}: this sweep built nothing. */
		CALIBRATED_FROM_DEFAULT,
		/** The raw estimate, uncorrected, because {@code --memory-calibration 1} switched this off. */
		ESTIMATED
	}

	private final List<Double> factors = new ArrayList<>();
	private final Double fixedFactor;

	/**
	 * @param fixedFactor what {@code --memory-calibration} says: a factor to use for every row instead of
	 * learning one, 1 to report the raw estimates, or null to learn from the sweep and fall back to
	 * {@link #DEFAULT_SPARSE_FACTOR}
	 */
	public MemoryCalibration(Double fixedFactor) {
		this.fixedFactor = fixedFactor;
		if (fixedFactor != null) {
			log.info(fixedFactor == 1
				? "--memory-calibration 1: memory_bytes of a matrix that was not built is the raw estimate, which "
				+ "the measurements say is about a third low."
				: String.format(Locale.ROOT, "--memory-calibration %.3f: the sparse half of every estimate is "
				+ "scaled by it, and nothing is learned from this sweep's own measurements.", fixedFactor));
		}
	}

	/**
	 * Adds one weighed matrix. Ignored when there is nothing to learn from it - a configuration with no
	 * sparse half cannot say anything about the factor of one, and a measurement that came back at zero
	 * says the probe failed rather than that the matrix was free.
	 */
	public void add(MatrixFootprint footprint, long measuredBytes) {

		if (footprint.sparseBytes() <= 0 || measuredBytes <= 0) {
			return;
		}

		double factor = (measuredBytes - footprint.zonalBytes()) / (double) footprint.sparseBytes();
		this.factors.add(factor);

		log.info("Calibration: the sparse half of this matrix cost {} times what was estimated; the factor "
				+ "carried forward is now {} over {} matrix(es).", String.format(Locale.ROOT, "%.3f", factor),
			String.format(Locale.ROOT, "%.3f", factor()), this.factors.size());
	}

	/**
	 * @return the factor the sparse half of an estimate is multiplied by
	 */
	public double factor() {

		if (this.fixedFactor != null) {
			return this.fixedFactor;
		}
		if (this.factors.isEmpty()) {
			return DEFAULT_SPARSE_FACTOR;
		}

		List<Double> sorted = new ArrayList<>(this.factors);
		Collections.sort(sorted);
		int middle = sorted.size() / 2;
		return sorted.size() % 2 == 1
			? sorted.get(middle)
			: (sorted.get(middle - 1) + sorted.get(middle)) / 2;
	}

	/**
	 * @return what a configuration is expected to cost: its zonal half as estimated, its sparse half
	 * corrected
	 */
	public long expectedBytes(MatrixFootprint footprint) {
		return footprint.zonalBytes() + Math.round(factor() * footprint.sparseBytes());
	}

	/**
	 * @return where {@link #expectedBytes} came from, for a configuration that was not built
	 */
	public Source source() {

		if (this.fixedFactor != null && this.fixedFactor == 1) {
			return Source.ESTIMATED;
		}
		return this.factors.isEmpty() && this.fixedFactor == null
			? Source.CALIBRATED_FROM_DEFAULT
			: Source.CALIBRATED_FROM_SWEEP;
	}

	/**
	 * @return how many weighed matrices the factor rests on
	 */
	public int samples() {
		return this.factors.size();
	}
}
