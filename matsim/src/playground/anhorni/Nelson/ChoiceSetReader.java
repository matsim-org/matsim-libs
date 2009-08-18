package playground.anhorni.Nelson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.TreeMap;

import org.matsim.core.gbl.Gbl;

public class ChoiceSetReader {
		
	public TreeMap<String, Person> read(String file) {
		
		TreeMap<String, Person> persons = new TreeMap<String, Person>();
		
		try {
			System.out.println("Reading: " + file);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			
			String curr_line = bufferedReader.readLine(); // Skip header
			while ((curr_line = bufferedReader.readLine()) != null) {
				
				String[] entries = curr_line.split("\t", -1);
				String personId = entries[0].trim();
				
				if (!persons.containsKey(personId)) {
					persons.put(personId, new Person(personId));
				}
				Person person = persons.get(personId);
				person.addTrip(this.getTrip(entries));				
			}
			
		} catch (IOException e) {
				Gbl.errorMsg(e);
		}
		return persons;
	}
	
	private Trip getTrip(String[] entries) {
		String id = entries[8].trim();
		
		Trip trip = new Trip(id);
		
		int routeChoice = Integer.parseInt(entries[11].trim());
		trip.setRouteChoice(routeChoice);
		
		this.addRoutes2Trip(trip, entries);
		return trip;		
	}
	
	private void addRoutes2Trip(Trip trip, String[] entries) {
		
		int offset = 73;
		int interval = 27;
		
		for (int i = 0; i < 61; i++) {
			Route route = new Route(i);
			
			double length = Double.parseDouble(entries[offset + i*interval + 0].trim());
			route.setLength(length);
			
			double riseAv = Double.parseDouble(entries[offset + i*interval + 1].trim());
			route.setRiseAv(riseAv);
			
			double riseMax = Double.parseDouble(entries[offset + i*interval + 2].trim());
			route.setRiseMax(riseMax);
			
			double bikeAv = Double.parseDouble(entries[offset + i*interval + 18].trim());
			route.setBikeAv(bikeAv);
			
			double tLights = Double.parseDouble(entries[offset + i*interval + 25].trim());
			route.setTLights(tLights);
			
			double ps = Double.parseDouble(entries[offset + i*interval + 26].trim());
			route.setPS(ps);
			
			trip.setRoute(i, route);
		}	
	}
}
