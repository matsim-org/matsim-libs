package playground.mzilske.bvg09;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitRouteImpl;
import org.matsim.transitSchedule.api.Departure;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.BasicVehicle;
import org.matsim.vehicles.BasicVehicleCapacity;
import org.matsim.vehicles.BasicVehicleType;
import org.matsim.vehicles.BasicVehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.LineRouteItem;
import org.matsim.visum.VisumNetwork.VehicleCombination;
import org.matsim.visum.VisumNetwork.VehicleUnit;


public class Visum2TransitSchedule {

	private static final Logger log = Logger.getLogger(Visum2TransitSchedule.class);

	private final VisumNetwork visum;
	private final TransitSchedule schedule;
	private final BasicVehicles vehicles;
	private final CoordinateTransformation coordinateTransformation = new IdentityTransformation();
	private final Map<String, TransportMode> transportModes = new HashMap<String, TransportMode>();
	private final BasicVehicleType defaultVehicleType;
	private final TransitScheduleFactory builder;
	private Map<Id, TransitStopFacility> stopFacilities;
	private long nextVehicleId;
	private Collection<String> transitLineFilter = null;
	
	public Visum2TransitSchedule(final VisumNetwork visum, final TransitSchedule schedule, final BasicVehicles vehicles) {
		this.visum = visum;
		this.schedule = schedule;
		this.vehicles = vehicles;
		if (!vehicles.getVehicleTypes().isEmpty()) {
			this.defaultVehicleType = vehicles.getVehicleTypes().entrySet().iterator().next().getValue();
		} else {
			this.defaultVehicleType = null;
		}
		builder = this.schedule.getFactory();
	}

	public void registerTransportMode(final String visumTransportMode, final TransportMode transportMode) {
		this.transportModes.put(visumTransportMode, transportMode);
	}

	public void convert() {
		convertVehicleTypes();
		stopFacilities = convertStopPoints(builder);
		convertLines();
	}

