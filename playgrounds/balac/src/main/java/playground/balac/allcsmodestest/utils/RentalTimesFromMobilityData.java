package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;

import org.matsim.core.utils.io.IOUtils;


public class RentalTimesFromMobilityData {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub		
		int[] rentalTimes = new int[24];
		int[] startTimes = new int[24];
		int count = 0;
		int startCount = 0;
		//int count1 = 0;
		double distance = 0.0;
	//	Set<Double> bla = new HashSet<Double>();
		
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Documents/MobilityData/Fahrten_Mobility_Real.txt");
		String s = readLink.readLine();
		s = readLink.readLine();
		while(s != null) {
			String[] arr = s.split("\t");
			if (Double.parseDouble(arr[5]) > 0.0 && Double.parseDouble(arr[5]) < 80.0) {
				if (arr[3].startsWith("9") && arr[4].startsWith("9")) {
			
					String[] arr1 = arr[3].split("\\s");
					String[] arr2 = arr[4].split("\\s");
				
					String[] arr3 = arr1[0].split("/");
					String[] arr4 = arr2[0].split("/");
					
					if (arr3[1].equals(arr4[1]) && Integer.parseInt(arr3[1]) >= 1 && Integer.parseInt(arr3[1]) <=30 && arr1.length == arr2.length) {
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
									distance += Double.parseDouble(arr[5]);
									rentalTimes[(int)((-rental) / 60)]++;
									count++;
									//bla.add(Double.parseDouble(arr[2]));
								}
							}
							

						}
						
					}
				
				}
			}
			
			s = readLink.readLine();
			
			
		}
		for (int i = 0; i < startTimes.length; i++) 
			System.out.println((double)startTimes[i]/(double)startCount * 100.0);
		System.out.println(distance/count);
		System.out.println(count);
		for (int i = 0; i < rentalTimes.length; i++) 
			System.out.println((double)rentalTimes[i]/(double)count * 100.0);

	}

}
