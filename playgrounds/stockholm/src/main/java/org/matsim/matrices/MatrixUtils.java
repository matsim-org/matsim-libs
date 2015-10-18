package org.matsim.matrices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Gunnar Flötteröd
 * 
 */
public class MatrixUtils {

	private MatrixUtils() {
	}

	public static Matrix newAnonymousMatrix() {
		return (new Matrices()).createMatrix("", "");
	}

	public static double get(final Matrix matrix, final String rowId,
			final String columnId) {
		final Entry entry = matrix.getEntry(rowId, columnId);
		return (entry != null ? entry.getValue() : 0.0);
	}

	public static void set(final Matrix matrix, final String rowId,
			final String columnId, final double value) {
		final Entry entry = matrix.getEntry(rowId, columnId);
		if (entry != null) {
			entry.setValue(value);
		} else {
			matrix.createEntry(rowId, columnId, value);
		}
	}

	public static void add(final Matrix matrix, final String rowId,
			final String columnId, final double addend) {
		set(matrix, rowId, columnId, get(matrix, rowId, columnId) + addend);
	}

	public static void inc(final Matrix matrix, final String rowId,
			final String columnId) {
		set(matrix, rowId, columnId, get(matrix, rowId, columnId) + 1.0);
	}

	public static void mult(final Matrix matrix, final double factor) {
		for (List<Entry> row : matrix.getFromLocations().values()) {
			for (Entry entry : row) {
				entry.setValue(factor * entry.getValue());
			}
		}
	}

	public static void add(final Matrix changedMatrix,
			final Matrix addedMatrix, final double factor) {
		for (Map.Entry<String, ArrayList<Entry>> addedRow : addedMatrix
				.getFromLocations().entrySet()) {
			final String rowId = addedRow.getKey();
			for (Entry addedEntry : addedRow.getValue()) {
				final String columnId = addedEntry.getToLocation();
				add(changedMatrix, rowId, columnId,
						factor * addedEntry.getValue());
			}
		}
	}

	public static void divHadamard(final Matrix changedMatrix,
			final Matrix factorMatrix) {
		for (Map.Entry<String, ArrayList<Entry>> changedRow : changedMatrix
				.getFromLocations().entrySet()) {
			final String rowId = changedRow.getKey();
			for (Entry changedEntry : changedRow.getValue()) {
				final String columnId = changedEntry.getToLocation();
				changedEntry.setValue(changedEntry.getValue()
						/ get(factorMatrix, rowId, columnId));
			}
		}
	}

	public static void main(String[] args) {

		Matrix m1 = (new Matrices()).createMatrix("1", "");
		Matrix m2 = (new Matrices()).createMatrix("2", "");

		set(m1, "1", "1", 1.0);
		set(m1, "1", "2", 2.0);
		set(m1, "2", "1", 3.0);
		set(m1, "2", "2", 4.0);

		// m1 is now [1 2; 3 4]

		mult(m1, 2.0);

		// m1 is now [2 4; 6 8]

		set(m2, "1", "1", 40.0);
		set(m2, "1", "2", 30.0);
		set(m2, "2", "1", 20.0);
		set(m2, "2", "2", 10.0);

		// m2 is now [40 30; 20 10]

		add(m2, m1, 10.0);

		// m1 is still [2 4; 6 8]
		// m2 is now [60 70; 80 90]

		System.out.println("m1 = [" + get(m1, "1", "1") + " "
				+ +get(m1, "1", "2") + "; " + +get(m1, "2", "1") + " "
				+ +get(m1, "2", "2") + "]");
		System.out.println("m2 = [" + get(m2, "1", "1") + " "
				+ +get(m2, "1", "2") + "; " + +get(m2, "2", "1") + " "
				+ +get(m2, "2", "2") + "]");

		divHadamard(m2, m1);

		// m1 is still [2 4; 6 8]
		// m2 is now [30 17.5; 13.333 11.25]

		System.out.println("m1 = [" + get(m1, "1", "1") + " "
				+ +get(m1, "1", "2") + "; " + +get(m1, "2", "1") + " "
				+ +get(m1, "2", "2") + "]");
		System.out.println("m2 = [" + get(m2, "1", "1") + " "
				+ +get(m2, "1", "2") + "; " + +get(m2, "2", "1") + " "
				+ +get(m2, "2", "2") + "]");

	}
}
