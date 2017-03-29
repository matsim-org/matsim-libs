/**
 * 
 */
package playground.tschlenther.parkingSearch.utils;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.axis2.util.IOUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

/**
 * @author tschlenther
 *
 */
public class ZoneParkingManager extends FacilityBasedParkingManager {

	private HashMap<String,HashSet<Id<Link>>> linksOfZone;
	private HashMap<String,Double> totalCapOfZone;
	private HashMap<String,Double> occupationOfZone;
	
	private final static String mierendorffLinks = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Zielzone.txt";
	private final static String klausenerLinks = "C:/Users/Work/Bachelor Arbeit/input/GridNet/Zonen/Homezone.txt";
	
	/**
	 * @param scenario
	 */
	@Inject
	public ZoneParkingManager(Scenario scenario) {
		super(scenario);
		
		this.linksOfZone = new HashMap<String,HashSet<Id<Link>>>();
		this.totalCapOfZone = new HashMap<String,Double>();
		this.occupationOfZone = new HashMap<String,Double>();
		
		readZone(mierendorffLinks);
		readZone(klausenerLinks);
		
		for(String zone: this.linksOfZone.keySet()){
			calculateTotalZoneParkCapacity(zone);
			this.occupationOfZone.put(zone,0.0);
		}
	}

	
	/**
	 * reads in a tabular file that declares which link id's are in the monitored zone
	 * the part between the last '/' and ".txt" of the given path is considered to be the zone name
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
				if(linksOfZone.get(zone).contains(linkId)){
					double newOcc = this.occupationOfZone.get(zone) + 1;
					if(this.totalCapOfZone.get(zone)<newOcc){
						throw new RuntimeException("occupancy of zone " + zone + " is higher than 100%. Capacity= " + this.totalCapOfZone.get(zone) + "  occupancy=" + newOcc);
					}
					this.occupationOfZone.put(zone,newOcc);
					return true;
				}
			}
			return true;
		} else
			return false;
	}
	
	@Override
	public boolean unParkVehicleHere(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		
		for(String zone : this.linksOfZone.keySet()){
			if(linksOfZone.get(zone).contains(linkId)){
				double newOcc = this.occupationOfZone.get(zone) - 1;
				if(newOcc < 0 ){
					//throw new RuntimeException("occupancy of zone " + zone + " is negative. Capacity= " + this.totalCapOfZone.get(zone) + "  occupancy=" + newOcc);
					newOcc = 0;
				}
				this.occupationOfZone.put(zone,newOcc);
			}
		}
		
		return super.unParkVehicleHere(vehicleId, linkId, time);
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
