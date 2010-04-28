package playground.andreas.bln.ana.events2counts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.network.NetworkLayer;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.andreas.bln.ana.events2timespacechart.ReadStopIDNamesMap;

public class Events2Counts implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private final static Logger log = Logger.getLogger(Events2Counts.class);

	String countsOutFile;
	String eventsInFile;
	HashMap<Id, String>  stopIDMap;
	Map<Id, Id> vehID2LineMap;
	
	NetworkLayer network;
	TransitSchedule transitSchedule;
	
	Map<Id, Map<Id, StopCountBox>> line2StopCountMap = new HashMap<Id, Map<Id,StopCountBox>>();
	Map<Id, Id> veh2CurrentStopMap = new HashMap<Id, Id>();
	Map<Id, List<List<Id>>> line2MainLinesMap;

	public Events2Counts(String countsOutFile, String eventsInFile, String stopIDMapFile, String networkFile, String transitScheduleFile) throws IOException {
		this(countsOutFile, eventsInFile, ReadStopIDNamesMap.readFile(stopIDMapFile), ReadTransitSchedule.readTransitSchedule(networkFile, transitScheduleFile));
	}
	
	public Events2Counts(String countsOutFile , String eventsInFile, HashMap<Id, String> stopIDMap, TransitSchedule transitSchedule){
		this.countsOutFile = countsOutFile;
		this.eventsInFile = eventsInFile;
		this.stopIDMap = stopIDMap;
		this.transitSchedule = transitSchedule;
	}

	private void run() {
		this.vehID2LineMap = CreateVehID2LineMap.createVehID2LineMap(this.transitSchedule);
		this.line2MainLinesMap = TransitSchedule2MainLine.createMainLinesFromTransitSchedule(this.transitSchedule);
		readEvents(this.eventsInFile);
		
		try {
			BufferedWriter writer;
		
			for (Id lineId : this.line2StopCountMap.keySet()) {

				boolean direction = true;
				
				for (List<Id> stopsOfRouteList : this.line2MainLinesMap.get(lineId)) {
					writer = new BufferedWriter(new FileWriter(new File("E:/_out/graphs/" + lineId.toString().trim() + "_" + direction)));
					direction = !direction;
					
					writer.write(new StopCountBox(null, null).getHeader()); writer.newLine();
					
					for (Id stopId : stopsOfRouteList) {
						writer.write(this.stopIDMap.get(new IdImpl(stopId.toString().split("\\.")[0])) + "; ");
						StopCountBox stopCountBox = this.line2StopCountMap.get(lineId).get(stopId);
//						int occu = 0;
						for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
//							occu = stopCountBox.accessCount[i] - stopCountBox.egressCount[i];
							writer.write(stopCountBox.accessCount[i] + "; " + stopCountBox.egressCount[i] + "; ");// + occu + "\t");							
						}
						writer.newLine();
						writer.flush();
					}
				}
				
				


			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		log.info("Finished");
		
	}
	
	private void readEvents(String filename){
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		
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

	public static void main(String[] args) {
		String inDir = "e:/_out/countsTest/";
		try {
			new Events2Counts(inDir + "countsFile.txt", inDir + "0.events.xml.gz", inDir + "stopareamap.txt", inDir + "network.xml", inDir + "transitSchedule.xml").run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private class StopCountBox{
		Id stopId;
		String realName;
		int[] accessCount = new int[35];
		int[] egressCount = new int[35];	 
		
		public StopCountBox(Id stopId, String realName){
			this.stopId = stopId;
			this.realName = realName;
		}
		
		public String getHeader(){
			StringBuffer string = new StringBuffer();
			string.append("Id; ");
			for (int i = 0; i < this.accessCount.length; i++) {
				string.append("Einstieg " + i + " - " + (i+1) + " Uhr; Ausstieg " + i + " - " + (i+1) + " Uhr; ");//Besetzung_" + i + "-" + (i+1) + "\t");
			}
			return string.toString();
		}
	}



	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id vehId = event.getVehicleId();
		Id stopId = event.getFacilityId();
		Id lineId = this.vehID2LineMap.get(vehId);
		
		if(this.line2StopCountMap.get(lineId) == null){
			this.line2StopCountMap.put(lineId, new HashMap<Id, StopCountBox>());
		}
		
		if(this.line2StopCountMap.get(lineId).get(stopId) == null){
			this.line2StopCountMap.get(lineId).put(stopId, new StopCountBox(stopId, this.stopIDMap.get(new IdImpl(stopId.toString().split("\\.")[0]))));
		}
		
		this.veh2CurrentStopMap.put(vehId, stopId);		
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		StopCountBox stopCountBox = this.line2StopCountMap.get(this.vehID2LineMap.get(event.getVehicleId())).get(this.veh2CurrentStopMap.get(event.getVehicleId()));
		stopCountBox.accessCount[(int) event.getTime()/3600]++;		
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		StopCountBox stopCountBox = this.line2StopCountMap.get(this.vehID2LineMap.get(event.getVehicleId())).get(this.veh2CurrentStopMap.get(event.getVehicleId()));
		stopCountBox.egressCount[(int) event.getTime()/3600]++;			
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.veh2CurrentStopMap.remove(event.getVehicleId());		
	}
	
}
