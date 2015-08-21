package playground.dhosse.gap.scenario;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.mid.MiDCSVReader;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupData;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupTemplates;
import playground.dhosse.gap.scenario.mid.MiDSurveyPerson;
import playground.dhosse.utils.EgapHashGenerator;
import playground.dhosse.utils.osm.OsmObjectsToFacilitiesParser;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 * @author dhosse
 *
 */

public class GAPScenarioBuilder {
	
	private static final Logger log = Logger.getLogger(GAPScenarioBuilder.class);
	
	private static Map<String, Geometry> munId2Geometry = new HashMap<>();
	private static Map<String, Geometry> munName2Geometry = new HashMap<>();
	
	private static Map<String, Geometry> geometries = new HashMap<>();
	
	static int counter = 0;
	static boolean equal;
	
//	private static List<String> linkOrigIdsToRemove = new ArrayList<>();
	private static QuadTree<Geometry> homeLocations;
	private static QuadTree<Geometry> workLocations;
	
	static QuadTree<ActivityFacility> educationQT;
	static QuadTree<ActivityFacility> shopQT;
	static QuadTree<ActivityFacility> leisureQT;
	
	private static ObjectAttributes agentAttributes = new ObjectAttributes();
	
	/**
	 * Creates the scenario network.
	 * 
	 * @param scenario
	 * @param osmFile
	 * @return
	 */
	public static Network createNetwork(Scenario scenario, String osmFile){
		
		//the links with the following osm ids connect the actual network with obsolete parts
		//the idea is to remove these connectors so the network cleaner will throw away the unnecessary parts of the network
//		linkOrigIdsToRemove.add("288789319");
//		linkOrigIdsToRemove.add("24686601");
//		linkOrigIdsToRemove.add("210386362");
//		linkOrigIdsToRemove.add("210386361");
//		linkOrigIdsToRemove.add("325882164");
//		linkOrigIdsToRemove.add("153080104");
//		linkOrigIdsToRemove.add("143209431");
//		linkOrigIdsToRemove.add("228946336");
//		linkOrigIdsToRemove.add("3995758");
//		linkOrigIdsToRemove.add("4997872");
//		linkOrigIdsToRemove.add("92993560");
//		linkOrigIdsToRemove.add("38717563");
//		linkOrigIdsToRemove.add("37927448");
//		linkOrigIdsToRemove.add("143948514");
//		linkOrigIdsToRemove.add("40308586");
//		linkOrigIdsToRemove.add("31392987");
//		linkOrigIdsToRemove.add("49206401");
//		linkOrigIdsToRemove.add("70943815");
//		linkOrigIdsToRemove.add("38937915");
		
		Network network = scenario.getNetwork();
			
		OsmNetworkReader onr = new OsmNetworkReader(network, GAPMain.ct);
		
		//hierarchy layers...
		onr.setHierarchyLayer(48.879, 9.739, 47.130, 12.1, 3); //primary network from oberbayern
		onr.setHierarchyLayer(47.9936, 10.5112, 47.130, 11.7114, 4);
		onr.setHierarchyLayer(47.7389, 10.8662, 47.3793, 11.4251, 6); //complete ways from lk garmisch-partenkirchen
		onr.setHierarchyLayer(47.4330, 11.1034, 47.2871, 11.2788, 6); //complete ways from seefeld & leutasch
		
		onr.parse(osmFile);
		
		Link l9139 = network.getLinks().get(Id.createLinkId("9139"));
		Link l9140 = network.getLinks().get(Id.createLinkId("9140"));
		
		Node n33127404a = network.getFactory().createNode(Id.createNodeId("33127404a"), new CoordImpl(657719, 5262324));
		network.addNode(n33127404a);
		
		Link l9139a = network.getFactory().createLink(Id.createLinkId("9139a"), n33127404a, l9139.getToNode());
		l9139a.setAllowedModes(l9139.getAllowedModes());
		l9139a.setCapacity(l9139.getCapacity());
		l9139a.setFreespeed(l9139.getFreespeed());
		l9139a.setLength(NetworkUtils.getEuclidianDistance(l9139a.getFromNode().getCoord(), l9139a.getToNode().getCoord()));
		l9139a.setNumberOfLanes(l9139.getNumberOfLanes());
		network.addLink(l9139a);
		
		l9139.setLength(l9139.getLength() - l9139a.getLength());
		l9139.setToNode(n33127404a);
		
		Link l9140a = network.getFactory().createLink(Id.createLinkId("9140a"), n33127404a, l9140.getToNode());
		l9140a.setAllowedModes(l9140.getAllowedModes());
		l9140a.setCapacity(l9140.getCapacity());
		l9140a.setFreespeed(l9140.getFreespeed());
		l9140a.setLength(NetworkUtils.getEuclidianDistance(l9140a.getFromNode().getCoord(), l9140a.getToNode().getCoord()));
		l9140a.setNumberOfLanes(l9140.getNumberOfLanes());
		network.addLink(l9140a);
		
		l9140.setLength(l9140a.getLength() - l9140a.getLength());
		l9140.setToNode(n33127404a);
		
		Link l5042 = network.getLinks().get(Id.createLinkId("5042"));
		Node n578757a = network.getFactory().createNode(Id.createNodeId("578757a"), new CoordImpl(657295, 5261694));
		network.addNode(n578757a);
		Link l5042a = network.getFactory().createLink(Id.createLinkId("5042a"), n578757a, l5042.getToNode());
		l5042a.setAllowedModes(l5042.getAllowedModes());
		l5042a.setCapacity(l5042.getCapacity());
		l5042a.setFreespeed(l5042.getFreespeed());
		l5042a.setLength(NetworkUtils.getEuclidianDistance(l5042a.getFromNode().getCoord(), l5042a.getToNode().getCoord()));
		l5042a.setNumberOfLanes(l5042.getNumberOfLanes());
		network.addLink(l5042a);
		
		l5042.setLength(l5042.getLength() - l5042a.getLength());
		
//		List<LinkImpl> linksToRemove = new ArrayList<>();
//		for(Link link : network.getLinks().values()){
//			LinkImpl l = (LinkImpl)link;
//			if(linkOrigIdsToRemove.contains(l.getOrigId())){
//				linksToRemove.add(l);
//			}
//		}
//		
//		for(LinkImpl link : linksToRemove){
//			network.removeLink(link.getId());
//		}
		
		new NetworkCleaner().run(network);
		
		return network;
		
	}
	
