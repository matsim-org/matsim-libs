package saleem.stockholmscenario.teleportation.ptoptimisation;

import static java.lang.Math.min;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import opdytsintegration.RecursiveCountAverage;
import opdytsintegration.TimeDiscretization;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.DynamicData;

/**
 * 
 * @author Mohammad Saleem
 *
 */
public class PTOccupancyAnalyser implements AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, PersonStuckEventHandler {

	// -------------------- MEMBERS --------------------

	private final DynamicData<Id<TransitStopFacility>> occupancies_veh;

	private final Set<Id<TransitStopFacility>> relevantStops;

	private final Map<Id<TransitStopFacility>, RecursiveCountAverage> stop2avg = new LinkedHashMap<>();

	private int lastCompletedBin = -1;
	private int totalStuck = 0;

	private final Set<Id<Person>> transitDrivers = new HashSet<>();
	private final Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private final Map<Id<Person>, Id<TransitStopFacility>> personStops = new HashMap<>();//To maintain person to stop mapping
	
	
	// -------------------- CONSTRUCTION --------------------

	public PTOccupancyAnalyser(final TimeDiscretization timeDiscretization,
			final Set<Id<TransitStopFacility>> relevantStops) {
		this(timeDiscretization.getStartTime_s(), timeDiscretization
				.getBinSize_s(), timeDiscretization.getBinCnt(), relevantStops);
	}

	public PTOccupancyAnalyser(final int startTime_s, final int binSize_s,
			final int binCnt, final Set<Id<TransitStopFacility>> relevantStops) {
		this.occupancies_veh = new DynamicData<>(startTime_s, binSize_s, binCnt);
		this.relevantStops = relevantStops;
		this.reset(-1);
	}

	// -------------------- INTERNALS --------------------

	private int lastCompletedBinEndTime() {
		return this.occupancies_veh.getStartTime_s()
				+ (this.lastCompletedBin + 1)
				* this.occupancies_veh.getBinSize_s();
	}

	private void completeBins(final int lastBinToComplete) {
		while (this.lastCompletedBin < lastBinToComplete) {
			this.lastCompletedBin++; // is now zero or larger
			final int lastCompletedBinEndTime = this.lastCompletedBinEndTime();
			for (Map.Entry<Id<TransitStopFacility>, RecursiveCountAverage> stop2avgEntry : this.stop2avg.entrySet()) {
				stop2avgEntry.getValue().advanceTo(lastCompletedBinEndTime);
				this.occupancies_veh.put(stop2avgEntry.getKey(),
						this.lastCompletedBin, stop2avgEntry.getValue()
								.getAverage());
				stop2avgEntry.getValue().resetTime(lastCompletedBinEndTime);
			}
		}
	}

	private void advanceToTime(final int time_s) {
		final int lastBinToComplete = this.occupancies_veh.bin(time_s) - 1;
		this.completeBins(min(lastBinToComplete,
				this.occupancies_veh.getBinCnt() - 1));
	}

	private RecursiveCountAverage avg(final Id<TransitStopFacility> stop) {
		RecursiveCountAverage avg = this.stop2avg.get(stop);
		if (avg == null) {
			avg = new RecursiveCountAverage(this.lastCompletedBinEndTime());
			this.stop2avg.put(stop, avg);
		}
		return avg;
	}
	private boolean relevant(Id<TransitStopFacility> stop) {
		return ((this.relevantStops == null) || this.relevantStops
				.contains(stop));
	}

	private void registerEntry(final Id<Person> person, final Id<TransitStopFacility> stop, final int time_s) {
		this.advanceToTime(time_s);
		this.avg(stop).inc(time_s);
		this.personStops.put(person, stop);//Register person against the stop

	}

	private void registerExit(final Id<Person> person, final Id<TransitStopFacility> stop, final int time_s) {
		this.advanceToTime(time_s);
		this.avg(stop).dec(time_s);
		this.personStops.remove(person);//Remove person mapping to the stop
	}

	public void advanceToEnd() {
		this.completeBins(this.occupancies_veh.getBinCnt() - 1);
	}

	 public Set<Id<TransitStopFacility>> observedStopSetView() {
		 return Collections.unmodifiableSet(this.occupancies_veh.keySet());
	 }
	 
	// -------------------- CONTENT ACCESS --------------------

	public double getOccupancy_veh(final Id<TransitStopFacility> stop, final int bin) {
		return this.occupancies_veh.getBinValue(stop, bin);
	}
	public int getTotalStuckOutsideStops(){
		return this.totalStuck;
	}
	public int getTotalLeftOnStopsAtEnd(){
		return this.personStops.size();
	}
	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void reset(final int iteration) {
		this.occupancies_veh.clear();
		this.stop2avg.clear();
		this.lastCompletedBin = -1;
		this.transitDrivers.clear();
		this.transitVehicles.clear();
		this.personStops.clear();
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		Id<TransitStopFacility> stopid = event.getWaitingAtStopId();
		if(relevant(stopid)){
			this.registerEntry(event.getPersonId(), stopid, (int) event.getTime());
		}
	}
	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.transitDrivers.add(event.getDriverId());
		this.transitVehicles.add(event.getVehicleId());
	}
	
	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (this.transitDrivers.contains(event.getPersonId()) || !this.transitVehicles.contains(event.getVehicleId())) {
			return; // ignore transit drivers or persons entering non-transit vehicles
		}
		Id<Person> personId = event.getPersonId();
		Id<TransitStopFacility> stopId = personStops.get(event.getPersonId());
		double time = event.getTime();
		if(relevant(stopId)){
			this.registerExit(personId, stopId, (int) time);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {//Just to check stuck people at the end
		totalStuck++;
	}
}
