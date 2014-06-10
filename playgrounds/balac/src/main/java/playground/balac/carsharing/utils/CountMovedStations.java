package playground.balac.carsharing.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.matsim.core.utils.io.IOUtils;
public class CountMovedStations {

	
	
	private int numberOfStations = 376;
	
	ArrayList<String> c = new ArrayList<String>();
	
	public void countMovedStations() throws IOException {
		BufferedReader readLink_1 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/Temporary_files/CS_StationsSummary_3_new.txt");
		BufferedReader readLink_2 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/CarSharing/CS_StationsSummary.txt");
		
		BufferedReader readLink3 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/CarSharing/CS_Stations_relocated.txt");

		BufferedReader readLink4 = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/CarSharing/CS_Stations.txt");
		
		HashMap<String, String> bla1 = new HashMap<String, String>();
		HashMap<String, String> bla2 = new HashMap<String, String>();
		HashMap<String, String> bla3 = new HashMap<String, String>();
		HashMap<String, String> bla4 = new HashMap<String, String>();
				
		ArrayList<String> c1 = new ArrayList<String>();
		ArrayList<String> c2 = new ArrayList<String>();

		readLink_1.readLine();
		readLink3.readLine();

		readLink4.readLine();
		
		for(int i = 0; i < numberOfStations; i++) {
			
			String s1 = readLink_1.readLine();
			String[] arr1 = s1.split("\t");			
			
			bla1.put(arr1[0], arr1[3]);
			
			String s2 = readLink_2.readLine();
			String[] arr2 = s2.split("\t");			
			c1.add(arr2[3]); //old links
			bla2.put(arr2[0], arr2[3]);
			if (!arr1[3].equals(arr2[3])) {
				c2.add(arr1[3]); //link of the relocated stations
				
			}

		
		}
		
		
		for(int i = 0; i < numberOfStations; i++) {
			
		if (i == 375)
			System.out.println();

			String s2 = readLink3.readLine();
			String[] arr2 = s2.split("\t");			
			System.out.println(bla1.get("2862"));
			bla3.put(bla1.get(arr2[0]), arr2[6]);
			if (bla3.size() != i +1)
				System.out.println();
			s2 = readLink4.readLine();
			arr2 = s2.split("\t");			
			
			bla4.put(bla2.get(arr2[0]), arr2[6]);
		}

		int changed = 0;
		for (String s: c2) {
			
			if (c1.contains(s)) {
				String s1 = bla3.get(s);
				String s2 = bla4.get(s);
				if (!s1.equals(s2))
					changed++;
				
			}
		}
		System.out.println(c2.size());
		System.out.println(changed);

		
		
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		CountMovedStations smc = new CountMovedStations();
		smc.countMovedStations();

	}

	
	
	
}