	/**
	 * Creates initial plans with home-work-home journeys at the moment.
	 * 
	 * @param scenario The scenario containing the population to be created.
	 * @param commuterFilename The text (csv) file containing information about the commuters.
	 * @param reverseCommuterFilename
	 * @param equallyDistributedEndTimes Defines whether the activity end times should be distributed equally within a given interval or normally.
	 * @return The population
	 */
	public static Population createPlans(Scenario scenario, String commuterFilename, String reverseCommuterFilename, boolean equallyDistributedEndTimes){
		
		double[] boundary = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
		homeLocations = new QuadTree<Geometry>(boundary[0], boundary[1], boundary[2], boundary[3]);
		workLocations = new QuadTree<Geometry>(boundary[0], boundary[1], boundary[2], boundary[3]);
		
		equal = equallyDistributedEndTimes;
		
		Population population = scenario.getPopulation();

		CommuterFileReader cdr = new CommuterFileReader();
		
//		cdr.setSpatialFilder("09180"); //Bayern
		cdr.addFilter("09180"); //GaPa (Kreis)
		cdr.addFilter("09180113"); //Bad Bayersoien
		cdr.addFilter("09180112"); //Bad Kohlgrub
		cdr.addFilter("09180114"); //Eschenlohe
		cdr.addFilter("09180115"); //Ettal
		cdr.addFilter("09180116"); //Farchant
		cdr.addFilter("09180117"); //Garmisch-Partenkirchen
		cdr.addFilter("09180118"); //Grainau
		cdr.addFilter("09180119"); //Großweil
		cdr.addFilter("09180122"); //Krün
		cdr.addFilter("09180123"); //Mittenwald
		cdr.addFilter("09180124"); //Murnau a Staffelsee
		cdr.addFilter("09180125"); //Oberammergau
		cdr.addFilter("09180126"); //Oberau
		cdr.addFilter("09180127"); //Ohlstadt
		cdr.addFilter("09180128"); //Riegsee
		cdr.addFilter("09180129"); //Saulgrub
		cdr.addFilter("09180131"); //Schwaigen
		cdr.addFilter("09180132"); //Seehausen a Staffelsee
		cdr.addFilter("09180134"); //Uffind a Staffelsee
		cdr.addFilter("09180135"); //Unterammergau
		cdr.addFilter("09180136"); //Wallgau
		
		cdr.read(reverseCommuterFilename, true);
		cdr.read(commuterFilename, false);
		
		initMunicipalities();
//		initLandUse();
		
		log.info("Building amenity quad trees for...");
		double[] bbox = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
		
		log.info("...education");
		educationQT = new QuadTree<ActivityFacility>(bbox[0], bbox[1], bbox[2], bbox[3]);
		for(ActivityFacility af : scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.education.name()).values()){
			educationQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
		}
		log.info("...shop");
		shopQT = new QuadTree<ActivityFacility>(bbox[0], bbox[1], bbox[2], bbox[3]);
		for(ActivityFacility af : scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.shop.name()).values()){
			shopQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
		}
		log.info("...leisure");
		leisureQT = new QuadTree<ActivityFacility>(bbox[0], bbox[1], bbox[2], bbox[3]);
		for(ActivityFacility af : scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.leisure.name()).values()){
			leisureQT.put(af.getCoord().getX(), af.getCoord().getY(), af);
		}
		log.info("...Done.");
		
		createPersonsWithDemographicData(scenario, cdr.getCommuterRelations());
