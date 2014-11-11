package playground.sergioo.hits2012Scheduling;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

public class RegressionInJava {

	public static void main(String[] args) {
		OLSMultipleLinearRegression mlr = new OLSMultipleLinearRegression();
		long t = System.currentTimeMillis();
		mlr.newSampleData(new double[]{2, 1, 1.5}, new double[][]{{2},{1},{1.1}});
		System.out.println(System.currentTimeMillis()-t);
		System.out.println(mlr.calculateRSquared()+" "+mlr.calculateAdjustedRSquared()+" "+mlr.calculateTotalSumOfSquares());
		System.out.println(System.currentTimeMillis()-t);
		System.out.println(mlr.estimateRegressionParameters()[0]);
		System.out.println(System.currentTimeMillis()-t);
		System.out.println(mlr.calculateRSquared()+" "+mlr.calculateAdjustedRSquared()+" "+mlr.calculateTotalSumOfSquares());
		System.out.println(System.currentTimeMillis()-t);
	}

}
