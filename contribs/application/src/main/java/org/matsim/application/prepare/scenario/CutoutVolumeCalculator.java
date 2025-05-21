package org.matsim.application.prepare.scenario;

import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.IntStream;

/**
 * TODO
 */
final class CutoutVolumeCalculator implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler, LinkEnterEventHandler { // TODO Remove linkEnterEventHandler

	/**
	 * Interval of the time slices in seconds
	 */
	private final double changeEventsInterval;

	/**
	 * End time of the intervals.
	 */
	private final double changeEventsMaxTime;

	/**
	 * Set of persons that are going to be removed (cutout) from the scenario
	 */
	private final Set<Id<Person>> cutoutPersons;

	/**
	 * Total and remaining volumes for each timeslice.
	 */
	private final Map<Id<Link>, Int2ObjectMap<CutoutVolume>> linkId2timeslice2volume = new HashMap<>();

	/**
	 * accumulatedLaod for each timeslice. Used in relativeAdjustmentOfCapacities and subtractLostVehiclesCapacities.
	 */
	private Map<Id<Link>, Int2DoubleOpenHashMap> linkId2timeslice2accLoad = null;

	/**
	 * Average PCE used in the calculation for the accumulated load.
	 */
	private double averagePCE;

	/**
	 * Map of vehicleId to personId updated when a person uses a new vehicle.
	 */
	private final Map<Id<Vehicle>, Id<Person>> vehicle2Person = new HashMap<>();

	// TODO DEBUG; Maybe change departTimes to vehicles, to obtain pce
	// Map of departure times of vehicles, that will be removed after the cutout
	private final Map<Id<Link>, List<Double>> staticDepartTimes = new HashMap<>();
	private final Scenario scenario;

	CutoutVolumeCalculator(double changeEventsInterval, double changeEventsMaxTime, Set<Id<Person>> cutoutPersons, Scenario scenario) {
		this.changeEventsInterval = changeEventsInterval;
		this.changeEventsMaxTime = changeEventsMaxTime;
		this.cutoutPersons = cutoutPersons;
		this.scenario = scenario;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicle2Person.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int timeslice = (int) Math.floor(event.getTime() / changeEventsInterval);

		Id<Person> personId = vehicle2Person.get(event.getVehicleId());
		if (personId == null) {
			return;
		}

		linkId2timeslice2volume.computeIfAbsent(event.getLinkId(), (k) -> new Int2ObjectOpenHashMap<>());

		CutoutVolume v = linkId2timeslice2volume.get(event.getLinkId()).computeIfAbsent(timeslice, (k) -> new CutoutVolume());
		if (cutoutPersons.contains(personId)) {
			v.cutoutVolume++;
		}

		v.totalVolume++;
	}

	/**
	 * Returns total number of vehicles, that have used (entered) this link
	 */
	public int getTotalLinkVolume(Id<Link> linkId, double time) {
		int timeslice = (int) Math.floor(time / changeEventsInterval);

		linkId2timeslice2volume.computeIfAbsent(linkId, k -> new Int2ObjectOpenHashMap<>());
		linkId2timeslice2volume.get(linkId).computeIfAbsent(timeslice, k -> new CutoutVolume());
		return linkId2timeslice2volume.get(linkId).get(timeslice).totalVolume;
	}

	/**
	 * Returns number of vehicles, that used this link which will be in the cutout-scenario
	 */
	public int getCutoutLinkVolume(Id<Link> linkId, double time) {
		int timeslice = (int) Math.floor(time / changeEventsInterval);

		linkId2timeslice2volume.computeIfAbsent(linkId, k -> new Int2ObjectOpenHashMap<>());
		linkId2timeslice2volume.get(linkId).computeIfAbsent(timeslice, k -> new CutoutVolume());
		return linkId2timeslice2volume.get(linkId).get(timeslice).cutoutVolume;
	}

	/**
	 * Returns the averaged accumulated load on this link for an averaged vehicle using the averaged accumulated load computed in {@link AveragedAccumulatedLoadCalculator}.
	 * The average waiting time can be computed by dividing the accumulated laod by average PCE.
	 * @param linkId TODO
	 * @return accumulated load in [Veh*h]
	 * @throws IllegalArgumentException when method is called with differing averagePCE values.
	 */
	public double getAccumulatedLoad(Id<Link> linkId, double time, double averagePCE) {
		if (linkId2timeslice2accLoad == null){
			linkId2timeslice2accLoad = new AveragedAccumulatedLoadCalculator(scenario.getNetwork(), staticDepartTimes, averagePCE, changeEventsMaxTime, changeEventsInterval).computeAverageAccumulatedLoadForNetwork();
			this.averagePCE = averagePCE;
		} else if (averagePCE != this.averagePCE){
			// TODO Maybe move the averagePCE variable somewhere else, since it will always be the same value
			throw new IllegalArgumentException("The averagePCE should not change by design.");
		}
		int timeslice = (int) Math.floor(time / changeEventsInterval);
		return linkId2timeslice2accLoad.get(linkId).get(timeslice);
	}

