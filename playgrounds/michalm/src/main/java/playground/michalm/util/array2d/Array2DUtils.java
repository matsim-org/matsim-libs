package playground.michalm.util.array2d;

import java.lang.reflect.Array;

/**
 * @author michalm
 */
public class Array2DUtils {
	public static double[][] transponse(double[][] array) {
		if (array == null || array.length == 0 || array[0].length == 0) {
			throw new RuntimeException("Null or empty array");
		}

		double[][] transposed = new double[array[0].length][array.length];

		for (int i = 0; i < transposed.length; i++) {
			for (int j = 0; j < array.length; j++) {
				transposed[i][j] = array[j][i];
			}
		}

		return transposed;
	}

	public static int[][] transponse(int[][] array) {
		if (array == null || array.length == 0 || array[0].length == 0) {
			throw new RuntimeException("Null or empty array");
		}

		int[][] transposed = new int[array[0].length][array.length];

		for (int i = 0; i < transposed.length; i++) {
			for (int j = 0; j < array.length; j++) {
				transposed[i][j] = array[j][i];
			}
		}

		return transposed;
	}

	public static Object[][] transponse(Object[][] array) {
		if (array == null || array.length == 0 || array[0].length == 0) {
			throw new RuntimeException("Null or empty array");
		}

		Object[][] transposed = (Object[][])Array.newInstance(array[0].getClass().getComponentType(),
				new int[] { array[0].length, array.length });

		for (int i = 0; i < transposed.length; i++) {
			for (int j = 0; j < array.length; j++) {
				transposed[i][j] = array[j][i];
			}
		}

		return transposed;
	}

	public static int[][] createIntArray(int rows, int cols) {
		return (int[][])Array.newInstance(int.class, rows, cols);
	}

	public static double[][] createDoubleArray(int rows, int cols) {
		return (double[][])Array.newInstance(double.class, rows, cols);
	}

	public static int[][] deepCopy(int[][] array) {
		int[][] clone = new int[array.length][];

		for (int i = 0; i < clone.length; i++) {
			clone[i] = array[i].clone();
		}

		return clone;
	}

	public static double[][] deepCopy(double[][] array) {
		double[][] clone = new double[array.length][];

		for (int i = 0; i < clone.length; i++) {
			clone[i] = array[i].clone();
		}

		return clone;
	}
}
