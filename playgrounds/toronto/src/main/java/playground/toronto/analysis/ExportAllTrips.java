package playground.toronto.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;

import playground.toronto.analysis.handlers.AgentTripChainHandler;
import playground.toronto.analysis.handlers.SimplePopulationHandler;
import playground.toronto.analysis.tripchains.Trip;

public class ExportAllTrips {

	
	public static void main(String[] args) throws IOException{
		String eventsFile = args[0];
		String exportFile = args[1];
		
		
		AgentTripChainHandler tripHandler = new AgentTripChainHandler();
		SimplePopulationHandler popHandler = new SimplePopulationHandler();
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(tripHandler);
		em.addHandler(popHandler);
		
		new MatsimEventsReader(em).readFile(eventsFile);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(exportFile));
		bw.write("pid,trip.number,start.time,end.time,ivtt.time,wait.time,walk.time");
		
		
		int totalTrips = 0;
		for (Id p : popHandler.getPop()){
			int numberOfTrips = tripHandler.getTripSize(p);
			for (int i = 0; i < numberOfTrips; i++){
				Trip trip = tripHandler.getTrip(p, i);
				if (trip.getAutoDriveTime() > 0 ) continue; //skip auto trips
				
				bw.newLine();
				bw.write(p + "," + i + "," + Time.writeTime(trip.getStartTime()) + "," +
						Time.writeTime(trip.getEndTime()) + "," + trip.getIVTT() + ","
						+ trip.getWaitTime() + "," + trip.getWalkTime());
				
				totalTrips++;
			}
		}
		bw.close();
		
			
		
	}
	
}
