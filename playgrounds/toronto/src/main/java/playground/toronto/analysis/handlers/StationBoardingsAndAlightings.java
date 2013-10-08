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
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;

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
	
	private HashMap<Id, Id> vehicleStopCache;
	private HashMap<Id, Integer> facilityBoardings;
	private HashMap<Id, Integer> facilityAlightings;
	private HashSet<Id> facilities;
	
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
		this.facilities = new HashSet<Id>();
		this.facilityAlightings = new HashMap<Id, Integer>();
		this.facilityBoardings = new HashMap<Id, Integer>();
		this.vehicleStopCache = new HashMap<Id, Id>();
	}
	
	public HashSet<Id> getActiveStops(){
		return this.facilities;
	}
	
	public int getAlightingsAtStop(Id stopId){
		Integer i = this.facilityAlightings.get(stopId);
		return i == null ? 0 : i;
	}
	
	public int getBoardingsAtStop(Id stopId){
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
			
			Id facilityId = this.vehicleStopCache.get(event.getVehicleId());
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
			
			Id facilityId = this.vehicleStopCache.get(event.getVehicleId());
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
		
		HashSet<Id> stations = new HashSet<Id>();
		addSubwayStationsToMap(stations);
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
		bw.write("SUBWAY STATION REPORT"); bw.newLine();
		bw.write("station_id\tboard.am\talight.am\tboard.mid\talight.mid\tboard.pm\talight.pm\tboard.eve\talight.eve\tboard.all\talight.all");
		
		for (Id i : stations){
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

	private static void addSubwayStationsToMap(HashSet<Id> stations){
		stations.add(new IdImpl(14481));
		stations.add(new IdImpl(14516));
		stations.add(new IdImpl(14484));
		stations.add(new IdImpl(14513));
		stations.add(new IdImpl(14531));
		stations.add(new IdImpl(14538));
		stations.add(new IdImpl(14532));
		stations.add(new IdImpl(14537));
		stations.add(new IdImpl(14414));
		stations.add(new IdImpl(14457));
		stations.add(new IdImpl(14488));
		stations.add(new IdImpl(14509));
		stations.add(new IdImpl(14487));
		stations.add(new IdImpl(14510));
		stations.add(new IdImpl(14489));
		stations.add(new IdImpl(14508));
		stations.add(new IdImpl(14480));
		stations.add(new IdImpl(14517));
		stations.add(new IdImpl(14416));
		stations.add(new IdImpl(14455));
		stations.add(new IdImpl(14493));
		stations.add(new IdImpl(14504));
		stations.add(new IdImpl(14410));
		stations.add(new IdImpl(14461));
		stations.add(new IdImpl(14534));
		stations.add(new IdImpl(14535));
		stations.add(new IdImpl(14491));
		stations.add(new IdImpl(14506));
		stations.add(new IdImpl(14435));
		stations.add(new IdImpl(14436));
		stations.add(new IdImpl(14478));
		stations.add(new IdImpl(14519));
		stations.add(new IdImpl(14417));
		stations.add(new IdImpl(14454));
		stations.add(new IdImpl(14476));
		stations.add(new IdImpl(14521));
		stations.add(new IdImpl(14428));
		stations.add(new IdImpl(14443));
		stations.add(new IdImpl(14409));
		stations.add(new IdImpl(14462));
		stations.add(new IdImpl(14430));
		stations.add(new IdImpl(14441));
		stations.add(new IdImpl(14544));
		stations.add(new IdImpl(14549));
		stations.add(new IdImpl(14404));
		stations.add(new IdImpl(14467));
		stations.add(new IdImpl(14431));
		stations.add(new IdImpl(14440));
		stations.add(new IdImpl(14492));
		stations.add(new IdImpl(14505));
		stations.add(new IdImpl(14474));
		stations.add(new IdImpl(14523));
		stations.add(new IdImpl(14469));
		stations.add(new IdImpl(14528));
		stations.add(new IdImpl(14472));
		stations.add(new IdImpl(14525));
		stations.add(new IdImpl(14475));
		stations.add(new IdImpl(14522));
		stations.add(new IdImpl(13398));
		stations.add(new IdImpl(14546));
		stations.add(new IdImpl(14547));
		stations.add(new IdImpl(14498));
		stations.add(new IdImpl(14499));
		stations.add(new IdImpl(14419));
		stations.add(new IdImpl(14452));
		stations.add(new IdImpl(14468));
		stations.add(new IdImpl(14529));
		stations.add(new IdImpl(14477));
		stations.add(new IdImpl(14520));
		stations.add(new IdImpl(14545));
		stations.add(new IdImpl(14548));
		stations.add(new IdImpl(14408));
		stations.add(new IdImpl(14463));
		stations.add(new IdImpl(14432));
		stations.add(new IdImpl(14439));
		stations.add(new IdImpl(14533));
		stations.add(new IdImpl(14536));
		stations.add(new IdImpl(14495));
		stations.add(new IdImpl(14502));
		stations.add(new IdImpl(13502));
		stations.add(new IdImpl(14541));
		stations.add(new IdImpl(14552));
		stations.add(new IdImpl(14543));
		stations.add(new IdImpl(14550));
		stations.add(new IdImpl(14425));
		stations.add(new IdImpl(14446));
		stations.add(new IdImpl(14405));
		stations.add(new IdImpl(14466));
		stations.add(new IdImpl(14471));
		stations.add(new IdImpl(14526));
		stations.add(new IdImpl(14422));
		stations.add(new IdImpl(14449));
		stations.add(new IdImpl(14479));
		stations.add(new IdImpl(14518));
		stations.add(new IdImpl(14490));
		stations.add(new IdImpl(14507));
		stations.add(new IdImpl(14418));
		stations.add(new IdImpl(14453));
		stations.add(new IdImpl(14424));
		stations.add(new IdImpl(14447));
		stations.add(new IdImpl(14413));
		stations.add(new IdImpl(14458));
		stations.add(new IdImpl(14470));
		stations.add(new IdImpl(14527));
		stations.add(new IdImpl(14473));
		stations.add(new IdImpl(14524));
		stations.add(new IdImpl(14542));
		stations.add(new IdImpl(14551));
		stations.add(new IdImpl(14406));
		stations.add(new IdImpl(14465));
		stations.add(new IdImpl(14530));
		stations.add(new IdImpl(14539));
		stations.add(new IdImpl(14486));
		stations.add(new IdImpl(14511));
		stations.add(new IdImpl(14427));
		stations.add(new IdImpl(14444));
		stations.add(new IdImpl(14482));
		stations.add(new IdImpl(14515));
		stations.add(new IdImpl(14421));
		stations.add(new IdImpl(14450));
		stations.add(new IdImpl(14411));
		stations.add(new IdImpl(14460));
		stations.add(new IdImpl(14429));
		stations.add(new IdImpl(14442));
		stations.add(new IdImpl(14426));
		stations.add(new IdImpl(14445));
		stations.add(new IdImpl(14483));
		stations.add(new IdImpl(14514));
		stations.add(new IdImpl(14423));
		stations.add(new IdImpl(14448));
		stations.add(new IdImpl(14412));
		stations.add(new IdImpl(14459));
		stations.add(new IdImpl(14420));
		stations.add(new IdImpl(14451));
		stations.add(new IdImpl(14496));
		stations.add(new IdImpl(14501));
		stations.add(new IdImpl(14497));
		stations.add(new IdImpl(14500));
		stations.add(new IdImpl(14415));
		stations.add(new IdImpl(14456));
		stations.add(new IdImpl(14434));
		stations.add(new IdImpl(14437));
		stations.add(new IdImpl(14494));
		stations.add(new IdImpl(14503));
		stations.add(new IdImpl(14485));
		stations.add(new IdImpl(14512));
		stations.add(new IdImpl(14407));
		stations.add(new IdImpl(14464));
		stations.add(new IdImpl(14433));
		stations.add(new IdImpl(14438));
	}

	
	
}