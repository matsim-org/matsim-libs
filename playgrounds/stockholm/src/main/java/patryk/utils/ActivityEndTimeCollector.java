package patryk.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;


public class ActivityEndTimeCollector {
	
	private final static String EVENTSFILE = "toll_sthlm/output/ITERS/it.40/40.events.xml.gz";
	private final static String ENDTIMESFILE = "activity_ends.txt"; 
	
	public static void main(String[] args) {
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		ActivityEnd handler = new ActivityEnd();
		eventsManager.addHandler(handler);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(EVENTSFILE);
		
		 ArrayList<Double> workStartTimes = handler.getWorkStartTimes();
		 ArrayList<Double> workEndTimes = handler.getWorkEndTimes();
		 ArrayList<Double> workDurTimes = handler.getDurTimes();
		 
//		System.out.println("home" + homeEndTimes.size() + "work" + workEndTimes.size());
		 
		 try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ENDTIMESFILE, false)))) {
		   for (int i=0; i<workStartTimes.size()-1; i++) {
			   out.println(workStartTimes.get(i) + "," + workEndTimes.get(i));
		   }
		}catch (IOException e) {
		    //exception handling left as an exercise for the reader
		}			

	}

}
