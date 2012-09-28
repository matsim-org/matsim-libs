package playground.toronto.analysis;

import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.charts.BarChart;
import org.matsim.core.utils.misc.Time;

import playground.toronto.analysis.handlers.AgentTripChainHandler;
import playground.toronto.analysis.handlers.SimplePopulationHandler;
import playground.toronto.analysis.tripchains.Trip;

public class TripComponentsDuration {

	private static TreeMap<Double,Integer> durations;
	private static String[] bin_names;
	private static double[] walk_dur;
	private static double[] wait_dur;
	private static double[] ivtt_dur;
		
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
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
		
		createDurationBins();
		walk_dur = new double[durations.size() + 1];
		wait_dur = new double[durations.size() + 1];
		ivtt_dur = new double[durations.size() + 1];
		
		for (Id p : popHandler.getPop()){
			int numberOfTrips = tripHandler.getTripSize(p);
			for (int i = 0; i < numberOfTrips; i++){
				Trip trip = tripHandler.getTrip(p, i);
				if (trip.getStartTime() < start || trip.getStartTime() > end) continue; //skips trips outside the interested time period.
				if (trip.getAutoDriveTime() > 0 ) continue; //skip auto trips
				
				addDur(trip.getIVTT(), ivtt_dur);
				addDur(trip.getWaitTime(), wait_dur);
				addDur(trip.getWalkTime(), walk_dur);
			}
		}
		
		printHistogram();
		
		BarChart chart = new BarChart("Histogram of Travel Components", "Duration Bin", "Number of Trips", bin_names);
		
		chart.addSeries("IVTT", ivtt_dur);
		chart.addSeries("Walk Times", walk_dur);
		chart.addSeries("Wait Times", wait_dur);
		
		chart.saveAsPng(outFileName, 800, 600);
		
	}
	
	private static void printHistogram(){
		System.out.println("Bin\twalk\twait\tivtt");
		for (int i = 0; i < ivtt_dur.length; i++){
			System.out.println(bin_names[i] + "\t" + walk_dur[i] + "\t" + wait_dur[i] + "\t" + ivtt_dur[i]);
		}
	}
	
	private static void addDur(double time, double[] array){
		Entry<Double, Integer> e = durations.ceilingEntry(time);
		int idx = (e == null) ? durations.size() : e.getValue();
		array[idx]++;
	}
	

	
	private static void createDurationBins(){
		durations = new TreeMap<Double, Integer>();
		bin_names = new String[21];
		
		durations.put(60.0,0); bin_names[0] = 1 + " min";
		durations.put(120.0,1); bin_names[1] = 2 + " min";
		durations.put(180.0,2); bin_names[2] = 3 + " min";
		durations.put(240.0,3); bin_names[3] = 4 + " min";
		durations.put(300.0,4); bin_names[4] = 5 + " min";
		durations.put(360.0,5); bin_names[5] = 6 + " min";
		durations.put(480.0,6); bin_names[6] = 8 + " min";
		durations.put(600.0,7); bin_names[7] = 10 + " min";
		durations.put(720.0,8); bin_names[8] = 12 + " min";
		durations.put(900.0,9); bin_names[9] = 15 + " min";
		durations.put(1200.0,10); bin_names[10] = 20 + " min";
		durations.put(1500.0,11); bin_names[11] = 25 + " min";
		durations.put(1800.0,12); bin_names[12] = 30 + " min";
		durations.put(2400.0,13); bin_names[13] = 40 + " min";
		durations.put(3000.0,14); bin_names[14] = 50 + " min";
		durations.put(3600.0,15); bin_names[15] = 60 + " min";
		durations.put(4500.0,16); bin_names[16] = 75 + " min";
		durations.put(5400.0,17); bin_names[17] = 90 + " min";
		durations.put(6300.0,18); bin_names[18] = 105 + " min";
		durations.put(7200.0,19); bin_names[19] = 120 + " min";
		bin_names[20] = "More";
	}
	
	
	
}
