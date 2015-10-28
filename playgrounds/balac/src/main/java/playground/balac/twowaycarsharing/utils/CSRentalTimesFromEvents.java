package playground.balac.twowaycarsharing.utils;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.balac.allcsmodestest.controler.listener.CSEventsHandler;


public class CSRentalTimesFromEvents {
	
	EventsManager events = (EventsManager) EventsUtils.createEventsManager();
      
    
    EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
    
    public void run(String s, String s1){
    	MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader networkReader = new MatsimNetworkReader(sc);
		networkReader.readFile(s1);
    	CSEventsHandler rentalTimes = new CSEventsHandler(sc.getNetwork());
    	
    	events.addHandler(rentalTimes);
    	reader.parse(s);
    	/*int[] rentalTime = rentalTimes.rentalTimes;
    	System.out.println(rentalTimes.number1());
    	System.out.println(rentalTimes.number());
    	for (int i = 0; i < rentalTime.length; i++) { 
			System.out.println((double)rentalTime[i]/(double)rentalTimes.number() * 100.0);
			
			*/
			
    	//}
    }
    
	private static class RentalTimes implements ActivityStartEventHandler{

		int[] rentalTimes = new int[24];
		HashMap<Id, Double> startTimes = new HashMap<Id, Double>();
		int count = 0;
		int count1 = 0;
		@Override
		public void reset(int iteration) {
			// TODO Auto-generated method stub
			
			
		}

		@Override
		public void handleEvent(ActivityStartEvent event) {
			// TODO Auto-generated method stub
			if (event.getActType().equals("cs_interaction")) {
				
				if (startTimes.containsKey(event.getPersonId())) {
					
					rentalTimes[(int)((event.getTime() - startTimes.get(event.getPersonId())) / 3600)]++;
					count++;
					if (event.getTime() - startTimes.get(event.getPersonId()) > 0.0 && event.getTime() - startTimes.get(event.getPersonId()) < 1800) {
						count1++;
					}
					startTimes.remove(event.getPersonId());
					 
					
				}
				else {
					
					startTimes.put(event.getPersonId(), event.getTime());				}
			}
		}
		
		public int[] rentalTimes() {
			
			return rentalTimes;
		}
		public int number() {
			return count;
		}
		public int number1() {
			return count1;
		}
		
		
	}
    public static void main(String[] args) {
    	CSRentalTimesFromEvents cp = new CSRentalTimesFromEvents();
		
		String eventsFilePath = args[0]; 
		
		
		cp.run(eventsFilePath, args[1]);

	}

}
