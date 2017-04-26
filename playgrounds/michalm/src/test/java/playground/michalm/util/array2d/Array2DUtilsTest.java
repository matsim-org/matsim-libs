package playground.michalm.util.array2d;

import org.junit.Test;

/**
 * @author michalm
 */
public class Array2DUtilsTest {
	private void assertTranspose(double[][] original, double[][] expected) {
		Array2DAssert.assertEquals(expected, Array2DUtils.transponse(original));
	}

	private void assertTranspose(int[][] original, int[][] expected) {
		Array2DAssert.assertEquals(expected, Array2DUtils.transponse(original));
	}

	private void assertTranspose(Object[][] original, Object[][] expected) {
		Array2DAssert.assertEquals(expected, Array2DUtils.transponse(original));
	}

	@Test
	public void testDoubleTranspose() {
		double[][] original;
		double[][] expected;

		original = new double[][] { { 1 }, { 2 } };
		expected = new double[][] { { 1, 2 } };
		assertTranspose(original, expected);

		original = new double[][] { { 1, 2 } };
		expected = new double[][] { { 1 }, { 2 } };
		assertTranspose(original, expected);

		original = new double[][] { { 1, 2 }, { 3, 4 } };
		expected = new double[][] { { 1, 3 }, { 2, 4 } };
		assertTranspose(original, expected);

		original = new double[][] { { 1, 2, 3 }, { 4, 5, 6 } };
		expected = new double[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } };
		assertTranspose(original, expected);

		original = new double[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } };
		expected = new double[][] { { 1, 2, 3 }, { 4, 5, 6 } };
		assertTranspose(original, expected);
	}

	@Test
	public void testIntTranspose() {
		int[][] original;
		int[][] expected;

		original = new int[][] { { 1 }, { 2 } };
		expected = new int[][] { { 1, 2 } };
		assertTranspose(original, expected);

		original = new int[][] { { 1, 2 } };
		expected = new int[][] { { 1 }, { 2 } };
		assertTranspose(original, expected);

		original = new int[][] { { 1, 2 }, { 3, 4 } };
		expected = new int[][] { { 1, 3 }, { 2, 4 } };
		assertTranspose(original, expected);

		original = new int[][] { { 1, 2, 3 }, { 4, 5, 6 } };
		expected = new int[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } };
		assertTranspose(original, expected);

		original = new int[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } };
		expected = new int[][] { { 1, 2, 3 }, { 4, 5, 6 } };
		assertTranspose(original, expected);
	}

	@Test
	public void testObjectTranspose() {
		Object[][] original;
		Object[][] expected;

		original = new Object[][] { { "1" }, { "2" } };
		expected = new Object[][] { { "1", "2" } };
		assertTranspose(original, expected);

		original = new Object[][] { { "1", "2" } };
		expected = new Object[][] { { "1" }, { "2" } };
		assertTranspose(original, expected);

		original = new Object[][] { { "1", "2" }, { "3", "4" } };
		expected = new Object[][] { { "1", "3" }, { "2", "4" } };
		assertTranspose(original, expected);

		original = new Object[][] { { "1", "2", "3" }, { "4", "5", "6" } };
		expected = new Object[][] { { "1", "4" }, { "2", "5" }, { "3", "6" } };
		assertTranspose(original, expected);

		original = new Object[][] { { "1", "4" }, { "2", "5" }, { "3", "6" } };
		expected = new Object[][] { { "1", "2", "3" }, { "4", "5", "6" } };
		assertTranspose(original, expected);
	}
}
