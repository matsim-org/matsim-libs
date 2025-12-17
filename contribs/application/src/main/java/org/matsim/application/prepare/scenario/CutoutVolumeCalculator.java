package org.matsim.application.prepare.scenario;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;

import java.io.IOException;
import java.util.*;

/**
 * This class computes a {@link CutoutVolume} for each link and time-interval. The volume represents the total amount of used capacity. <br>
 * The {@link  CutoutVolume} contains a total volume and a cutout volume. The cutout volume represents the volume created only by vehicles/agents,
 * that will stay in the simulation after completing the cutout. The totalVolume represents the volume of all vehicles.
 */
public class CutoutVolumeCalculator implements LinkLeaveEventHandler, PersonEntersVehicleEventHandler {

	private static final class CutoutVolume {
		int cutoutVolume = 0;
		int totalVolume = 0;
	}

	private static final Logger log = LogManager.getLogger(CutoutVolumeCalculator.class);

	private final Scenario scenario;

	/**
	 * Interval of the time slices in seconds
	 */
	private final double changeEventsInterval;

	/**
	 * Set of persons that are going to be removed (cutout) from the scenario
	 */
	private final Set<Id<Person>> cutoutPersons;

	/**
	 * Total and remaining volumes for each timeslice.
	 */
	private final Map<Id<Link>, Int2ObjectMap<CutoutVolume>> linkId2timeslice2volume = new HashMap<>();

	/**
	 * Map of vehicleId to personId updated when a person uses a new vehicle.
	 */
	private final Map<Id<Vehicle>, Id<Person>> vehicleId2Person = new HashMap<>();

	private int vehIdNotFoundWarnings = 0;

	/**
	 * @param changeEventsInterval Interval at which changeEvents are printed in seconds
	 * @param cutoutPersonIds Set of id of persons that will stay in the cutout-scenario
	 */
	CutoutVolumeCalculator(Scenario scenario, double changeEventsInterval, Set<Id<Person>> cutoutPersonIds) {
		this.scenario = scenario;
		this.changeEventsInterval = changeEventsInterval;
		this.cutoutPersons = cutoutPersonIds;
	}

	private int getTimeSlice(double time){
		return (int) Math.floor(time / changeEventsInterval);
	}

	/**
	 * Returns total number of vehicles, that have used (entered) this link.
	 */
	public int getTotalLinkVolume(Id<Link> linkId, double time) {
		int timeslice = getTimeSlice(time);

		linkId2timeslice2volume.computeIfAbsent(linkId, k -> new Int2ObjectOpenHashMap<>());
		linkId2timeslice2volume.get(linkId).computeIfAbsent(timeslice, k -> new CutoutVolume());
		return linkId2timeslice2volume.get(linkId).get(timeslice).totalVolume;
	}

	/**
	 * Returns number of vehicles, that used this link which will be in the cutout-scenario
	 */
	public int getCutoutLinkVolume(Id<Link> linkId, double time) {
		int timeslice = getTimeSlice(time);

		linkId2timeslice2volume.computeIfAbsent(linkId, k -> new Int2ObjectOpenHashMap<>());
		linkId2timeslice2volume.get(linkId).computeIfAbsent(timeslice, k -> new CutoutVolume());
		return linkId2timeslice2volume.get(linkId).get(timeslice).cutoutVolume;
	}

	// TODO Remove for final commit, this is just for testing/validating
	/// Prints out random links with different types
	public void printSample(String path) throws IOException {
		/*
		4556354 -> highway, secondary
		-4553259 -> highway, tertiary
		-28225794 -> highway, residential
		4764111 -> highway, motorway
		-1103167081 -> highway, unclassified
		 */

		Set<Id<Link>> selectedIds = HashSet.newHashSet(5);
		selectedIds.add(Id.createLinkId("4556354"));
		selectedIds.add(Id.createLinkId("-4553259"));
		selectedIds.add(Id.createLinkId("-28225794"));
		selectedIds.add(Id.createLinkId("4764111"));
		selectedIds.add(Id.createLinkId("-1103167081"));

		List<Id<Link>> links = scenario.getNetwork().getLinks().values().stream().filter(l -> selectedIds.contains(l.getId())).map(Identifiable::getId).toList();

		// Save the results in a file
		CSVPrinter writer = new CSVPrinter(
			IOUtils.getBufferedWriter(path),
			CSVFormat.DEFAULT);
		writer.printRecord(
			"linkId",
			"time",
			"cutoutVolume",
			"totalVolume"
		);

		for(double t = 0; t < 86400; t += Math.round(changeEventsInterval)) {
			for (Id<Link> l : links) {
				writer.printRecord(
					l, t, getCutoutLinkVolume(l, t), getTotalLinkVolume(l, t)
				);
			}
		}

		writer.flush();
		writer.close();
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleId2Person.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		int timeslice = (getTimeSlice(event.getTime()));

		Id<Person> personId = vehicleId2Person.get(event.getVehicleId());
		if (personId == null) {
			throw new RuntimeException("Tried to hande a LinkLeaveEvent, that can not be assigned to a personId!");
		}

		// Get the vehicle PCU TODO Check that it works as intended
		int vehiclePcu;
		if (scenario.getVehicles().getVehicles().containsKey(event.getVehicleId())){
			vehiclePcu = (int) Math.round(scenario.getVehicles().getVehicles().get(event.getVehicleId()).getType().getPcuEquivalents());
		} else {
			vehiclePcu = 1;
			if (vehIdNotFoundWarnings < 10){
				log.warn("Vehicle was not found {}, setting pcu=1", event.getVehicleId());
				vehIdNotFoundWarnings++;
			}
		}

		linkId2timeslice2volume.computeIfAbsent(event.getLinkId(), k -> new Int2ObjectOpenHashMap<>());
		CutoutVolume v = linkId2timeslice2volume.get(event.getLinkId()).computeIfAbsent(timeslice, k -> new CutoutVolume());
		if (cutoutPersons.contains(personId)) {
			v.cutoutVolume += vehiclePcu;
		}

		v.totalVolume += vehiclePcu;
	}
}
