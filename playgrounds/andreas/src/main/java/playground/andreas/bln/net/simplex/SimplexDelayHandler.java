package playground.andreas.bln.net.simplex;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.xml.sax.SAXException;

public class SimplexDelayHandler {
	
	private final static Logger log = Logger.getLogger(SimplexDelayHandler.class);
	private LinkedList<DelayHandler> handlerList;
	
	ArrayList<Double> timeSteps = new ArrayList<Double>();

	
	public void readEvents(String filename){
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		Collections.sort(this.timeSteps);
		
		this.handlerList = new LinkedList<DelayHandler>();
		
		for (int i = 0; i < this.timeSteps.size(); i++) {
			
			if( i == 0){
				DelayHandler handler = new DelayHandler(0.0, this.timeSteps.get(i).doubleValue());
				events.addHandler(handler);
				this.handlerList.add(handler);
			} else {
				DelayHandler handler = new DelayHandler(this.timeSteps.get(i-1).doubleValue(), this.timeSteps.get(i).doubleValue());
				events.addHandler(handler);
				this.handlerList.add(handler);
			}
			
		}		
		
		DelayHandler handler = new DelayHandler(this.timeSteps.get(this.timeSteps.size()-1).doubleValue(), Double.MAX_VALUE);
		events.addHandler(handler);
		this.handlerList.add(handler);
		
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
	}
	
	public LinkedList<DelayHandler> getHandler(){
		return this.handlerList;
	}
	
	public void addStartTime(double time){
		this.timeSteps.add(Double.valueOf(time));
	}

	static class DelayHandler implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler{
		
		private int positiveDepartureCounter = 0;
		private int negativeArrivalCounter = 0;
		
		SimplexDelayCountBox posDepartureDelay = new SimplexDelayCountBox();
		SimplexDelayCountBox negArrivalDelay = new SimplexDelayCountBox();
				
		private double startTime;
		private double endTime;
		
		private final static Logger dhLog = Logger.getLogger(DelayHandler.class);
		
		public DelayHandler(double hvzStartTime, double hvzEndTime){
			this.startTime = hvzStartTime;
			this.endTime = hvzEndTime;
		}
		
		@Override
		public void reset(int iteration) {
			dhLog.warn("Should not happen, since scenario runs one iteration only.");			
		}
		
		@Override
		public void handleEvent(VehicleArrivesAtFacilityEvent event) {
			
			if (event.getTime() >= this.startTime && event.getTime() < this.endTime){
				// It is for em
				this.negArrivalDelay.addEntry(event.getDelay());
				this.negativeArrivalCounter++;	
			} else{
				// Not for me				
			}				
					
		}

		@Override
		public void handleEvent(VehicleDepartsAtFacilityEvent event) {
			
			if (event.getTime() >= this.startTime && event.getTime() < this.endTime){
				// It is for me
				this.posDepartureDelay.addEntry(event.getDelay());
				this.positiveDepartureCounter++;
			} else{
				// Not for me				
			}				
			
		}		
				
	}
}
