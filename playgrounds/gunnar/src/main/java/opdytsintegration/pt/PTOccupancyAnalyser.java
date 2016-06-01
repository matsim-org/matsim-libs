package opdytsintegration.pt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import opdytsintegration.MATSimCountingStateAnalyzer;
import opdytsintegration.utils.TimeDiscretization;

/**
 * 
 * @author Mohammad Saleem
 *
 */
public class PTOccupancyAnalyser extends MATSimCountingStateAnalyzer<TransitStopFacility>
		implements AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler,
		PersonStuckEventHandler {

	// -------------------- MEMBERS --------------------

	private final Set<Id<TransitStopFacility>> relevantStops;

	private int totalStuck = 0;

	private Set<Id<Person>> transitDrivers = new HashSet<>();
	private Set<Id<Vehicle>> transitVehicles = new HashSet<>();
	private Map<Id<Person>, Id<TransitStopFacility>> personStops = new HashMap<>();
	// To maintain person to stop mapping

	// -------------------- CONSTRUCTION --------------------

	public PTOccupancyAnalyser(final TimeDiscretization timeDiscretization,
			final Set<Id<TransitStopFacility>> relevantStops) {
		this(timeDiscretization.getStartTime_s(), timeDiscretization.getBinSize_s(), timeDiscretization.getBinCnt(),
				relevantStops);
	}

	public PTOccupancyAnalyser(final int startTime_s, final int binSize_s, final int binCnt,
			final Set<Id<TransitStopFacility>> relevantStops) {
		super(startTime_s, binSize_s, binCnt);
		this.relevantStops = relevantStops;
	}

	// -------------------- INTERNALS --------------------

	private boolean relevant(Id<TransitStopFacility> stop) {
		return ((this.relevantStops == null) || this.relevantStops.contains(stop));
	}

	protected void registerEntry(final Id<Person> person, final Id<TransitStopFacility> stop, final int time_s) {
		super.registerIncrease(stop, time_s);
		this.personStops.put(person, stop);// Register person against the stop
	}

	private void registerExit(final Id<Person> person, final Id<TransitStopFacility> stop, final int time_s) {
		super.registerDecrease(stop, time_s);
		this.personStops.remove(person);// Remove person mapping to the stop
	}

	// -------------------- CONTENT ACCESS --------------------

	public int getTotalStuckOutsideStops() {
		return this.totalStuck;
	}

	public int getTotalLeftOnStopsAtEnd() {
		return this.personStops.size();
	}
	// ---------- IMPLEMENTATION OF *EventHandler INTERFACES ----------

	@Override
	public void reset(final int iteration) {
		super.reset(iteration);
		if(this.transitDrivers==null) {
			this.transitDrivers = new HashSet<Id<Person>>();
		}else{
			this.transitDrivers.clear();
		}
		if(this.transitVehicles==null) {
			this.transitVehicles = new HashSet<Id<Vehicle>>();
		}else{
			this.transitVehicles.clear();
		}
		if(this.personStops==null) {
			this.personStops = new HashMap<Id<Person>, Id<TransitStopFacility>>();
		}else{
			this.personStops.clear();		}
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		Id<TransitStopFacility> stopid = event.getWaitingAtStopId();
		if (relevant(stopid)) {
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
			return; 
			// ignore transit drivers or persons entering non-transit vehicles
		}
		Id<Person> personId = event.getPersonId();
		Id<TransitStopFacility> stopId = personStops.get(event.getPersonId());
		double time = event.getTime();
		if (relevant(stopId)) {
			this.registerExit(personId, stopId, (int) time);
		}
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		// Just to check stuck people at the end
		totalStuck++;
	}
}