	private long convertLines() {
		nextVehicleId = 0;
		for (VisumNetwork.TransitLine line : this.visum.lines.values()){
			TransitLine tLine = builder.createTransitLine(line.id);
			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()) {
				if (timeProfile.lineName.equals(line.id)) {
					TransitRoute tRoute = convertRouteProfile(line, timeProfile);
					BasicVehicleType vehType = determineVehicleType(line, timeProfile);
					convertDepartures(line, timeProfile, vehType, tRoute);
					tLine.addRoute(tRoute);
				}
			}
			if (tLine.getRoutes().size() > 0) {
				boolean keepPlan = isThisLineInRegionOfInterest(tLine);
				if(keepPlan){
					this.schedule.addTransitLine(tLine);
				} else {
					log.warn("The line " + tLine.getId() + " was not added to the transit schedule because it does not serve stops within m44/344 area.");
				}
			} else {
				log.warn("The line " + tLine.getId() + " was not added to the transit schedule because it does not contain any routes.");
			}
		}
		return nextVehicleId;
	}

	private boolean isThisLineInRegionOfInterest(TransitLine transitLine) {
		if (transitLineFilter != null) {
			if (transitLineFilter.contains(transitLine.getId().toString())) {
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	private BasicVehicleType determineVehicleType(
			VisumNetwork.TransitLine line, VisumNetwork.TimeProfile timeProfile) {
		BasicVehicleType vehType;
		VisumNetwork.VehicleCombination vehCombination = this.visum.vehicleCombinations.get(timeProfile.vehCombNr);
		if (vehCombination == null) {
			vehCombination = this.visum.vehicleCombinations.get(line.vehCombNo);
		}
		if (vehCombination == null) {
			vehType = defaultVehicleType;
			log.warn("Could not find vehicle combination with id=" + timeProfile.vehCombNr + " used in line " + line.id.toString() + ". Using the first vehicle type available!");				
		} else {
			vehType = this.vehicles.getVehicleTypes().get(new IdImpl(vehCombination.id));
		}
		return vehType;
	}

	private void convertDepartures(VisumNetwork.TransitLine line,
			VisumNetwork.TimeProfile timeProfile, BasicVehicleType vehType,
			TransitRoute tRoute) {
		for (VisumNetwork.Departure d : this.visum.departures.values()){
			if (d.lineName.equals(line.id.toString()) && d.lineRouteName.equals(timeProfile.lineRouteName.toString()) && d.TRI.equals(timeProfile.index.toString())) {
				Departure departure = builder.createDeparture(new IdImpl(d.index), Time.parseTime(d.dep));
				BasicVehicle veh = this.vehicles.getFactory().createVehicle(new IdImpl("tr_" + nextVehicleId++), vehType);
				this.vehicles.getVehicles().put(veh.getId(), veh);
				departure.setVehicleId(veh.getId());
				tRoute.addDeparture(departure);
			}
		}
	}

	private TransitRoute convertRouteProfile(VisumNetwork.TransitLine line,
			VisumNetwork.TimeProfile timeProfile) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
			if (tpi.lineName.equals(line.id.toString()) && tpi.lineRouteName.equals(timeProfile.lineRouteName.toString()) && tpi.timeProfileName.equals(timeProfile.index.toString()) && tpi.DCode.equals(timeProfile.DCode.toString())){
				String lineRouteItemKey = line.id.toString() +"/"+ timeProfile.lineRouteName.toString()+"/"+ tpi.lRIIndex.toString()+"/"+tpi.DCode;
				LineRouteItem lineRouteItem = this.visum.lineRouteItems.get(lineRouteItemKey);
				Id stopFacilityId = lineRouteItem.stopPointNo;
				if (stopFacilityId != null) {
					TransitStopFacility stopFacility = stopFacilities.get(stopFacilityId);
					if (stopFacility == null) {
						throw new RuntimeException();
					}
					TransitRouteStop s = builder.createTransitRouteStop(stopFacility,Time.parseTime(tpi.arr),Time.parseTime(tpi.dep));
					s.setAwaitDepartureTime(true);
					stops.add(s);
				} // else, this TimeProfileItem corresponds to a LineRouteItem without a stop.
			}
		}
		TransportMode mode = this.transportModes.get(line.tCode);
		if (mode == null) {
			log.error("Could not find TransportMode for " + line.tCode + ", more info: " + line.id);
		}
		TransitRouteImpl tRoute = (TransitRouteImpl) builder.createTransitRoute(new IdImpl(timeProfile.lineName.toString()+"."+timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()+"."+timeProfile.DCode.toString()),null,stops,mode);
		tRoute.setLineRouteName(timeProfile.lineRouteName.toString());
		tRoute.setDirection(timeProfile.DCode.toString());
		return tRoute;
	}

	private Map<Id, TransitStopFacility> convertStopPoints(
			TransitScheduleFactory builder) {
		final Map<Id, TransitStopFacility> stopFacilities = new TreeMap<Id, TransitStopFacility>();

		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			Coord coord = this.coordinateTransformation.transform(this.visum.stops.get(this.visum.stopAreas.get(stopPoint.stopAreaId).StopId).coord);
			TransitStopFacility stop = builder.createTransitStopFacility(stopPoint.id, coord, false);
			stopFacilities.put(stopPoint.id, stop);
			this.schedule.addStopFacility(stop);
		}
		return stopFacilities;
	}

	private void convertVehicleTypes() {
		VehiclesFactory vb = this.vehicles.getFactory();
		for (VehicleCombination vehComb : this.visum.vehicleCombinations.values()) {
			BasicVehicleType type = vb.createVehicleType(new IdImpl(vehComb.id));
			type.setDescription(vehComb.name);
			BasicVehicleCapacity capacity = vb.createVehicleCapacity();
			VehicleUnit vu = this.visum.vehicleUnits.get(vehComb.vehUnitId);
			capacity.setSeats(Integer.valueOf(vehComb.numOfVehicles * vu.seats));
			capacity.setStandingRoom(Integer.valueOf(vehComb.numOfVehicles * (vu.passengerCapacity - vu.seats)));
			type.setCapacity(capacity);
			this.vehicles.getVehicleTypes().put(type.getId(), type);
		}
	}

	public Collection<String> getTransitLineFilter() {
		return transitLineFilter;
	}

	public void setTransitLineFilter(Collection<String> transitLineFilter) {
		this.transitLineFilter = transitLineFilter;
	}
	
}


