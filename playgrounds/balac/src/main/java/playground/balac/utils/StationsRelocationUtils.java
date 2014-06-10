package playground.balac.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;

public class StationsRelocationUtils {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		BufferedReader readLink_1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Temporary_files/solutionProgress_12_new.txt");
		BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/StationsRelocation/" + "average_12_new.txt");

		double[] arr = new double[120];
		double previous = 0.0;
		int numberGenerations = 500;
		readLink_1.readLine();
		for(int i = 1; i <= numberGenerations; i++){
			String s1 = readLink_1.readLine();
			double sum  = 0.0;
			String[] arr1 = s1.split("\t");
			for (int j = 0; j < 24; j++) {
				sum += Double.parseDouble(arr1[80 - j]);
				
			}
			
			previous = sum/24.0;
			outLink.write(Double.toString(sum/24.0));
			/*if (i > 1) {
				outLink.write(String.valueOf(Double.parseDouble(arr1[51]) - previous) );
				
			}*/
			//previous = Double.parseDouble(arr1[51]); 
			//arr[i - 1] = Double.parseDouble(arr1[51]);
			outLink.newLine();
		}
		int number = 10;
		
	/*	for(int i = 0; i < numberGenerations - number; i++) {
			double sum = 0.0;
			for (int j = i; j < i + number; j++) {
				sum +=arr[j];
				
			}
			outLink.write(String.valueOf(sum/(double)number));
			outLink.newLine();
		}*/
		
		
		outLink.flush();
		outLink.close();
		
		

		
		
		
	}

}
