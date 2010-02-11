package playground.andreas.bln.ana;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.xml.sax.SAXException;

public class DelayEval {
	
	private final static Logger log = Logger.getLogger(DelayEval.class);

	
	public void readEvents(String filename){
		EventsManagerImpl events = new EventsManagerImpl();
		DelayHandler handler = new DelayHandler();
		handler.addTermStop("781015.1"); //344 nord
		//handler.addTermStop("792200.2"); //344 sued aussetzer
		handler.addTermStop("792200.3"); // m44 nord
		
		handler.addTermStop("792040.2"); //344 sued
		
		handler.addTermStop("812013.2"); // m44 einsetzer gross
		//handler.addTermStop("801030.2"); // m44 einsetzer klein
		handler.addTermStop("812020.2"); //m44 sued
		events.addHandler(handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		try {
			reader.parse(filename);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		handler.printStats();
	}
	
	public static void main(String[] args) {
		
		DelayEval delayEval = new DelayEval();
		delayEval.readEvents("E:/_out/0.events.xml.gz");

		


	}
	
	static class DelayHandler implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
		
		private int stopCounter = 0;
		private int termCounter = 0;
		
		HashMap<Integer, DelayCountBox> delayMap = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTerm = new HashMap<Integer, DelayCountBox>();
		LinkedList<IdImpl> termList = new LinkedList<IdImpl>();
		
		private final static Logger dhLog = Logger.getLogger(DelayHandler.class);
		
		public DelayHandler(){
			for (int i = 0; i < 30; i++) {
				this.delayMap.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTerm.put(new Integer(i), new DelayCountBox());
			}
		}
		
		public void addTermStop(String stop){
			this.termList.add(new IdImpl(stop));
		}

		@Override
		public void reset(int iteration) {
			dhLog.warn("Should not happen, since scenario runs one iteration only.");			
		}
		
		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
//			dhLog.info("Found event with " + event.getDelay() + "s delay.");
			
			if(this.termList.contains(event.getFacilityId())){
			
				DelayCountBox delBox = this.delayMapTerm.get(Integer.valueOf((int) event.getTime()/3600));
				delBox.addEntry(event.getDelay());
			
				this.delayMapTerm.put(Integer.valueOf((int) event.getTime()/3600), delBox); 
				this.termCounter++;
			
			}
			

			
//			dhLog.info("Found event with " + event.getDelay() + "s delay.");
		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
//			dhLog.info("Found event with " + event.getDelay() + "s delay.");
			DelayCountBox delBox = this.delayMap.get(Integer.valueOf((int) event.getTime()/3600));
			delBox.addEntry(event.getDelay());
			
			this.delayMap.put(Integer.valueOf((int) event.getTime()/3600), delBox); 
			this.stopCounter++;
			
//			dhLog.info("Found event with " + event.getDelay() + "s delay.");
		}
		
		void printStats(){
			dhLog.info("Evaluated " + this.stopCounter + " stops"); 
			System.out.println("Hour: , Stops: , AccumulatedDelay: s, Average: ");
			
			for (int i = 0; i < this.delayMap.size(); i++) {
				System.out.println(i + "-" + (i+1) + ", " + this.delayMap.get(Integer.valueOf(i)).getNumberOfEntries() + ", " + this.delayMap.get(Integer.valueOf(i)).getAccumulatedDelay() + ", " + this.delayMap.get(Integer.valueOf(i)).getAverageDelay());
			}
//			for (Entry<Integer, DelayCountBox> entry : this.delayMap.entrySet()) {
//				System.out.println(entry.getKey() + "-" + (entry.getKey().intValue()+1) + ", " + entry.getValue().getNumberOfEntries() + ", " + entry.getValue().getAccumulatedDelay() + ", " + entry.getValue().getAverageDelay());
//			}
			
			dhLog.info("Evaluated " + this.termCounter + " terms"); 
			System.out.println("Hour: , Stops: , AccumulatedDelay: s, Average: ");
			
			for (int i = 0; i < this.delayMapTerm.size(); i++) {
				System.out.println(i + "-" + (i+1) + ", " + this.delayMapTerm.get(Integer.valueOf(i)).getNumberOfEntries() + ", " + this.delayMapTerm.get(Integer.valueOf(i)).getAccumulatedDelay() + ", " + this.delayMapTerm.get(Integer.valueOf(i)).getAverageDelay());
			}
//			for (Entry<Integer, DelayCountBox> entry : this.delayMapTerm.entrySet()) {
//				System.out.println(entry.getKey() + "-" + (entry.getKey().intValue()+1) + ", " + entry.getValue().getNumberOfEntries() + ", " + entry.getValue().getAccumulatedDelay() + ", " + entry.getValue().getAverageDelay());
//			}
		}
		
		
		
	}


}

class DelayCountBox{
	
	private int numberOfEntries = 0;
	private double accumulatedDelay = 0.0;
	
	public void addEntry(double delay){
		this.numberOfEntries++;
		this.accumulatedDelay += delay;
	}
	
	public int getNumberOfEntries(){
		return this.numberOfEntries;
	}
	
	public double getAccumulatedDelay(){
		return this.accumulatedDelay;
	}
	
	public double getAverageDelay(){
		return this.accumulatedDelay / this.numberOfEntries;
	}
	
	
	
}
