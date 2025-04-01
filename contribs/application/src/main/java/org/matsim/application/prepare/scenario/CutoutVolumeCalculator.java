package org.matsim.application.prepare.scenario;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.ArrayMap;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * TODO
 */
final class CutoutVolumeCalculator implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler {

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

	CutoutVolumeCalculator(double interval, Set<Id<Person>> cutoutPersons) {
		this.interval = interval;
		this.cutoutPersons = cutoutPersons;
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
	public int getTotalLinkUsage(Id<Link> linkId, double time) {
		int timeslice = (int) Math.floor(time / interval);

		linkId2timeslice2listOfVehIds.putIfAbsent(linkId, new ArrayMap<>());
		linkId2timeslice2listOfVehIds.get(linkId).putIfAbsent(timeslice, new ArrayList<>());
		return linkId2timeslice2listOfVehIds.get(linkId).get(timeslice).size();
	}

	/**
	 * Returns number of vehicles, that used this link which will be in the cutout-scenario
	 */
	public int getCutoutLinkUsage(Id<Link> linkId, double time) {
		int timeslice = (int) Math.floor(time / interval);

		linkId2timeslice2listOfVehIds.putIfAbsent(linkId, new ArrayMap<>());
		linkId2timeslice2listOfVehIds.get(linkId).putIfAbsent(timeslice, new ArrayList<>());
		return (int) linkId2timeslice2listOfVehIds
			.get(linkId)
			.get(timeslice)
			.stream()
			.filter(id -> scenario.getVehicles().getVehicles().containsKey(id))
			.count();
	}

	private static final class CutoutVolume {

		int cutoutVolume = 0;
		int totalVolume = 0;

	}

}
