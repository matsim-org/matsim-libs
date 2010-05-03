/**
 * 
 */
package playground.johannes.teach.telematics.ha2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import org.matsim.core.utils.collections.Tuple;


/**
 * @author fearonni
 *
 */
public class FindNashGG {

//	private static double maxError = 1400;
	
	private static double STEP = 100;
	
	private static Random random = new Random(1);
	
//	private static double numRoutes = 2;
	
	private static String baseDir;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		baseDir = args[0];
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(baseDir + "results.txt"));
		writer.write("tt_pred_1\ttt_pred_2\ttt_real_1\ttt_real_2\terror\taccepted");
		writer.newLine();
		
		for(int j = 0; j < 500; j++) {
		
		double error_i = Double.MAX_VALUE;
		double error_iminus1 = Double.MAX_VALUE;
		
		double[] tt_pred_i = new double[]{random.nextInt(1000)+432, random.nextInt(1000)+432};
		double[] tt_pred_last = new double[2];
		for(int i = 0; i < 200; i++) {
//		while(error_i > maxError) {
			/*
			 * run the mobility simulation
			 */
			Tuple<double[], double[]> results = runMobsim(tt_pred_i);
			double[] tt_real = results.getFirst();
			double[] n_real = results.getSecond();
			/*
			 * evaluate the target function
			 */
			double tt_real_min = Math.min(tt_real[0], tt_real[1]);
			double[] delta_tt = new double[2];
			delta_tt[0] = tt_real[0] - tt_real_min;
			delta_tt[1] = tt_real[1] - tt_real_min;
			error_iminus1 = error_i;
			error_i = delta_tt[0] * n_real[0] + delta_tt[1] * n_real[1];
			
			writer.write(Double.toString(tt_pred_i[0]));
			writer.write("\t");
			writer.write(Double.toString(tt_pred_i[1]));
			writer.write("\t");
			writer.write(Double.toString(tt_real[0]));
			writer.write("\t");
			writer.write(Double.toString(tt_real[1]));
			writer.write("\t");
			writer.write(Double.toString(error_i));
			writer.write("\t");
//			/*
//			 * check if we improved
//			 */
//			if(error_i > error_iminus1) {
//				tt_pred_i[0] = tt_pred_last[0];
//				tt_pred_i[1] = tt_pred_last[1];
//				
//				writer.write("no");
//			} else {
//				writer.write("yes");
//			}
//			writer.newLine();
//			writer.flush();
//			/*
//			 * remember this prediction
//			 */
//			tt_pred_last[0] = tt_pred_i[0];
//			tt_pred_last[1] = tt_pred_i[1];
			
			double p = 1 - (error_i / (error_i + error_iminus1));
			if(random.nextDouble() < p) {
				writer.write("yes");
			} else {
				tt_pred_i[0] = tt_pred_last[0];
				tt_pred_i[1] = tt_pred_last[1];
				writer.write("no");
			}
			tt_pred_last[0] = tt_pred_i[0];
			tt_pred_last[1] = tt_pred_i[1];
			/*
			 * generate a new prediction
			 */
			int k = 0;
			double proba_k = 0.5;
			if(random.nextDouble() < proba_k) {
				k = 1;
			}
			
//			double proba_upDown = 0.5;
//			if(random.nextDouble() < proba_upDown) {
//				tt_pred_i[k] = tt_pred_i[k] + STEP; 
//			} else {
//				tt_pred_i[k] = tt_pred_i[k] - STEP;
//			}
//			STEP = 100;//Math.pow(error_i, 1.0)/10000.0;
			STEP = error_i/1000.0;
			 
			tt_pred_i[k] = tt_pred_i[k] + ((random.nextDouble() * 2 * STEP) - STEP);
			
			while(tt_pred_i[0] < 432 || tt_pred_i[1] < 432) {
				tt_pred_i[k] = tt_pred_i[k] + ((random.nextDouble() * 2 * STEP) - STEP);
			}
			
			writer.newLine();
			writer.flush();
		}
		}
		writer.close();
	}
	
	private static Tuple<double[], double[]> runMobsim(double[] prediction) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(baseDir + "matsim/prediction.txt"));
		writer.write("tt_pred_1\ttt_pred_2");
		writer.newLine();
		writer.write(Double.toString(prediction[0]));
		writer.write("\t");
		writer.write(Double.toString(prediction[1]));
		writer.close();
		/*
		 * run matsim
		 */
		Controller.main(new String[]{baseDir + "matsim/config.ha2.xml"});
		/*
		 * read results
		 */
		double[] tt_real = new double[2];
		double[] n_real = new double[2];
		BufferedReader reader = new BufferedReader(new FileReader(baseDir + "matsim/output/routeTravelTimes.txt"));
		reader.readLine();
		String line = reader.readLine();
//		while((line = reader.readLine()) != null) {
			String[] tokens = line.split("\t");
			n_real[0] = Double.parseDouble(tokens[1]);
			n_real[1] = Double.parseDouble(tokens[2]);
			tt_real[0] = Double.parseDouble(tokens[3]);
			tt_real[1] = Double.parseDouble(tokens[4]);
//		}
		
		return new Tuple<double[], double[]>(tt_real, n_real);
	}

}
