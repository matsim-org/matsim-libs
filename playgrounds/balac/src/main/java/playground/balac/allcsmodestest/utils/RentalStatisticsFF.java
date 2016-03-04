package playground.balac.allcsmodestest.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class RentalStatisticsFF {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/1.300.FF_CS");
		int[] rentalTimes = new int[30];
		int[] distance = new int[100];

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
		
		double egressDistance = 0.0;
		double accessdistance = 0.0;
		
		while(s != null) {
			String[] arr = s.split("\\s");
			if (Double.parseDouble(arr[5]) != 0.0 && Double.parseDouble(arr[6]) < 1800) {
				double time = Double.parseDouble(arr[6]);
				accessdistance += time * 1.05;
				//distance[(int)(time / 1.05)]++;
				bla.add(Double.parseDouble(arr[0]));
				usedCars.add(arr[8]);
				double startTime = Double.parseDouble(arr[1]);
				rentalStart[(int)((startTime) / 3600)]++;			
	
				double endTime = Double.parseDouble(arr[2]);
				rentalTimes[(int)((endTime - startTime) / 3600)]++;
				di += Double.parseDouble(arr[5]);
				time1 += endTime -startTime;
				if (endTime - startTime < 1800) 
					count1++;
				count++;
			}
			s = readLink.readLine();		
			
		}
		System.out.println("Number of different users is: " + bla.size());
		System.out.println("Number of used cars: " + usedCars.size());
		System.out.println("Avg. access distance: " + accessdistance/count);
		System.out.println(count);
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
		System.out.println("Turnover is: " + time1 / 60.0 * 0.37);
		
	}

}
