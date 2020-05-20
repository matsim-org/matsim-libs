package ch.sbb.matsim.routing.pt.raptor;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Collect for each departure of a transit vehicle at a stop some statistical values
 * like the latest time an agent can arrive to catch a departure, or the vehicle load
 * upon departure at a stop.
 *
 *
 * @author mrieser / Simunto GmbH
 */
public class WaitingTimeTracker implements AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final IdMap<TransitLine, LineData> lineData = new IdMap<>(TransitLine.class);
	private final Map<Id<Vehicle>, VehicleData> vehicleData = new HashMap<>();
	private final Map<Id<Person>, Double> waitingStarttimes = new HashMap<>();

	private final static VehicleData DUMMY_VEHDATA = new VehicleData(null, null, null);

	public WaitingTimeTracker() {
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		// store information about the current service of the transit vehicle
		this.vehicleData.put(event.getVehicleId(), new VehicleData(event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// store at what stop the transit vehicle currently is
		this.vehicleData.getOrDefault(event.getVehicleId(), DUMMY_VEHDATA).stopFacilityId = event.getFacilityId();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		// if nobody entered, store the departure time as latest time
		VehicleData vehData = this.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			LineData line = this.lineData.computeIfAbsent(vehData.lineId, id -> new LineData());
			RouteData route = line.routeData.computeIfAbsent(vehData.routeId, id -> new RouteData());
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, id -> new StopData());
			DepartureData dep = stop.getOrCreate(vehData.departureId);
			dep.vehDepTime = event.getTime();
			dep.paxCountAtDeparture = vehData.currentPaxCount;
		}

	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		this.waitingStarttimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		VehicleData vehData = this.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			vehData.currentPaxCount++;
			double waitStart = this.waitingStarttimes.remove(event.getPersonId());
			LineData line = this.lineData.computeIfAbsent(vehData.lineId, id -> new LineData());
			RouteData route = line.routeData.computeIfAbsent(vehData.routeId, id -> new RouteData());
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, id -> new StopData());
			stop.getOrCreate(vehData.departureId).addWaitingPerson(waitStart);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		VehicleData vehData = this.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			vehData.currentPaxCount--;
		}
	}

	@Override
	public void reset(int iteration) {
		this.vehicleData.clear();
		this.waitingStarttimes.clear();
		this.lineData.clear();
	}

	public DepartureData getNextAvailableDeparture(Id<TransitLine> transitLine, Id<TransitRoute> transitRoute, Id<TransitStopFacility> stopFacility, double time) {
		LineData line = this.lineData.get(transitLine);
		if (line == null) {
			return null;
		}
		RouteData route = line.routeData.get(transitRoute);
		if (route == null) {
			return null;
		}
		StopData stop = route.stopData.get(stopFacility);
		if (stop == null) {
			return null;
		}
		List<DepartureData> departures = stop.getSortedDepartures();
		if (departures.isEmpty()) {
			return null;
		}
		DepartureData firstDep = departures.get(0);
//		if (time < firstDep.latestWaitStart) {
//			return firstDep;
//		}

		for (DepartureData dep : departures) {
			if (time <= dep.latestWaitStart) {
				return dep;
			}
		}
		return null;
	}

	public DepartureData getDepartureData(Id<TransitLine> transitLine, Id<TransitRoute> transitRoute, Id<TransitStopFacility> stopFacility, Id<Departure> departure) {
		LineData line = this.lineData.get(transitLine);
		if (line == null) {
			return null;
		}
		RouteData route = line.routeData.get(transitRoute);
		if (route == null) {
			return null;
		}
		StopData stop = route.stopData.get(stopFacility);
		if (stop == null) {
			return null;
		}
		for (DepartureData d : stop.depList) {
			if (d.departureId.equals(departure)) {
				return d;
			}
		}
		return null;
	}

	private static class LineData {
		Map<Id<TransitRoute>, RouteData> routeData = new HashMap<>();
	}

	private static class RouteData {
		Map<Id<TransitStopFacility>, StopData> stopData = new HashMap<>();
	}

	private static class StopData {
		Map<Id<Departure>, DepartureData> depData = new HashMap<>();
		boolean sorted = true;
		List<DepartureData> depList = new ArrayList<>();

		private DepartureData getOrCreate(Id<Departure> depId) {
			DepartureData data = this.depData.get(depId);
			if (data == null) {
				data = new DepartureData(depId);
				this.depList.add(data);
				this.depData.put(depId, data);
				this.sorted = false;
			}
			return data;
		}

		private List<DepartureData> getSortedDepartures() {
			if (!this.sorted) {
				this.depList.sort((o1, o2) -> Double.compare(o1.vehDepTime, o2.vehDepTime));
				this.sorted = true;
			}

			ListIterator<DepartureData> li = this.depList.listIterator(this.depList.size());
			DepartureData laterDep = null;
			while (li.hasPrevious()) {
				DepartureData dep = li.previous();
				if (laterDep == null) {
					dep.latestWaitStart = dep.vehDepTime;
					laterDep = dep;
				} else {
					if (laterDep.earliestWaitStart < dep.vehDepTime) {
						// there were agents that could not board

						if (Double.isFinite(dep.latestWaitStart)) {
							// but at least some agents could board
							laterDep.earliestWaitStart = dep.latestWaitStart;
							laterDep = dep;
						}
					} else {
						// looks like everybody who wanted could board
						laterDep.earliestWaitStart = dep.vehDepTime;
						dep.latestWaitStart = dep.vehDepTime;
						laterDep = dep;
					}
				}
			}
			return this.depList;
		}
	}

	public static final class DepartureData {
		final Id<Departure> departureId;
		double vehDepTime = Double.NEGATIVE_INFINITY;
		double earliestWaitStart = Double.POSITIVE_INFINITY;
		double latestWaitStart = Double.NEGATIVE_INFINITY;
		int paxCountAtDeparture = -1;

		public DepartureData(Id<Departure> departureId) {
			this.departureId = departureId;
		}

		void addWaitingPerson(double startWaitTime) {
			if (startWaitTime < this.earliestWaitStart) {
				this.earliestWaitStart = startWaitTime;
			}
			if (startWaitTime > this.latestWaitStart) {
				this.latestWaitStart = startWaitTime;
			}
		}

		double getLatestWaitStart() {
			if (Double.isFinite(this.latestWaitStart)) {
				return this.latestWaitStart;
			}
			return Time.getUndefinedTime();
		}
	}

	private static class VehicleData {
		private final Id<TransitLine> lineId;
		private final Id<TransitRoute> routeId;
		private final Id<Departure> departureId;
		private Id<TransitStopFacility> stopFacilityId = null;
		private int currentPaxCount = 0;

		public VehicleData(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<Departure> departureId) {
			this.lineId = lineId;
			this.routeId = routeId;
			this.departureId = departureId;
		}
	}
}
