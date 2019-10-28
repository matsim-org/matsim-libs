package playground.vsp.andreas.utils.ana;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;


public class Line2StopRelationAnalyzer implements TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	
	private final static Logger log = Logger.getLogger(Line2StopRelationAnalyzer.class);
	
	private HashMap<Id, HashMap<Id, HashMap<Id, HashMap<String, Integer>>>> line2StartStop2EndStop2TripsMap = new HashMap<Id, HashMap<Id,HashMap<Id,HashMap<String,Integer>>>>();
	
	private HashMap<Id, Id> vehId2lineIdMap = new HashMap<Id, Id>();
	private HashMap<Id, Id> vehId2stopIdMap = new HashMap<Id, Id>();	
	private HashMap<Id, Id> personId2startStopId = new HashMap<Id, Id>();
	
	private String pIdentifier = "para_";
	
	
	

	public Line2StopRelationAnalyzer(String pIdentifier) {
		this.pIdentifier = pIdentifier;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehId2lineIdMap.put(event.getVehicleId(), event.getTransitLineId());		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehId2stopIdMap.put(event.getVehicleId(), event.getFacilityId());		
	}

	@Override
	public void reset(int iteration) {
		this.line2StartStop2EndStop2TripsMap = new HashMap<Id, HashMap<Id,HashMap<Id,HashMap<String,Integer>>>>();
		this.vehId2lineIdMap = new HashMap<Id, Id>();
		this.vehId2stopIdMap = new HashMap<Id, Id>();	
		this.personId2startStopId = new HashMap<Id, Id>();
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(!event.getPersonId().toString().contains(this.pIdentifier) && !event.getPersonId().toString().contains("pt_")){
			Id lineId = this.vehId2lineIdMap.get(event.getVehicleId());		
			if (this.line2StartStop2EndStop2TripsMap.get(lineId) == null) {
				this.line2StartStop2EndStop2TripsMap.put(lineId, new HashMap<Id, HashMap<Id,HashMap<String,Integer>>>());
			}

			Id startStop = this.personId2startStopId.get(event.getPersonId());
			if (this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop) == null) {
				this.line2StartStop2EndStop2TripsMap.get(lineId).put(startStop, new HashMap<Id, HashMap<String,Integer>>());
			}

			Id endStop = this.vehId2stopIdMap.get(event.getVehicleId());
			if (this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop) == null) {
				this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).put(endStop, new HashMap<String, Integer>());
			}
			
			String personId = event.getPersonId().toString().substring(0, 2);
			if (this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop).get(personId) == null) {
				this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop).put(personId, new Integer(0));
			}

			int oldCountsValue = this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop).get(personId);
			this.line2StartStop2EndStop2TripsMap.get(lineId).get(startStop).get(endStop).put(personId, new Integer(oldCountsValue + 1));
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(!event.getPersonId().toString().contains(this.pIdentifier) && !event.getPersonId().toString().contains("pt_")){
			this.personId2startStopId.put(event.getPersonId(), this.vehId2stopIdMap.get(event.getVehicleId()));
		}
	}

	public static void main(String[] args) {
		Line2StopRelationAnalyzer ana = new Line2StopRelationAnalyzer("para_");
		
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(ana);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(manager);
		reader.readFile("e:/_runs-svn/run1650/all2all_5min/ITERS/it.10000/all2all_5min.10000.events.xml.gz");
		ana.writeToFile("e:/_runs-svn/run1650/all2all_5min/ITERS/it.10000/all2all_5min.10000.events.ana.txt");
		
		
	}

	private void writeToFile(String fileName) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
			writer.write("# lineId\tstartStopId\tendStopId\ttrips"); writer.newLine();
			
			for (Entry<Id, HashMap<Id, HashMap<Id, HashMap<String, Integer>>>> lineEntry : this.line2StartStop2EndStop2TripsMap.entrySet()) {
				Id lineId = lineEntry.getKey();
				for (Entry<Id, HashMap<Id, HashMap<String, Integer>>> startStopEntry : lineEntry.getValue().entrySet()) {
					Id startStopId = startStopEntry.getKey();
					for (Entry<Id, HashMap<String, Integer>> endStopEntry : startStopEntry.getValue().entrySet()) {
						Id endStopId = endStopEntry.getKey();
						
						StringBuffer strB = new StringBuffer();						
						for (Entry<String, Integer> personIdEntry : endStopEntry.getValue().entrySet()) {
							strB.append(personIdEntry.getKey() + " " + personIdEntry.getValue().intValue() + "; ");
						}

						writer.write(lineId + "\t" + startStopId + "\t" + endStopId + "\t" + strB); writer.newLine();
					}
				}
			}
			writer.flush();
			writer.close();			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
