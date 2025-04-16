package org.matsim.application.prepare.scenario;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
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
	private final double interval;

	/**
	 * Set of persons that are going to be removed (cutout) from the scenario
	 */
	private final Set<Id<Person>> cutoutPersons;

	private final Map<Id<Link>, Int2ObjectMap<CutoutVolume>> linkId2timeslice2volume = new HashMap<>();

	private final Map<Id<Vehicle>, Id<Person>> vehicle2Person = new HashMap<>();

	// TODO DEBUG; MAybe change departTimes to vehicles, to obtain pce
	private final Map<Id<Link>, List<Double>> departTimes = new HashMap<>();
	private final Scenario scenario;

	CutoutVolumeCalculator(double interval, Set<Id<Person>> cutoutPersons, Scenario scenario) {
		this.interval = interval;
		this.cutoutPersons = cutoutPersons;
		this.scenario = scenario;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicle2Person.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int timeslice = (int) Math.floor(event.getTime() / interval);

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
		int timeslice = (int) Math.floor(time / interval);

		linkId2timeslice2volume.computeIfAbsent(linkId, k -> new Int2ObjectOpenHashMap<>());
		linkId2timeslice2volume.get(linkId).computeIfAbsent(timeslice, k -> new CutoutVolume());
		return linkId2timeslice2volume.get(linkId).get(timeslice).totalVolume;
	}

	/**
	 * Returns number of vehicles, that used this link which will be in the cutout-scenario
	 */
	public int getCutoutLinkVolume(Id<Link> linkId, double time) {
		int timeslice = (int) Math.floor(time / interval);

		linkId2timeslice2volume.computeIfAbsent(linkId, k -> new Int2ObjectOpenHashMap<>());
		linkId2timeslice2volume.get(linkId).computeIfAbsent(timeslice, k -> new CutoutVolume());
		return linkId2timeslice2volume.get(linkId).get(timeslice).cutoutVolume;
	}

	/**
	 * Calculates the estimated waiting time for a vehicle on a link
	 * @param pce PCE of the vehicle
	 * @param lastTime time last vehicle entered the link
	 * @param lastPCE PCE of the last vehicle that entered the link
	 * @param capacity of the link
	 * @return estimated waiting time
	 */
	private double calculateWaitingTime(double currentTime, double pce, double lastTime, double lastPCE, double capacity){
		double timeDelta = currentTime - lastTime;
		if (timeDelta < 0)
			throw new RuntimeException("Time delta is negative, implicating wrong order of vehicles!"); // TODO Write error msg

		//Checks if the link ist sill in "cooldown" from last vehicle, if not, then the vehicle can continue immediately
		if (timeDelta > 3600*(lastPCE/capacity)) return 0;

		// If the link is still in "cooldown" we calculate the waiting time, by calculating the remaining cooldown-time
		return (3600*(lastPCE/capacity))-timeDelta;
		// TODO Calculate "overusage", which "carries" the usage along
	}

	/* TODO
		1: Skip pt
		2: Check for multiple lanes
		3: Check for bikes
		4: Other possible edgecases?
	*/

	/**
	 * This method derives the waiting times for each vehicle - that had been on this link - using the information given from the events.
	 * If all waiting times are =0, then the capacity had not been used fully, meaning that the travel times were not influenced.
	 */
	public double getTotalEstimatedLinkWaitingTime(Id<Link> linkId){
		double capacity = scenario.getNetwork().getLinks().get(linkId).getCapacity();
		List<Double> departures = departTimes.get(linkId);

		/*List<Double> deltas = IntStream.range(1, departures.size())
			.mapToObj(i -> departures.get(i) - departures.get(i - 1))
			.toList();*/

		double totalWaitingTime = 0;
		for(int i = 1; i < departures.size(); i++){
			totalWaitingTime += calculateWaitingTime(departures.get(i), 1, departures.get(i-1), 1, capacity);
		}
		return totalWaitingTime;
	}

	/**
	 * Computes the difference between the average accumulated capacity and the total capacity integrated over time of this link, given the vehicle arrival pattern computed by the events.
	 * It results in a value, that can be interpreted as the sum of capacity which is reserved due to waiting vehicles considering the burstiness of the traffic.
	 * If a new vehicle arrives it would on average wait the AveragedAccumulatedLoad divided by the PCE of the vehicle.
	 * @param linkId TODO
	 * @return TODO
	 */
	public double getAveragedAccumulatedLoad(Id<Link> linkId){
		// TODO This is currently just a placeholder. Later it should contain the PCE of the vehicle (before!)
		double ref_PCE = 1;

		double capacity = scenario.getNetwork().getLinks().get(linkId).getCapacity();
		List<Double> departures = departTimes.get(linkId);

		List<Double> deltas = IntStream.range(1, departures.size())
			.mapToObj(i -> departures.get(i) - departures.get(i - 1))
			.toList();

		double accumulated_load = 0;
		for(int i = 0; i < deltas.size(); i++){
			if(deltas.get(i) > ref_PCE/capacity)
				return capacity/2;

			accumulated_load += (deltas.get(i)*capacity)/2 + (capacity-ref_PCE+deltas.get(i)*capacity)*deltas.get(i); // TODO Check this formula
		}

		return accumulated_load;
	}

	// TODO DEBUG
	private double getLinkBurstiness(Id<Link> linkId){
		List<Double> departures = departTimes.get(linkId);

		List<Double> deltas = IntStream.range(1, departures.size())
			.mapToObj(i -> departures.get(i) - departures.get(i - 1))
			.toList();

		List<Boolean> bursts = deltas.stream().map(d -> d < (1/scenario.getNetwork().getLinks().get(linkId).getCapacity())*3600).toList();

//		List<Boolean>

		double avg = departures.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
		double avgDev = departures.stream().mapToDouble(num -> Math.abs(num - avg)).average().orElse(0.0);

		return Math.pow(avgDev, 2)/departures.size();
	}

	// TODO DEBUG
	@Override
	public void handleEvent(LinkEnterEvent event) {
		// TODO DEBUG
		{
			departTimes.computeIfAbsent(event.getLinkId(), k -> new ArrayList<>());
			departTimes.get(event.getLinkId()).add(event.getTime());
		}
	}

	private static final class CutoutVolume {

		int cutoutVolume = 0;
		int totalVolume = 0;

	}

}