//		createPersons(population, cdr.getCommuterRelations());
		
		return population;
		
	}
	
	/**
	 * Creates counting stations for GaPa
	 * 
	 * @param network
	 * @return
	 */
	public static Counts createCountingStations(Network network){

		//create counts container and set the description, the name and the year of the survey
		Counts counts = new Counts();
		counts.setDescription("eGaP");
		counts.setName("Pendler");
		counts.setYear(2005);
		
		//create counting stations one by one and fill the hourly values
		//Olympiastraße
		//Richtung Süden
		Count count = counts.createAndAddCount(Id.createLinkId("2669"), "Olympiastr Richtung St-Martin-Str");
		count.createVolume(7, 7+8+9+13);
		count.createVolume(8, 16+22+25+40);
		count.createVolume(9, 18+30+36+40);
		count.createVolume(10, 37+41+41+41);
		count.createVolume(16, 45+49+45+70);
		count.createVolume(17, 66+65+66+56);
		count.createVolume(18, 48+69+63+50);
		count.createVolume(19, 48+34+31+24);
		//Richtung Norden
		Count count2 = counts.createAndAddCount(Id.createLinkId("2670"), "Olympiastr Richtung Landratsamt");
		count2.createVolume(7, 12+5+17+24);
		count2.createVolume(8, 26+45+66+62);
		count2.createVolume(9, 48+69+76+92);
		count2.createVolume(10, 79+77+76+72);
		count2.createVolume(16, 65+63+72+72);
		count2.createVolume(17, 81+77+71+79);
		count2.createVolume(18, 84+61+70+62);
		count2.createVolume(19, 58+70+30+53);
		
		//Alpspitzstraße
		//Richtung Süden
		Count count3 = counts.createAndAddCount(Id.createLinkId("5129"), "Alpspitzstr Richtung Süden");
		count3.createVolume(7, 15+10+23+21);
		count3.createVolume(8, 28+30+48+49);
		count3.createVolume(9, 45+45+61+69);
		count3.createVolume(10, 51+65+70+67);
		count3.createVolume(11, 91+51+72+78);
		count3.createVolume(12, 84+84+79+79);
		count3.createVolume(13, 66+67+65+55);
		count3.createVolume(14, 73+52+68+54);
		count3.createVolume(15, 59+75+72+86);
		count3.createVolume(16, 62+34+39+73);
		count3.createVolume(17, 62+71+76+71);
		count3.createVolume(18, 81+61+65+84);
		count3.createVolume(19, 61+57+68+60);
		count3.createVolume(20, 50+45+43+41);
		//Richtung Norden
		Count count4 = counts.createAndAddCount(Id.createLinkId("5128"), "Alpspitzstr Richtung Norden");
		count4.createVolume(7, 11+16+22+23);
		count4.createVolume(8, 14+24+37+62);
		count4.createVolume(9, 45+41+37+34);
		count4.createVolume(10, 52+51+50+47);
		count4.createVolume(11, 69+58+51+63);
		count4.createVolume(12, 64+77+65+56);
		count4.createVolume(13, 85+60+48+69);
		count4.createVolume(14, 71+45+50+56);
		count4.createVolume(15, 47+45+61+44);
		count4.createVolume(16, 55+53+54+49);
		count4.createVolume(17, 70+51+78+76);
		count4.createVolume(18, 66+71+71+59);
		count4.createVolume(19, 74+59+50+43);
		count4.createVolume(20, 45+41+31+33);
		
		//Burgstraße
		//Burgstraße Richtung Süden
		Count count5 = counts.createAndAddCount(Id.createLinkId("22050"), "Burgstr Richtung Süden");
		count5.createVolume(7, 28+43+57+72);
		count5.createVolume(8, 85+119+140+170);
		count5.createVolume(9, 130+118+139+139);
		count5.createVolume(10, 115+123+122+148);
		count5.createVolume(16, 111+139+106+140);
		count5.createVolume(17, 157+130+131+126);
		count5.createVolume(18, 133+132+113+124);
		count5.createVolume(19, 131+115+103+127);
		//Burgstraße Richtung Norden
		Count count6 = counts.createAndAddCount(Id.createLinkId("16043"), "Burgstr Richtung Norden");
		count6.createVolume(7,77+18+34+59);
		count6.createVolume(8, 69+60+80+97);
		count6.createVolume(9, 79+79+78+93);
		count6.createVolume(10, 89+97+117+104);
		count6.createVolume(16, 141+134+121+118);
		count6.createVolume(17, 173+138+171+169);
		count6.createVolume(18, 183+166+161+169);
		count6.createVolume(19, 162+131+105+108);
		
		//Alleestraße Ost
		//Richtung Osten
		Count count7 = counts.createAndAddCount(Id.createLinkId("16083"), "Alleestraße Richtung Osten");
		count7.createVolume(7, 30+16+20+33);
		count7.createVolume(8, 41+56+90+96);
		count7.createVolume(9, 80+93+89+104);
		count7.createVolume(10, 77+77+87+98);
		count7.createVolume(16, 110+115+97+100);
		count7.createVolume(17, 121+87+96+101);
		count7.createVolume(18, 84+101+85+70);
		count7.createVolume(19, 89+67+62+55);
		//Richtung Westen
		Count count8 = counts.createAndAddCount(Id.createLinkId("16082"), "Alleestraße Richtung Westen");
		count8.createVolume(7, 10+13+20+36);
		count8.createVolume(8, 35+42+61+62);
		count8.createVolume(9, 46+59+82+78);
		count8.createVolume(10, 76+75+77+73);
		count8.createVolume(16, 94+94+105+77);
		count8.createVolume(17, 134+111+132+84);
		count8.createVolume(18, 140+86+99+111);
		count8.createVolume(19, 106+82+88+74);
		
		//Promenadenstraße
		//Richtung Süden
		Count count9 = counts.createAndAddCount(Id.createLinkId("27194"), "Promenadenstraße Richtung Süden");
		count9.createVolume(7, 32+39+54+65);
		count9.createVolume(8, 70+103+117+155);
		count9.createVolume(9, 100+123+143+145);
		count9.createVolume(10, 130+130+137+156);
		count9.createVolume(16, 128+142+118+142);
		count9.createVolume(17, 170+157+159+140);
		count9.createVolume(18, 180+139+127+137);
		count9.createVolume(19, 139+132+115+125);
		//Richtung Norden
		Count count10 = counts.createAndAddCount(Id.createLinkId("798"), "Promenadenstraße Richtung Norden");
		count10.createVolume(7, 97+21+32+62);
		count10.createVolume(8, 74+68+106+117);
		count10.createVolume(9, 87+105+90+121);
		count10.createVolume(10, 113+98+138+120);
		count10.createVolume(16, 179+157+126+122);
		count10.createVolume(17, 188+128+144+179);
		count10.createVolume(18, 145+176+157+148);
		count10.createVolume(19, 160+133+107+122);
		
		//Loisachstraße
		//Richtung Osten
		Count count11 = counts.createAndAddCount(Id.createLinkId("12348"), "Loisachstraße Richtung Osten");
		count11.createVolume(7, 11+7+12+16);
		count11.createVolume(8, 21+26+45+48);
		count11.createVolume(9, 39+53+45+50);
		count11.createVolume(10, 36+46+35+47);
		count11.createVolume(16, 37+44+47+52);
		count11.createVolume(17, 39+54+70+56);
		count11.createVolume(18, 76+49+35+44);
		count11.createVolume(19, 38+38+32+20);
		//Richtung Westen
		Count count12 = counts.createAndAddCount(Id.createLinkId("12347"), "Loisachstraße Richtung Westen");
		count12.createVolume(7, 7+11+15+29);
		count12.createVolume(8, 36+35+67+49);
		count12.createVolume(9, 43+40+46+46);
		count12.createVolume(10, 44+38+31+30);
		count12.createVolume(16, 42+43+48+31);
		count12.createVolume(17, 54+42+51+35);
		count12.createVolume(18, 46+38+32+51);
		count12.createVolume(19, 45+38+48+55);
		
		return counts;
		
	}
	
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
		
		OsmObjectsToFacilitiesParser reader = new OsmObjectsToFacilitiesParser(GAPMain.dataDir + "/Netzwerk/garmisch-latest.osm", GAPMain.ct, osmToMatsimTypeMap, keys);
		reader.parse();
		reader.writeFacilities(GAPMain.matsimInputDir + "facilities/facilities.xml");
		reader.writeFacilityAttributes(GAPMain.matsimInputDir + "facilities/facilityAttribues.xml");
		reader.writeFacilityCoordinates(GAPMain.matsimInputDir + "facilities.csv");
		
		new FacilitiesReaderMatsimV1(scenario).readFile(GAPMain.matsimInputDir + "facilities/facilities.xml");
		
	}
	
	/**
	 * Initializes land use data from OSM.
	 * Since the data concerning home and work locations is pretty useless, this method mainly collects information about osm objects
	 * with the tags
	 * <ul>
	 * <li>natural (e.g. bare rock)</li>
	 * <li>landuse (e.g. forest, meadow, ...)</li>
	 * <li>touristic poi's</li>
	 * </ul> 
	 * @deprecated The land use data from osm is not detailed enough for our purposes...
	 */
	@SuppressWarnings({ "unused" })
	private static void initLandUse(){
		
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize("/home/dhosse/Downloads/oberbayern-latest.shp/landuse.shp");
		
		List<String> homeTypes = new ArrayList<>();
		homeTypes.add("residential");
		List<String> workTypes = new ArrayList<>();
		workTypes.add("commercial");
		workTypes.add("industrial");
		workTypes.add("retail");
		
		for(SimpleFeature feature : features){
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			String type = (String) feature.getAttribute("type");
			
			if(homeTypes.contains(type)){
				
				Coord c = GAPMain.ct.transform(MGC.point2Coord(geometry.getCentroid()));
				if(homeLocations.getMaxEasting() >= c.getX() && homeLocations.getMinEasting() <= c.getX() &&
						homeLocations.getMaxNorthing() >= c.getY() && homeLocations.getMinNorthing() <= c.getY()){
					homeLocations.put(c.getX(), c.getY(), geometry);
				}
				
			} else if(workTypes.contains(type)){
				
				Coord c = GAPMain.ct.transform(MGC.point2Coord(geometry.getCentroid()));
				if(workLocations.getMaxEasting() >= c.getX() && workLocations.getMinEasting() <= c.getX() &&
						workLocations.getMaxNorthing() >= c.getY() && workLocations.getMinNorthing() <= c.getY()){
					workLocations.put(c.getX(), c.getY(), geometry);
				}
				
			}
			
		}
		
		Collection<SimpleFeature> features2 = new ShapeFileReader().readFileAndInitialize(GAPMain.dataDir + "20150112_Unternehmen_Adressen.shp");
		for(SimpleFeature feature : features2){
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Coord c = GAPMain.ct.transform(MGC.point2Coord(geometry.getCentroid()));
			workLocations.put(c.getX(), c.getY(), geometry);
			
		}
		
	}

	/**
	 * 
	 * Parses shape files in order to extract their geometries.
	 * This is needed to shoot activity coordinates in municipalities or counties.
	 * 
	 */
	private static void initMunicipalities(){
		
		//read municipalities shape files and pass the geometries and their ids into a map
		Collection<SimpleFeature> features = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "/Gemeinden/Gemeinden_2009_WGS84_mit Raumtypen_und_Gemeindetypen.shp");
		
		for(SimpleFeature feature : features){
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Long id = (Long) feature.getAttribute("GEM_KENNZ");
			
			StringBuffer sb = new StringBuffer();
			sb.append("0");
			sb.append(id.toString());
			
			munId2Geometry.put(sb.toString(), geometry);
			
		}
		
		//read county shape file
		Collection<SimpleFeature> features2 = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "kreise_2009_12.shp");
		
		for(SimpleFeature feature : features2){
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			Long id = (Long) feature.getAttribute("GKZ");
			
			StringBuffer sb = new StringBuffer();
			sb.append("0");
			sb.append(id.toString());
			
			munId2Geometry.put(sb.toString(), geometry);
			
		}
		
		Collection<SimpleFeature> features3 = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "Gebietsstand_2007/gemeinden_2007_bebaut.shp");
		
		Geometry allBuiltAreas = null;
		
		for(SimpleFeature feature : features3){
			
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			String name = (String)feature.getAttribute("GEMNAME");
			
			munName2Geometry.put(name, geometry);
			
//			if(allBuiltAreas == null){
//				
//				allBuiltAreas = geometry;
//				
//			} else{
//				
//				allBuiltAreas = allBuiltAreas.union(geometry);
//				
//			}
			
		}
		
