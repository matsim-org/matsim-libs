package playground.andreas.bln.ana.events2timespacechart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

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
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

class VehEventFilter implements VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler{
	
	private final static Logger log = Logger.getLogger(VehEventFilter.class);
	private HashMap<Id, BufferedWriter> writerMap = new HashMap<Id, BufferedWriter>();
	private HashMap<Id, Integer> enterMap = new HashMap<Id, Integer>();
	private HashMap<Id, Integer> leaveMap = new HashMap<Id, Integer>();
	
	HashMap<Id,Double> stopIdDistanceMap;
	
	private final String outputDir;
	
	public static void main(String[] args) {
		VehEventFilter vehEventFilter = new VehEventFilter("E:/_out/veh/");
		vehEventFilter.addVehToEvaluate(Id.create("veh_14", Vehicle.class));
		vehEventFilter.addVehToEvaluate(Id.create("veh_15", Vehicle.class));
		vehEventFilter.addVehToEvaluate(Id.create("veh_17", Vehicle.class));
		vehEventFilter.readEvents("E:/_out/veh/0.events.xml.gz");
	}
	
	public VehEventFilter(String outputDir){
		this.outputDir = outputDir;
		new File(this.outputDir).mkdir();
	}	

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(this.writerMap.containsKey(event.getVehicleId())){
			this.leaveMap.put(event.getVehicleId(), Integer.valueOf(this.leaveMap.get(event.getVehicleId()).intValue() + 1));
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(this.writerMap.containsKey(event.getVehicleId())){
			this.enterMap.put(event.getVehicleId(), Integer.valueOf(this.enterMap.get(event.getVehicleId()).intValue() + 1));		
		}
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		
		if(this.writerMap.containsKey(event.getVehicleId())){
			
			this.enterMap.put(event.getVehicleId(), new Integer(0));
			this.leaveMap.put(event.getVehicleId(), new Integer(0));

			BufferedWriter writer = this.writerMap.get(event.getVehicleId());

			StringBuffer buffer = new StringBuffer();
			buffer.append("Arriving, ");
			buffer.append(event.getTime()); buffer.append(", ");
			buffer.append(Time.writeTime(event.getTime())); buffer.append(", ");
			buffer.append(event.getFacilityId()); buffer.append(", ");
			buffer.append(event.getDelay()); buffer.append(", ");

			try {
				writer.write(buffer.toString());
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		
		if(this.writerMap.containsKey(event.getVehicleId())){			
			BufferedWriter writer = this.writerMap.get(event.getVehicleId());

			StringBuffer buffer = new StringBuffer();
			buffer.append("Leaving, ");
			buffer.append(event.getTime()); buffer.append(", ");
			buffer.append(Time.writeTime(event.getTime())); buffer.append(", ");
			buffer.append(event.getFacilityId()); buffer.append(", ");
			buffer.append(event.getDelay()); buffer.append(", ");
			buffer.append(this.leaveMap.get(event.getVehicleId()) + ", ");
			buffer.append(this.enterMap.get(event.getVehicleId()));

			try {
				writer.write(buffer.toString());
				writer.newLine();
				writer.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			this.enterMap.put(event.getVehicleId(), new Integer(0));
			this.leaveMap.put(event.getVehicleId(), new Integer(0));
		}
		
	}
	
	@Override
	public void reset(int iteration) {
		log.error("Should not happen, since scenario runs one iteration only.");			
	}
	
	public void readEvents(String filename){
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(filename);
	}

	public void addVehToEvaluate(Id veh) {
		try {
			this.writerMap.put(veh, new BufferedWriter(new FileWriter(new File(this.outputDir + veh.toString() + "_events.txt"))));
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
}