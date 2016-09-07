package playground.wrashid.parkingChoice.priceoptimization.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingArrivalEventHandler;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEvent;
import org.matsim.contrib.parking.PC2.simulation.ParkingDepartureEventHandler;

public class ArrivalDepartureParkingHandler implements ParkingArrivalEventHandler,
	ParkingDepartureEventHandler {
	
	private Map<Id<PC2Parking>, Double> arrivalsTimeMap = new HashMap<Id<PC2Parking>, Double>();
	private Map<Id<PC2Parking>, Integer> arrivalsCountMap = new HashMap<Id<PC2Parking>, Integer>();
	private Map<Id<PC2Parking>, Double> departuresTimeMap = new HashMap<Id<PC2Parking>, Double>();
	private Map<Id<PC2Parking>, Integer> departuresCountMap = new HashMap<Id<PC2Parking>, Integer>();
	private HashMap<Id<PC2Parking>, Double> rentableDur = new HashMap<Id<PC2Parking>, Double>();
	private HashMap<Id<PC2Parking>, double[]> rentableDurPerHour = new HashMap<Id<PC2Parking>, double[]>();

	private Map<Id<PC2Parking>, Integer> arrDepBeforePeakCountMap = new HashMap<Id<PC2Parking>, Integer>();
	private Map<Id<PC2Parking>, Double> arrivalsPeakTimeMap = new HashMap<Id<PC2Parking>, Double>();
	private Map<Id<PC2Parking>, Double> departuresPeakTimeMap = new HashMap<Id<PC2Parking>, Double>();
	private Set<Id<PC2Parking>> ids = new TreeSet<Id<PC2Parking>>();
	
	double startMorning = 7.0 * 60 * 60;
	double endMorning = 11.0 * 60 * 60;

	@Override
	public void reset(int iteration) {
		arrivalsTimeMap = new HashMap<Id<PC2Parking>, Double>();
		arrivalsCountMap = new HashMap<Id<PC2Parking>, Integer>();
		departuresTimeMap = new HashMap<Id<PC2Parking>, Double>();
		departuresCountMap = new HashMap<Id<PC2Parking>, Integer>();
		rentableDur = new HashMap<Id<PC2Parking>, Double>();
		
		arrDepBeforePeakCountMap = new HashMap<Id<PC2Parking>, Integer>();
		arrivalsPeakTimeMap = new HashMap<Id<PC2Parking>, Double>();
		departuresPeakTimeMap = new HashMap<Id<PC2Parking>, Double>();
		ids = new TreeSet<Id<PC2Parking>>();
	}

	@Override
	public void handleEvent(ParkingDepartureEvent event) {
		if (event.getTime() < 24 * 60 * 60) {

			Id<PC2Parking> parkingId = event.getParkingId(event.getAttributes());
			
			int startIndex = (int)(event.getTime() / 3600.0);
			
			double[] durations = this.rentableDurPerHour.get(parkingId);
			
			durations[startIndex] -= ((startIndex + 1) * 3600 - event.getTime());
			
			for (int i = startIndex + 1; i < 24; i++) {
				
				durations[i] -= 3600.0;
			}
			this.rentableDurPerHour.put(parkingId, durations);
			
			
			
			if (event.getTime() < this.startMorning) {
				this.arrDepBeforePeakCountMap.put(parkingId, this.arrDepBeforePeakCountMap.get(parkingId) - 1);
				ids.add(parkingId);
			}
			
			if (this.departuresTimeMap.containsKey(parkingId)) {
				double time = this.departuresTimeMap.get(parkingId);
				int count = this.departuresCountMap.get(parkingId);
				this.departuresTimeMap.put(parkingId, time + event.getTime());
				this.departuresCountMap.put(parkingId, count + 1);
			}
			else {
				
				this.departuresTimeMap.put(parkingId, event.getTime());
				this.departuresCountMap.put(parkingId, 1);
			}
			if (event.getTime() >= this.startMorning && event.getTime() <= this.endMorning) {
				ids.add(parkingId);
	
				if (this.departuresPeakTimeMap.containsKey(parkingId)) {
					double time = this.departuresPeakTimeMap.get(parkingId);
					this.departuresPeakTimeMap.put(parkingId, time + this.endMorning - event.getTime());
				}
				else {
					
					this.departuresPeakTimeMap.put(parkingId, this.endMorning - event.getTime());
				}
			}
		}
				
	}

	@Override
	public void handleEvent(ParkingArrivalEvent event) {
		if (event.getTime() < 24 * 60 * 60) {
			Id<PC2Parking> parkingId = event.getParkingId(event.getAttributes());
			
			if (this.rentableDurPerHour.containsKey(parkingId)) {
				
				int startIndex = (int)(event.getTime() / 3600.0);
				
				double[] durations = this.rentableDurPerHour.get(parkingId);
				
				durations[startIndex] += (startIndex + 1) * 3600 - event.getTime();
				
				for (int i = startIndex + 1; i < 24; i++) {
					
					durations[i] += 3600.0;
				}
				this.rentableDurPerHour.put(parkingId, durations);
			}
			else {
				int startIndex = (int)(event.getTime() / 3600.0);

				double[] durations = new double[24];
				
				durations[startIndex] += (startIndex + 1) * 3600 - event.getTime();
				
				for (int i = startIndex + 1; i < 24; i++) {
					
					durations[i] += 3600.0;
				}
				this.rentableDurPerHour.put(parkingId, durations);

			}
			
			
			
			
			if (this.arrDepBeforePeakCountMap.containsKey(parkingId)
					&& event.getTime() < this.startMorning) {
				
				this.arrDepBeforePeakCountMap.put(parkingId, this.arrDepBeforePeakCountMap.get(parkingId) + 1);
				ids.add(parkingId);
			}
			else
				this.arrDepBeforePeakCountMap.put(parkingId, 1);
			if (this.arrivalsTimeMap.containsKey(parkingId)) {
							
				double time = this.arrivalsTimeMap.get(parkingId);
				int count = this.arrivalsCountMap.get(parkingId);
				this.arrivalsTimeMap.put(parkingId, time + event.getTime());
				this.arrivalsCountMap.put(parkingId, count + 1);
			}
			else {
				
				this.arrivalsTimeMap.put(parkingId, event.getTime());
				this.arrivalsCountMap.put(parkingId, 1);
			}
			if (event.getTime() >= this.startMorning && event.getTime() <= this.endMorning) {
				
				ids.add(parkingId);
	
				if (this.arrivalsPeakTimeMap.containsKey(parkingId)) {
					
					double time = this.arrivalsPeakTimeMap.get(parkingId);
					this.arrivalsPeakTimeMap.put(parkingId, time + this.endMorning -  event.getTime());
				}
				else {			
					this.arrivalsPeakTimeMap.put(parkingId, this.endMorning - event.getTime());
				}
			}
		}
	}
	public HashMap<Id<PC2Parking>, Double> getRentablePeakDur() {		
		
		for (Id<PC2Parking> id : this.ids) {
			int count = 0;
			if (this.arrDepBeforePeakCountMap.containsKey(id))
				count = this.arrDepBeforePeakCountMap.get(id);
			double arrT = 0.0;
			double depT = 0.0;
			if (this.arrivalsPeakTimeMap.containsKey(id))
				arrT = this.arrivalsPeakTimeMap.get(id);
			if (this.departuresPeakTimeMap.containsKey(id))
				depT = this.departuresPeakTimeMap.get(id);
			
			
			rentableDur.put(id, count * (this.endMorning - this.startMorning) - depT + arrT);
		}
		
		return rentableDur;
	}
	public HashMap<Id<PC2Parking>, Double> getRentableDur() {
		
		for (Id<PC2Parking> id : this.arrivalsCountMap.keySet()) {
			if (this.departuresTimeMap.get(id) != null) {
				double totalDuration = this.departuresTimeMap.get(id) - this.arrivalsTimeMap.get(id) ;
				totalDuration += 24 * 60 * 60 * (this.arrivalsCountMap.get(id) - this.departuresCountMap.get(id));
				this.rentableDur.put(id, totalDuration);
			}
			else {
				double totalDuration = 24 * 60 * 60 * (this.arrivalsCountMap.get(id)) - this.arrivalsTimeMap.get(id);
				this.rentableDur.put(id, totalDuration);
				
			}
			
		}
		
		return rentableDur;
	}
	
	public HashMap<Id<PC2Parking>, double[]> getRentableDurPerHour() {
		
		return this.rentableDurPerHour;
	}

}
