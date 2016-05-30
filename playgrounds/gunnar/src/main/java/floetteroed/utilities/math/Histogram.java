/*
 * Copyright 2015 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.floetteroed@abe.kth.se
 *
 */
package floetteroed.utilities.math;

import java.util.Arrays;

/**
 * 
 * TODO new
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Histogram {

	// -------------------- MEMBERS --------------------

	private final double[] bounds;

	private final int[] bins;

	private int totalCount;

	// -------------------- CONSTRUCTION --------------------

	public Histogram(final double... bounds) {

		if (bounds == null) {
			throw new IllegalArgumentException("bounds array must not be null");
		}

		for (int i = 1; i < bounds.length; i++) {
			if (bounds[i - 1] >= bounds[i]) {
				throw new IllegalArgumentException(
						"bounds are not in ascending order");
			}
		}

		this.bounds = bounds;
		this.bins = new int[bounds.length + 1];
		clear();
	}

	// TODO NEW
	public static Histogram newHistogramWithUniformBins(
			final double startValue, final double binSize, final int binCnt) {
		final double[] bounds = new double[binCnt + 1];
		for (int bin = 0; bin <= binCnt; bin++) {
			bounds[bin] = startValue + bin * binSize;
		}
		return new Histogram(bounds);
	}

	// -------------------- WRITE ACCESS --------------------

	public void clear() {
		Arrays.fill(this.bins, 0);
		this.totalCount = 0;
	}

	public void add(final double value) {
		int i = 0;
		while (i < this.bounds.length && value >= this.bounds[i]) {
			i++;
		}
		this.bins[i]++;
		this.totalCount++;
	}

	// TODO NEW
	public void makeNonZero() {
		for (int i = 0; i < this.bins.length; i++) {
			if (this.bins[i] == 0) {
				this.bins[i]++;
				this.totalCount++;
			}
		}
	}
	
	// -------------------- READ ACCESS --------------------

	public int binCnt() {
		return this.bins.length;
	}

	public double lowerBound(final int bin) {
		if (bin == 0) {
			return Double.NEGATIVE_INFINITY;
		} else {
			return this.bounds[bin - 1];
		}
	}

	public double upperBound(final int bin) {
		if (bin == this.binCnt() - 1) {
			return Double.POSITIVE_INFINITY;
		} else {
			return this.bounds[bin];
		}
	}

	public int cnt() {
		return this.totalCount;
	}

	public int cnt(final int bin) {
		return this.bins[bin];
	}

	public double freq(final int bin) {
		final double cnt = (double) this.cnt(bin);
		if (cnt > 0.0) {
			return cnt / this.totalCount;
		} else {
			return 0.0;
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	@Override
	public String toString() {
		final StringBuffer result = new StringBuffer();
		if (this.binCnt() == 1) {
			result.append("(-oo, +oo) : 1.0 (" + totalCount + " of "
					+ totalCount + ")");
		} else {
			result.append("(-oo, " + bounds[0] + ") : " + freq(0) + " ("
					+ bins[0] + " of " + totalCount + ")\n");
			for (int i = 1; i < bounds.length; i++) {
				result.append("[" + bounds[i - 1] + ", " + bounds[i] + ") : "
						+ freq(i) + " (" + bins[i] + " of " + totalCount
						+ ")\n");
			}
			result.append("[" + bounds[bounds.length - 1] + ", +oo) : "
					+ freq(bounds.length) + " (" + bins[bounds.length] + " of "
					+ totalCount + ")");
		}
		return result.toString();
	}
}