//		for(Entry<String,Geometry> gEntry : munId2Geometry.entrySet()){
//			
//			String key = gEntry.getKey();
//			
//			Geometry g = gEntry.getValue();
//			
//			Geometry intersection = g.intersection(allBuiltAreas);
//			
//			if(intersection != null){
//				
//				geometries.put(key, intersection);
//				
//			}
//			
//		}
		
	}
	
	/**
	 * This method creates an initial population of car commuters based on the survey by the Arbeitsagentur.
	 * The commuter data elements are used as templates.
	 * 
	 * @param population
	 * @param relations
	 */
	private static void createCommuters(Population population, List<CommuterDataElement> relations){

		PopulationFactoryImpl factory = (PopulationFactoryImpl) population.getFactory();
		
		//parse over commuter relations
		for(CommuterDataElement relation : relations){
			
			//this is just for the reason that the shape file does not contain any diphtongs
			//therefore, they are removed from the relation names as well
			String[] diphtong = new String[]{"ä","ö","ü","ß"};
			
			String fromId = relation.getFromId();
			String fromName = relation.getFromName();
			String toId = relation.getToId();
			String toName = relation.getToName();
			
			if(fromName.contains(",")){
				String[] f = fromName.split(",");
				fromName = f[0];
			}
			
			if(toName.contains(",")){
				String[] f = toName.split(",");
				toName = f[0];
			}
			
			for(String s : diphtong){
				fromName.replace(s, "");
				toName.replace(s, "");
			}
			
			//assert the transformation to be gauss-kruger (transformation of the county geometries)
			String fromTransf = "GK4";
			String toTransf = "GK4";
			
			//get the geometries mapped to the keys specified in the relation
			//this should be municipalities
			Geometry from = munName2Geometry.get(fromName);
			Geometry to = munName2Geometry.get(toName);
			
			//if any geometry is null, get the county geometry mapped to the key and set the transformation to utm32n
			if(from == null){
				from = munId2Geometry.get(toId);
				fromTransf = "UTM";
			}
			
			if(to == null){
				to = munId2Geometry.get(toId);
				toTransf = "UTM";
			}
			
			//if still any geometry should be null, skip this entry
			if(from != null && to != null){
				
				//create as many persons as are specified in the commuter relation
				for(int i = 0; i < relation.getCommuters(); i++){
					
					Person person = factory.createPerson(Id.createPersonId(fromId + "_" + toId + "_" + i));
					
					Plan plan = factory.createPlan();
					
					Coord homeCoord = null;
					Coord workCoord = null;
					
					//shoot the activity coords inside the given geometries
					if(fromTransf.equals("GK4")){
						homeCoord = GAPMain.gk4ToUTM32N.transform(shoot(from));
					} else{
						homeCoord = GAPMain.ct.transform(shoot(from));
					}
					if(toTransf.equals("GK4")){
						workCoord = GAPMain.gk4ToUTM32N.transform(shoot(to));
					} else{
						workCoord = GAPMain.ct.transform(shoot(to));
					}
					
					Activity actHome = factory.createActivityFromCoord("home", homeCoord);
					
					//create an activity end time (they can either be equally or normally distributed, depending on the boolean that
					//has been passed to the method
					double endTime = 0.;
					if(equal){
						endTime = createEquallyDistributedTime(5);
					} else{
						endTime = createNormallyDistributedTime(7);
					}
					
					actHome.setEndTime(endTime);
					plan.addActivity(actHome);
					
					plan.addLeg(factory.createLeg(TransportMode.car));
					
					//create other activity and set the end time nine hours after the first activity's end time
					Activity actWork = factory.createActivityFromCoord("work", workCoord);
					endTime = endTime + 9*3600;
					actWork.setEndTime(endTime);
					plan.addActivity(actWork);
					
					plan.addLeg(factory.createLeg(TransportMode.car));
					
					actHome = factory.createActivityFromCoord("home", homeCoord);
					plan.addActivity(actHome);
					
					person.addPlan(plan);
					person.setSelectedPlan(plan);
					
					population.addPerson(person);
					
				}
				
			} else{
				
				log.warn("Could not find geometries for:" + fromId + " (" + fromName + "), " + toId + " (" + toName + ").");
				log.warn("Continuing with next relation...");
				
			}
			
		}
		
	}
	
	private static void createPersonsWithDemographicData(Scenario scenario, List<CommuterDataElement> relations){
		
		MiDCSVReader reader = new MiDCSVReader();
		reader.read(GAPMain.matsimInputDir + "MID_Daten_mit_Wegeketten/travelsurvey.csv");
		Map<String, MiDSurveyPerson> persons = reader.getPersons();
		
		MiDPersonGroupTemplates templates = new MiDPersonGroupTemplates();
		
		for(MiDSurveyPerson person : persons.values()){
			
			templates.handlePerson(person);
			
		}
		
		Map<String, MiDPersonGroupData> personGroupData = EgapPopulationUtils.createMiDPersonGroups();
		
//		createCommuters(scenario.getPopulation(), relations);
		createPersonsFromGroup(0, 8577, scenario, personGroupData, templates);
		createPersonsFromGroup(10, 13658, scenario, personGroupData, templates);
		createPersonsFromGroup(20, 7606, scenario, personGroupData, templates);
		createPersonsFromGroup(30, 8162, scenario, personGroupData, templates);
		createPersonsFromGroup(40, 17516, scenario, personGroupData, templates);
		createPersonsFromGroup(50, 13575, scenario, personGroupData, templates);
		createPersonsFromGroup(60, 9383, scenario, personGroupData, templates);
		createPersonsFromGroup(70, 5497, scenario, personGroupData, templates);
		createPersonsFromGroup(80, 1472, scenario, personGroupData, templates);
		
		new ObjectAttributesXmlWriter(agentAttributes).writeFile(GAPMain.matsimInputDir + "Pläne/agentAttributes.xml.gz");
		
	}

	private static void createPersonsFromGroup(int a0, int amount, Scenario scenario, Map<String, MiDPersonGroupData> groupData, MiDPersonGroupTemplates templates){
		
		PopulationFactoryImpl factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		
		for(int i = 0; i < amount; i++){
			
			Person person = factory.createPerson(Id.createPersonId(a0 + "_" + (a0 + 9) + "_" + i));
			Plan plan = factory.createPlan();
			
			int age = (int)(a0 + GAPMain.random.nextDouble() * (a0 + 9));
			agentAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
			int sex = EgapPopulationUtils.setSex(age);
			agentAttributes.putAttribute(person.getId().toString(), Global.SEX, sex);
			
			MiDPersonGroupData data = groupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));
			
			if(data == null){
				i--;
				continue;
			}
			
			boolean isEmployed = setBooleanAttribute(person.getId().toString(), data.getpEmployment(), Global.EMPLOYED);
			boolean carAvail = setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
			boolean hasLicense = setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
			
			String personHash = EgapHashGenerator.generatePersonHash(age, sex, carAvail, hasLicense, isEmployed);
			
			List<MiDSurveyPerson> templatePersons = templates.getPersonGroups().get(personHash);
			
			if(templatePersons != null){
				
				if(templatePersons.size() > 0){
					
					MiDSurveyPerson templatePerson = null;
					
					do{
						
						int randomIndex = (int)(GAPMain.random.nextDouble() * templatePersons.size());
						templatePerson = templatePersons.get(randomIndex);
						
					} while (templatePerson == null);
					
					//create a home activity as starting point
					Coord homeCoord = GAPMain.gk4ToUTM32N.transform(shoot(munId2Geometry.get("09180")));
					Activity homeActivity = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);

					Leg firstLeg = (Leg) templatePerson.getPlan().getPlanElements().get(0);
					
					double timeShift = 0.;
					
					if(firstLeg.getDepartureTime() != Time.UNDEFINED_TIME){

						do{

							timeShift = createRandomEndTime();

							if(firstLeg.getDepartureTime() + timeShift > 0){
								homeActivity.setEndTime(firstLeg.getDepartureTime() + timeShift);
							} else{
								timeShift = 0;
							}
							
						} while(timeShift == 0);
						
					}
					
					plan.addActivity(homeActivity);
					
					int index = 1;
					
					for(PlanElement pe : templatePerson.getPlan().getPlanElements()){
						
						if(pe instanceof Activity){
							
							Activity act = (Activity)pe;
							
							String type = act.getType();
							double startTime = act.getStartTime();
							double endTime = act.getEndTime();
							
							Coord c = null;
							
							if(type.equals(Global.ActType.home.name())){
								
								c = homeCoord;
								
							} else if(type.equals(Global.ActType.other.name()) || type.equals(Global.ActType.work.name())){ //if the act type equals "other" or "work", shoot a random coordinate
								
								Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								double x = GAPMain.random.nextDouble() * distance;
								double y = Math.sqrt(distance * distance - x * x);
								
								c = new CoordImpl(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
							} else{ //for all activities apart from "other" and "home", shoot a random coordinate and get the nearest activity facility
								
								Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								ActivityFacility facility = null;
								
								do{
									
									double x = GAPMain.random.nextDouble() * distance;
									double y = Math.sqrt(distance * distance - x * x);
									
									if(type.equals(Global.ActType.education.name())){
										
										facility = educationQT.get(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.shop.name())){
										
										facility = shopQT.get(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									} else if(type.equals(Global.ActType.leisure.name())){
										
										facility = leisureQT.get(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
										
									}
									
								}while(facility == null);
								
								c = new CoordImpl(facility.getCoord().getX(), facility.getCoord().getY());
								
							}
							
							Activity newAct = factory.createActivityFromCoord(type, c);
							
							newAct.setStartTime(startTime + timeShift);
							
							//acts must not have zero duration, a minimum duration of 0.5 hours is assumed...
							if(act.getEndTime() - act.getStartTime() <= 0){
								timeShift += 1800;
							}
							
							newAct.setEndTime(endTime + timeShift);
							plan.addActivity(newAct);
							
						} else{
							
							Leg leg = (Leg)pe;
							
							
							plan.addLeg(factory.createLeg(leg.getMode()));
							
						}
						
						index++;
						
					}
					
					person.addPlan(plan);
					
					scenario.getPopulation().addPerson(person);
					
				} else{
					
					i--;
					
				}
				
			} else{
				
				i--;
				
			}
			
		}
		
	}
	
	private static boolean setBooleanAttribute(String personId, double proba, String attribute){
		
		double random = GAPMain.random.nextDouble();
		boolean attr = random <= proba ? true : false;
		agentAttributes.putAttribute(personId, attribute, attr);
		
		return attr;
		
	}
	
