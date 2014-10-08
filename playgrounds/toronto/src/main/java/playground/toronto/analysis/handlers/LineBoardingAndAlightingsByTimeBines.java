package playground.toronto.analysis.handlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.vehicles.Vehicle;

public class LineBoardingAndAlightingsByTimeBines implements
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		TransitDriverStartsEventHandler {

	private HashMap<Id<Vehicle>, Id<TransitLine>> vehicleLineCache;
	private final TreeMap<Double, Integer> timeBins;
	private HashMap<Id<TransitLine>, int[]> lineBoardings;
	private HashMap<Id<TransitLine>, int[]> lineAlightings;
	
	public LineBoardingAndAlightingsByTimeBines(String binsFile) throws IOException{
		this.timeBins = generateBinsFromFile(binsFile);
		this.vehicleLineCache = new HashMap<>();
		this.lineAlightings = new HashMap<>();
		this.lineBoardings = new HashMap<>();
	}
	
	private int[] getEmptyBins(){
		return new int[this.timeBins.size()];
	}
	
	public void printLineResults(Id<TransitLine> lineId){
		int[] boardings = this.lineBoardings.get(lineId);
		int[] alightings = this.lineAlightings.get(lineId);
		
		System.out.println("Boardings/Alightings for Line " + lineId);
		System.out.println("from\tto\tboardings\talightings");
		
		Double prevTime = null;
		int i = 0;
		for (Double d : this.timeBins.navigableKeySet()){
			String out = "";
			if (prevTime != null) out += Time.writeTime(prevTime);
			out += "\t" + Time.writeTime(d);
			out += "\t" + boardings[i] + "\t" + alightings[i++];
			
			System.out.println(out);
			
			prevTime = d;
		}
		
	}
	
	private TreeMap<Double, Integer> generateBinsFromFile(String fileName) throws IOException{
		TreeMap<Double, Integer> set = new TreeMap<Double, Integer>();
		int i = 0;
		set.put(0.0, i++);
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		String line = "";
		while ((line = br.readLine()) != null){
			
			Double time = null;
			
			try {
				int t = Integer.parseInt(line);
				time = Time.convertHHMMInteger(t);
			} catch (NumberFormatException e) {
				try {
					time = Time.parseTime(line);
				} catch (IllegalArgumentException e1) {
				}
			}
			
			if (time != null) set.put(time, i++);
		}
		br.close();
		
		set.put(Double.MAX_VALUE, i);
		return set;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}
	
	public static void main(String[] args) throws IOException{
		String eventsFile = args[0];
		String binsFile = args[1];
		
		Id<TransitLine> YUS = Id.create(21948, TransitLine.class);
		Id<TransitLine> BLR = Id.create(21945, TransitLine.class);
		Id<TransitLine> SHE = Id.create(21946, TransitLine.class);
		Id<TransitLine> SRT = Id.create(21947, TransitLine.class);
		
		LineBoardingAndAlightingsByTimeBines handler = new LineBoardingAndAlightingsByTimeBines(binsFile);
		
		EventsManager em = EventsUtils.createEventsManager();
		em.addHandler(handler);
		
		new MatsimEventsReader(em).readFile(eventsFile);
		
		handler.printLineResults(YUS);
		handler.printLineResults(BLR);
		handler.printLineResults(SHE);
		//handler.printLineResults(SRT);
		
		
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehicleLineCache.put(event.getVehicleId(), event.getTransitLineId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.vehicleLineCache.containsKey(event.getVehicleId())){
			Id<TransitLine> lineId = this.vehicleLineCache.get(event.getVehicleId());
			if (!this.lineAlightings.containsKey(lineId)){
				this.lineAlightings.put(lineId, getEmptyBins());
			}
			this.lineAlightings.get(lineId)[this.timeBins.ceilingEntry(event.getTime()).getValue()]++;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleLineCache.containsKey(event.getVehicleId())){
			Id<TransitLine> lineId = this.vehicleLineCache.get(event.getVehicleId());
			if (!this.lineBoardings.containsKey(lineId)){
				this.lineBoardings.put(lineId, getEmptyBins());
			}
			this.lineBoardings.get(lineId)[this.timeBins.ceilingEntry(event.getTime()).getValue()]++;
		}
	}

}