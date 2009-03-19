package playground.anhorni.locationchoice.facilities;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

public class CompareFacilities {
	
	private final static Logger log = Logger.getLogger(CompareFacilities.class);
	
	public void compareCoordinates(List<ZHFacilityComposed> konradFacilities, List<ZHFacilityComposed> datapulsFacilities) {
		
		TreeMap<String, ZHFacilityComposed> datapulsFacilitiesMap = createTree(datapulsFacilities);
		
		int coordinatesNotMatched = 0;
		int recordNotPresent = 0;
		
		Iterator<ZHFacilityComposed> facilities_it = konradFacilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed konradFacility = facilities_it.next();
			ZHFacilityComposed datapulsFacility = datapulsFacilitiesMap.get(konradFacility.getPLZ() + konradFacility.getStreet());
			
			if (datapulsFacility != null) {
				if (konradFacility.getCoords().calcDistance(datapulsFacility.getCoords()) > 10.0) {
					log.info(konradFacility.getRetailerCategory() + " " +  konradFacility.getStreet() + 
							" X: " + konradFacility.getCoords().getX() +" Y: " + konradFacility.getCoords().getY() +
							datapulsFacility.getRetailerCategory() + " " +  datapulsFacility.getStreet() + 
							" X: " + datapulsFacility.getCoords().getX() +" Y: " + datapulsFacility.getCoords().getY() + 
							" coordinates do not match");
					coordinatesNotMatched++;
				}
			}
			else {
				log.info(konradFacility.getRetailerCategory() + " " +  konradFacility.getStreet() + " is not in datapuls data set");
				recordNotPresent++;
			}
		}
		
		log.info("Number of not matched coordinates: " + coordinatesNotMatched);
		log.info("Number of not in datapuls dataset: " + recordNotPresent);		
	}
	
	private TreeMap<String, ZHFacilityComposed>  createTree(List<ZHFacilityComposed> datapulsFacilities) {
		TreeMap<String, ZHFacilityComposed> datapulsFacilitiesMap = new TreeMap<String, ZHFacilityComposed>();
		Iterator<ZHFacilityComposed> facilities_it = datapulsFacilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed facility = facilities_it.next();
		
			String key = facility.getPLZ()+ facility.getStreet();
			datapulsFacilitiesMap.put(key, facility);
		}
		return datapulsFacilitiesMap;
	}

}
