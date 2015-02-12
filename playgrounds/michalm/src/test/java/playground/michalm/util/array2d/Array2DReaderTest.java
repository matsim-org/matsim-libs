package playground.michalm.util.array2d;

import java.io.StringReader;

import junit.framework.TestCase;


/**
 * @author michalm
 */
public class Array2DReaderTest
    extends TestCase
{
    private void assertDoubleArray(String contents, double[][] expected)
    {
        double[][] actual = (double[][])Array2DReader.getArray(new StringReader(contents),
                expected[0].length, Array2DReader.DOUBLE_STRATEGY);

        Array2DAssert.assertEquals(expected, actual);
    }


    private void assertIntArray(String contents, int[][] expected)
    {
        int[][] actual = (int[][])Array2DReader.getArray(new StringReader(contents),
                expected[0].length, Array2DReader.INT_STRATEGY);

        Array2DAssert.assertEquals(expected, actual);
    }


    private void assertStringArray(String contents, String[][] expected)
    {
        String[][] actual = (String[][])Array2DReader.getArray(new StringReader(contents),
                expected[0].length, Array2DReader.STRING_STRATEGY);

        Array2DAssert.assertEquals(expected, actual);
    }


    public void testDoubleArray()
    {
        String contents;
        double[][] expected;

        contents = "1.2\t1.3\n-3.5\t-3E-1";
        expected = new double[][] { { 1.2, 1.3 }, { -3.5, -0.3 } };
        assertDoubleArray(contents, expected);

        contents = "1.2\t1.3\t5e+2";
        expected = new double[][] { { 1.2, 1.3, 500 } };
        assertDoubleArray(contents, expected);

    }


    public void testIntArray()
    {
        String contents;
        int[][] expected;

        contents = "1\t3\n-3\t0";
        expected = new int[][] { { 1, 3 }, { -3, 0 } };
        assertIntArray(contents, expected);

        contents = "456456456";
        expected = new int[][] { { 456456456 } };
        assertIntArray(contents, expected);
    }


    public void testStringArray()
    {
        String contents;
        String[][] expected;

        contents = "a\tcc\nze3\tzde43";
        expected = new String[][] { { "a", "cc" }, { "ze3", "zde43" } };
        assertStringArray(contents, expected);
    }


    public void testExceptions()
    {
        String contents;
        int[][] expected;

        // "Non-empty line after matrix"
        contents = "1\t3\n\n-3\t0";
        expected = new int[][] { { 1, 3 } };
        try {
            assertIntArray(contents, expected);
            fail();
        }
        catch (RuntimeException e) {}

        // "Too many elements"
        contents = "1\t3";
        expected = new int[][] { { 1 } };
        try {
            assertIntArray(contents, expected);
            fail();
        }
        catch (RuntimeException e) {}

        // "Too few elements"
        contents = "1";
        expected = new int[][] { { 1, 3 } };
        try {
            assertIntArray(contents, expected);
            fail();
        }
        catch (RuntimeException e) {}
    }
}
