package playground.yu.integration.cadyts.demandCalibration.withCarCounts.experiments.smearLinkUtilOffsets;

public class ArrayUtils {
	public static int[][] complete(int[][] array) {
		for (int i = 0; i < array.length; i++)
			if (array[i] == null)
				array[i] = new int[] {};
		return array;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int[][] array = new int[4][];
		array[0] = new int[4];
		array[2] = new int[1];
		array[3] = new int[3];

		complete(array);

		System.out.println("array:");
		for (int i = 0; i < array.length; i++) {
			System.out.println("row:\t" + i + "\t" + array[i]);
			for (int j = 0; j < array[i].length; j++)
				System.out.println("row:\t" + i + "\tcol:\t" + j + "\t"
						+ array[i][j]);
		}
		System.out.println("a[1].length =\t" + array[1].length);
	}

}
