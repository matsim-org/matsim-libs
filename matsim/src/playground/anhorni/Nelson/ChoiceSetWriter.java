package playground.anhorni.Nelson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeMap;

import org.matsim.core.utils.io.IOUtils;

public class ChoiceSetWriter {
		
	
	public void write(String outfile, TreeMap<String, Person> persons) {
		int personCnt = 0;
		int tripCnt = 0;
		
		try {
			BufferedWriter out = IOUtils.getBufferedWriter(outfile);
	
			out.write("PersonId\tUniqueId_trip\tRouteNr\tLength\tRiseAv\tRiseMax\tBikeAv\tTLights\tPS\n");
			Iterator<Person> person_it = persons.values().iterator();
			while (person_it.hasNext()) {
				Person person = person_it.next();
				personCnt++;
								
				Iterator<Trip> trip_it = person.getTrips().iterator();
				while (trip_it.hasNext()) {
					Trip trip = trip_it.next();
					
					tripCnt++;
										
					for (int i = 0; i < 61; i++) {
						// route is not the chosen alternative
						if (i != trip.getRouteChoice()) {
							
							out.write(person.getId() + "\t");
							out.write(trip.getId() + "\t");
							
							Route route = trip.getRoutes()[i];
							
							out.write(i + "\t");
							out.write(route.getLength()  + "\t");
							out.write(route.getRiseAv()  + "\t");
							out.write(route.getRiseMax()  + "\t");
							out.write(route.getBikeAv()  + "\t");
							out.write(route.getTLights()  + "\t");
							out.write(route.getPS()  + "\n");
						}
					}
				}
			}
			System.out.println("-" + personCnt + " persons");
			System.out.println("-" + tripCnt + " trips");
			System.out.println("Written to: " + outfile);
			out.flush();			
			out.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
