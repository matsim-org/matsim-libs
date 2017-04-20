package playground.michalm.util.array2d;

import static org.junit.Assert.fail;

import java.io.*;

import org.junit.Test;

/**
 * @author michalm
 */
public class Array2DReaderTest {
	private void assertDoubleArray(String contents, double[][] expected) throws IOException {
		double[][] actual = (double[][])Array2DReader.getArray(createReader(contents), expected[0].length,
				Array2DReader.DOUBLE_STRATEGY);

		Array2DAssert.assertEquals(expected, actual);
	}

	private void assertIntArray(String contents, int[][] expected) throws IOException {
		int[][] actual = (int[][])Array2DReader.getArray(createReader(contents), expected[0].length,
				Array2DReader.INT_STRATEGY);

		Array2DAssert.assertEquals(expected, actual);
	}

	private void assertStringArray(String contents, String[][] expected) throws IOException {
		String[][] actual = (String[][])Array2DReader.getArray(createReader(contents), expected[0].length,
				Array2DReader.STRING_STRATEGY);

		Array2DAssert.assertEquals(expected, actual);
	}

	private static BufferedReader createReader(String contents) {
		return new BufferedReader(new StringReader(contents));
	}

	@Test
	public void testDoubleArray() throws IOException {
		String contents;
		double[][] expected;

		contents = "1.2\t1.3\n-3.5\t-3E-1";
		expected = new double[][] { { 1.2, 1.3 }, { -3.5, -0.3 } };
		assertDoubleArray(contents, expected);

		contents = "1.2\t1.3\t5e+2";
		expected = new double[][] { { 1.2, 1.3, 500 } };
		assertDoubleArray(contents, expected);

	}

	@Test
	public void testIntArray() throws IOException {
		String contents;
		int[][] expected;

		contents = "1\t3\n-3\t0";
		expected = new int[][] { { 1, 3 }, { -3, 0 } };
		assertIntArray(contents, expected);

		contents = "456456456";
		expected = new int[][] { { 456456456 } };
		assertIntArray(contents, expected);
	}

	@Test
	public void testStringArray() throws IOException {
		String contents;
		String[][] expected;

		contents = "a\tcc\nze3\tzde43";
		expected = new String[][] { { "a", "cc" }, { "ze3", "zde43" } };
		assertStringArray(contents, expected);
	}

	@Test
	public void testExceptions() throws IOException {
		String contents;
		int[][] expected;

		// "Non-empty line after matrix"
		contents = "1\t3\n\n-3\t0";
		expected = new int[][] { { 1, 3 } };
		try {
			assertIntArray(contents, expected);
			fail();
		} catch (RuntimeException e) {
		}

		// "Too many elements"
		contents = "1\t3";
		expected = new int[][] { { 1 } };
		try {
			assertIntArray(contents, expected);
			fail();
		} catch (RuntimeException e) {
		}

		// "Too few elements"
		contents = "1";
		expected = new int[][] { { 1, 3 } };
		try {
			assertIntArray(contents, expected);
			fail();
		} catch (RuntimeException e) {
		}
	}
}
