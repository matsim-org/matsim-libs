package playground.balac.utils.mobilitydataanalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.core.utils.io.IOUtils;

public class Analysis {
	
	
	public static void main(String[] args) throws IOException {

		
		int[] days = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};
		
		final BufferedReader readLink = IOUtils.getBufferedReader(args[0]);
		final BufferedReader readLinkStations = IOUtils.getBufferedReader(args[1]);
		final BufferedWriter rentalsNew = IOUtils.getBufferedWriter(args[4]);

		final BufferedWriter outBahnhofStations = IOUtils.getBufferedWriter(args[2]);
		final BufferedWriter outNonBahnhofStations = IOUtils.getBufferedWriter(args[3]);

		Set<String> bahnhofStations = new HashSet<String>();
		Set<Station> allStations = new HashSet<Station>();

		Set<Station> allBStations = new HashSet<Station>();
		Set<Station> allNBStations = new HashSet<Station>();

		readLinkStations.readLine();
		String s = readLinkStations.readLine();
		int bCars = 0;
		int id  = 1;
		while (s!=null) {
			
			String[] line = s.split("\t");
			Station newS = new Station(Double.parseDouble(line[7]), Double.parseDouble(line[8]), Integer.parseInt(line[9]), id);
			allStations.add(newS);
			//allBStations.add(new Station(Double.parseDouble(line[7]), Double.parseDouble(line[8]), Integer.parseInt(line[9])));
			if ((line[1].startsWith("Bahnhof") || line[1].endsWith("Bahnhof") || line[1].contains("Hauptbahnhof")) ) {
				
			//	outBahnhofStations.write(line[7] + "," + line[8] +"," + line[9]);
			//	outBahnhofStations.newLine();
				allBStations.add(newS);

				
				bahnhofStations.add(line[7] + line[8]);
				bCars+= Integer.parseInt(line[9]);
			}
			else {
				allNBStations.add(newS);

		//		outNonBahnhofStations.write(line[7] + "," + line[8] +"," + line[9]);
		//		outNonBahnhofStations.newLine();
				
			}
			id++;
			s = readLinkStations.readLine();
		}
	//	outBahnhofStations.flush();
	//	outBahnhofStations.close();
		
	//	outNonBahnhofStations.flush();
	//	outNonBahnhofStations.close();
		
		readLink.readLine();
		
		s = readLink.readLine();
		
		Set<String> stations = new HashSet<String>();
		Set<String> cars = new HashSet<String>();
		Set<String> users = new HashSet<String>();
		Map<String,Integer> rentalsPerUser = new HashMap<String,Integer>();
		
		Set<String> carsBahnhof = new HashSet<String>();
		int rentalsBahnhof = 0;
		int[] rentalsPerMonth = new int[12];
		int[] rentalsPerDay = new int[7];
		double[] kmPerDayAmount = new double[7];
		int rentals = 0;
		int count = 0;
		int idd = 0;
		while (s != null) {
			String[] arr = s.split(",");
			
			double coordX = Double.parseDouble(arr[2]);
			double coordY = Double.parseDouble(arr[3]);
			
			boolean indB = false;
			boolean indNB = false;
			for (Station ss : allBStations) {
				
				if (coordX < ss.getCoordX() + 0.0005 && coordX > ss.getCoordX() - 0.0005 && 
						coordY< ss.getCoordY() + 0.0005 && coordY > ss.getCoordY() - 0.0005) {
					indB = true;
					allStations.remove(ss);
					idd = ss.getId();
					break;
				}
				
			}
			
			for (Station ss : allNBStations) {
				
				if (coordX < ss.getCoordX() + 0.0005 && coordX > ss.getCoordX() - 0.0005 && 
						coordY< ss.getCoordY() + 0.0005 && coordY > ss.getCoordY() - 0.0005) {
					indNB = true;
					allStations.remove(ss);
					idd = ss.getId();
					break;
				}
				
			}		
			
			if (!indB && !indNB)
				rentalsNew.write(s + "," + indB + "," + indNB +"," + "-1");
			else	
				rentalsNew.write(s + "," + indB + "," + indNB +","+ idd);
			rentalsNew.newLine();
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
					
					if (rentalsPerUser.containsKey(arr[0]))
						rentalsPerUser.put(arr[0], rentalsPerUser.get(arr[0]) + 1);
					else
						rentalsPerUser.put(arr[0], 1);
					
					rentalsPerMonth[Integer.parseInt(rentalStartMonth) - 1]++;
				
					int dayOfTheYear = days[Integer.parseInt(rentalStartMonth) - 1] + Integer.parseInt(rentalStartDay);
				
					rentalsPerDay[ (dayOfTheYear % 7 + 3) % 7]++;
					kmPerDayAmount[ (dayOfTheYear % 7 + 3) % 7] += kmTravelled;
	
					
				}
			}
			s = readLink.readLine();
			
		}
		
		for (Station ss: allStations) {
			
			allBStations.remove(ss);
			allNBStations.remove(ss);
			
		}
		
		for(Station ss: allBStations) {
			
			outBahnhofStations.write(ss.getId() + "," + ss.getCoordX() + "," + ss.getCoordY() + "," + ss.getCars());
			outBahnhofStations.newLine();
		}
		
		for(Station ss: allNBStations) {
			
			outNonBahnhofStations.write(ss.getId() + "," + ss.getCoordX() + "," + ss.getCoordY() + "," + ss.getCars());
			outNonBahnhofStations.newLine();
		}
		outBahnhofStations.flush();
		outBahnhofStations.close();
		
		outNonBahnhofStations.flush();
		outNonBahnhofStations.close();
		
		
		rentalsNew.flush();
		rentalsNew.close();
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

		System.out.println(count);
	}

}
