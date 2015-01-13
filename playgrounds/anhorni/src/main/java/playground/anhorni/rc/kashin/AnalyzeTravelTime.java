package playground.anhorni.rc.kashin;

import java.util.Arrays;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

import playground.anhorni.rc.WithindayListener;

public class AnalyzeTravelTime {
	
	private static final Logger log = Logger.getLogger(WithindayListener.class);
	
	public static void main(String [] args) {
		AnalyzeTravelTime analyzer = new AnalyzeTravelTime();
		analyzer.run(args);
	}
	
	public void run(String [] args) {
		Vector<Double> traveltimes = new Vector<Double>();
		for (String file : Arrays.copyOfRange(args, 1, args.length-1)) {
			log.info("computing file: " + file);
			double traveltime = this.getOverallTravelTime(file);
			traveltimes.add(traveltime);
		}
		this.plotGraph(traveltimes, args[0]);
	}
	
	
	private double getOverallTravelTime(String eventsfile) {
		EventsManager events = EventsUtils.createEventsManager();
		
		TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator();
		events.addHandler(travelTimeCalculator);
		new MatsimEventsReader(events).readFile(eventsfile);
						
		double traveltime = travelTimeCalculator.getOverallTravelTime();
		return traveltime;
	}
	
	
	private void plotGraph(Vector<Double> traveltimes, String outfile) {
		//TODO: see counts package
	}

}
