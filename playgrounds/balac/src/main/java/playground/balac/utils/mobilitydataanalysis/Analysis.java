package playground.balac.utils.mobilitydataanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class Analysis {

	public static void main(String[] args) throws IOException {

		
		int[] days = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};
		
		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);

		readLink.readLine();
		
		String s = readLink.readLine();
		
		Set<String> stations = new HashSet<String>();
		Set<String> cars = new HashSet<String>();
		Set<String> users = new HashSet<String>();
		
		int[] rentalsPerMonth = new int[12];
		int[] rentalsPerDay = new int[7];
		double[] kmPerDayAmount = new double[7];
		int rentals = 0;
		
		while (s != null) {
			
			String[] arr = s.split(",");
			
			stations.add(arr[2] + arr[3]);
			cars.add(arr[1]);
			users.add(arr[0]);
			rentals++;
			
			
			String rentalStart = arr[4];
			String rentalEnd = arr[5];
			
			String rentalStartDate = rentalStart.split("\\s")[0];
			String rentalStartMonth = rentalStartDate.split("/")[0];
			String rentalStartDay = rentalStartDate.split("/")[1];
			String rentalStartYear = rentalStartDate.split("/")[2];
			if (rentalStartYear.equals("2010")) {
				rentalsPerMonth[Integer.parseInt(rentalStartMonth) - 1]++;
			
				int dayOfTheYear = days[Integer.parseInt(rentalStartMonth) - 1] + Integer.parseInt(rentalStartDay);
			
				rentalsPerDay[ (dayOfTheYear % 7 + 3) % 7]++;
			}
			s = readLink.readLine();
			
		}
		
		System.out.println("Number of stations in the area: " + stations.size());
		System.out.println("Number of cars used in the area: " + cars.size());
		System.out.println("Number of different actual users in the area: " + users.size());
		System.out.println("Number of rentals in the area: " + Integer.toString(rentals));
		System.out.println("Rentals per month  [Jan - Dec]:");

		for(int i = 0; i < 12; i++) {
			
			System.out.println(rentalsPerMonth[i]);
		}
		
		System.out.println("Rentals per week day [Mo - Su]:");
		for(int i = 0; i < 7; i++) {
			
			System.out.println(rentalsPerDay[i]);
		}


		
	}

}