	// TODO DEBUG
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO DEBUG
		// Check if this vehicle is static traffic
		if(cutoutPersons.contains(vehicle2Person.get(event.getVehicleId()))){
			staticDepartTimes.computeIfAbsent(event.getLinkId(), k -> new ArrayList<>());
			staticDepartTimes.get(event.getLinkId()).add(event.getTime());
		}
	}

	private static final class CutoutVolume {

		int cutoutVolume = 0;
		int totalVolume = 0;

	}

	/**
	 * The purpose of this class is to encapsulate the {@link AveragedAccumulatedLoadCalculator#computeAverageAccumulatedLoadForLink} method, making sure it is used correctly.
	 */
	private static final class AveragedAccumulatedLoadCalculator {

		// General resources
		Network network;
		Map<Id<Link>, List<Double>> departTimes;
		double averagePCE;
		double changeEventsMaxTime;
		double changeEventsInterval;

		// State variables
		Object2DoubleOpenHashMap<Id<Link>> linkId2accumulatedCapacity = new Object2DoubleOpenHashMap<>();

		private AveragedAccumulatedLoadCalculator(Network network, Map<Id<Link>, List<Double>> departTimes, double averagePCE, double changeEventsMaxTime, double changeEventsInterval){
			this.network = network;
			this.departTimes = departTimes;
			this.averagePCE = averagePCE;
			this.changeEventsMaxTime = changeEventsMaxTime;
			this.changeEventsInterval = changeEventsInterval;
		}

		Map<Id<Link>, Int2DoubleOpenHashMap> computeAverageAccumulatedLoadForNetwork(){
			Map<Id<Link>, Int2DoubleOpenHashMap> linkId2Time2AccLoad = new HashMap<>();

			for (Link link : network.getLinks().values()){
				for (double time = 0; time < changeEventsMaxTime; time += changeEventsInterval) {
					// Compute the averaged load for this link and timeslice
					double accLoad = computeAverageAccumulatedLoadForLink(link.getId(), averagePCE, time, changeEventsInterval);

					int timeslice = (int) Math.floor(time / changeEventsInterval);
					linkId2Time2AccLoad.computeIfAbsent(link.getId(), k -> new Int2DoubleOpenHashMap());
					linkId2Time2AccLoad.get(link.getId()).put(timeslice, accLoad);
				}
			}

			return linkId2Time2AccLoad;
		}

		/**
		 * Computes the difference between the accumulated capacity and the needed capacity (given vehicle pce-value) integrated over time, given the vehicle arrival pattern computed by the events.
		 * It results in a value, that can be interpreted as the sum of capacity which is reserved due to waiting vehicles, also considering the burstiness of the traffic.
		 * If a new vehicle arrives it would on average wait l/pce with
		 * <ul>
		 *     <li>l: AccumulatedLoad</li>
		 *     <li>pce: average vehicle pce (of scenario)</li>
		 * </ul>
		 *
		 * <i>NOTE: There may be some unresolved edgecases for differen averagePCE value. Especially for PCE values, that reach the capacity limit. </i>
		 *
		 * @param linkId
		 * @param averagePCE
		 * @return
		 */
		private double computeAverageAccumulatedLoadForLink(Id<Link> linkId, double averagePCE, double time, double changeEventsInterval) {
			/*
				TODO ref_PCE is currently just a placeholder. Later it should contain the PCE of the vehicle (before!).
				TODO It should be replace by a map like the current departures-map and used for every iteration in the loop.
			 */
			double ref_PCE = 1;
			double ref_cap = network.getLinks().get(linkId).getCapacity();

			// Get all the departures from this link and filter them to just the current timeslice. Also add interval borders as departures
			List<Double> departures = departTimes.get(linkId).stream().filter(d -> d >= time && d < time + changeEventsInterval).toList();

			List<Double> deltas = IntStream.range(1, departures.size())
				.mapToObj(i -> departures.get(i) - departures.get(i - 1))
				.toList();

			// Get the accumulated capacity of this link, use the total capacity as initial value if acc-cap is not set yet
			linkId2accumulatedCapacity.putIfAbsent(linkId, ref_cap);
			double accumulated_capacity = linkId2accumulatedCapacity.getDouble(linkId);

			// The load respective the average vehicle PCE
			double accumulated_load = 0;
			for (int i = 1; i < deltas.size(); i++) {
				// Applying (i-1)-th event
				accumulated_capacity -= ref_PCE;
				// TODO negative values?

				// Getting the accumulated capacity, immediately after the link event has been applied (-> acc-cap at its minimum during the delta-time-interval)
				double cap_min = accumulated_capacity;

				// Restoring the link capacity
				accumulated_capacity += (deltas.get(i) / 3600) * ref_cap;
				if (accumulated_capacity > ref_cap)
					accumulated_capacity = ref_cap;

				// Getting the restored accumulated capacity
				double cap_restored = accumulated_capacity;

				// Now we check, for three different cases:
				if (cap_restored < averagePCE*time) {
					// Case 1: The acc-cap was less than the average PCE after the (i-1)-th link event. The vehicle would need to wait for the cap to restore
					accumulated_load += deltas.get(i) * ((ref_PCE - cap_restored) + ((cap_restored - cap_min) / 2));
				} else if (cap_min < averagePCE*time) {
					// Case 2: The acc-cap was partially less than the average PCE after the (i-1)-th link event. The vehicle would neet to wait, but not as long as in case 2
					accumulated_load += deltas.get(i) * ((cap_min - averagePCE) / 2);
				}
				// Case 3: The capacity was never lower than the needed average PCE so the vehicle would have no waiting time. The load is remains unchanged.
				accumulated_capacity = cap_restored;
			}

			// Now compute the restored accumulated capacitiy at the end of the timeslice
			// If there are no departs, then refresh the accumulated capacity for the whole timeslice
			if (!departures.isEmpty()) {
				double delta_end = time + changeEventsInterval - departures.getLast();
				accumulated_capacity += (delta_end / 3600) * ref_cap;
			} else {
				accumulated_capacity += (changeEventsInterval / 3600) * ref_cap;
			}
			if (accumulated_capacity > ref_cap)
				accumulated_capacity = ref_cap;

			linkId2accumulatedCapacity.put(linkId, accumulated_capacity);
			return accumulated_load;
		}
	}
}
