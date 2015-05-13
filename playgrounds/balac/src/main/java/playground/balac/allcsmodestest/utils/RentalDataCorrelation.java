package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.matsim.core.utils.io.IOUtils;

import playground.balac.utils.math.Correlation;

public class RentalDataCorrelation {

	public static void main(String[] args) throws IOException, MathException {
		// TODO Auto-generated method stub
		int count1 = 0;
		int[] startTimes = new int[30];
		int[] rentalTimes = new int[30];
		int startCount = 0;

		BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Documents/MobilityData/Fahrten2010_march.txt");
		String s = readLink.readLine();
		s = readLink.readLine();
		
		Set<Double> fahrzugIDs = new TreeSet<Double>();
		while(s != null) {
			String[] arr = s.split("\t");
			if (Double.parseDouble(arr[6]) > 0.0) {
				if (arr[4].startsWith("3") && arr[5].startsWith("3")) {
			
					String[] arr1 = arr[4].split("\\s");
					String[] arr2 = arr[5].split("\\s");
				
					String[] arr3 = arr1[0].split("/");
					String[] arr4 = arr2[0].split("/");
					
					if (arr3[1].equals(arr4[1]) && Integer.parseInt(arr3[1]) >= 8 && Integer.parseInt(arr3[1]) <= 8 && arr1.length == arr2.length) {
						if (true) {
							
							String[] arr5 = arr1[1].split(":");
							String[] arr6 = arr2[1].split(":");
							
							int starth = Integer.parseInt(arr5[0]);
							startTimes[starth]++;
							startCount++;
							int startmin = Integer.parseInt(arr5[1]);
							
							int endh = Integer.parseInt(arr6[0]);
							int endmin = Integer.parseInt(arr6[1]);
							if (endh >= starth ){//&& !arr[2].equals("Combi") && !arr[2].equals("Transport")) {
								double rental = starth*60 +startmin - endh*60 - endmin;
								if (rental < 0) {
									fahrzugIDs.add(Double.parseDouble(arr[1]));
									if ( Double.parseDouble(arr[6]) <= 60.0 ) {
										
									rentalTimes[(int)((-rental) / 60)]++;
									count1++;
									}
									//bla.add(Double.parseDouble(arr[2]));
								}
							}
							

						}
						
					}
					
				
				}
			}
			
			s = readLink.readLine();
			
			
		}
		
		readLink = IOUtils.getBufferedReader("C:/Users/balacm/Documents/Papers/STRC2015/Base/1.250.RT_CS");
		int[] rentalTimes1 = new int[30];
		
		int[] rentalStart = new int[30];
		Set<Double> bla = new HashSet<Double>();
		Set<String> usedCars = new HashSet<String>();
		
		s = readLink.readLine();
		s = readLink.readLine();
		int count = 0;
		
		while(s != null) {
			String[] arr = s.split("\\s");
			if (Double.parseDouble(arr[4]) != 0.0) {
				double time = Double.parseDouble(arr[6]);
				bla.add(Double.parseDouble(arr[0]));
			//	usedCars.add(arr[8]);
				double startTime = Double.parseDouble(arr[1]);

			//	distanceTraveled[(int)((Double.parseDouble(arr[4])) / 2000)]++;		
			
				double endTime = Double.parseDouble(arr[2]);
				rentalTimes1[(int)((endTime - startTime) / 3600)]++;
				rentalStart[(int)((startTime) / 3600)]++;			
				
				count++;
				
				usedCars.add(arr[7]);
			}
			s = readLink.readLine();		
			
		}
		double[] rt1 = new double[24];
		double[] rt2 = new double[24];
		for (int i = 0; i < 24; i++) {
			
			rt1[i] = (double)rentalTimes[i]/(double)count1 * 100.0;
			rt2[i] = (double)rentalTimes1[i]/(double)count * 100.0;

		}
		double cor = Correlation.computeCorrelation(rt1, rt2);
		
		System.out.println(cor);
		
		double[] st1 = new double[24];
		double[] st2 = new double[24];
		for (int i = 0; i < 24; i++) {
			
			st1[i] = (double)startTimes[i]/(double)count1 * 100.0;
			st2[i] = (double)rentalStart[i]/(double)count * 100.0;

		}
		
		double cor2 = Correlation.computeCorrelation(st1, st2);
		
		double[][] x = new double[24][2];
		for (int i = 0; i < 24; i++) {
			
			x[i][0] = st1[i];
			x[i][1] = st2[i];
		}
		
		PearsonsCorrelation p = new PearsonsCorrelation(x);
		//System.out.println(p.correlation(rt1, rt2));
		System.out.println(p.getCorrelationPValues());

		System.out.println(cor2);
		

	}

}
