package playground.balac.utils.mobilitydataanalysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.IOUtils;

public class MergingData {

	public void analyse(String[] args) throws IOException {
		int[] days = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365};

		Map<String, Member> members = new HashMap<String, Member>();
		Map<String, Station> stations = new HashMap<String, Station>();

		QuadTree<Station> stationsQuadTree;	
		double minx = 45.0D;
		double miny = 6.0D;
		double maxx = 48.0;
		double maxy = 11.0;

		stationsQuadTree = new QuadTree<Station>(minx, miny, maxx, maxy);

		final BufferedReader readRentals = IOUtils.getBufferedReader(args[0]);
		final BufferedReader readMembers = IOUtils.getBufferedReader(args[1]);
		final BufferedReader readStations = IOUtils.getBufferedReader(args[2]);
		
		final BufferedWriter mergedData = IOUtils.getBufferedWriter(args[3]);
		mergedData.write("person_ID;age;sex;lloc_lat;lloc_long;cs_stat;cars;cs_lat;cs_long;start;dur;distance;day");
		mergedData.newLine();
		readRentals.readLine();
		readMembers.readLine();
		readStations.readLine();
		
		String s = readMembers.readLine();
		
		while (s != null) {
			
			String[] arr = s.split(",");
			int intage;
			if (arr[3].split("/").length < 3)
				intage = -99;
			else 
				intage = 2010 - Integer.parseInt(arr[3].split("/")[2]);
			
			Member m = new Member (arr[0], arr[4],arr[5],arr[2], Integer.toString(intage));
			members.put(arr[0], m);
			
			s = readMembers.readLine();
		}
		
		s = readStations.readLine();
		int i = 0;
		while (s != null) {
			String[] arr = s.split(",");
			Station st = new Station(arr[7] + arr[8], Integer.toString(i), arr[7], arr[8], arr[9]);
			stations.put(arr[7] + arr[8], st);
			stationsQuadTree.put(Double.parseDouble(arr[7]), Double.parseDouble(arr[8]), st);
			s = readStations.readLine();
			i++;
		}
		
		s = readRentals.readLine();
		int count = 0;
		while (s != null) {
			String[] arr = s.split(",");
			if (!members.containsKey(arr[0]))// || !stations.containsKey(arr[2] + arr[3]))
				count++;
			else {
				
				String rentalStart = arr[4];
				String rentalEnd = arr[5];
				
				Calendar dt1 = Calendar.getInstance();
				Calendar dt2 = Calendar.getInstance();

				if (rentalStart.split("\\s").length > 1 && rentalEnd.split("\\s").length > 1) {
				
				String rentalStartDate = rentalStart.split("\\s")[0];
				String rentalStartMonth = rentalStartDate.split("/")[0];
				String rentalStartDay = rentalStartDate.split("/")[1];
				String rentalStartYear = rentalStartDate.split("/")[2];
				String rentalStartTime = rentalStart.split("\\s")[1];
				
				String rentalEndDate = rentalEnd.split("\\s")[0];
				String rentalEndMonth = rentalEndDate.split("/")[0];
				String rentalEndDay = rentalEndDate.split("/")[1];
				String rentalEndYear = rentalEndDate.split("/")[2];
				if (rentalEnd.split("\\s").length < 2)
					System.out.println();
				String rentalEndTime = rentalEnd.split("\\s")[1];

				dt1.set(Integer.parseInt(rentalStartYear), Integer.parseInt(rentalStartMonth), 
						Integer.parseInt(rentalStartDay), Integer.parseInt(rentalStartTime.split(":")[0]), Integer.parseInt(rentalStartTime.split(":")[1]));
				
				dt2.set(Integer.parseInt(rentalEndYear), Integer.parseInt(rentalEndMonth), 
						Integer.parseInt(rentalEndDay), Integer.parseInt(rentalEndTime.split(":")[0]), Integer.parseInt(rentalEndTime.split(":")[1]));
				int diff = (int)((dt2.getTimeInMillis() - dt1.getTimeInMillis() ) / 1000 / 60);
				if (rentalStartYear.equals("2010")) {
				
					int dayOfTheYear = days[Integer.parseInt(rentalStartMonth) - 1] + Integer.parseInt(rentalStartDay);
					
					String dayofweek = Integer.toString((dayOfTheYear % 7 + 3) % 7 + 1);
					
					Member mm = members.get(arr[0]);
					Station ss = stationsQuadTree.getClosest(Double.parseDouble(arr[2]), Double.parseDouble(arr[3]));
							
					String rentalStartRFormat = rentalStartYear + "-" + rentalStartMonth + "-" + rentalStartDay + " " + rentalStartTime + ":00";
					mergedData.write(arr[0] + ";" + mm.age + ";" + mm.sex + ";" + mm.lat + ";" + mm.lon + ";" + 
					ss.idStation + ";" + ss.cars + ";" + ss.lat + ";" + ss.lon + ";" + rentalStartRFormat + ";" + Integer.toString(diff) + ";" + arr[6] + ";" + dayofweek);
					mergedData.newLine();
				}
			}
		}
			s = readRentals.readLine();

		}
		mergedData.flush();
		mergedData.close();
		
		System.out.println(count);

	}
	public static void main(String[] args) throws IOException {

		MergingData md = new MergingData();
		md.analyse(args);
		
	}
	
	public class Member {
		
		public String id;
		public String lat;
		public String lon;
		public String sex;
		public String age;
		
		public Member(String id, String lat, String lon, String sex, String age) {
			
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.age = age;
			this.sex = sex;
		}
		
		
		
	}
	
	public class Station {
		public String id;
		public String idStation;
		public String lat;
		public String lon;
		public String cars;
		
		public Station(String id, String idStation, String lat, String lon, String cars) {
			
			this.id = id;
			this.idStation = idStation;
			this.lat = lat;
			this.lon = lon;
			this.cars = cars;
		}
	}
	

}
