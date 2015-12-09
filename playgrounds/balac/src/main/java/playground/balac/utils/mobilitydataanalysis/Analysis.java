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
		final BufferedReader readLinkStations = IOUtils.getBufferedReader(args[1]);
		Set<String> bahnhofStations = new HashSet<String>();

		readLinkStations.readLine();
		String s = readLinkStations.readLine();
		int bCars = 0;
		while (s!=null) {
			
			String[] line = s.split("\t");
			
			if ((line[1].startsWith("Bahnhof") || line[1].endsWith("Bahnhof") ) && !line[6].equals("1")) {
				bahnhofStations.add(line[4] + line[5]);
				bCars+= Integer.parseInt(line[6]);
			}
			s = readLinkStations.readLine();
		}
		
		
		readLink.readLine();
		
		s = readLink.readLine();
		
		Set<String> stations = new HashSet<String>();
		Set<String> cars = new HashSet<String>();
		Set<String> users = new HashSet<String>();
		
		Set<String> carsBahnhof = new HashSet<String>();
		int rentalsBahnhof = 0;
		int[] rentalsPerMonth = new int[12];
		int[] rentalsPerDay = new int[7];
		double[] kmPerDayAmount = new double[7];
		int rentals = 0;
		
		while (s != null) {
			
			String[] arr = s.split(",");
			double kmTravelled = Double.parseDouble(arr[6]);
			if (kmTravelled > 0.0) {
				stations.add(arr[2] + arr[3]);
				cars.add(arr[1]);
				users.add(arr[0]);
				rentals++;
				
				if (bahnhofStations.contains(arr[2] + arr[3])) {
						carsBahnhof.add(arr[1]);
						rentalsBahnhof++;
				}
				
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
					kmPerDayAmount[ (dayOfTheYear % 7 + 3) % 7] += kmTravelled;
	
					
				}
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

		System.out.println("Average milage per rental per week day [Mo - Su]:");
		for(int i = 0; i < 7; i++) {
			
			System.out.println(kmPerDayAmount[i]/rentalsPerDay[i]);
		}
		System.out.println("Cars at bahnhofs: " + bCars);
		System.out.println("Cars used at bahnhof: " + carsBahnhof.size());
		System.out.println("Rentals at bahnhof: " + rentalsBahnhof);

		
	}

}
