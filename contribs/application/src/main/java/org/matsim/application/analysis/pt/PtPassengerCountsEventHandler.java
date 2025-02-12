package org.matsim.application.analysis.pt;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import java.util.HashMap;
import java.util.Map;

/**
 * Event handler to count the number of passengers using public transport services differentiated by vehicle type.
 * Passenger will be counted once per vehicle type per trip.
 */
public class PtPassengerCountsEventHandler implements PersonEntersVehicleEventHandler, TransitDriverStartsEventHandler, ActivityEndEventHandler {

	private final TransitSchedule schedule;
	private final Vehicles vehicles;

	/**
	 * All driver ids.
	 */
	private final IntSet drivers = new IntOpenHashSet();

	/**
	 * Store which passengers have been counted for each vehicle type.
	 */
	private final Map<Id<VehicleType>, IntSet> byVehicle = new HashMap<>();

	/**
	 * Store mapping of vehicle to agency.
	 */
	private final Map<Id<Vehicle>, String> vehicleToAgency = new HashMap<>();

	/**
	 * Counts per vehicle type and hour.
	 */
	private final Int2ObjectMap<Object2IntMap<Id<VehicleType>>> counts = new Int2ObjectAVLTreeMap<>();

	/**
	 * Counts per vehicle type and hour.
	 */
	private final Int2ObjectMap<Object2IntMap<AgencyVehicleType>> agencyCounts = new Int2ObjectAVLTreeMap<>();

	public PtPassengerCountsEventHandler(TransitSchedule schedule, Vehicles vehicles) {
		this.schedule = schedule;
		this.vehicles = vehicles;
	}

	/**
	 * Map containing the counts of passengers per vehicle type.
	 */
	public Int2ObjectMap<Object2IntMap<Id<VehicleType>>> getCounts() {
		return counts;
	}

	/**
	 * Map containing the counts of passengers per agency and vehicle type.
	 * May be empty if the agency attribute is not used.
	 */
	public Int2ObjectMap<Object2IntMap<AgencyVehicleType>> getAgencyCounts() {
		return agencyCounts;
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		drivers.add(event.getDriverId().index());

		TransitLine line = schedule.getTransitLines().get(event.getTransitLineId());
		if (line != null) {
			Object gtfsAgencyId = line.getAttributes().getAttribute("gtfs_agency_id");
			if (gtfsAgencyId != null)
				vehicleToAgency.put(event.getVehicleId(), gtfsAgencyId.toString());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {

		Id<Vehicle> vehicleId = event.getVehicleId();
		Vehicle vehicle = vehicles.getVehicles().get(vehicleId);

		if (vehicle == null)
			return;

		// Don't count drivers as passengers
		if (drivers.contains(event.getPersonId().index()))
			return;

		VehicleType vehicleType = vehicle.getType();
		Id<VehicleType> id = vehicleType.getId();

		IntSet counted = byVehicle.computeIfAbsent(id, k -> new IntOpenHashSet());

		// Count passenger only once per vehicle type per trip
		if (!counted.contains(event.getPersonId().index())) {
			counted.add(event.getPersonId().index());

			int hour = (int) (event.getTime() / 3600);

			counts.computeIfAbsent(hour, k -> new Object2IntAVLTreeMap<>()).merge(id, 1, Integer::sum);

			String agency = vehicleToAgency.get(vehicleId);
			if (agency != null) {
				agencyCounts.computeIfAbsent(hour, k -> new Object2IntAVLTreeMap<>()).merge(new AgencyVehicleType(agency, id), 1, Integer::sum);
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {

		if (TripStructureUtils.isStageActivityType(event.getActType()))
			return;

		// When trip is finished, a passenger can be counted again.
		byVehicle.values().forEach(p -> p.remove(event.getPersonId().index()));
	}

	@Override
	public void reset(int iteration) {
		byVehicle.clear();
		counts.clear();
		agencyCounts.clear();
		drivers.clear();
	}


	public record AgencyVehicleType(String agency, Id<VehicleType> vehicleType) implements Comparable<AgencyVehicleType> {

		@Override
		public int compareTo(AgencyVehicleType o) {
			int cmp = agency.compareTo(o.agency);
			if (cmp == 0)
				cmp = vehicleType.compareTo(o.vehicleType);
			return cmp;
		}
	}
}
