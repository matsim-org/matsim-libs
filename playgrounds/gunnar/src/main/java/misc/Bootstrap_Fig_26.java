package misc;

import java.io.IOException;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import floetteroed.utilities.math.BasicStatistics;
import floetteroed.utilities.math.Matrix;
import floetteroed.utilities.math.MatrixReader;
import floetteroed.utilities.math.Vector;

public class Bootstrap_Fig_26 {

	public static void main(String[] args) {

		final MatrixReader reader = new MatrixReader();
		try {
			reader.read("./SimObs.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
		final Matrix data = reader.getResult().get(0);

		final Random rnd = new Random();
		final int maxR = 1000;
		final DescriptiveStatistics stats = new DescriptiveStatistics();
		final int[] wasBestCnt = new int[data.rowSize()];
		for (int r = 0; r < maxR; r++) {

			double min = Double.POSITIVE_INFINITY;
			int bestIndex = -1;
			for (int i = 0; i < data.rowSize(); i++) {
				final Vector row = data.getRow(i);
				final BasicStatistics rowStats = new BasicStatistics();
				for (int j = 0; j < row.size(); j++) {
					rowStats.add(row.get(rnd.nextInt(row.size())));
				}
				if (rowStats.getAvg() < min) {
					bestIndex = i;
					min = rowStats.getAvg();
				}
			}
			wasBestCnt[bestIndex]++;
			stats.addValue(min);

			System.out.println();
			System.out.println("Mean(min obj. fct. val)=" + stats.getMean());
			System.out.println(
					"95% confidence interval=[" + stats.getPercentile(2.5) + "," + stats.getPercentile(97.5) + "]");
			// double probaSum = 0;
			for (int i = 0; i < wasBestCnt.length; i++) {
				final double proba = wasBestCnt[i] / (r + 1.0);
				System.out.println("Pr(theta=" + (-6.0 + i * 0.25) + " is min.)=" + proba);
				// System.out.print("\t");
				// probaSum += proba;
			}
			System.out.println();
			// System.out.println("\tprobaSum = " + probaSum);
		}
	}

}
