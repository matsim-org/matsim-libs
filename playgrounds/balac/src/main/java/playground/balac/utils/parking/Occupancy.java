package playground.balac.utils.parking;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.utils.io.IOUtils;

public class Occupancy implements BasicEventHandler {

	//Map<String, Integer> count = new HashMap<String, Integer>();
	int[] count = new int[1440 * 3];
	
	Set<String> garages = new TreeSet<String>();
	public Occupancy(Set<String> garages) {
		
		this.garages = garages;
	}
	
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(Event event) {
		if (event.getTime() < 86400) {
		if (event.getEventType().equalsIgnoreCase(ParkingArrivalEvent.EVENT_TYPE) ) {
			
			if (garages.contains(event.getAttributes().get(ParkingArrivalEvent.ATTRIBUTE_PARKING_ID))) {
				
				count[(int)(event.getTime()/20)]++;
				
			}
			
				
			
		}
		else {
			if (garages.contains(event.getAttributes().get(ParkingDepartureEvent.ATTRIBUTE_PARKING_ID))) {
				
				count[(int)(event.getTime()/20)]--;
				
			}
			
		}
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		final BufferedReader readLink = IOUtils.getBufferedReader("C:/Users/balacm/Desktop/garageParkingIds.txt");

		EventsManager events = EventsUtils.createEventsManager();
		
		Set<String> garages = new TreeSet<String>();
		String s = readLink.readLine();
		while(s != null) {
			
			garages.add(s);
			s = readLink.readLine();
			
		}
		Occupancy occ = new Occupancy(garages);
		events.addHandler(occ); 

		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(args[0]);
		
		int[] count = occ.getCounts();
		
		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:/Users/balacm/Desktop/countMATSim.txt");
		for (int i = 1; i < count.length; i++) {
			
			count[i] = count[i] + count[i - 1];
		}
		for (int i = 0; i < count.length; i++) {
			
			outLink.write(Integer.toString(10 * count[i]));
			outLink.newLine();
		}
		outLink.flush();
		outLink.close();
		
	}


	private int[] getCounts() {

		return count;
		
	}

}
