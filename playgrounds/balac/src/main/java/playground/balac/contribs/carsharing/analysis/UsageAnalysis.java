package playground.balac.contribs.carsharing.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class UsageAnalysis {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub

		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/440.CS_run8.txt");
		int[] rentalTimes = new int[30];
		int[] distance = new int[100];

		int[] rentalStart = new int[35];
		Set<String> bla = new HashSet<>();
		Set<String> usedCarsT = new HashSet<String>();
		Set<String> usedCarsC = new HashSet<String>();

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
			String[] arr = s.split(",");
			//if (Double.parseDouble(arr[5]) != 0.0 && Double.parseDouble(arr[6]) < 1800) {
			if (arr[13].startsWith("Mobility")) {
				double time = Double.parseDouble(arr[10]);
				accessdistance += time * 1.0;
				//distance[(int)(time / 1.05)]++;
				bla.add((arr[0]));
				
				if (arr[14].equals("car"))
					usedCarsC.add(arr[12]);
				else
					usedCarsT.add(arr[12]);

				double startTime = Double.parseDouble(arr[2]);
				rentalStart[(int)((startTime) / 3600)]++;			
	
				double endTime = Double.parseDouble(arr[3]);
				rentalTimes[(int)((endTime - startTime) / 3600)]++;
				di += Double.parseDouble(arr[8]);
				time1 += endTime -startTime;
				if (endTime - startTime < 1800) 
					count1++;
				count++;
			}
			//}
			s = readLink.readLine();		
			
		}
		System.out.println("Number of rentals is: " + count);

		System.out.println("Number of different users is: " + bla.size());
		System.out.println("Number of used cars: " + usedCarsC.size());
		System.out.println("Number of used transporters: " + usedCarsT.size());
		System.out.println("Average rental time is: " + time1/count);
		System.out.println("Avg. access distance: " + accessdistance/count);
		
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
