package playground.anhorni.locationchoice.preprocess.facilities.assembleFacilitiesVariousSources;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import org.apache.log4j.Logger;

public class CompareFacilities {
	
	private final static Logger log = Logger.getLogger(CompareFacilities.class);
	
	public TreeMap<String, ZHFacilityComposed> compare(List<ZHFacilityComposed> konradFacilities, TreeMap<String, ZHFacilityComposed> datapulsFacilitiesMap) {
		
		List<String> keys2Remove = new Vector<String>();
		
		int coordinatesNotMatched = 0;
		int recordNotPresent = 0;
		
		Iterator<ZHFacilityComposed> facilities_it = konradFacilities.iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed konradFacility = facilities_it.next();
			String key = konradFacility.getPLZ() + konradFacility.getStreet();
			ZHFacilityComposed datapulsFacility = datapulsFacilitiesMap.get(key);
			
			if (datapulsFacility != null) {
				if (konradFacility.getCoords().calcDistance(datapulsFacility.getCoords()) > 100.0) {
					log.info(konradFacility.getRetailerCategory() + " " +  konradFacility.getStreet() + 
							" X: " + konradFacility.getCoords().getX() +" Y: " + konradFacility.getCoords().getY() +
							datapulsFacility.getRetailerCategory() + " " +  datapulsFacility.getStreet() + 
							" X: " + datapulsFacility.getCoords().getX() +" Y: " + datapulsFacility.getCoords().getY() + 
							" coordinates do not match");
					coordinatesNotMatched++;
				}
				
				// remove the facility from the map
				keys2Remove.add(datapulsFacility.getPLZ()+ datapulsFacility.getStreet());			
			}
			else {
				log.info(key + " is not in datapuls data set");
				recordNotPresent++;
			}
		}
		for (String key : keys2Remove) {
			datapulsFacilitiesMap.remove(key);	
		}
		log.info("Number of not matched coordinates: " + coordinatesNotMatched);
		log.info("Number of not in datapuls dataset: " + recordNotPresent + "\n --------------------------");	
		printAdditionalDatapulsFacilities(datapulsFacilitiesMap);
		return datapulsFacilitiesMap;
	}
	
	
	private void printAdditionalDatapulsFacilities(TreeMap<String, ZHFacilityComposed> datapulsFacilitiesMap) {
		
		int additonalDatapulsFacilities = 0;
		Iterator<ZHFacilityComposed> facilities_it = datapulsFacilitiesMap.values().iterator();
		while (facilities_it.hasNext()) {
			ZHFacilityComposed datapulsFacility = facilities_it.next();			
			if (datapulsFacility.getRetailerCategory().equals("1") ||
					datapulsFacility.getRetailerCategory().equals("2") ||
					datapulsFacility.getRetailerCategory().equals("3") ||
					datapulsFacility.getRetailerCategory().equals("5")){
				additonalDatapulsFacilities++;
				log.info(datapulsFacility.getId() + " not in Konrad facilities");
			}
		}
		log.info("Number of additonal datapuls facilities: " + additonalDatapulsFacilities);
	}

}
