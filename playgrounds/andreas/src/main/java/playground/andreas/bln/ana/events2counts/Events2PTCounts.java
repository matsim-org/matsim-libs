package playground.andreas.bln.ana.events2counts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

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
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.xml.sax.SAXException;

import playground.andreas.bln.ana.events2timespacechart.ReadStopIDNamesMap;

public class Events2PTCounts implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private final static Logger log = Logger.getLogger(Events2PTCounts.class);

	String outDir;
	String countsOutFile;
	String eventsInFile;
	HashMap<Id, String>  stopID2NameMap;
	Map<Id, Id> vehID2LineMap;
	Map<String, Id> string2LineIDMap;
	
	TransitSchedule transitSchedule;
	
	Map<Id, Map<Id, StopCountBox>> line2StopCountMap;
	public Map<Id, Map<Id, StopCountBox>> getLine2StopCountMap() {
		return this.line2StopCountMap;
	}

	Map<Id, Id> veh2CurrentStopMap = new HashMap<Id, Id>();
	Map<Id, List<List<Id>>> line2MainLinesMap;

	public Events2PTCounts(String outDir, String eventsInFile, String stopIDMapFile, String networkFile, String transitScheduleFile) throws IOException {
		this(outDir, eventsInFile, ReadStopIDNamesMap.readFile(stopIDMapFile), ReadTransitSchedule.readTransitSchedule(networkFile, transitScheduleFile));
	}
	
	public Events2PTCounts(String outDir, String eventsInFile, HashMap<Id, String> stopIDMap, TransitSchedule transitSchedule){
		this.eventsInFile = eventsInFile;
		this.stopID2NameMap = stopIDMap;
		this.transitSchedule = transitSchedule;
		this.outDir = outDir;		
	}

	public void run() {
		this.line2StopCountMap = new HashMap<Id, Map<Id,StopCountBox>>();
		this.vehID2LineMap = CreateVehID2LineMap.createVehID2LineMap(this.transitSchedule);
		this.string2LineIDMap = CreateString2LineIDMap.createString2LineIDMap(this.transitSchedule);
		this.line2MainLinesMap = TransitSchedule2MainLine.createMainLinesFromTransitSchedule(this.transitSchedule);
		readEvents(this.eventsInFile);
		
		check();
		
		dump();

		log.info("Finished");
		
	}
	
	protected void check() {
		for (Entry<Id, Map<Id, StopCountBox>> line2StopEntry : this.line2StopCountMap.entrySet()) {
			
			int inOutBalance = 0;
			
			for (StopCountBox stopCountBox : line2StopEntry.getValue().values()) {
				inOutBalance += stopCountBox.getTotalAccessSum() - stopCountBox.getTotalEgressSum();
			}
			
			if (inOutBalance != 0){
				log.warn(line2StopEntry.getKey() + " please check " + inOutBalance);
			} else {
//				log.info(line2StopEntry.getKey() + " is balanced " + inOutBalance);
			}
			
		}
		
	}

	public void dump() {
		try {
			BufferedWriter writer;
		
			for (Id lineId : this.line2StopCountMap.keySet()) {

				if(this.transitSchedule.getTransitLines().get(lineId) != null){

					for (TransitRoute transitRoute : this.transitSchedule.getTransitLines().get(lineId).getRoutes().values()) {

						// prepare occupancy

						TreeMap<Id, ArrayList<Integer>> stop2OccuMap = new TreeMap<Id, ArrayList<Integer>>();

						for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
							for (TransitRouteStop routeStop : transitRoute.getStops()) {
								if(stop2OccuMap.get(routeStop.getStopFacility().getId()) == null){
									stop2OccuMap.put(routeStop.getStopFacility().getId(), new ArrayList<Integer>(new StopCountBox(null, null).accessCount.length));
								}
								StopCountBox stopCountBox = this.line2StopCountMap.get(lineId).get(routeStop.getStopFacility().getId());
								if(stopCountBox != null) {
									stop2OccuMap.get(routeStop.getStopFacility().getId()).add(i, new Integer(stopCountBox.accessCount[i] - stopCountBox.egressCount[i]));
								}
							}
						}

						for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
							int occu = 0;
							for (TransitRouteStop routeStop : transitRoute.getStops()) {
								occu = occu + stop2OccuMap.get(routeStop.getStopFacility().getId()).get(i).intValue();
								stop2OccuMap.get(routeStop.getStopFacility().getId()).set(i, new Integer(occu));							
							}
						}



						writer = new BufferedWriter(new FileWriter(new File(this.outDir + lineId.toString().trim() + "_" + transitRoute.getId().toString().trim() + ".txt")));
						writer.write("# RouteID: " + transitRoute.getId() + ", " + transitRoute.getStops().size() + " out of " + this.line2StopCountMap.get(lineId).size() + " stops used"); writer.newLine();
						writer.write(new StopCountBox(null, null).getHeader()); writer.newLine();
						
						int occupancy = 0;
						for (TransitRouteStop routeStop : transitRoute.getStops()) {							

							writer.write(this.stopID2NameMap.get(new IdImpl(routeStop.getStopFacility().getId().toString().split("\\.")[0])) + "; ");

							StopCountBox stopCountBox = this.line2StopCountMap.get(lineId).get(routeStop.getStopFacility().getId());
							occupancy += stopCountBox.getTotalAccessSum() - stopCountBox.getTotalEgressSum();
							writer.write(stopCountBox.getTotalAccessSum() + "; " + stopCountBox.getTotalEgressSum() + "; " + occupancy + "; ");

							for (int i = 0; i < new StopCountBox(null, null).accessCount.length; i++) {
								writer.write(stopCountBox.accessCount[i] + "; " + stopCountBox.egressCount[i] + "; " + stop2OccuMap.get(routeStop.getStopFacility().getId()).get(i) + "; ");
							}
							writer.newLine();
							writer.flush();
						}
					}
				} else {
					log.warn(lineId + " got removed?");
				}
			}
			
			new GnuFileWriter(this.outDir).write(this.transitSchedule);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id vehId = event.getVehicleId();
		Id stopId = event.getFacilityId();
		Id lineId = this.vehID2LineMap.get(vehId);
		
		if(lineId == null){
			log.error("Veh id == null");
		}
		
		if(this.line2StopCountMap.get(lineId) == null){
			this.line2StopCountMap.put(lineId, new HashMap<Id, StopCountBox>());
		}
		
		if(this.line2StopCountMap.get(lineId).get(stopId) == null){
			this.line2StopCountMap.get(lineId).put(stopId, new StopCountBox(stopId, this.stopID2NameMap.get(new IdImpl(stopId.toString().split("\\.")[0]))));
		}
		
		this.veh2CurrentStopMap.put(vehId, stopId);		
	}
	
	private Id getLineIDFromRoute(Id routeID){
		return this.string2LineIDMap.get(routeID.toString().split("\\.")[0]);
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
		if(this.veh2CurrentStopMap.remove(event.getVehicleId()) == null){
			log.error("Vehicle " + event.getVehicleId() + " departs from facility " + event.getFacilityId() + " but has not arrived before.");		
		}
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	public static void main(String[] args) {
		String outDir = "e:/_out/countsTest/";
		try {
			new Events2PTCounts(outDir, "750.events.xml.gz", "stopareamap.txt", "network.xml.gz", "transitSchedule.xml.gz").run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
