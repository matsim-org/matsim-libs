package playground.dhosse.gap.scenario.facilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.GAPScenarioBuilder;

import com.vividsolutions.jts.geom.Geometry;

public class FacilitiesCreator {
	
	private static Logger log = Logger.getLogger(FacilitiesCreator.class);
	
	private static HashMap<Geometry, String> geometry2MunId = new HashMap<>();
	
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
		
//		Map<String, String> osmToMatsimTypeMap = new HashMap<>();
//		osmToMatsimTypeMap.put("alpine_hut", "tourism1");
//		osmToMatsimTypeMap.put("apartment", "tourism1");
//		osmToMatsimTypeMap.put("attraction", "tourism2");
//		osmToMatsimTypeMap.put("artwork", "tourism2");
//		osmToMatsimTypeMap.put("camp_site", "tourism1");
//		osmToMatsimTypeMap.put("caravan_site", "tourism1");
//		osmToMatsimTypeMap.put("chalet", "tourism1");
//		osmToMatsimTypeMap.put("gallery", "tourism2");
//		osmToMatsimTypeMap.put("guest_house", "tourism1");
//		osmToMatsimTypeMap.put("hostel", "tourism1");
//		osmToMatsimTypeMap.put("hotel", "tourism1");
//		osmToMatsimTypeMap.put("information", "tourism2");
//		osmToMatsimTypeMap.put("motel", "tourism1");
//		osmToMatsimTypeMap.put("museum", "tourism2");
//		osmToMatsimTypeMap.put("picnic_site", "tourism2");
//		osmToMatsimTypeMap.put("theme_park", "tourism2");
//		osmToMatsimTypeMap.put("viewpoint", "tourism2");
//		osmToMatsimTypeMap.put("wilderness_hut", "tourism1");
//		osmToMatsimTypeMap.put("zoo", "tourism2");
//		
//		//education
//		osmToMatsimTypeMap.put("college", "education");
//		osmToMatsimTypeMap.put("kindergarten", "education");
//		osmToMatsimTypeMap.put("school", "education");
//		osmToMatsimTypeMap.put("university", "education");
//		
//		//leisure
//		osmToMatsimTypeMap.put("arts_centre", "leisure");
//		osmToMatsimTypeMap.put("cinema", "leisure");
//		osmToMatsimTypeMap.put("community_centre", "leisure");
//		osmToMatsimTypeMap.put("fountain", "leisure");
//		osmToMatsimTypeMap.put("nightclub", "leisure");
//		osmToMatsimTypeMap.put("planetarium", "leisure");
//		osmToMatsimTypeMap.put("social_centre", "leisure");
//		osmToMatsimTypeMap.put("theatre", "leisure");
//		osmToMatsimTypeMap.put("gym", "leisure");
//		osmToMatsimTypeMap.put("adult_gaming_centre", "leisure");
//		osmToMatsimTypeMap.put("amusement_arcade", "leisure");
//		osmToMatsimTypeMap.put("beach_resort", "leisure");
//		osmToMatsimTypeMap.put("bandstand", "leisure");
//		osmToMatsimTypeMap.put("bird_hide", "leisure");
//		osmToMatsimTypeMap.put("dance", "leisure");
//		osmToMatsimTypeMap.put("dog_park", "leisure");
//		osmToMatsimTypeMap.put("firepit", "leisure");
//		osmToMatsimTypeMap.put("fishing", "leisure");
//		osmToMatsimTypeMap.put("garden", "leisure");
//		osmToMatsimTypeMap.put("golf_course", "leisure");
//		osmToMatsimTypeMap.put("hackerspace", "leisure");
//		osmToMatsimTypeMap.put("ice_rink", "leisure");
//		osmToMatsimTypeMap.put("marina", "leisure");
//		osmToMatsimTypeMap.put("miniature_golf", "leisure");
//		osmToMatsimTypeMap.put("nature_reserve", "leisure");
//		osmToMatsimTypeMap.put("park", "leisure");
//		osmToMatsimTypeMap.put("pitch", "leisure");
//		osmToMatsimTypeMap.put("playground", "leisure");
//		osmToMatsimTypeMap.put("slipway", "leisure");
//		osmToMatsimTypeMap.put("sports_centre", "leisure");
//		osmToMatsimTypeMap.put("stadium", "leisure");
//		osmToMatsimTypeMap.put("summer_camp", "leisure");
//		osmToMatsimTypeMap.put("swimming_pool", "leisure");
//		osmToMatsimTypeMap.put("swimming_area", "leisure");
//		osmToMatsimTypeMap.put("track", "leisure");
//		osmToMatsimTypeMap.put("water_park", "leisure");
//		osmToMatsimTypeMap.put("wildlife_hide", "leisure");
//		
//		//shopping
//		osmToMatsimTypeMap.put("alcohol", "shop");
//		osmToMatsimTypeMap.put("bakery", "shop");
//		osmToMatsimTypeMap.put("beverages", "shop");
//		osmToMatsimTypeMap.put("butcher", "shop");
//		osmToMatsimTypeMap.put("cheese", "shop");
//		osmToMatsimTypeMap.put("chocolate", "shop");
//		osmToMatsimTypeMap.put("coffee", "shop");
//		osmToMatsimTypeMap.put("confectionery", "shop");
//		osmToMatsimTypeMap.put("convenience", "shop");
//		osmToMatsimTypeMap.put("deli", "shop");
//		osmToMatsimTypeMap.put("dairy", "shop");
//		osmToMatsimTypeMap.put("farm", "shop");
//		osmToMatsimTypeMap.put("greengrocer", "shop");
//		osmToMatsimTypeMap.put("pasta", "shop");
//		osmToMatsimTypeMap.put("pastry", "shop");
//		osmToMatsimTypeMap.put("seafood", "shop");
//		osmToMatsimTypeMap.put("tea", "shop");
//		osmToMatsimTypeMap.put("wine", "shop");
//		osmToMatsimTypeMap.put("department_store", "shop");
//		osmToMatsimTypeMap.put("general", "shop");
//		osmToMatsimTypeMap.put("kiosk", "shop");
//		osmToMatsimTypeMap.put("mall", "shop");
//		osmToMatsimTypeMap.put("supermarket", "shop");
//		osmToMatsimTypeMap.put("baby_goods", "shop");
//		osmToMatsimTypeMap.put("bag", "shop");
//		osmToMatsimTypeMap.put("boutique", "shop");
//		osmToMatsimTypeMap.put("clothes", "shop");
//		osmToMatsimTypeMap.put("fabric", "shop");
//		osmToMatsimTypeMap.put("fashion", "shop");
//		osmToMatsimTypeMap.put("jewelry", "shop");
//		osmToMatsimTypeMap.put("leather", "shop");
//		osmToMatsimTypeMap.put("shoes", "shop");
//		osmToMatsimTypeMap.put("tailor", "shop");
//		osmToMatsimTypeMap.put("watches", "shop");
//		osmToMatsimTypeMap.put("charity", "shop");
//		osmToMatsimTypeMap.put("second_hand", "shop");
//		osmToMatsimTypeMap.put("variety_store", "shop");
//		osmToMatsimTypeMap.put("beauty", "shop");
//		osmToMatsimTypeMap.put("chemist", "shop");
//		osmToMatsimTypeMap.put("cosmetics", "shop");
//		osmToMatsimTypeMap.put("erotic", "shop");
//		osmToMatsimTypeMap.put("hairdresser", "shop");
//		osmToMatsimTypeMap.put("hearing_aid", "shop");
//		osmToMatsimTypeMap.put("herbalist", "shop");
//		osmToMatsimTypeMap.put("massage", "shop");
//		osmToMatsimTypeMap.put("medical_supply", "shop");
//		osmToMatsimTypeMap.put("optician", "shop");
//		osmToMatsimTypeMap.put("perfumery", "shop");
//		osmToMatsimTypeMap.put("tattoo", "shop");
//		osmToMatsimTypeMap.put("bathroom_furnishing", "shop");
//		osmToMatsimTypeMap.put("doityourself", "shop");
//		osmToMatsimTypeMap.put("electrical", "shop");
//		osmToMatsimTypeMap.put("energy", "shop");
//		osmToMatsimTypeMap.put("florist", "shop");
//		osmToMatsimTypeMap.put("furnace", "shop");
//		osmToMatsimTypeMap.put("garden_centre", "shop");
//		osmToMatsimTypeMap.put("garden_furniture", "shop");
//		osmToMatsimTypeMap.put("gas", "shop");
//		osmToMatsimTypeMap.put("glaziery", "shop");
//		osmToMatsimTypeMap.put("hardware", "shop");
//		osmToMatsimTypeMap.put("houseware", "shop");
//		osmToMatsimTypeMap.put("locksmith", "shop");
//		osmToMatsimTypeMap.put("paint", "shop");
//		osmToMatsimTypeMap.put("trade", "shop");
//		osmToMatsimTypeMap.put("antiques", "shop");
//		osmToMatsimTypeMap.put("bed", "shop");
//		osmToMatsimTypeMap.put("candles", "shop");
//		osmToMatsimTypeMap.put("carpet", "shop");
//		osmToMatsimTypeMap.put("curtain", "shop");
//		osmToMatsimTypeMap.put("furniture", "shop");
//		osmToMatsimTypeMap.put("interior_decoration", "shop");
//		osmToMatsimTypeMap.put("kitchen", "shop");
//		osmToMatsimTypeMap.put("lamps", "shop");
//		osmToMatsimTypeMap.put("window_blind", "shop");
//		osmToMatsimTypeMap.put("computer", "shop");
//		osmToMatsimTypeMap.put("electronics", "shop");
//		osmToMatsimTypeMap.put("hifi", "shop");
//		osmToMatsimTypeMap.put("mobile_phone", "shop");
//		osmToMatsimTypeMap.put("radiotechnics", "shop");
//		osmToMatsimTypeMap.put("vacuum_cleaner", "shop");
//		osmToMatsimTypeMap.put("bicycle", "shop");
//		osmToMatsimTypeMap.put("car", "shop");
//		osmToMatsimTypeMap.put("car_repair", "shop");
//		osmToMatsimTypeMap.put("car_parts", "shop");
//		osmToMatsimTypeMap.put("fishing", "shop");
//		osmToMatsimTypeMap.put("free_flying", "shop");
//		osmToMatsimTypeMap.put("hunting", "shop");
//		osmToMatsimTypeMap.put("motorcycle", "shop");
//		osmToMatsimTypeMap.put("outdoor", "shop");
//		osmToMatsimTypeMap.put("scuba_driving", "shop");
//		osmToMatsimTypeMap.put("sports", "shop");
//		osmToMatsimTypeMap.put("tyres", "shop");
//		osmToMatsimTypeMap.put("swimming_pool", "shop");
//		osmToMatsimTypeMap.put("art", "shop");
//		osmToMatsimTypeMap.put("craft", "shop");
//		osmToMatsimTypeMap.put("frame", "shop");
//		osmToMatsimTypeMap.put("games", "shop");
//		osmToMatsimTypeMap.put("model", "shop");
//		osmToMatsimTypeMap.put("music", "shop");
//		osmToMatsimTypeMap.put("musical_instrument", "shop");
//		osmToMatsimTypeMap.put("photo", "shop");
//		osmToMatsimTypeMap.put("trophy", "shop");
//		osmToMatsimTypeMap.put("video", "shop");
//		osmToMatsimTypeMap.put("video_games", "shop");
//		osmToMatsimTypeMap.put("anime", "shop");
//		osmToMatsimTypeMap.put("books", "shop");
//		osmToMatsimTypeMap.put("gift", "shop");
//		osmToMatsimTypeMap.put("newsagent", "shop");
//		osmToMatsimTypeMap.put("stationary", "shop");
//		osmToMatsimTypeMap.put("ticket", "shop");
//		osmToMatsimTypeMap.put("copyshop", "shop");
//		osmToMatsimTypeMap.put("dry_cleaning", "shop");
//		osmToMatsimTypeMap.put("e-cigarette", "shop");
//		osmToMatsimTypeMap.put("funeral_directors", "shop");
//		osmToMatsimTypeMap.put("laundry", "shop");
//		osmToMatsimTypeMap.put("money_lender", "shop");
//		osmToMatsimTypeMap.put("pawnbroker", "shop");
//		osmToMatsimTypeMap.put("pet", "shop");
//		osmToMatsimTypeMap.put("pyrotechnics", "shop");
//		osmToMatsimTypeMap.put("religion", "shop");
//		osmToMatsimTypeMap.put("storage_rental", "shop");
//		osmToMatsimTypeMap.put("tobacco", "shop");
//		osmToMatsimTypeMap.put("toys", "shop");
//		osmToMatsimTypeMap.put("travel_agency", "shop");
//		osmToMatsimTypeMap.put("weapons", "shop");
//		
//		osmToMatsimTypeMap.put("bank", "other");
//		osmToMatsimTypeMap.put("clinic", "other");
//		osmToMatsimTypeMap.put("dentist", "other");
//		osmToMatsimTypeMap.put("doctors", "other");
//		osmToMatsimTypeMap.put("hospital", "other");
//		osmToMatsimTypeMap.put("nursing_home", "other");
//		osmToMatsimTypeMap.put("pharmacy", "other");
//		osmToMatsimTypeMap.put("social_facility", "other");
//		osmToMatsimTypeMap.put("veterinary", "other");
//		osmToMatsimTypeMap.put("blood_donation", "other");
//		osmToMatsimTypeMap.put("courthouse", "other");
//		osmToMatsimTypeMap.put("marketplace", "other");
//		osmToMatsimTypeMap.put("place_of_worship", "other");
//		osmToMatsimTypeMap.put("police", "other");
//		osmToMatsimTypeMap.put("post_office", "other");
//		osmToMatsimTypeMap.put("townhall", "other");
//		osmToMatsimTypeMap.put("bar", "other");
//		osmToMatsimTypeMap.put("bbq", "other");
//		osmToMatsimTypeMap.put("biergarten", "other");
//		osmToMatsimTypeMap.put("cafe", "other");
//		osmToMatsimTypeMap.put("fast_food", "other");
//		osmToMatsimTypeMap.put("food_court", "other");
//		osmToMatsimTypeMap.put("ice_cream", "other");
//		osmToMatsimTypeMap.put("pub", "other");
//		osmToMatsimTypeMap.put("restaurant", "other");
//		
//		Set<String> keys = new HashSet<>();
//		keys.add("tourism");
//		keys.add("amenity");
//		keys.add("shop");
//		keys.add("leisure");
//		
//		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(Global.adminBordersDir + "/Gebietsstand_2007/gemeinden_2007.shp");
//		geometry2MunId = new HashMap<>();
//		for(SimpleFeature feature : features){
//			Geometry g = (Geometry)feature.getDefaultGeometry();
//			Long id = (Long)feature.getAttribute("GEM_KENNZ");
//			geometry2MunId.put(g, "0"+Long.toString(id));
//		}
//
//		OsmObjectsToFacilitiesParser reader = new OsmObjectsToFacilitiesParser(Global.dataDir + "/Netzwerk/garmisch-latest.osm", Global.ct, osmToMatsimTypeMap, keys);
//		reader.parse();
//		reader.writeFacilities(Global.matsimInputDir + "facilities/facilities.xml");
//		reader.writeFacilityAttributes(Global.matsimInputDir + "facilities/facilityAttribues.xml");
//		reader.writeFacilityCoordinates(Global.matsimInputDir + "facilities.csv");
//		
//		Set<Id<ActivityFacility>> facilityIds = new HashSet<>();
//		
//		log.info("Assigning activity facilities to municipalities...");
//		for(ActivityFacility facility : reader.getFacilities().getFacilities().values()){
//			
//			if(!scenario.getActivityFacilities().getFacilities().containsKey(facility.getId()))
//				scenario.getActivityFacilities().addActivityFacility(facility);
//			else continue;
//			
//			for(Geometry g : geometry2MunId.keySet()){
//				
//				boolean added = false;
//				
//				if(g.contains(MGC.coord2Point(Global.UTM32NtoGK4.transform(facility.getCoord())))){
//					
//					double d = 1/CoordUtils.calcDistance(MGC.point2Coord(g.getCentroid()), facility.getCoord());
//					for(ActivityOption ao : facility.getActivityOptions().values()){
////						if(ao.getType().equals(Global.ActType.work)) continue;
////						ao.setCapacity(ao.getCapacity() * d);
//					}
//					
//					if(facility.getActivityOptions().containsKey(Global.ActType.education.name())){
//						
//						if(!GAPScenarioBuilder.getMunId2EducationFacilities().containsKey(geometry2MunId.get(g))){
//							
//							GAPScenarioBuilder.getMunId2EducationFacilities().put(geometry2MunId.get(g), new ArrayList<ActivityFacility>());
//						}
//						
//						GAPScenarioBuilder.getMunId2EducationFacilities().get(geometry2MunId.get(g)).add(facility);
//						added = true;
//						
//					}
//					
//					if(facility.getActivityOptions().containsKey(Global.ActType.leisure.name())){
//						
//						if(!GAPScenarioBuilder.getMunId2LeisureFacilities().containsKey(geometry2MunId.get(g))){
//							
//							GAPScenarioBuilder.getMunId2LeisureFacilities().put(geometry2MunId.get(g), new ArrayList<ActivityFacility>());
//						}
//						
//						GAPScenarioBuilder.getMunId2LeisureFacilities().get(geometry2MunId.get(g)).add(facility);
//						added = true;
//						
//					}
//					
//					if(facility.getActivityOptions().containsKey(Global.ActType.other.name())){
//						
//						if(!GAPScenarioBuilder.getMunId2OtherFacilities().containsKey(geometry2MunId.get(g))){
//							
//							GAPScenarioBuilder.getMunId2OtherFacilities().put(geometry2MunId.get(g), new ArrayList<ActivityFacility>());
//						}
//						
//						GAPScenarioBuilder.getMunId2OtherFacilities().get(geometry2MunId.get(g)).add(facility);
//						added = true;
//						
//					}
//					
//					if(facility.getActivityOptions().containsKey(Global.ActType.shop.name())){
//						
//						if(!GAPScenarioBuilder.getMunId2ShopFacilities().containsKey(geometry2MunId.get(g))){
//							
//							GAPScenarioBuilder.getMunId2ShopFacilities().put(geometry2MunId.get(g), new ArrayList<ActivityFacility>());
//						}
//						
//						GAPScenarioBuilder.getMunId2ShopFacilities().get(geometry2MunId.get(g)).add(facility);
//						added = true;
//						
//					}
//					
//					if(added) break;
//					
//				}
//				
//			}
//			
//		}
		
	}
	
	public static void readWorkplaces(Scenario scenario, String file){
		
		Log.info("Reading workplaces from " + file);
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		final int idxX = 0;
		final int idxY = 1;
		final int idxWorkCapacity = 3;
		final int idxActivityOptions = 4;
		
		int counter = 0;
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split(",");
				
				Coord coord = Global.ct.transform(new Coord(Double.parseDouble(parts[idxX]), Double.parseDouble(parts[idxY])));
				
				ActivityFacility facility = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(Global.ActType.work.name() + "_" + counter, ActivityFacility.class), coord);
				ActivityOption work = scenario.getActivityFacilities().getFactory().createActivityOption(Global.ActType.work.name());
				work.setCapacity(Double.parseDouble(parts[idxWorkCapacity]));
				facility.addActivityOption(work);
				String[] activityOptions = parts[idxActivityOptions].split(";");
				for(String ao : activityOptions){
					
					if(!facility.getActivityOptions().containsKey(ao)){
						ActivityOption activityOption = scenario.getActivityFacilities().getFactory().createActivityOption(ao);
						activityOption.setCapacity(1);
						facility.addActivityOption(activityOption);
					}
					
				}
				
				GAPScenarioBuilder.getWorkLocations().put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
				
				Coord gk4Coord = Global.UTM32NtoGK4.transform(facility.getCoord());
				String munId = null;
				
				for(Geometry g : geometry2MunId.keySet()){
					if(g.contains(MGC.coord2Point(gk4Coord))){
						munId = geometry2MunId.get(g);
						break;
					}
				}
				
				if(munId != null){
					if(!GAPScenarioBuilder.getMunId2WorkLocation().containsKey(munId)){
						GAPScenarioBuilder.getMunId2WorkLocation().put(munId, new ArrayList<ActivityFacility>());
					}
					GAPScenarioBuilder.getMunId2WorkLocation().get(munId).add(facility);
					if(!scenario.getActivityFacilities().getFacilities().containsKey(facility.getId()))
						scenario.getActivityFacilities().addActivityFacility(facility);
					
//					for(String s : facility.getActivityOptions().keySet()){
//						
//						Map<String, List<ActivityFacility>> facilities = getFacilitiesForActType(s);
//						if(facilities != null){
//							if(!facilities.containsKey(munId)){
//								facilities.put(munId, new ArrayList<ActivityFacility>());
//							}
//							facilities.get(munId).add(facility);
//						}
//						
//					}
					
				}
				
				counter++;
				
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
		Log.info("Done reading workplaces.");
		
	}
	
	private static Map<String, List<ActivityFacility>> getFacilitiesForActType(String type){
		
		if(type.equals(Global.ActType.education.name())){
			
			return GAPScenarioBuilder.getMunId2EducationFacilities();
			
		} else if(type.equals(Global.ActType.leisure.name())){
			
			return GAPScenarioBuilder.getMunId2LeisureFacilities();
			
		} else if(type.equals(Global.ActType.other.name())){
			
			return GAPScenarioBuilder.getMunId2OtherFacilities();
			
		} else if(type.equals(Global.ActType.shop.name())){
			
			return GAPScenarioBuilder.getMunId2ShopFacilities();
			
		} else if(type.equals(Global.ActType.work.name())){
			
			return GAPScenarioBuilder.getMunId2WorkLocation();
			
		} else {
			
			return null;
			
		}
		
	}
	
	public static String getMunIdContainingCoord(Coord c){
		
		Coord gk4 = Global.UTM32NtoGK4.transform(c);
		
		for(Geometry g : geometry2MunId.keySet()){
			if(g.contains(MGC.coord2Point(gk4))){
				return geometry2MunId.get(g);
			}
		}
		
		return null;
		
	}

}
