package playground.balac.utils.mobilitydataanalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class MergingData {

	public void analyse(String[] args) throws IOException {
		
		Map<String, Member> members = new HashMap<String, Member>();
		Map<String, Station> stations = new HashMap<String, Station>();

		
		final BufferedReader readRentals = IOUtils.getBufferedReader(args[0]);
		final BufferedReader readMembers = IOUtils.getBufferedReader(args[1]);
		final BufferedReader readStations = IOUtils.getBufferedReader(args[2]);
		
		readRentals.readLine();
		readMembers.readLine();
		readStations.readLine();
		
		String s = readMembers.readLine();
		
		while (s != null) {
			
			String[] arr = s.split(",");
			Member m = new Member (arr[0], arr[4],arr[5],arr[2],arr[3]);
			members.put(arr[0], m);
			
			s = readMembers.readLine();
		}
		
		s = readStations.readLine();
		
		while (s != null) {
			String[] arr = s.split(",");
			Station st = new Station(arr[7] + arr[8], arr[7], arr[8], arr[9]);
			stations.put(arr[7] + arr[8], st);
			s = readStations.readLine();
		}
		
		s = readRentals.readLine();
		int count = 0;
		while (s != null) {
			String[] arr = s.split(",");
			if (!members.containsKey(arr[0]))// || !stations.containsKey(arr[2] + arr[3]))
				count++;
			s = readRentals.readLine();
		}
		
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
		public String lat;
		public String lon;
		public String cars;
		
		public Station(String id, String lat, String lon, String cars) {
			
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.cars = cars;
		}
	}
	

}
