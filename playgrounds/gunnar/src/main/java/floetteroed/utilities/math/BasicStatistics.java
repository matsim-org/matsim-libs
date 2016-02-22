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

import java.io.Serializable;

/**
 * Calculates recursively the average, variance, minimum, and maximum of a
 * number sequence.
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class BasicStatistics implements Serializable {

	// -------------------- CONSTANTS --------------------

	private static final long serialVersionUID = 1L;

	// -------------------- MEMBER VARIABLES --------------------

	private int size;

	private double sum;

	private double sqrSum;

	private double min;

	private double max;

	// -------------------- CONSTRUCTION --------------------

	public BasicStatistics() {
		clear();
	}

	// -------------------- PUBLIC ACCESS --------------------

	public void clear() {
		this.size = 0;
		this.sum = 0;
		this.sqrSum = 0;
		this.min = Double.POSITIVE_INFINITY;
		this.max = Double.NEGATIVE_INFINITY;
	}

	public void add(final double val) {
		this.sum += val;
		this.sqrSum += val * val;
		this.min = Math.min(this.min, val);
		this.max = Math.max(this.max, val);
		this.size++;
	}
	
	// TODO NEW
	public BasicStatistics(final Iterable<Double> iterable) {
		this();
		this.addAll(iterable);
	}
	
	// TODO NEW
	public void addAll(final Iterable<Double> iterable) {
		for (double val : iterable) {
			this.add(val);
		}
	}

	public int size() {
		return this.size;
	}

	public double getAvg() {
		return this.sum / this.size;
	}

	public double getVar() {
		if (this.size() < 2) {
			return Double.POSITIVE_INFINITY;
		} else {
			return (this.sqrSum - this.sum * this.sum / this.size)
					/ (this.size - 1.0);
		}
	}

	public double getStddev() {
		return Math.sqrt(this.getVar());
	}

	public double getMin() {
		return this.min;
	}

	public double getMax() {
		return this.max;
	}
}
