package ch.sbb.matsim.contrib.railsim.prototype;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;

/**
 * Note: These statistics refer to the MATSim transit vehicles, which in our current implementation is interpreted as the front of the train path.
 *
 * @author Ihab Kaddoura
 */
public class TrainStatistics implements LinkEnterEventHandler, LinkLeaveEventHandler, TransitDriverStartsEventHandler, VehicleArrivesAtFacilityEventHandler {
	private static final Logger log = LogManager.getLogger(TrainStatistics.class);

	@Inject
	private Scenario scenario;

	// the following maps are required to compute the length of the reserved train path
	private final HashMap<Id<Vehicle>, Double> vehicleId2speedOnPreviousLink = new HashMap<>();
	private final HashMap<Id<Vehicle>, Double> vehicleId2lastLinkEnterTime = new HashMap<>();
	private final HashMap<Id<Vehicle>, Id<Link>> vehicleId2previousLink = new HashMap<>();

	private final HashMap<Id<Vehicle>, Id<TransitLine>> vehicleId2currentTransitLine = new HashMap<>();
	private final HashMap<Id<Vehicle>, Id<TransitRoute>> vehicleId2currentTransitRoute = new HashMap<>();
	private final HashMap<Id<Link>, TransitStopFacility> link2stop = new HashMap<>();
	private final HashMap<Id<TransitStopFacility>, Id<Link>> stop2link = new HashMap<>();

	private final HashMap<Id<Vehicle>, Id<TransitStopFacility>> vehicle2lastStop = new HashMap<>();
	private final HashMap<Id<Vehicle>, Id<TransitStopFacility>> vehicle2nextStop = new HashMap<>();

	private final HashMap<Id<Vehicle>, Id<Link>> vehicle2lastStopLink = new HashMap<>();

	@Override
	public void reset(int iteration) {

		for (TransitStopFacility facility : this.scenario.getTransitSchedule().getFacilities().values()) {
			if (this.link2stop.containsKey(facility.getLinkId())) {
				log.warn("transitStopFacility: " + facility.getId() + " 1st link: " + link2stop.get(facility.getLinkId()));
				log.warn("transitStopFacility: " + facility.getId() + " 2nd link: " + facility.getLinkId());
				throw new RuntimeException("A transit stop facility is located on more than one link. Aborting...");
			}
			this.link2stop.put(facility.getLinkId(), facility);
			this.stop2link.put(facility.getId(), facility.getLinkId());
		}

		if (iteration > 0) throw new RuntimeException("Running more than 1 iteration. Aborting...");
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.vehicleId2lastLinkEnterTime.put(event.getVehicleId(), event.getTime());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (vehicleId2lastLinkEnterTime.get(event.getVehicleId()) == null) {
			// At the very beginning vehicles are 'put' on the link without entering...
			this.vehicleId2speedOnPreviousLink.put(event.getVehicleId(), null);

		} else {
			double t = event.getTime() - this.vehicleId2lastLinkEnterTime.get(event.getVehicleId());
			double s = this.scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
			double v = s / t;
			this.vehicleId2speedOnPreviousLink.put(event.getVehicleId(), v);
		}

		this.vehicleId2previousLink.put(event.getVehicleId(), event.getLinkId());
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.vehicleId2currentTransitLine.put(event.getVehicleId(), event.getTransitLineId());
		this.vehicleId2currentTransitRoute.put(event.getVehicleId(), event.getTransitRouteId());
	}

	/**
	 * @return the speed on the previous link.
	 * If there is no information about the previous link or if the vehicle was stopping at the previous link, the speed on the previous link is 0.
	 */
	public double getSpeedOnPreviousLink(Id<Vehicle> vehicleId) {

		if (vehicleId2speedOnPreviousLink.get(vehicleId) == null) {
			// There is no speed stored for the vehicle, probably the beginning of the transit route.
			return 0.;
		} else {

			if (this.vehicle2lastStop.get(vehicleId) == null) {
				throw new RuntimeException("A vehicle should always have a last stop. Aborting...");
			}

			if (this.vehicle2lastStopLink.get(vehicleId).toString().equals(this.vehicleId2previousLink.get(vehicleId).toString())) {
				// the vehicle was stopping at the previous link
				return 0.;
			}

			return vehicleId2speedOnPreviousLink.get(vehicleId);

		}
	}

	/**
	 * @return the vehicleId2lastLinkEnterTime
	 */
	public HashMap<Id<Vehicle>, Double> getVehicleId2lastLinkEnterTime() {
		return vehicleId2lastLinkEnterTime;
	}

	/**
	 * @return the vehicleId2currentTransitLine
	 */
	public HashMap<Id<Vehicle>, Id<TransitLine>> getVehicleId2currentTransitLine() {
		return vehicleId2currentTransitLine;
	}

	/**
	 * @return the vehicleId2currentTransitRoute
	 */
	public HashMap<Id<Vehicle>, Id<TransitRoute>> getVehicleId2currentTransitRoute() {
		return vehicleId2currentTransitRoute;
	}

	/**
	 * @return the link2stop
	 */
	public HashMap<Id<Link>, TransitStopFacility> getLink2stop() {
		return link2stop;
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.vehicle2lastStop.put(event.getVehicleId(), event.getFacilityId());
		this.vehicle2lastStopLink.put(event.getVehicleId(), this.stop2link.get(event.getFacilityId()));

		// find next stop
		Id<TransitLine> lineId = this.vehicleId2currentTransitLine.get(event.getVehicleId());
		Id<TransitRoute> routeId = this.vehicleId2currentTransitRoute.get(event.getVehicleId());

		List<TransitRouteStop> stops = this.scenario.getTransitSchedule().getTransitLines().get(lineId).getRoutes().get(routeId).getStops();
		int index = 0;
		for (TransitRouteStop stop : stops) {
			if (stop.getStopFacility().getId().toString().equals(event.getFacilityId().toString())) {
				break;
			}
			index++;
		}

		int indexNextStop = index + 1;
		TransitRouteStop nextStop = null;
		if (stops.size() == indexNextStop) {
			// final stop
		} else {
			nextStop = stops.get(indexNextStop);
		}
		if (nextStop == null) {
			this.vehicle2nextStop.put(event.getVehicleId(), null);
		} else {
			this.vehicle2nextStop.put(event.getVehicleId(), nextStop.getStopFacility().getId());
		}
	}

	/**
	 * @return the vehicle2lastStop
	 */
	public HashMap<Id<Vehicle>, Id<TransitStopFacility>> getVehicle2lastStop() {
		return vehicle2lastStop;
	}

	/**
	 * @return the vehicle2nextStop
	 */
	public HashMap<Id<Vehicle>, Id<TransitStopFacility>> getVehicle2nextStop() {
		return vehicle2nextStop;
	}


}