//	private static void createPersons(int a0, int aX, int amount, Scenario scenario, PopulationFactory factory, Set<MiDPersonGroupData> personGroupData){
//		
//		for(int i = 0; i < 7606; i++){
//			
//			Person p = factory.createPerson(Id.createPersonId(a0 + "_" + aX + "_" + i));
//			
//			Plan plan = factory.createPlan();
//			
//			int ag = (int) (a0 + GAPMain.random.nextDouble() * aX);
//			agentAttributes.putAttribute(p.getId().toString(), Global.AGE, ag);
//			int sex = EgapPopulationUtils.setSex(ag);
//			agentAttributes.putAttribute(p.getId().toString(), Global.SEX, sex);
//			
//			Coord homeCoord = GAPMain.gk4ToUTM32N.transform(shoot(munId2Geometry.get("09180")));
//			
//			Activity home = factory.createActivityFromCoord(Global.ActType.home.name(), homeCoord);
//			plan.addActivity(home);
//			
//			int planIndex = 0;
//			
//			for(MiDPersonGroupData pgd : personGroupData){
//				
//				if(pgd.getA0() == a0 && pgd.getAX() == aX && pgd.getSex() == sex){
//					
//					double rnd = GAPMain.random.nextDouble();
//					
//					if(pgd.getData().size() < 1) continue;
//					
//					MiDData template = pgd.getData().get((int)(rnd*(pgd.getData().size() - 1)));
//					
//					MiDStatsContainer stats = null;
//					
//					for(MiDWaypoint wp : template.getWayPoints()){
//						
//						planIndex+=2;
//						
//						String purpose = wp.getPurpose();
//						
//						if(purpose.equalsIgnoreCase(Global.ActType.home.name())){
//							
//							stats = pgd.getHomeStats();
//							
//						} else if(purpose.equalsIgnoreCase(Global.ActType.work.name())){
//							
//							stats = pgd.getWorkStats();
//							
//						} else if(purpose.equalsIgnoreCase(Global.ActType.education.name())){
//							
//							stats = pgd.getEducationStats();
//							
//						} else if(purpose.equalsIgnoreCase(Global.ActType.shop.name())){
//							
//							stats = pgd.getShopStats();
//							
//						} else if(purpose.equalsIgnoreCase(Global.ActType.leisure.name())){
//							
//							stats = pgd.getLeisureStats();
//							
//						} else {
//							
//							purpose = Global.ActType.other.name();
//							stats = pgd.getOtherStats();
//							
//						}
//						
//						String mode = wp.getMode();
//						if(wp.getMode().equals("car (passenger)")){
//							
//							mode = TransportMode.ride;
//							
//						}
//						
//						double startTime = 0.;
//						
//						int cnt = 0;
//						
//						do{
//							startTime = wp.getStartTime() + createRandomEndTime();
//							cnt++;
//						} while(startTime <= 0 && cnt < 10);
//						
//						if(cnt >= 10){
//							
//							startTime = wp.getStartTime();
//							
//						}
//						
//						double endTime = startTime + (wp.getEndTime() - wp.getStartTime());
//						if(endTime < startTime){
//							endTime = 24*3600;
//						}
//						
//						Leg leg = factory.createLeg(mode);
//						plan.addLeg(leg);
//						
//						double distance = wp.getDistance() != Double.NEGATIVE_INFINITY ? wp.getDistance() : 500.;
//						
//						double xD = 2 * (GAPMain.random.nextDouble() - 0.5) * distance;
//						double yD = Math.sqrt(distance*distance - xD*xD);
//						
//						Activity lastAct = (Activity) plan.getPlanElements().get(planIndex - 2);
//						
//						if(startTime != Time.UNDEFINED_TIME){
//							lastAct.setEndTime(startTime);
//						} else{
//							lastAct.setMaximumDuration(3600);
//						}
//						
//						Coord c = new CoordImpl(lastAct.getCoord().getX() + xD, lastAct.getCoord().getY() + yD);
//						
//						if(purpose.equals(Global.ActType.home.name())){
//							
//							c = home.getCoord();
//							
//						} else if(purpose.equals(Global.ActType.education)){
//							
//							c = educationQT.get(c.getX(), c.getY()).getCoord();
//							
//						} else if(purpose.equals(Global.ActType.shop)){
//							
//							c = shopQT.get(c.getX(), c.getY()).getCoord();
//							
//						} else if(purpose.equals(Global.ActType.leisure)){
//							
//							c = leisureQT.get(c.getX(), c.getY()).getCoord();
//							
//						}
//						
//						Activity act = factory.createActivityFromCoord(purpose, c);
//						act.setStartTime(endTime);
//						plan.addActivity(act);
//						
//					}
//					
//					break;
//					
//				}
//				
//			}
//			
//			p.addPlan(plan);
//			p.setSelectedPlan(plan);
//			
//			scenario.getPopulation().addPerson(p);
//			
//		}
//		
//	}
	
	private static Coord shoot(Geometry geometry){
		
		Point point = null;
		double x, y;
		
		do{
			
			x = geometry.getEnvelopeInternal().getMinX() + GAPMain.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxX() - geometry.getEnvelopeInternal().getMinX());
	  	    y = geometry.getEnvelopeInternal().getMinY() + GAPMain.random.nextDouble() * (geometry.getEnvelopeInternal().getMaxY() - geometry.getEnvelopeInternal().getMinY());
	  	    point = MGC.xy2Point(x, y);
			
		}while(!geometry.contains(point));
		
		return MGC.point2Coord(point);
		
	}
	
	private static double createEquallyDistributedTime( double from){
		
		return from * 3600 + GAPMain.random.nextDouble() * 2 * 3600;
		
	}
	
	private static double createNormallyDistributedTime(double mean){
		
		double r1 = GAPMain.random.nextDouble();
		double r2 = GAPMain.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		
		double endTime = 60*60 * normal + mean * 3600;
		
		return endTime;
		
	}
	
	private static double createRandomEndTime(){
		
		//draw two random numbers [0;1] from uniform distribution
		double r1 = GAPMain.random.nextDouble();
		double r2 = GAPMain.random.nextDouble();
		
		//Box-Muller-Method in order to get a normally distributed variable
		double normal = Math.cos(2 * Math.PI * r1) * Math.sqrt(-2 * Math.log(r2));
		double endTime = 20*60 * normal;
		
		return endTime;
		
	}
	
}
