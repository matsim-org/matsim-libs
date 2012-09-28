package playground.toronto.analysis;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;

import playground.toronto.analysis.handlers.AgentTripChainHandler;
import playground.toronto.analysis.handlers.SimplePopulationHandler;
import playground.toronto.analysis.tripchains.Trip;

public class TripComponentsByPercentage {

	
	private static TreeMap<Double, Integer> percentages;
	private static double[] walk_pct;
	private static double[] wait_pct;
	private static double[] ivtt_pct;
	
	public static void main(String[] args) {
		int skippedTravellers = 0;
		
		String eventsFile = args[0];
		double start = 0.0;
		double end = Double.POSITIVE_INFINITY;
		String outFileName = args[1];
		
		if (args.length == 4){
			start = Time.convertHHMMInteger(Integer.parseInt(args[2]));
			end = Time.convertHHMMInteger(Integer.parseInt(args[3]));
		}
			
		AgentTripChainHandler tripHandler = new AgentTripChainHandler();
		SimplePopulationHandler popHandler = new SimplePopulationHandler();
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(tripHandler);
		em.addHandler(popHandler);
		
		new MatsimEventsReader(em).readFile(eventsFile);
		
		createPercentageBins();
		
		walk_pct = new double[percentages.size()];
		wait_pct = new double[percentages.size()];
		ivtt_pct = new double[percentages.size()];
		
		for (Id p : popHandler.getPop()){
			int numberOfTrips = tripHandler.getTripSize(p);
			for (int i = 0; i < numberOfTrips; i++){
				Trip trip = tripHandler.getTrip(p, i);
				if (trip.getStartTime() < start || trip.getStartTime() > end) continue; //skips trips outside the interested time period.
				if (trip.getAutoDriveTime() > 0 ) continue; //skip auto trips
				
				 double walk = trip.getWalkTime();
				 double wait = trip.getWaitTime();
				 double ivtt = trip.getIVTT();
				 double total = walk + wait + ivtt;
				 
				 if (total <= 0.0){
					 skippedTravellers++;
					 continue;
				 }
				 
				 addPct(walk / total, walk_pct);
				 addPct(wait / total, wait_pct);
				 addPct(ivtt / total, ivtt_pct);
				 
			}
		}
		
		System.out.println("Skipped " + skippedTravellers + " persons with 0 travel time.");
		
		printHistogram();
		
	}
	
	private static void printHistogram(){
		System.out.println("walk\twait\tivtt");
		for (int i = 0; i < ivtt_pct.length; i++){
			System.out.println(walk_pct[i] + "\t" + wait_pct[i] + "\t" + ivtt_pct[i]);
		}
	}
	
	private static void addPct(double pct, double[] bins){
		bins[percentages.ceilingEntry(pct).getValue()]++;
	}
	
	private static void createPercentageBins(){
		percentages = new TreeMap<Double, Integer>();
		for (int i = 0; i < 10; i++) percentages.put((i+1.0) / 10.0, i);
		
	}
	
}
