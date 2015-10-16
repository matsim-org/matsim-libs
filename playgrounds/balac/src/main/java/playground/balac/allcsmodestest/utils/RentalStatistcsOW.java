package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class RentalStatistcsOW {

	public static void main(String[] args) throws IOException {

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.1000.OW_CS");
		int[] rentalTimes = new int[30];
		int[] distance = new int[50];
		int[] egressBins = new int[21];

		int[] rentalStart = new int[35];
		Set<Double> bla = new HashSet<Double>();
		Set<String> usedCars = new HashSet<String>();

		String s = readLink.readLine();
		s = readLink.readLine();
		int count = 0;
		int count1 = 0;
		int countZero = 0;
		double di = 0.0;
		double time1 = 0.0;
		
		double avgaccestime = 0.0;
		double avgegresstime = 0.0;
		double totalRentalTime = 0.0;
		while(s != null) {
			String[] arr = s.split("\\s");
			if (Double.parseDouble(arr[5]) != 0.0) {
				
				avgaccestime += Double.parseDouble(arr[6]);
				avgegresstime += Double.parseDouble(arr[7]);
				egressBins[(int)(Double.parseDouble(arr[7])/60)]++;
				double time = Double.parseDouble(arr[6]);
				distance[(int)(time * 0.9 / 130.0)]++;
				bla.add(Double.parseDouble(arr[0]));
				double startTime = Double.parseDouble(arr[1]);
				rentalStart[(int)((startTime) / 3600)]++;			
	
				double endTime = Double.parseDouble(arr[2]);
				rentalTimes[(int)((endTime - startTime) / 3600)]++;
				if (endTime - startTime < 1200)
					totalRentalTime += 1200;
				else
					totalRentalTime += endTime - startTime;
				di += Double.parseDouble(arr[5]);
				time1 += endTime -startTime;
				if (endTime - startTime < 1800) 
					count1++;
				count++;
				usedCars.add(arr[8]);

			}
			s = readLink.readLine();		
			
		}
		System.out.println("Average access time is: " + Double.toString(avgaccestime / count));
		System.out.println("Average egress time is: " + Double.toString(avgegresstime / count));

		System.out.println("Number of different users is: " + bla.size());
		System.out.println("Number of unique used cars is: " + usedCars.size());
		System.out.println("Turnover :" + totalRentalTime / 3600.0 * 15.0);

		System.out.println(countZero);
		System.out.println(di/count);
		System.out.println(time1/count);
		for (int i = 0; i < rentalTimes.length; i++) 
			System.out.println((double)rentalTimes[i]/(double)count * 100.0);
		System.out.println(count1);	
		for (int i = 0; i < distance.length; i++) 
			System.out.println((double)distance[i]/(double)count * 100.0);
		System.out.println();
		for (int i = 0; i < rentalStart.length; i++) 
			System.out.println((double)rentalStart[i]/(double)count * 100.0);
		System.out.println();

		for (int i = 0; i < egressBins.length; i++) 
			System.out.println((double)egressBins[i]/(double)count * 100.0);
		
	}

}
