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
package floetteroed.utilities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import floetteroed.utilities.math.MathHelpers;



/**
 * 
 * @author Gunnar Flötteröd
 * 
 * @param <K>
 *            the key type
 */
public class DynamicData<K> implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	private final int startTime_s;

	private final int binSize_s;

	private int binCnt;

	// -------------------- MEMBER VARIABLES --------------------

	protected final Map<K, double[]> data = new HashMap<K, double[]>();

	// -------------------- CONSTRUCTION --------------------

	public DynamicData(final int startTime_s, final int binSize_s,
			final int binCnt) {

		// CHECK

		if (binSize_s <= 0) {
			throw new IllegalArgumentException(
					"binSize_s must be strictly positive");
		}

		if (binCnt <= 0) {
			throw new IllegalArgumentException(
					"binCnt must be strictly positive");
		}

		// CONTINUE

		this.startTime_s = startTime_s;
		this.binSize_s = binSize_s;
		this.binCnt = binCnt;
	}

	// -------------------- BASIC CONTENT ACCESS --------------------

	public int getStartTime_s() {
		return this.startTime_s;
	}

	public int getBinSize_s() {
		return this.binSize_s;
	}

	public int getBinCnt() {
		return this.binCnt;
	}

	public int bin(final int time_s) {
		return (time_s - this.startTime_s) / this.binSize_s;
	}

	public int binStart_s(final int bin) {
		return this.startTime_s + bin * this.binSize_s;
	}

	protected double[] getNonNullDataArray(final K key) {
		double[] dataArray = this.data.get(key);
		if (dataArray == null) {
			dataArray = new double[this.getBinCnt()];
			this.data.put(key, dataArray);
		}
		return dataArray;
	}

	public void put(final K key, final int bin, final double value) {
		this.getNonNullDataArray(key)[bin] = value;
	}

	public void add(final K key, final int bin, final double value) {
		this.getNonNullDataArray(key)[bin] += value;
	}

	public void clear() {
		this.data.clear();
	}

	public Set<K> keySet() {
		return this.data.keySet();
	}

	public double getBinValue(final K key, final int bin) {
		final double[] dataArray = this.data.get(key);
		if (dataArray == null) {
			return 0;
		} else {
			return dataArray[bin];
		}
	}

	// -------------------- ADVANCED CONTENT ACCESS --------------------

	/**
	 * Returns the sum of entry values for all time bins that overlap with the
	 * given time interval in the following way: For every bin that is entirely
	 * contained in [startTime_s, endTime_s), its entry value is summed up; for
	 * every bin that only partially contained in that interval, only a fraction
	 * that corresponds to the overlap is summed up.
	 * 
	 * @param key
	 *            the key of the queried time series
	 * @param startTime_s
	 *            the start time in seconds (inclusive)
	 * @param endTime_s
	 *            the end time in seconds (exclusive)
	 * 
	 */
	public double getSum(final K key, final int startTime_s, final int endTime_s) {

		final double[] dataArray = this.data.get(key);
		if (dataArray == null) {
			return 0;
		}

		final int startBin = Math.max(bin(startTime_s), 0);
		final int endBin = Math.min(bin(endTime_s - 1), this.getBinCnt() - 1);

		double result = 0;
		for (int bin = startBin; bin <= endBin; bin++) {
			final double weight = MathHelpers.overlap(binStart_s(bin),
					binStart_s(bin) + getBinSize_s(), startTime_s, endTime_s)
					/ this.getBinSize_s();
			result += weight * dataArray[bin];
		}
		return result;
	}

	/**
	 * Returns the average entry value for all time bins that overlap with the
	 * given time interval.
	 * 
	 * @param key
	 *            the key of the queried time series
	 * @param startTime_s
	 *            the start time in seconds (inclusive)
	 * @param endTime_s
	 *            the end time in seconds (exclusive)
	 * 
	 */
	public double getAverage(final K key, final int startTime_s,
			final int endTime_s) {
		final double binCnt = ((double) (endTime_s - startTime_s))
				/ this.getBinSize_s();
		return this.getSum(key, startTime_s, endTime_s) / binCnt;
	}

	/**
	 * Overrides all entries in this instance for which other has an entry by
	 * the latter; all other entries of this remain unchanged.
	 * 
	 */
	public void overrideWithNonZeros(final DynamicData<K> source) {
		for (Map.Entry<K, double[]> sourceEntry : source.data.entrySet()) {
			final K key = sourceEntry.getKey();
			final double[] newValue = MathHelpers.override(this.data.get(key),
					sourceEntry.getValue(), false);
			this.data.put(key, newValue);
		}
	}

	public void resize(final int newBinCnt) {
		if (newBinCnt == this.binCnt) {
			return;
		}
		if (newBinCnt <= 0) {
			throw new IllegalArgumentException(
					"binCnt must be strictly positive");
		}
		final List<K> allKeys = new ArrayList<K>(this.data.keySet()); // fast
		for (K key : allKeys) {
			final double[] oldArray = this.data.get(key);
			if (oldArray != null) {
				final double[] newArray = new double[newBinCnt];
				System.arraycopy(oldArray, 0, newArray, 0, Math.min(
						this.binCnt, newBinCnt));
				this.data.put(key, newArray);
			}
		}
		this.binCnt = newBinCnt;
	}
}
