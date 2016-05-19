package floetteroed.utilities.math;

import static java.lang.System.arraycopy;

import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 * 
 */
public class Tensor {

	// -------------------- CONSTANTS --------------------

	private final int[] dimensions;

	// -------------------- MEMBERS --------------------

	private final double[] data;

	// --------------------CONSTRUCTION --------------------

	private Tensor(final int[] dimension, final double[] data) {
		this.dimensions = dimension;
		this.data = data;
	}

	public Tensor(final int... dimensions) {
		this(dimensions, new double[size(dimensions)]);
	}

	// TODO move elsewhere
	private static int[] list2array(final List<Integer> list) {
		final int[] array = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	// TODO NEW
	public Tensor(final List<Integer> dimensionList) {
		this(list2array(dimensionList));
	}

	// -------------------- INTERNALS --------------------

	private static int size(int[] dimensions) {
		int result = 1;
		for (int dim : dimensions) {
			result *= dim;
		}
		return result;
	}

	public int linearIndex(final int... indices) {
		int result = 0;
		int factor = 1;
		for (int i = 0; i < indices.length; i++) {
			result += factor * indices[i];
			factor *= this.dimensions[i];
		}
		return result;
	}

	public void iterate(final int... indicesSoFar) {
		if (indicesSoFar.length == this.dimensions.length) {
			for (int i : indicesSoFar) {
				System.out.print(i + " ");
			}
			System.out.println(this.get(indicesSoFar));
		} else {
			final int[] newIndices = new int[indicesSoFar.length + 1];
			arraycopy(indicesSoFar, 0, newIndices, 0, indicesSoFar.length);
			for (int i = 0; i < this.dimensions[newIndices.length - 1]; i++) {
				newIndices[newIndices.length - 1] = i;
				this.iterate(newIndices);
			}
		}
	}

	private int[] removeElement(final int index, final int[] src) {
		final int[] dst = new int[src.length - 1];
		arraycopy(src, 0, dst, 0, index);
		arraycopy(src, index + 1, dst, index, src.length - index - 1);
		return dst;
	}

	public void project(int projectedDimension, final Tensor projection, final int... indices) {
		if (indices.length == this.dimensions.length) {
			final int[] reducedIndices = this.removeElement(projectedDimension, indices);
			projection.add(this.get(indices), reducedIndices);
		} else {
			final int[] newIndices = new int[indices.length + 1];
			arraycopy(indices, 0, newIndices, 0, indices.length);
			for (int i = 0; i < this.dimensions[newIndices.length - 1]; i++) {
				newIndices[newIndices.length - 1] = i;
				this.project(projectedDimension, projection, newIndices);
			}
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void set(final double value, final int... indices) {
		this.data[this.linearIndex(indices)] = value;
	}

	public void add(final double value, final int... indices) {
		this.data[this.linearIndex(indices)] += value;
	}

	public double get(final int... indices) {
		return this.data[this.linearIndex(indices)];
	}

	public Tensor newProjected(final int removedDimension) {
		/*
		 * create a new tensor
		 */
		final int[] newDimensions = this.removeElement(removedDimension, this.dimensions);
		final Tensor result = new Tensor(newDimensions);

		/*
		 * fill that tensor
		 */
		this.project(removedDimension, result, new int[0]);

		return result;
	}

	public int size() {
		return size(this.dimensions);
	}

	public Tensor copy() {
		final int[] newDimensions = new int[this.dimensions.length];
		arraycopy(this.dimensions, 0, newDimensions, 0, this.dimensions.length);
		final double[] newData = new double[this.data.length];
		arraycopy(this.data, 0, newData, 0, this.data.length);
		return new Tensor(newDimensions, newData);
	}

	public void fill(final double value) {
		Arrays.fill(this.data, value);
	}

	public void makeProbability() {
		double sum = 0;
		for (int i = 0; i < this.data.length; i++) {
			if (this.data[i] < 0.0) {
				this.data[i] = 0.0;
			} else if (this.data[i] > 1.0) {
				this.data[i] = 1.0;
				sum += 1.0;
			} else {
				sum += this.data[i];
			}
		}
		for (int i = 0; i < this.data.length; i++) {
			this.data[i] /= sum;
		}
	}

	// TODO NEW
	public static Vector[] getMarginals3D(final Tensor tensor) {
		final Vector marginal1 = new Vector(tensor.dimensions[0]);
		final Vector marginal2 = new Vector(tensor.dimensions[1]);
		final Vector marginal3 = new Vector(tensor.dimensions[2]);
		for (int i = 0; i < tensor.dimensions[0]; i++) {
			for (int j = 0; j < tensor.dimensions[1]; j++) {
				for (int k = 0; k < tensor.dimensions[2]; k++) {
					final double val = tensor.get(i, j, k);
					marginal1.add(i, val);
					marginal2.add(j, val);
					marginal3.add(k, val);
				}
			}
		}
		marginal1.mult(1.0 / marginal1.absValueSum());
		marginal2.mult(1.0 / marginal2.absValueSum());
		marginal3.mult(1.0 / marginal3.absValueSum());
		return new Vector[] { marginal1, marginal2, marginal3 };
	}

	// TODO NEW
	public static Tensor newFromMarginals3D(final Vector... marginals) {
		final Tensor result = new Tensor(marginals[0].size(), marginals[1].size(), marginals[2].size());
		for (int i = 0; i < marginals[0].size(); i++) {
			final double val1 = marginals[0].get(i);
			for (int j = 0; j < marginals[1].size(); j++) {
				final double val2 = marginals[1].get(j);
				for (int k = 0; k < marginals[2].size(); k++) {
					final double val3 = marginals[2].get(k);
					result.set(val1 * val2 * val3, i, j, k);
				}
			}
		}
		return result;
	}

	// TODO NEW
	public static Tensor newMarginalized3D(final Tensor tensor) {
		return newFromMarginals3D(getMarginals3D(tensor));
	}

	// TODO NEW
	public void add(final Tensor other, final double weight) {
		for (int i = 0; i < this.data.length; i++) {
			this.data[i] += weight * other.data[i];
		}
	}

	// TODO NEW
	public void mult(final double fact) {
		for (int i = 0; i < this.data.length; i++) {
			this.data[i] *= fact;
		}
	}

	// TODO NEW
	public int[] dimensions() {
		return this.dimensions;
	}

	// TODO NEW
	public static double maxAbsDiff(final Tensor a, final Tensor b) {
		double result = 0;
		for (int i = 0; i < a.data.length; i++) {
			result = Math.max(result, Math.abs(a.data[i] - b.data[i]));
		}
		return result;
	}
}
