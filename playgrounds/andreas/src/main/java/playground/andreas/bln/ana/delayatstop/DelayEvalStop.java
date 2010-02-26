package playground.andreas.bln.ana.delayatstop;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.xml.sax.SAXException;

public class DelayEvalStop {
	
	private final static Logger log = Logger.getLogger(DelayEvalStop.class);
	private int treshold = 0;

	
	public void readEvents(String filename){
		EventsManagerImpl events = new EventsManagerImpl();
		DelayHandler handler = new DelayHandler(this.treshold);
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
		
		DelayEvalStop delayEval = new DelayEvalStop();
		delayEval.setTreshold_s(60);
		delayEval.readEvents("E:/_out/m3_traffic_jam/ITERS/it.0/0.events.xml.gz");

	}
	
	private void setTreshold_s(int treshold) {
		this.treshold = treshold;
	}

	static class DelayHandler implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
		
		private int stopCounter = 0;
		private int termCounter = 0;
		
		HashMap<Integer, DelayCountBox> delayMap = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTerm = new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTreshold =  new HashMap<Integer, DelayCountBox>();
		HashMap<Integer, DelayCountBox> delayMapTermTreshold  =  new HashMap<Integer, DelayCountBox>();
		LinkedList<IdImpl> termList = new LinkedList<IdImpl>();
		
		private int treshold;


		
		private final static Logger dhLog = Logger.getLogger(DelayHandler.class);
		
		public DelayHandler(int treshold){
			for (int i = 0; i < 30; i++) {
				this.delayMap.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTerm.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTreshold.put(new Integer(i), new DelayCountBox());
			}
			
			for (int i = 0; i < 30; i++) {
				this.delayMapTermTreshold.put(new Integer(i), new DelayCountBox());
			}
			
			this.treshold = treshold;
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
			
			if(this.termList.contains(event.getFacilityId())){
			
				DelayCountBox delBox = this.delayMapTerm.get(Integer.valueOf((int) event.getTime()/3600));
				DelayCountBox delBoxTreshold = this.delayMapTermTreshold.get(Integer.valueOf((int) event.getTime()/3600));
				if(event.getDelay() < -1 * this.treshold){
					delBoxTreshold.addEntry(event.getDelay());
				} else {
					delBoxTreshold.addEntry(0.0);
				}
				
				delBox.addEntry(Math.min(0.0, event.getDelay()));
			
				this.delayMapTerm.put(Integer.valueOf((int) event.getTime()/3600), delBox);
				this.delayMapTermTreshold.put(Integer.valueOf((int) event.getTime()/3600), delBoxTreshold); 
				this.termCounter++;
			
			}			

		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
			DelayCountBox delBox = this.delayMap.get(Integer.valueOf((int) event.getTime()/3600));
			DelayCountBox delBoxTreshold = this.delayMapTreshold.get(Integer.valueOf((int) event.getTime()/3600));
			if(event.getDelay() > this.treshold){
				delBoxTreshold.addEntry(event.getDelay());
			} else {
				delBoxTreshold.addEntry(0.0);
			}
			
			delBox.addEntry(Math.max(0.0, event.getDelay()));
			
			this.delayMap.put(Integer.valueOf((int) event.getTime()/3600), delBox);
			this.delayMapTreshold.put(Integer.valueOf((int) event.getTime()/3600), delBoxTreshold);
			this.stopCounter++;

		}
		
		void printStats(){
			
			if(this.delayMap.size() == this.delayMapTerm.size()){
				
				System.out.println("Stunde, Anzahl der Abfahrten, Verspätung je Abfahrt, Verspätung je Abfahrt > " + this.treshold + " s,  , Anzahl der Ankünfte, Verfrühung je Ankunft, Verfrühung je Ankunft < " + this.treshold + " s");
				for (int i = 0; i < this.delayMap.size(); i++) {
					StringBuffer string = new StringBuffer();
					string.append(i + "-" + (i+1) + ", ");
					string.append(this.delayMap.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMap.get(Integer.valueOf(i)).getAverageDelay() + ", ");
//					string.append(this.delayMapTreshold.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMapTreshold.get(Integer.valueOf(i)).getAverageDelay() + ", ");
					string.append(", ");
					string.append(this.delayMapTerm.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMapTerm.get(Integer.valueOf(i)).getAverageDelay() + ", ");
//					string.append(this.delayMapTermTreshold.get(Integer.valueOf(i)).getNumberOfEntries() + ", ");
					string.append(this.delayMapTermTreshold.get(Integer.valueOf(i)).getAverageDelay());
					System.out.println(string.toString());
				
				}
				
			}
		}		
	}


}
