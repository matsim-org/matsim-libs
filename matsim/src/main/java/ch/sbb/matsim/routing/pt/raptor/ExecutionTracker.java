package ch.sbb.matsim.routing.pt.raptor;

import ch.sbb.matsim.routing.pt.raptor.ExecutionData.DepartureData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.LineData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.RouteData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.StopData;
import ch.sbb.matsim.routing.pt.raptor.ExecutionData.VehicleData;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.AgentWaitingForPtEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;

import javax.inject.Inject;

/**
 * Collects various data related to public transport during the execution of the plans (=during mobsim).
 * For example, for each departure of a transit vehicle at a stop the latest time an agent can arrive
 * to catch a departure is recorder, as is the vehicle load upon departure at a stop.
 *
 * @author mrieser / Simunto GmbH
 */
public class ExecutionTracker implements AgentWaitingForPtEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private final ExecutionData data;

	private final static VehicleData DUMMY_VEHDATA = new VehicleData(null, null, null);

	@Inject
	public ExecutionTracker(ExecutionData data) {
		this.data = data;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		// store information about the current service of the transit vehicle
		this.data.vehicleData.put(event.getVehicleId(), new VehicleData(event.getTransitLineId(), event.getTransitRouteId(), event.getDepartureId()));
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		// store at what stop the transit vehicle currently is
		this.data.vehicleData.getOrDefault(event.getVehicleId(), DUMMY_VEHDATA).stopFacilityId = event.getFacilityId();
	}

	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		// if nobody entered, store the departure time as latest time
		VehicleData vehData = this.data.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			LineData line = this.data.lineData.computeIfAbsent(vehData.lineId, id -> new LineData());
			RouteData route = line.routeData.computeIfAbsent(vehData.routeId, id -> new RouteData());
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, id -> new StopData());
			DepartureData dep = stop.getOrCreate(vehData.departureId);
			dep.vehDepTime = event.getTime();
			dep.paxCountAtDeparture = vehData.currentPaxCount;
		}
	}

	@Override
	public void handleEvent(AgentWaitingForPtEvent event) {
		this.data.waitingStarttimes.put(event.getPersonId(), event.getTime());
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		VehicleData vehData = this.data.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			vehData.currentPaxCount++;
			double waitStart = this.data.waitingStarttimes.remove(event.getPersonId());
			LineData line = this.data.lineData.computeIfAbsent(vehData.lineId, id -> new LineData());
			RouteData route = line.routeData.computeIfAbsent(vehData.routeId, id -> new RouteData());
			StopData stop = route.stopData.computeIfAbsent(vehData.stopFacilityId, id -> new StopData());
			stop.getOrCreate(vehData.departureId).addWaitingPerson(waitStart);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		VehicleData vehData = this.data.vehicleData.get(event.getVehicleId());
		if (vehData != null) {
			vehData.currentPaxCount--;
		}
	}

	@Override
	public void reset(int iteration) {
		this.data.reset();
	}

}
