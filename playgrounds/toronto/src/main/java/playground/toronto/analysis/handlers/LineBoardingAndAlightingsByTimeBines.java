package playground.toronto.analysis.handlers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.TransitDriverStartsEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.utils.misc.Time;

public class LineBoardingAndAlightingsByTimeBines implements
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		TransitDriverStartsEventHandler {

	private HashMap<Id, Id> vehicleLineCache;
	private final TreeMap<Double, Integer> timeBins;
	private HashMap<Id, int[]> lineBoardings;
	private HashMap<Id, int[]> lineAlightings;
	
	public LineBoardingAndAlightingsByTimeBines(String binsFile) throws IOException{
		this.timeBins = generateBinsFromFile(binsFile);
	}
	
	private int[] getEmptyBins(){
		return new int[this.timeBins.size()];
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
		
		set.put(Double.MAX_VALUE, i);
		return set;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehicleLineCache.put(event.getVehicleId(), event.getTransitLineId());
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (this.vehicleLineCache.containsKey(event.getVehicleId())){
			Id lineId = this.vehicleLineCache.get(event.getVehicleId());
			if (!this.lineAlightings.containsKey(lineId)){
				this.lineAlightings.put(lineId, getEmptyBins());
			}
			this.lineAlightings.get(lineId)[this.timeBins.ceilingEntry(event.getTime()).getValue()]++;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.vehicleLineCache.containsKey(event.getVehicleId())){
			Id lineId = this.vehicleLineCache.get(event.getVehicleId());
			if (!this.lineBoardings.containsKey(lineId)){
				this.lineBoardings.put(lineId, getEmptyBins());
			}
			this.lineBoardings.get(lineId)[this.timeBins.ceilingEntry(event.getTime()).getValue()]++;
		}

	}

}
