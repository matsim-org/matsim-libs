package playground.andreas.bln.ana.events2counts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import playground.andreas.bln.ana.events2timespacechart.ReadStopIDNamesMap;

public class Events2PTCounts implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	
	private final static Logger log = Logger.getLogger(Events2PTCounts.class);

	String outDir;
	String countsOutFile;
	String eventsInFile;
	HashMap<Id<TransitStopFacility>, String>  stopID2NameMap;
	Map<Id<Vehicle>, Id<TransitLine>> vehID2LineMap;
	Map<String, Id<TransitLine>> string2LineIDMap;
	
	TransitSchedule transitSchedule;
	
	Map<Id<TransitLine>, Map<Id<TransitStopFacility>, StopCountBox>> line2StopCountMap;
	public Map<Id<TransitLine>, Map<Id<TransitStopFacility>, StopCountBox>> getLine2StopCountMap() {
		return this.line2StopCountMap;
	}

	Map<Id<Vehicle>, Id<TransitStopFacility>> veh2CurrentStopMap = new HashMap<>();
	Map<Id<TransitLine>, List<List<Id>>> line2MainLinesMap;

	public Events2PTCounts(String outDir, String eventsInFile, String stopIDMapFile, String networkFile, String transitScheduleFile) throws IOException {
		this(outDir, eventsInFile, ReadStopIDNamesMap.readFile(stopIDMapFile), ReadTransitSchedule.readTransitSchedule(networkFile, transitScheduleFile));
	}
	
	public Events2PTCounts(String outDir, String eventsInFile, HashMap<Id<TransitStopFacility>, String> stopIDMap, TransitSchedule transitSchedule){
		this.eventsInFile = eventsInFile;
		this.stopID2NameMap = stopIDMap;
		this.transitSchedule = transitSchedule;
		this.outDir = outDir;		
	}

	public void run() {
		this.line2StopCountMap = new HashMap<>();
		this.vehID2LineMap = CreateVehID2LineMap.createVehID2LineMap(this.transitSchedule);
		this.string2LineIDMap = CreateString2LineIDMap.createString2LineIDMap(this.transitSchedule);
		this.line2MainLinesMap = TransitSchedule2MainLine.createMainLinesFromTransitSchedule(this.transitSchedule);
		readEvents(this.eventsInFile);
		
		check();
		
		dump();

		log.info("Finished");
		
	}
	
	protected void check() {
		for (Entry<Id<TransitLine>, Map<Id<TransitStopFacility>, StopCountBox>> line2StopEntry : this.line2StopCountMap.entrySet()) {
			
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
		
			for (Id<TransitLine> lineId : this.line2StopCountMap.keySet()) {

				if(this.transitSchedule.getTransitLines().get(lineId) != null){

					for (TransitRoute transitRoute : this.transitSchedule.getTransitLines().get(lineId).getRoutes().values()) {

						// prepare occupancy

						TreeMap<Id<TransitStopFacility>, ArrayList<Integer>> stop2OccuMap = new TreeMap<>();

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

							writer.write(this.stopID2NameMap.get(Id.create(routeStop.getStopFacility().getId().toString().split("\\.")[0], TransitStopFacility.class)) + "; ");

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
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(filename);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		Id<Vehicle> vehId = event.getVehicleId();
		Id<TransitStopFacility> stopId = event.getFacilityId();
		Id<TransitLine> lineId = this.vehID2LineMap.get(vehId);
		
		if(lineId == null){
			log.error("Veh id == null");
		}
		
		if(this.line2StopCountMap.get(lineId) == null){
			this.line2StopCountMap.put(lineId, new HashMap<Id<TransitStopFacility>, StopCountBox>());
		}
		
		if(this.line2StopCountMap.get(lineId).get(stopId) == null){
			this.line2StopCountMap.get(lineId).put(stopId, new StopCountBox(stopId, this.stopID2NameMap.get(Id.create(stopId.toString().split("\\.")[0], TransitStopFacility.class))));
		}
		
		this.veh2CurrentStopMap.put(vehId, stopId);		
	}
	
	private Id<TransitLine> getLineIDFromRoute(Id<TransitRoute> routeID){
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
