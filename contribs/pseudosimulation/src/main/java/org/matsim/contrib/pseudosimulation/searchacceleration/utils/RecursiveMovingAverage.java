/*
 * Copyright 2018 Gunnar Flötteröd
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
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.utils;

import java.util.LinkedList;

/**
 *
 * @author Gunnar Flötteröd
 *
 *         TODO Move to utilities package.
 */
public class RecursiveMovingAverage {

	private final int memoryLength;

	private final LinkedList<Double> data = new LinkedList<>();

	private double sum = 0.0;

	public RecursiveMovingAverage(final int memoryLength) {
		this.memoryLength = memoryLength;
	}

	public void add(final double value) {

		this.data.addFirst(value);
		this.sum += value;

		while (this.data.size() > this.memoryLength) {
			this.sum -= this.data.removeLast();
		}
	}

	public int memoryLength() {
		return this.memoryLength;
	}

	public int size() {
		return this.data.size();
	}

	public double sum() {
		return this.sum;
	}
	
	public double average() {
		return this.sum / this.data.size();
	}

	public double mostRecentValue() {
		return this.data.getFirst();
	}
	
	public static void main(String[] args) {
		int memory = 4;
		double[] data = { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0 };
		double[] result = { 0, 0, 0, 0, 0.25, 0.5, 0.75, 1, 0.75, 0.5, 0.25, 0 };
		int[] size = { 1, 2, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4 };
		
		RecursiveMovingAverage rma = new RecursiveMovingAverage(memory);
		System.out.println("average-error\tsize-error");
		for (int i = 0; i < data.length; i++) {
			rma.add(data[i]);
			System.out.println((rma.average() - result[i]) + "\t" + (rma.size() - size[i]));
		}
	}
}
