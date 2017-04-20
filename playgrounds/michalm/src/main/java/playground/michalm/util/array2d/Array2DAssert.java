package playground.michalm.util.array2d;

import org.junit.Assert;

/**
 * @author michalm
 */
public class Array2DAssert {
	public static void assertEquals(double[][] expected, double[][] actual) {
		assertEquals(expected, actual, 0);
	}

	public static void assertEquals(double[][] expected, double[][] actual, double delta) {
		Assert.assertEquals(expected.length, actual.length);

		for (int i = 0; i < actual.length; i++) {
			Assert.assertArrayEquals(expected[i], actual[i], delta);
		}
	}

	public static void assertEquals(int[][] expected, int[][] actual) {
		Assert.assertEquals(expected.length, actual.length);

		for (int i = 0; i < actual.length; i++) {
			Assert.assertArrayEquals(expected[i], actual[i]);
		}
	}

	public static void assertEquals(Object[][] expected, Object[][] actual) {
		Assert.assertEquals(expected.length, actual.length);

		for (int i = 0; i < actual.length; i++) {
			Assert.assertArrayEquals(expected[i], actual[i]);
		}
	}
}
