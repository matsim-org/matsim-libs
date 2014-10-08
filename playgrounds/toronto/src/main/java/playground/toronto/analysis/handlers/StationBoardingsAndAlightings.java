package playground.toronto.analysis.handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

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
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Counts all agents boarding to or alighting from all transit facilities during a specified time period.
 * 
 * @author pkucirek
 *
 */
public class StationBoardingsAndAlightings implements
		VehicleArrivesAtFacilityEventHandler,
		VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler,
		PersonLeavesVehicleEventHandler {

	private final double startTime;
	private final double endTime;
	
	private HashMap<Id<Vehicle>, Id<TransitStopFacility>> vehicleStopCache;
	private HashMap<Id<TransitStopFacility>, Integer> facilityBoardings;
	private HashMap<Id<TransitStopFacility>, Integer> facilityAlightings;
	private HashSet<Id<TransitStopFacility>> facilities;
	
	public StationBoardingsAndAlightings(){
		this.startTime = 0;
		this.endTime = Double.MAX_VALUE;
		init();
	}
	
	public StationBoardingsAndAlightings(double start, double end){
		this.startTime = start;
		this.endTime = end;
		init();
	}
	
	private void init(){
		this.facilities = new HashSet<>();
		this.facilityAlightings = new HashMap<>();
		this.facilityBoardings = new HashMap<>();
		this.vehicleStopCache = new HashMap<>();
	}
	
	public HashSet<Id<TransitStopFacility>> getActiveStops(){
		return this.facilities;
	}
	
	public int getAlightingsAtStop(Id<TransitStopFacility> stopId){
		Integer i = this.facilityAlightings.get(stopId);
		return i == null ? 0 : i;
	}
	
	public int getBoardingsAtStop(Id<TransitStopFacility> stopId){
		Integer i = this.facilityBoardings.get(stopId);
		return i == null ? 0 : i;
	}
	
	@Override
	public void reset(int iteration) {
		this.vehicleStopCache.clear();
		this.facilities.clear();
		this.facilityAlightings.clear();
		this.facilityBoardings.clear();
	}
	
	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.vehicleStopCache.containsKey(event.getVehicleId())
				&& event.getTime() >= this.startTime && event.getTime() <= this.endTime){
			//Skips persons exiting vehicles not at stops and not in the time period.
			
			Id<TransitStopFacility> facilityId = this.vehicleStopCache.get(event.getVehicleId());
			Integer i;
			if (!this.facilityAlightings.containsKey(facilityId)){
				i = new Integer(1);
			}else{
				i = this.facilityAlightings.get(facilityId) + 1;
			}
			this.facilityAlightings.put(facilityId, i);
			
			this.facilities.add(facilityId);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleStopCache.containsKey(event.getVehicleId())
				&& event.getTime() >= this.startTime && event.getTime() <= this.endTime){
			//Skips persons entering vehicles not at stops and not in the time period.
			
			Id<TransitStopFacility> facilityId = this.vehicleStopCache.get(event.getVehicleId());
			Integer i;
			if (!this.facilityBoardings.containsKey(facilityId)){
				i = new Integer(1);
			}else{
				i = this.facilityBoardings.get(facilityId) + 1;
			}
			this.facilityBoardings.put(facilityId, i);
			
			this.facilities.add(facilityId);
		}
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		this.vehicleStopCache.remove(event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicleStopCache.put(event.getVehicleId(), event.getFacilityId());
	}
	
	public static void main(String[] args) throws IOException{
		String eventsFile = args[0];
		String outputFile = args[1];
		
		StationBoardingsAndAlightings handlerAM = new StationBoardingsAndAlightings(0, Time.parseTime("08:59:59"));
		StationBoardingsAndAlightings handlerMID = new StationBoardingsAndAlightings(Time.parseTime("09:00:00"), Time.parseTime("14:59:59"));
		StationBoardingsAndAlightings handlerPM = new StationBoardingsAndAlightings(Time.parseTime("15:00:00"), Time.parseTime("18:59:59"));
		StationBoardingsAndAlightings handlerEVE = new StationBoardingsAndAlightings(Time.parseTime("19:00:00"), Time.parseTime("21:59:59"));
		StationBoardingsAndAlightings handlerALL = new StationBoardingsAndAlightings();
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(handlerALL); em.addHandler(handlerEVE); em.addHandler(handlerPM); em.addHandler(handlerMID); em.addHandler(handlerAM);
		
		new MatsimEventsReader(em).readFile(eventsFile);
		
		HashSet<Id<TransitStopFacility>> stations = new HashSet<>();
		addSubwayStationsToMap(stations);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		bw.write("SUBWAY STATION REPORT"); bw.newLine();
		bw.write("station_id\tboard.am\talight.am\tboard.mid\talight.mid\tboard.pm\talight.pm\tboard.eve\talight.eve\tboard.all\talight.all");
		
		for (Id<TransitStopFacility> i : stations){
			bw.newLine();
			bw.write(i + "\t");
			bw.write("\t"+ handlerAM.getBoardingsAtStop(i));	bw.write("\t" + handlerAM.getAlightingsAtStop(i));
			bw.write("\t"+ handlerMID.getBoardingsAtStop(i));	bw.write("\t" + handlerMID.getAlightingsAtStop(i));
			bw.write("\t"+ handlerPM.getBoardingsAtStop(i));	bw.write("\t" + handlerPM.getAlightingsAtStop(i));
			bw.write("\t"+ handlerEVE.getBoardingsAtStop(i));	bw.write("\t" + handlerEVE.getAlightingsAtStop(i));
			bw.write("\t"+ handlerALL.getBoardingsAtStop(i));	bw.write("\t" + handlerALL.getAlightingsAtStop(i));
		}
		bw.close();
		
	}

	private static void addSubwayStationsToMap(HashSet<Id<TransitStopFacility>> stations){
		stations.add(Id.create(14481, TransitStopFacility.class));
		stations.add(Id.create(14516, TransitStopFacility.class));
		stations.add(Id.create(14484, TransitStopFacility.class));
		stations.add(Id.create(14513, TransitStopFacility.class));
		stations.add(Id.create(14531, TransitStopFacility.class));
		stations.add(Id.create(14538, TransitStopFacility.class));
		stations.add(Id.create(14532, TransitStopFacility.class));
		stations.add(Id.create(14537, TransitStopFacility.class));
		stations.add(Id.create(14414, TransitStopFacility.class));
		stations.add(Id.create(14457, TransitStopFacility.class));
		stations.add(Id.create(14488, TransitStopFacility.class));
		stations.add(Id.create(14509, TransitStopFacility.class));
		stations.add(Id.create(14487, TransitStopFacility.class));
		stations.add(Id.create(14510, TransitStopFacility.class));
		stations.add(Id.create(14489, TransitStopFacility.class));
		stations.add(Id.create(14508, TransitStopFacility.class));
		stations.add(Id.create(14480, TransitStopFacility.class));
		stations.add(Id.create(14517, TransitStopFacility.class));
		stations.add(Id.create(14416, TransitStopFacility.class));
		stations.add(Id.create(14455, TransitStopFacility.class));
		stations.add(Id.create(14493, TransitStopFacility.class));
		stations.add(Id.create(14504, TransitStopFacility.class));
		stations.add(Id.create(14410, TransitStopFacility.class));
		stations.add(Id.create(14461, TransitStopFacility.class));
		stations.add(Id.create(14534, TransitStopFacility.class));
		stations.add(Id.create(14535, TransitStopFacility.class));
		stations.add(Id.create(14491, TransitStopFacility.class));
		stations.add(Id.create(14506, TransitStopFacility.class));
		stations.add(Id.create(14435, TransitStopFacility.class));
		stations.add(Id.create(14436, TransitStopFacility.class));
		stations.add(Id.create(14478, TransitStopFacility.class));
		stations.add(Id.create(14519, TransitStopFacility.class));
		stations.add(Id.create(14417, TransitStopFacility.class));
		stations.add(Id.create(14454, TransitStopFacility.class));
		stations.add(Id.create(14476, TransitStopFacility.class));
		stations.add(Id.create(14521, TransitStopFacility.class));
		stations.add(Id.create(14428, TransitStopFacility.class));
		stations.add(Id.create(14443, TransitStopFacility.class));
		stations.add(Id.create(14409, TransitStopFacility.class));
		stations.add(Id.create(14462, TransitStopFacility.class));
		stations.add(Id.create(14430, TransitStopFacility.class));
		stations.add(Id.create(14441, TransitStopFacility.class));
		stations.add(Id.create(14544, TransitStopFacility.class));
		stations.add(Id.create(14549, TransitStopFacility.class));
		stations.add(Id.create(14404, TransitStopFacility.class));
		stations.add(Id.create(14467, TransitStopFacility.class));
		stations.add(Id.create(14431, TransitStopFacility.class));
		stations.add(Id.create(14440, TransitStopFacility.class));
		stations.add(Id.create(14492, TransitStopFacility.class));
		stations.add(Id.create(14505, TransitStopFacility.class));
		stations.add(Id.create(14474, TransitStopFacility.class));
		stations.add(Id.create(14523, TransitStopFacility.class));
		stations.add(Id.create(14469, TransitStopFacility.class));
		stations.add(Id.create(14528, TransitStopFacility.class));
		stations.add(Id.create(14472, TransitStopFacility.class));
		stations.add(Id.create(14525, TransitStopFacility.class));
		stations.add(Id.create(14475, TransitStopFacility.class));
		stations.add(Id.create(14522, TransitStopFacility.class));
		stations.add(Id.create(13398, TransitStopFacility.class));
		stations.add(Id.create(14546, TransitStopFacility.class));
		stations.add(Id.create(14547, TransitStopFacility.class));
		stations.add(Id.create(14498, TransitStopFacility.class));
		stations.add(Id.create(14499, TransitStopFacility.class));
		stations.add(Id.create(14419, TransitStopFacility.class));
		stations.add(Id.create(14452, TransitStopFacility.class));
		stations.add(Id.create(14468, TransitStopFacility.class));
		stations.add(Id.create(14529, TransitStopFacility.class));
		stations.add(Id.create(14477, TransitStopFacility.class));
		stations.add(Id.create(14520, TransitStopFacility.class));
		stations.add(Id.create(14545, TransitStopFacility.class));
		stations.add(Id.create(14548, TransitStopFacility.class));
		stations.add(Id.create(14408, TransitStopFacility.class));
		stations.add(Id.create(14463, TransitStopFacility.class));
		stations.add(Id.create(14432, TransitStopFacility.class));
		stations.add(Id.create(14439, TransitStopFacility.class));
		stations.add(Id.create(14533, TransitStopFacility.class));
		stations.add(Id.create(14536, TransitStopFacility.class));
		stations.add(Id.create(14495, TransitStopFacility.class));
		stations.add(Id.create(14502, TransitStopFacility.class));
		stations.add(Id.create(13502, TransitStopFacility.class));
		stations.add(Id.create(14541, TransitStopFacility.class));
		stations.add(Id.create(14552, TransitStopFacility.class));
		stations.add(Id.create(14543, TransitStopFacility.class));
		stations.add(Id.create(14550, TransitStopFacility.class));
		stations.add(Id.create(14425, TransitStopFacility.class));
		stations.add(Id.create(14446, TransitStopFacility.class));
		stations.add(Id.create(14405, TransitStopFacility.class));
		stations.add(Id.create(14466, TransitStopFacility.class));
		stations.add(Id.create(14471, TransitStopFacility.class));
		stations.add(Id.create(14526, TransitStopFacility.class));
		stations.add(Id.create(14422, TransitStopFacility.class));
		stations.add(Id.create(14449, TransitStopFacility.class));
		stations.add(Id.create(14479, TransitStopFacility.class));
		stations.add(Id.create(14518, TransitStopFacility.class));
		stations.add(Id.create(14490, TransitStopFacility.class));
		stations.add(Id.create(14507, TransitStopFacility.class));
		stations.add(Id.create(14418, TransitStopFacility.class));
		stations.add(Id.create(14453, TransitStopFacility.class));
		stations.add(Id.create(14424, TransitStopFacility.class));
		stations.add(Id.create(14447, TransitStopFacility.class));
		stations.add(Id.create(14413, TransitStopFacility.class));
		stations.add(Id.create(14458, TransitStopFacility.class));
		stations.add(Id.create(14470, TransitStopFacility.class));
		stations.add(Id.create(14527, TransitStopFacility.class));
		stations.add(Id.create(14473, TransitStopFacility.class));
		stations.add(Id.create(14524, TransitStopFacility.class));
		stations.add(Id.create(14542, TransitStopFacility.class));
		stations.add(Id.create(14551, TransitStopFacility.class));
		stations.add(Id.create(14406, TransitStopFacility.class));
		stations.add(Id.create(14465, TransitStopFacility.class));
		stations.add(Id.create(14530, TransitStopFacility.class));
		stations.add(Id.create(14539, TransitStopFacility.class));
		stations.add(Id.create(14486, TransitStopFacility.class));
		stations.add(Id.create(14511, TransitStopFacility.class));
		stations.add(Id.create(14427, TransitStopFacility.class));
		stations.add(Id.create(14444, TransitStopFacility.class));
		stations.add(Id.create(14482, TransitStopFacility.class));
		stations.add(Id.create(14515, TransitStopFacility.class));
		stations.add(Id.create(14421, TransitStopFacility.class));
		stations.add(Id.create(14450, TransitStopFacility.class));
		stations.add(Id.create(14411, TransitStopFacility.class));
		stations.add(Id.create(14460, TransitStopFacility.class));
		stations.add(Id.create(14429, TransitStopFacility.class));
		stations.add(Id.create(14442, TransitStopFacility.class));
		stations.add(Id.create(14426, TransitStopFacility.class));
		stations.add(Id.create(14445, TransitStopFacility.class));
		stations.add(Id.create(14483, TransitStopFacility.class));
		stations.add(Id.create(14514, TransitStopFacility.class));
		stations.add(Id.create(14423, TransitStopFacility.class));
		stations.add(Id.create(14448, TransitStopFacility.class));
		stations.add(Id.create(14412, TransitStopFacility.class));
		stations.add(Id.create(14459, TransitStopFacility.class));
		stations.add(Id.create(14420, TransitStopFacility.class));
		stations.add(Id.create(14451, TransitStopFacility.class));
		stations.add(Id.create(14496, TransitStopFacility.class));
		stations.add(Id.create(14501, TransitStopFacility.class));
		stations.add(Id.create(14497, TransitStopFacility.class));
		stations.add(Id.create(14500, TransitStopFacility.class));
		stations.add(Id.create(14415, TransitStopFacility.class));
		stations.add(Id.create(14456, TransitStopFacility.class));
		stations.add(Id.create(14434, TransitStopFacility.class));
		stations.add(Id.create(14437, TransitStopFacility.class));
		stations.add(Id.create(14494, TransitStopFacility.class));
		stations.add(Id.create(14503, TransitStopFacility.class));
		stations.add(Id.create(14485, TransitStopFacility.class));
		stations.add(Id.create(14512, TransitStopFacility.class));
		stations.add(Id.create(14407, TransitStopFacility.class));
		stations.add(Id.create(14464, TransitStopFacility.class));
		stations.add(Id.create(14433, TransitStopFacility.class));
		stations.add(Id.create(14438, TransitStopFacility.class));
	}

	
	
}