package playground.vsp.andreas.mzilske.bvg09;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitRouteImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;
import org.matsim.visum.VisumNetwork;
import org.matsim.visum.VisumNetwork.LineRouteItem;
import org.matsim.visum.VisumNetwork.VehicleCombination;
import org.matsim.visum.VisumNetwork.VehicleUnit;


public class Visum2TransitSchedule {

	private static final Logger log = Logger.getLogger(Visum2TransitSchedule.class);

	private final VisumNetwork visum;
	private final TransitSchedule schedule;
	private final Vehicles vehicles;
	private final CoordinateTransformation coordinateTransformation = new IdentityTransformation();
	private final Map<String, String> transportModes = new HashMap<String, String>();
	private final VehicleType defaultVehicleType;
	private final TransitScheduleFactory builder;
	private Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities;
	private long nextVehicleId;
	private Collection<String> transitLineFilter = null;

	public Visum2TransitSchedule(final VisumNetwork visum, final TransitSchedule schedule, final Vehicles vehicles) {
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

	public void registerTransportMode(final String visumTransportMode, final String transportMode) {
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
			TransitLine tLine = builder.createTransitLine(Id.create(line.id, TransitLine.class));
			for (VisumNetwork.TimeProfile timeProfile : this.visum.timeProfiles.values()) {
				if (timeProfile.lineName.equals(line.id)) {
					TransitRoute tRoute = convertRouteProfile(line, timeProfile);
					VehicleType vehType = determineVehicleType(line, timeProfile);
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

	private VehicleType determineVehicleType(
			VisumNetwork.TransitLine line, VisumNetwork.TimeProfile timeProfile) {
		VehicleType vehType;
		VisumNetwork.VehicleCombination vehCombination = this.visum.vehicleCombinations.get(timeProfile.vehCombNr);
		if (vehCombination == null) {
			vehCombination = this.visum.vehicleCombinations.get(line.vehCombNo);
		}
		if (vehCombination == null) {
			log.warn("Could not find vehicle combination with id=" + timeProfile.vehCombNr + " used in line " + line.id.toString() + ". Using default type " + line.tCode + " vehicle.");
			vehType = this.vehicles.getVehicleTypes().get(Id.create(line.tCode, VehicleType.class));

			if(vehType == null){
				vehType = defaultVehicleType;
				log.warn("Could not find default vehicle for line type " + line.tCode + " used in line " + line.id.toString() + ". Using the first vehicle type available!");
			}

		} else {
			vehType = this.vehicles.getVehicleTypes().get(Id.create(vehCombination.id, VehicleType.class));
		}
		return vehType;
	}

	private void convertDepartures(VisumNetwork.TransitLine line,
			VisumNetwork.TimeProfile timeProfile, VehicleType vehType,
			TransitRoute tRoute) {
		for (VisumNetwork.Departure d : this.visum.departures.values()){
			if (d.lineName.equals(line.id.toString()) && d.lineRouteName.equals(timeProfile.lineRouteName.toString()) && d.TRI.equals(timeProfile.index.toString())) {
				Departure departure = builder.createDeparture(Id.create(d.index, Departure.class), Time.parseTime(d.dep));
				Vehicle veh = this.vehicles.getFactory().createVehicle(Id.create("tr_" + nextVehicleId++, Vehicle.class), vehType);
				this.vehicles.addVehicle( veh);
				departure.setVehicleId(veh.getId());
				tRoute.addDeparture(departure);
			}
		}
	}

	private TransitRoute convertRouteProfile(VisumNetwork.TransitLine line,
			VisumNetwork.TimeProfile timeProfile) {
		List<TransitRouteStop> stops = new ArrayList<TransitRouteStop>();
		for (VisumNetwork.TimeProfileItem tpi : this.visum.timeProfileItems.values()){
			if (tpi.lineName.equals(line.id.toString()) && tpi.lineRouteName.equals(timeProfile.lineRouteName.toString()) && tpi.timeProfileName.equals(timeProfile.index.toString()) && tpi.dirCode.equals(timeProfile.dirCode.toString())){
				String lineRouteItemKey = line.id.toString() +"/"+ timeProfile.lineRouteName.toString()+"/"+ tpi.lRIIndex.toString()+"/"+tpi.dirCode;
				LineRouteItem lineRouteItem = this.visum.lineRouteItems.get(lineRouteItemKey);
				Id<TransitStopFacility> stopFacilityId = Id.create(lineRouteItem.stopPointNo, TransitStopFacility.class);
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
		String mode = this.transportModes.get(line.tCode);
		if (mode == null) {
			log.error("Could not find TransportMode for " + line.tCode + ", more info: " + line.id);
		}
		TransitRouteImpl tRoute = (TransitRouteImpl) builder.createTransitRoute(Id.create(timeProfile.lineName.toString()+"."+timeProfile.lineRouteName.toString()+"."+ timeProfile.index.toString()+"."+timeProfile.dirCode.toString(), TransitRoute.class),null,stops,mode);
//		tRoute.setLineRouteName(timeProfile.lineRouteName.toString());
		tRoute.getAttributes().putAttribute("lineRouteName",timeProfile.lineRouteName.toString());
		tRoute.setDirection(timeProfile.dirCode.toString());
		return tRoute;
	}

	private Map<Id<TransitStopFacility>, TransitStopFacility> convertStopPoints(
			TransitScheduleFactory builder) {
		final Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = new TreeMap<>();

		for (VisumNetwork.StopPoint stopPoint : this.visum.stopPoints.values()){
			Coord coord = this.coordinateTransformation.transform(this.visum.stops.get(this.visum.stopAreas.get(stopPoint.stopAreaId).StopId).coord);
			TransitStopFacility stop = builder.createTransitStopFacility(Id.create(stopPoint.id, TransitStopFacility.class), coord, false);
			stopFacilities.put(Id.create(stopPoint.id, TransitStopFacility.class), stop);
			this.schedule.addStopFacility(stop);
		}
		return stopFacilities;
	}

	private void convertVehicleTypes() {
		VehiclesFactory vb = this.vehicles.getFactory();
		for (VehicleCombination vehComb : this.visum.vehicleCombinations.values()) {
			VehicleType type = vb.createVehicleType(Id.create(vehComb.id, VehicleType.class));
			type.setDescription(vehComb.name);
			VehicleCapacity capacity = vb.createVehicleCapacity();
			VehicleUnit vu = this.visum.vehicleUnits.get(vehComb.vehUnitId);
			capacity.setSeats(Integer.valueOf(vehComb.numOfVehicles * vu.seats));
			capacity.setStandingRoom(Integer.valueOf(vehComb.numOfVehicles * (vu.passengerCapacity - vu.seats)));
			type.setCapacity(capacity);
			this.vehicles.addVehicleType(type);
		}
		

		// add default types
		for (Entry<String, VehicleType> entry : DefaultVehTypes.getDefaultVehicleTypes().entrySet()) {
			this.vehicles.addVehicleType( entry.getValue());
		}
	}

	public Collection<String> getTransitLineFilter() {
		return transitLineFilter;
	}

	public void setTransitLineFilter(Collection<String> transitLineFilter) {
		this.transitLineFilter = transitLineFilter;
	}

}


