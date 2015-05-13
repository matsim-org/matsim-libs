package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.core.utils.io.IOUtils;


public class RentalTimesFromMobilityData {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub		
		int[] rentalTimes = new int[24];
		int[] startTimes = new int[24];
		int count = 0;
		int count1 = 0;
		int startCount = 0;
		//int count1 = 0;
		double distance = 0.0;
		Set<String> bla = new HashSet<String>();
		Set<String> bla1 = new HashSet<String>();

		int[] distanceTraveled = new int[50];
		double[] timeBla = new double[50];
		int[] countBla = new int[50];
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Documents/MobilityData/Fahrten2010_march.txt");
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
					
					if (arr3[1].equals(arr4[1]) && Integer.parseInt(arr3[1]) >= 15 && Integer.parseInt(arr3[1]) <= 15 && arr1.length == arr2.length) {
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
									if ( Double.parseDouble(arr[6]) <= 160.0 ) {
										if (timeBla[(int)((Double.parseDouble(arr[6])) / 5.0)] < -rental) {
											timeBla[(int)((Double.parseDouble(arr[6])) / 5.0)] = -rental;
											
										}
										distanceTraveled[(int)((Double.parseDouble(arr[6])) / 5.0)]++;	
										//timeBla[(int)((Double.parseDouble(arr[6])) / 5.0)] += (-rental);
										//countBla[(int)((Double.parseDouble(arr[6])) / 5.0)] ++;
									distance += Double.parseDouble(arr[6]);
									bla.add(arr[0]);
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
		System.out.println(bla.size());
		System.out.println(bla1.size());
		
		System.out.println(fahrzugIDs.size());
		
		for (int i = 0; i < startTimes.length; i++) 
			System.out.println((double)startTimes[i]/(double)startCount * 100.0);
		
		System.out.println(distance/count1);
		
		System.out.println("rentals: " + count1);
		
		for (int i = 0; i < rentalTimes.length; i++) 
			System.out.println((double)rentalTimes[i]/(double)count1 * 100.0);
		
		System.out.println();

		for (int i = 0; i < distanceTraveled.length; i++) 
			System.out.println((double)distanceTraveled[i]/(double)count1 * 100.0);
		
		for (int i = 0; i < timeBla.length; i++) 
			System.out.println(timeBla[i]);

	}

}
