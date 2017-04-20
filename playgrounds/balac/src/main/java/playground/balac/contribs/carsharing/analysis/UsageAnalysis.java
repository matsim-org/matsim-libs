package playground.balac.contribs.carsharing.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class UsageAnalysis {
	
	
	
	private static void outputStatistics(String filePath) throws IOException {
		

		// TODO Auto-generated method stub

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/500.CS.txt");
		int[] rentalTimes = new int[30];

		int[] rentalStart = new int[35];
		Set<String> bla = new HashSet<>();
		Set<String> usedCarsT = new HashSet<String>();
		Set<String> usedCarsC = new HashSet<String>();

		String s = readLink.readLine();
		s = readLink.readLine();
		int count = 0;
		int countC = 0;
		int countT = 0;

		double time1 = 0.0;
		
		double egressDistance = 0.0;
		double accessdistance = 0.0;
		int rentalStop = 0;
		double rentalTimeStop = 0.0;
		while(s != null) {
			String[] arr = s.split(",");
			if (arr[12].startsWith("TW")) {

			//if (Double.parseDouble(arr[5]) != 0.0 && Double.parseDouble(arr[6]) < 1800) {
			if (arr[13].startsWith("Catchacar")) {
				double time = Double.parseDouble(arr[10]);
				accessdistance += time * 1.0;
				//distance[(int)(time / 1.05)]++;
				bla.add((arr[0]));
				
				if (arr[14].equals("car")) {
					usedCarsC.add(arr[12]);
					countC++;
				}
				else {
					countT++;
					usedCarsT.add(arr[12]);
				}

				double startTime = Double.parseDouble(arr[2]);
				rentalStart[(int)((startTime) / 3600)]++;			
	
				double endTime = Double.parseDouble(arr[3]);
				rentalTimes[(int)((endTime - startTime) / 3600)]++;
				time1 += endTime -startTime;
				
				count++;
				double inVehicleTime = Double.parseDouble(arr[9]);
				
				if (endTime - startTime > inVehicleTime + time) {
					rentalStop++;
					rentalTimeStop += endTime - startTime;
				}
			}
			//}
			}
			s = readLink.readLine();		
			
		}
		System.out.println();

		System.out.println("Number of rentals is: " + count);
		System.out.println("Number of rentals with stopover is: " + rentalStop);

		System.out.println("Number of car rentals is: " + countC);
		System.out.println("Number of transporter rentals is: " + countT);

		System.out.println("Number of different users is: " + bla.size());
		System.out.println("Number of used cars: " + usedCarsC.size());
		System.out.println("Number of used transporters: " + usedCarsT.size());
		System.out.println("Average rental time is: " + time1/count/3600.0);
		System.out.println("Average rental time with stopovers is: " + rentalTimeStop/rentalStop);

		System.out.println("Avg. access distance: " + accessdistance/count);
		
		
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/500.CS.txt");
		int[] rentalTimes = new int[30];
		int[] distance = new int[100];

		int[] rentalStart = new int[35];
		Set<String> bla = new HashSet<>();
		Set<String> usedCarsT = new HashSet<String>();
		Set<String> usedCarsC = new HashSet<String>();

		String s = readLink.readLine();
		s = readLink.readLine();
		int count = 0;
		int countC = 0;
		int countT = 0;

		int count1 = 0;
		int countZero = 0;
		double di = 0.0;
		double time1 = 0.0;
		
		double egressDistance = 0.0;
		double accessdistance = 0.0;
		double turnover = 0.0;
		int rentalStop = 0;
		double rentalTimeStop = 0.0;
		while(s != null) {
			String[] arr = s.split(",");
			if (arr[12].startsWith("FF")) {

			//if (Double.parseDouble(arr[5]) != 0.0 && Double.parseDouble(arr[6]) < 1800) {
			if (arr[13].startsWith("Catchacar")) {
				double time = Double.parseDouble(arr[10]);
				accessdistance += time * 1.0;
				//distance[(int)(time / 1.05)]++;
				bla.add((arr[0]));
				
				if (arr[14].equals("car")) {
					usedCarsC.add(arr[12]);
					countC++;
				}
				else {
					countT++;
					usedCarsT.add(arr[12]);
				}

				double startTime = Double.parseDouble(arr[2]);
				
				rentalStart[(int)((startTime) / 3600)]++;			
	
				double endTime = Double.parseDouble(arr[3]);
				rentalTimes[(int)((endTime - startTime) / 3600)]++;
				di += Double.parseDouble(arr[8]);
				time1 += endTime -startTime;
				
				if (startTime>= 14 * 3600 && startTime <= 18 * 3600)
					turnover += 0.5 * (endTime - startTime) /60.0 * 0.3;
				else
					turnover += (endTime - startTime) /60.0 * 0.3;
				
				
				count++;
				double inVehicleTime = Double.parseDouble(arr[9]);
				
				if (endTime - startTime > inVehicleTime + time) {
					rentalStop++;
					rentalTimeStop += endTime - startTime;
				}
			}
			//}
			}
			s = readLink.readLine();		
			
		}
		System.out.println("Number of rentals is: " + count);
		System.out.println("Number of rentals with stopover is: " + rentalStop);

		System.out.println("Number of car rentals is: " + countC);
		System.out.println("Number of transporter rentals is: " + countT);

		System.out.println("Number of different users is: " + bla.size());
		System.out.println("Number of used cars: " + usedCarsC.size());
		System.out.println("Number of used transporters: " + usedCarsT.size());
		System.out.println("Average rental time is: " + time1/count);
		System.out.println("Average rental time with stopovers is: " + rentalTimeStop/rentalStop);

		System.out.println("Avg. access distance: " + accessdistance/count);
		System.out.println("Turnover: " + turnover);

		outputStatistics("");
		/*System.out.println("Avg. access distance: " + accessdistance/count);
		System.out.println(count);
		System.out.println(di/count);
		
		for (int i = 0; i < rentalTimes.length; i++) 
			System.out.println((double)rentalTimes[i]/(double)count * 100.0);
		System.out.println(count1);	
		for (int i = 0; i < distance.length; i++) 
			System.out.println((double)distance[i]/(double)count * 100.0);
		System.out.println();
		for (int i = 0; i < rentalStart.length; i++) 
			System.out.println((double)rentalStart[i]/(double)count * 100.0);
		System.out.println("Turnover is: " + time1 / 60.0 * 0.37);*/
		
	}
}
