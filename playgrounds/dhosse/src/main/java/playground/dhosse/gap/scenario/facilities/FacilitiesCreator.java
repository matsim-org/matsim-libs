package playground.dhosse.gap.scenario.facilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;
import playground.dhosse.utils.osm.OsmObjectsToFacilitiesParser;

public class FacilitiesCreator {
	
	/**
	 * 
	 * Parses a given osm file in order to extract the amenities defined in it.
	 * Amenities are needed to create activity facilities for activity types
	 * <ul>
	 * <li>tourism (splitted into tourism1 (tourist's 'home') and tourism2 (attractions)</li>
	 * <li>education</li>
	 * <li>shop</li>
	 * </ul>
	 * 
	 * @param scenario
	 */
	public static void initAmenities(Scenario scenario){
		
		Map<String, String> osmToMatsimTypeMap = new HashMap<>();
		osmToMatsimTypeMap.put("alpine_hut", "tourism1");
		osmToMatsimTypeMap.put("apartment", "tourism1");
		osmToMatsimTypeMap.put("attraction", "tourism2");
		osmToMatsimTypeMap.put("artwork", "tourism2");
		osmToMatsimTypeMap.put("camp_site", "tourism1");
		osmToMatsimTypeMap.put("caravan_site", "tourism1");
		osmToMatsimTypeMap.put("chalet", "tourism1");
		osmToMatsimTypeMap.put("gallery", "tourism2");
		osmToMatsimTypeMap.put("guest_house", "tourism1");
		osmToMatsimTypeMap.put("hostel", "tourism1");
		osmToMatsimTypeMap.put("hotel", "tourism1");
		osmToMatsimTypeMap.put("information", "tourism2");
		osmToMatsimTypeMap.put("motel", "tourism1");
		osmToMatsimTypeMap.put("museum", "tourism2");
		osmToMatsimTypeMap.put("picnic_site", "tourism2");
		osmToMatsimTypeMap.put("theme_park", "tourism2");
		osmToMatsimTypeMap.put("viewpoint", "tourism2");
		osmToMatsimTypeMap.put("wilderness_hut", "tourism1");
		osmToMatsimTypeMap.put("zoo", "tourism2");
		
		//education
		osmToMatsimTypeMap.put("college", "education");
		osmToMatsimTypeMap.put("kindergarten", "education");
		osmToMatsimTypeMap.put("school", "education");
		osmToMatsimTypeMap.put("university", "education");
		
		//leisure
		osmToMatsimTypeMap.put("arts_centre", "leisure");
		osmToMatsimTypeMap.put("cinema", "leisure");
		osmToMatsimTypeMap.put("community_centre", "leisure");
		osmToMatsimTypeMap.put("fountain", "leisure");
		osmToMatsimTypeMap.put("nightclub", "leisure");
		osmToMatsimTypeMap.put("planetarium", "leisure");
		osmToMatsimTypeMap.put("social_centre", "leisure");
		osmToMatsimTypeMap.put("theatre", "leisure");
		
		//shopping
		osmToMatsimTypeMap.put("alcohol", "shop");
		osmToMatsimTypeMap.put("bakery", "shop");
		osmToMatsimTypeMap.put("beverages", "shop");
		osmToMatsimTypeMap.put("butcher", "shop");
		osmToMatsimTypeMap.put("cheese", "shop");
		osmToMatsimTypeMap.put("chocolate", "shop");
		osmToMatsimTypeMap.put("coffee", "shop");
		osmToMatsimTypeMap.put("confectionery", "shop");
		osmToMatsimTypeMap.put("convenience", "shop");
		osmToMatsimTypeMap.put("deli", "shop");
		osmToMatsimTypeMap.put("dairy", "shop");
		osmToMatsimTypeMap.put("farm", "shop");
		osmToMatsimTypeMap.put("greengrocer", "shop");
		osmToMatsimTypeMap.put("pasta", "shop");
		osmToMatsimTypeMap.put("pastry", "shop");
		osmToMatsimTypeMap.put("seafood", "shop");
		osmToMatsimTypeMap.put("tea", "shop");
		osmToMatsimTypeMap.put("wine", "shop");
		osmToMatsimTypeMap.put("department_store", "shop");
		osmToMatsimTypeMap.put("general", "shop");
		osmToMatsimTypeMap.put("kiosk", "shop");
		osmToMatsimTypeMap.put("mall", "shop");
		osmToMatsimTypeMap.put("supermarket", "shop");
		osmToMatsimTypeMap.put("baby_goods", "shop");
		osmToMatsimTypeMap.put("bag", "shop");
		osmToMatsimTypeMap.put("boutique", "shop");
		osmToMatsimTypeMap.put("clothes", "shop");
		osmToMatsimTypeMap.put("fabric", "shop");
		osmToMatsimTypeMap.put("fashion", "shop");
		osmToMatsimTypeMap.put("jewelry", "shop");
		osmToMatsimTypeMap.put("leather", "shop");
		osmToMatsimTypeMap.put("shoes", "shop");
		osmToMatsimTypeMap.put("tailor", "shop");
		osmToMatsimTypeMap.put("watches", "shop");
		//TODO many more types of amenities to come...
		
		Set<String> keys = new HashSet<>();
		keys.add("tourism");
		keys.add("amenity");
		keys.add("shop");

		OsmObjectsToFacilitiesParser reader = new OsmObjectsToFacilitiesParser(Global.dataDir + "/Netzwerk/garmisch-latest.osm", Global.ct, osmToMatsimTypeMap, keys);
		reader.parse();
//		reader.writeFacilities(Global.matsimInputDir + "facilities/facilities.xml");
//		reader.writeFacilityAttributes(Global.matsimInputDir + "facilities/facilityAttribues.xml");
		reader.writeFacilityCoordinates(Global.matsimInputDir + "facilities.csv");
		
		for(ActivityFacility facility : reader.getFacilities().getFacilities().values()){
			
			scenario.getActivityFacilities().addActivityFacility(facility);
			
		}
		
	}
	
	public static void readWorkplaces(Scenario scenario, String file){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		final int idxX = 0;
		final int idxY = 1;
		
		int counter = 0;
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split(",");
				
				Coord coord = Global.ct.transform(new Coord(Double.parseDouble(parts[idxX]), Double.parseDouble(parts[idxY])));
				
				ActivityFacility facility = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(Global.ActType.work.name() + "_" + counter, ActivityFacility.class), coord);
				ActivityOption work = scenario.getActivityFacilities().getFactory().createActivityOption(Global.ActType.work.name());
				facility.addActivityOption(work);
				
				scenario.getActivityFacilities().addActivityFacility(facility);
				
				GAPScenarioBuilder.getWorkLocations().put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
				
				counter++;
				
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
	}

}
