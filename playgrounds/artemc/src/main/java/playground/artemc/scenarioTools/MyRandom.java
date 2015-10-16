package playground.artemc.scenarioTools;

import java.util.ArrayList;


/**
 * Created by artemc on 7/7/15.
 */
public class MyRandom {


	public static void main(String[] args){

		long seed = Double.doubleToLongBits(Math.random());
		System.out.println(seed);

		java.util.Random generator = new java.util.Random(4600062981430244332L);

		ArrayList<Double> values = new ArrayList<Double>();
		Double sum =0.0;
		for(int i=0;i<8000;i++){
			/*Generate random factor  between 0.4 and 1.6 from normal distribution with mean=1 and std=0.3*/

			double randomValue = 0.0;
			do {
				randomValue = generator.nextGaussian();
			}while(randomValue< -2.0 || randomValue > 2.0);

			values.add(randomValue);
			sum = sum + randomValue;
		}

//		long seed = Double.doubleToLongBits(Math.random());
//
//		double alpha = 9.0;
//		double beta = Math.sqrt(1/(4*alpha * (alpha-1)));
//		double mean = alpha * beta;
//		double invMean = (alpha - 1) * beta;
//
//		System.out.println(mean);
//
//		RandomGenerator rg = new JDKRandomGenerator();
//		rg.setSeed(46000629);;
//		double[] sample = new GammaDistribution(rg, alpha, beta).sample(8000);
//
//		Double sum =0.0;
//		Double invSum=0.0;
//		Double max=0.5;
//		Double min=0.5;
//
//		sum =0.0;
//		for(int i=0;i<sample.length;i++){
//			sum = sum + sample[i];
//			invSum = invSum + 1/sample[i];
//
//			if(sample[i]<min)
//				min=sample[i];
//
//			if(sample[i]>max)
//				max=sample[i];
//		}
//
//		mean = sum / 8000.0;
//		invMean = invSum/8000.0;
//
//		for(int i=0;i<sample.length;i++){
//			sample[i] = 0.5 * sample[i] / mean;
//			invSum = invSum + 1/sample[i];
//
//			mean = sum / 8000.0;
//			invMean = invSum/8000.0;
//		}
//
//
//		System.out.println(sum / 8000.0+","+max+","+min);
//		System.out.println(invSum / 8000.0);
//
	}

}
