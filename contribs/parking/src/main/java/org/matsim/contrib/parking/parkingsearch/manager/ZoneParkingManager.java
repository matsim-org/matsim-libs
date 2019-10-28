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
 * @author tschlenther
 */
public class ZoneParkingManager extends FacilityBasedParkingManager {

	private HashMap<String,HashSet<Id<Link>>> linksOfZone;
	private HashMap<String,Double> totalCapOfZone;
	private HashMap<String,Double> occupationOfZone;
	
	/**
	 * @param scenario
	 */
	@Inject
	public ZoneParkingManager(Scenario scenario, String[] pathToZoneTxtFiles) {
		super(scenario);
		
		this.linksOfZone = new HashMap<String,HashSet<Id<Link>>>();
		this.totalCapOfZone = new HashMap<String,Double>();
		this.occupationOfZone = new HashMap<String,Double>();
		
		for(String zone: pathToZoneTxtFiles){
			readZone(zone);
		}
		
		for(String zone: this.linksOfZone.keySet()){
			calculateTotalZoneParkCapacity(zone);
			this.occupationOfZone.put(zone,0.0);
		}
	}

	
	/**
	 * reads in a tabular file that declares which link id's are in the monitored zone
     * the part between the last '/' and the file type extension in the given path is considered to be the zone name
	 * @param pathToZoneFile
	 */
	void readZone(String pathToZoneFile){
		String zone = pathToZoneFile.substring(pathToZoneFile.lastIndexOf("/")+1, pathToZoneFile.lastIndexOf("."));
		
		HashSet<Id<Link>> links = new HashSet<Id<Link>>();
		
		TabularFileParserConfig config = new TabularFileParserConfig();
        config.setDelimiterTags(new String[] {"\t"});
        config.setFileName(pathToZoneFile);
        config.setCommentTags(new String[] { "#" });
        new TabularFileParser().parse(config, new TabularFileHandler() {
			@Override
			public void startRow(String[] row) {
				Id<Link> linkId = Id.createLinkId(row[0]);
				links.add(linkId);
			}
		
        });
		
        this.linksOfZone.put(zone, links);       
	}
	
	private void calculateTotalZoneParkCapacity(String zoneName){
		double cap = 0.0;
		for(Id<Link> link : this.linksOfZone.get(zoneName)){
			cap += getNrOfAllParkingSpacesOnLink(link);
		} 
		this.totalCapOfZone.put(zoneName, cap);
	}

	
	@Override
	public boolean parkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (parkVehicleAtLink(vehicleId, linkId, time)) {
			for(String zone : this.linksOfZone.keySet()){
				if(linksOfZone.get(zone).contains(linkId) && this.facilitiesPerLink.containsKey(linkId)){
					double newOcc = this.occupationOfZone.get(zone) + 1;
					if(this.totalCapOfZone.get(zone)<newOcc){
						String s = "FacilityID: " + this.parkingLocations.get(vehicleId);
						String t = "Occupied: " + this.occupation.get(this.parkingLocations.get(vehicleId));
						String u = "Capacity: " + this.parkingFacilities.get(this.parkingLocations.get(vehicleId)).getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();
						String v = "TotalCapacityOnLink: " + getNrOfAllParkingSpacesOnLink(linkId);
						throw new RuntimeException("occupancy of zone " + zone + " is higher than 100%. Capacity= " + this.totalCapOfZone.get(zone) + "  occupancy=" + newOcc + "time = " + time
								+ "\n" + s + "\n" + t + "\n" + u + "\n" + v);
					}
					this.occupationOfZone.put(zone,newOcc);
					return true;								// assumes: link is only part of exactly 1 zone
				}
			}
			return true;
		} else
			return false;
	}
	
	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		if (!this.parkingLocations.containsKey(vehicleId)) {
			return true;
			// we assume the person parks somewhere else
		} else {
			Id<ActivityFacility> fac = this.parkingLocations.remove(vehicleId);
			this.occupation.get(fac).decrement();
	
			Id<Link> parkingLink = this.parkingFacilities.get(fac).getLinkId();
			for(String zone : this.linksOfZone.keySet()){
				if(linksOfZone.get(zone).contains(parkingLink)){
					double newOcc = this.occupationOfZone.get(zone) - 1;
					if(newOcc < 0 ){
						//in iteration 0 agents can "leave parking spaces" (get into traffic), but the manager didn't record them to be parked  
						newOcc = 0;
					}
					this.occupationOfZone.put(zone,newOcc);
				}
			}
				return true;
		}
	}
	
	
	public double getOccupancyRatioOfZone(String zone){
		if(!(this.linksOfZone.keySet().contains(zone))) throw new RuntimeException("zone " + zone + " was not defined. thus, could'nt calculate occupancy ratio.");

		return (this.occupationOfZone.get(zone) / this.totalCapOfZone.get(zone));
	}
	
	public Set<String> getZones(){
		return this.linksOfZone.keySet();
	}
	
	public double getTotalCapacityOfZone(String zone){
		return this.totalCapOfZone.get(zone);
	}
	
}
