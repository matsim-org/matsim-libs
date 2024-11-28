/**
 *
 */
package org.matsim.contrib.parking.parkingsearch.manager;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Extends the facility based manager, thus parks vehicles at facilities but also keeps track of the occupancy of zones. A zone is defined by a set
 * of links.
 *
 * @author tschlenther
 */
public class ZoneParkingManager extends FacilityBasedParkingManager {

	private final HashMap<String, HashSet<Id<Link>>> linksByZone;
	private final HashMap<String, Double> totalCapByZone;
	private final HashMap<String, Double> occupationByZone;

	/**
	 * @param scenario
	 */
	@Inject
	public ZoneParkingManager(Scenario scenario, String[] pathToZoneTxtFiles) {
		super(scenario);

		this.linksByZone = new HashMap<String, HashSet<Id<Link>>>();
		this.totalCapByZone = new HashMap<String, Double>();
		this.occupationByZone = new HashMap<String, Double>();

		for (String zone : pathToZoneTxtFiles) {
			readZone(zone);
		}

		for (String zone : this.linksByZone.keySet()) {
			calculateTotalZoneParkCapacity(zone);
			this.occupationByZone.put(zone, 0.0);
		}
	}


	/**
	 * reads in a tabular file that declares which link id's are in the monitored zone
	 * the part between the last '/' and the file type extension in the given path is considered to be the zone name
	 *
	 * @param pathToZoneFile
	 */
	void readZone(String pathToZoneFile) {
		String zone = pathToZoneFile.substring(pathToZoneFile.lastIndexOf("/") + 1, pathToZoneFile.lastIndexOf("."));

		HashSet<Id<Link>> links = new HashSet<Id<Link>>();

		TabularFileParserConfig config = new TabularFileParserConfig();
		config.setDelimiterTags(new String[]{"\t"});
		config.setFileName(pathToZoneFile);
		config.setCommentTags(new String[]{"#"});
		new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				links.add(linkId);
			}

		});

		this.linksByZone.put(zone, links);
	}

	private void calculateTotalZoneParkCapacity(String zoneName) {
		double cap = 0.0;
		for (Id<Link> link : this.linksByZone.get(zoneName)) {
			cap += getNrOfAllParkingSpacesOnLink(link);
		}
		this.totalCapByZone.put(zoneName, cap);
	}


	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (parkVehicleAtLink(vehicleId, linkId, time)) {
			for (String zone : this.linksByZone.keySet()) {
				if (linksByZone.get(zone).contains(linkId) && this.parkingFacilitiesByLink.containsKey(linkId)) {
					double newOcc = this.occupationByZone.get(zone) + 1;
					if (this.totalCapByZone.get(zone) < newOcc) {
						String s = "FacilityID: " + this.parkingFacilityLocationByVehicleId.get(vehicleId);
						String t = "Occupied: " + this.infoByFacilityId.get(this.parkingFacilityLocationByVehicleId.get(vehicleId)).occupation;
						String u =
							"Capacity: " + this.parkingFacilitiesById.get(this.parkingFacilityLocationByVehicleId.get(vehicleId)).getActivityOptions()
																	 .get(
																		 ParkingUtils.ParkingStageInteractionType).getCapacity();
						String v = "TotalCapacityOnLink: " + getNrOfAllParkingSpacesOnLink(linkId);
						throw new RuntimeException("occupancy of zone " + zone + " is higher than 100%. Capacity= " + this.totalCapByZone.get(
							zone) + "  occupancy=" + newOcc + "time = " + time
							+ "\n" + s + "\n" + t + "\n" + u + "\n" + v);
					}
					this.occupationByZone.put(zone, newOcc);
					return true;                                // assumes: link is only part of exactly 1 zone
				}
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingFacilityLocationByVehicleId.containsKey(vehicleId)) {
			return true;
			// we assume the person parks somewhere else
		} else {
			Id<ActivityFacility> fac = this.parkingFacilityLocationByVehicleId.remove(vehicleId);
			this.infoByFacilityId.get(fac).occupation--;

			Id<Link> parkingLink = this.parkingFacilitiesById.get(fac).getLinkId();
			for (String zone : this.linksByZone.keySet()) {
				if (linksByZone.get(zone).contains(parkingLink)) {
					double newOcc = this.occupationByZone.get(zone) - 1;
					if (newOcc < 0) {
						//in iteration 0 agents can "leave parking spaces" (get into traffic), but the manager didn't record them to be parked
						newOcc = 0;
					}
					this.occupationByZone.put(zone, newOcc);
				}
			}
			return true;
		}
	}


	public double getOccupancyRatioOfZone(String zone) {
		if (!(this.linksByZone.keySet().contains(zone))) {
			throw new RuntimeException("zone " + zone + " was not defined. thus, could'nt calculate occupancy ratio.");
		}

		return (this.occupationByZone.get(zone) / this.totalCapByZone.get(zone));
	}

	public Set<String> getZones() {
		return this.linksByZone.keySet();
	}

	public double getTotalCapacityOfZone(String zone) {
		return this.totalCapByZone.get(zone);
	}

}
