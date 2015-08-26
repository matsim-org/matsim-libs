package playground.dhosse.gap.scenario;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.OsmNetworkReader;
import org.matsim.core.utils.misc.Time;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.scenario.mid.MiDCSVReader;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupData;
import playground.dhosse.gap.scenario.mid.MiDPersonGroupTemplates;
import playground.dhosse.gap.scenario.mid.MiDSurveyPerson;
import playground.dhosse.gap.scenario.population.Municipalities;
import playground.dhosse.gap.scenario.population.Municipality;
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
	
	private static QuadTree<Geometry> builtAreaQT;
	
	static int counter = 0;
	static boolean equal;
	
	static int nPersons = 0;
	
	static QuadTree<ActivityFacility> workLocations;
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
//		homeLocations = new QuadTree<Geometry>(boundary[0], boundary[1], boundary[2], boundary[3]);
		workLocations = new QuadTree<ActivityFacility>(boundary[0], boundary[1], boundary[2], boundary[3]);
		
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
		
		initMunicipalities(scenario);
		
		readWorkplaces(scenario, GAPMain.dataDir + "20150112_Unternehmen_Adressen_geokoordiniert.csv");
		
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
	
	private static void readWorkplaces(Scenario scenario, String file){
		
		BufferedReader reader = IOUtils.getBufferedReader(file);
		
		final int idxX = 0;
		final int idxY = 1;
		
		int counter = 0;
		
		try {
			
			String line = reader.readLine();
			
			while((line = reader.readLine()) != null){
				
				String[] parts = line.split(",");
				
				Coord coord = GAPMain.ct.transform(new CoordImpl(Double.parseDouble(parts[idxX]), Double.parseDouble(parts[idxY])));
				
				ActivityFacility facility = scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(Global.ActType.work.name() + "_" + counter, ActivityFacility.class), coord);
				ActivityOption work = scenario.getActivityFacilities().getFactory().createActivityOption(Global.ActType.work.name());
				facility.addActivityOption(work);
				
				scenario.getActivityFacilities().addActivityFacility(facility);
				
				workLocations.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
				
				counter++;
				
			}
			
		} catch (IOException e) {
			
			e.printStackTrace();
			
		}
		
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
	 * 
	 * Parses shape files in order to extract their geometries.
	 * This is needed to shoot activity coordinates in municipalities or counties.
	 * 
	 */
	private static void initMunicipalities(Scenario scenario){
		
		builtAreaQT = new QuadTree<>(4070000, 5190000, 4730000, 6106925);
		
		Collection<SimpleFeature> builtAreas = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "Gebietsstand_2007/gemeinden_2007_bebaut.shp");
		
		log.info("Processing built areas...");
		
		for(SimpleFeature f : builtAreas){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Long identifier = (Long) f.getAttribute("GEM_KENNZ");
			
			String id = "0" + Long.toString(identifier);
			
			munId2Geometry.put(id, geometry);
			
			Coord c = MGC.point2Coord(geometry.getCentroid());
			builtAreaQT.put(c.getX(), c.getY(), geometry);
			
		}
		
		log.info("Processing administrative boundaries...");
		
		Collection<SimpleFeature> counties = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "Gebietsstand_2007/kreise_2007_12.shp");
		
		for(SimpleFeature f : counties){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			String identifier = (String) f.getAttribute("KENNZAHL");

			munId2Geometry.put(identifier, geometry);
			
		}
		
		Collection<SimpleFeature> regBez = new ShapeFileReader().readFileAndInitialize("/home/dhosse/Downloads/germany/DEU_adm/DEU_adm2.shp");
		
		for(SimpleFeature f : regBez){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Long identifier = (Long) f.getAttribute("ID_2");
			
			munId2Geometry.put("0" + Long.toString(identifier), geometry);
			
		}
		
		Collection<SimpleFeature> c = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "bundeslaender.shp");
		
		for(SimpleFeature f : c){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Long identifier = (Long) f.getAttribute("LAND");
			
			munId2Geometry.put("0" + Long.toString(identifier), geometry);
			
		}
		
		Collection<SimpleFeature> countries = new ShapeFileReader().readFileAndInitialize(GAPMain.adminBordersDir + "europa_staaten.shp");
		
		for(SimpleFeature f : countries){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			String identifier = (String) f.getAttribute("NUTS0");
			
			munId2Geometry.put("0" + identifier, geometry);
			
		}
		
	}
	
	/**
	 * This method creates an initial population of car commuters based on the survey by the Arbeitsagentur.
	 * The commuter data elements are used as templates.
	 * 
	 * @param population
	 * @param relations
	 */
	private static void createCommuters(Population population, Collection<CommuterDataElement> relations, Map<String, MiDPersonGroupData> groupData){

		PopulationFactoryImpl factory = (PopulationFactoryImpl) population.getFactory();
		
		int gpCounter = 0;
		
		//parse over commuter relations
		for(CommuterDataElement relation : relations){
			
			//this is just for the reason that the shape file does not contain any diphtongs
			//therefore, they are removed from the relation names as well
			String[] diphtong = {"ä", "ö", "ü", "ß"};
			
			String fromId = relation.getFromId();
			String fromName = relation.getFromName();
			String toId = relation.getToId();
			String toName = relation.getToName();
			
			if(fromId.startsWith("09180")){
				gpCounter++;
			}
			
			if(fromName.contains(",")){
				String[] f = fromName.split(",");
				fromName = f[0];
			}
			
			if(toName.contains(",")){
				String[] f = toName.split(",");
				toName = f[0];
			}
			
			for(String s : diphtong){
				fromName = fromName.replace(s, "");
				toName = toName.replace(s, "");
			}
			
			//assert the transformation to be gauss-kruger (transformation of the municipal, county and geometries)
			String fromTransf = "GK4";
			String toTransf = "GK4";
			
			if(fromId.length() == 3 && !fromId.contains("AT")){
				fromTransf = "";
			}
			if(toId.length() == 3){
				toTransf = "";
			}
			
			//get the geometries mapped to the keys specified in the relation
			//this should be municipalities
			Geometry from = munId2Geometry.get(fromId);
			Geometry to = munId2Geometry.get(toId);
			
			//if still any geometry should be null, skip this entry
			if(from != null && to != null){
				
				//create as many persons as are specified in the commuter relation
				for(int i = 0; i < relation.getCommuters(); i++){
					
					Person person = factory.createPerson(Id.createPersonId(fromId + "_" + toId + "_" + i));
					
					int age = EgapPopulationUtils.setAge(20, 79);
					agentAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
					int sex = EgapPopulationUtils.setSex(age);
					agentAttributes.putAttribute(person.getId().toString(), Global.SEX, sex);
					
					MiDPersonGroupData data = groupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));

					boolean hasLicense = setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
					boolean carAvail = setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
					
					agentAttributes.putAttribute(person.getId().toString(), Global.EMPLOYED, true);
					agentAttributes.putAttribute(person.getId().toString(), Global.LICENSE, hasLicense);
					agentAttributes.putAttribute(person.getId().toString(), Global.CAR_AVAIL, carAvail);
					
					if(fromId.contains("09180")){
						agentAttributes.putAttribute(person.getId().toString(), Global.INHABITANT, true);
					}
					
					if(hasLicense){
						
						agentAttributes.putAttribute(person.getId().toString(), Global.POTENTIAL_CARSHARING_USER, true);
						
						if(carAvail){
							
							agentAttributes.putAttribute(person.getId().toString(), Global.COMMUTER, true);
							
						}
						
					}
					
					createOrdinaryODPlan(factory, person, fromId, toId, from, to, fromTransf, toTransf);
					
					population.addPerson(person);
					
				}
				
			} else{
				
				log.warn("Could not find geometries for:" + fromId + " (" + fromName + "), " + toId + " (" + toName + ").");
				log.warn("Continuing with next relation...");
				
			}
			
		}
		
		nPersons -= gpCounter;
		
	}
	
	/**
	 * This is the standard version of plan generation. A simple home-work-home journey is created.
	 *  
	 * @param factory
	 * @param person
	 * @param fromId
	 * @param toId
	 * @param from
	 * @param to
	 * @param fromTransf
	 * @param toTransf
	 */
	private static void createOrdinaryODPlan(PopulationFactory factory, Person person, String fromId, String toId, Geometry from, Geometry to, String fromTransf, String toTransf){
		
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
		
		if(fromId.length() < 8 && !fromId.contains("A")){
			
			Coord c = GAPMain.UTM32NtoGK4.transform(homeCoord);
			Geometry nearestToHome = builtAreaQT.get(c.getX(), c.getY());
			homeCoord = GAPMain.gk4ToUTM32N.transform(shoot(nearestToHome));
			
		}
		
		if(toId.length() < 8 && !toId.contains("A")){
			
			Coord c = GAPMain.UTM32NtoGK4.transform(workCoord);
			Geometry nearestToWork = builtAreaQT.get(c.getX(), c.getY());
			workCoord = GAPMain.gk4ToUTM32N.transform(shoot(nearestToWork));
			if(toId.startsWith("09180")){
				workCoord = workLocations.get(workCoord.getX(), workCoord.getY()).getCoord();
			}
			
		}
		
		Activity actHome = factory.createActivityFromCoord("home", homeCoord);
		actHome.setStartTime(0.);
		
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
		actWork.setStartTime(actHome.getEndTime() + 3600);
		endTime = endTime + (9 + 2*GAPMain.random.nextDouble())*3600;
		actWork.setEndTime(endTime);
		plan.addActivity(actWork);
		
		plan.addLeg(factory.createLeg(TransportMode.car));
		
		actHome = factory.createActivityFromCoord("home", homeCoord);
		actHome.setStartTime(actWork.getEndTime() + 3600);
		plan.addActivity(actHome);
		
		person.addPlan(plan);
		person.setSelectedPlan(plan);
		
	}
	
	private static void createPersonsWithDemographicData(Scenario scenario, Map<String, CommuterDataElement> relations){
		
		MiDCSVReader reader = new MiDCSVReader();
		reader.read(GAPMain.matsimInputDir + "MID_Daten_mit_Wegeketten/travelsurvey.csv");
		Map<String, MiDSurveyPerson> persons = reader.getPersons();
		
		MiDPersonGroupTemplates templates = new MiDPersonGroupTemplates();
		
		for(MiDSurveyPerson person : persons.values()){
			
			templates.handlePerson(person);
			
		}
		
		Map<String, MiDPersonGroupData> personGroupData = EgapPopulationUtils.createMiDPersonGroups();
		
		for(Entry<String, Municipality> entry : Municipalities.getMunicipalities().entrySet()){
			
			createPersonsFromPersonGroup(entry.getKey(), 10, 18, 19, entry.getValue().getnStudents(), scenario, personGroupData, templates);
			
			int nCommuters = 0;
			List<String> keysToRemove = new ArrayList<>();
			
			for(String relation : relations.keySet()){
				
				if(relation.startsWith(entry.getKey())){

					nCommuters += relations.get(relation).getCommuters();
					createCommutersFromKey(scenario, relations.get(relation), personGroupData, templates);
					keysToRemove.add(relation);
					
				}
				
			}
			
			for(String s : keysToRemove){
				relations.remove(s);
			}
			
			createPersonsFromPersonGroup(entry.getKey(), 20, 65, 69, entry.getValue().getnAdults() - nCommuters, scenario, personGroupData, templates);
			createPersonsFromPersonGroup(entry.getKey(), 65, 89, 89, entry.getValue().getnPensioners(), scenario, personGroupData, templates);
			
		}
		
		nPersons = scenario.getPopulation().getPersons().size();
		
		createCommuters(scenario.getPopulation(), relations.values(), personGroupData);
		
//		createCommuters(scenario.getPopulation(), relations, personGroupData);
//		createPersonsFromPersonGroup(0, 8577, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(10, 13658, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(20, 7606, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(30, 8162, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(40, 17516, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(50, 13575, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(60, 9383, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(70, 5497, scenario, personGroupData, templates);
//		createPersonsFromPersonGroup(80, 1472, scenario, personGroupData, templates);
		
		new ObjectAttributesXmlWriter(agentAttributes).writeFile(GAPMain.matsimInputDir + "Pläne/agentAttributes.xml.gz");
		
	}
	
	private static void createCommutersFromKey(Scenario scenario, CommuterDataElement relation, Map<String, MiDPersonGroupData> personGroupData, MiDPersonGroupTemplates templates){
		
		PopulationFactoryImpl factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		
		//parse over commuter relations
			//this is just for the reason that the shape file does not contain any diphtongs
			//therefore, they are removed from the relation names as well
			String[] diphtong = {"ä", "ö", "ü", "ß"};
			
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
				fromName = fromName.replace(s, "");
				toName = toName.replace(s, "");
			}
			
			//assert the transformation to be gauss-kruger (transformation of the municipal, county and geometries)
			String fromTransf = "GK4";
			String toTransf = "GK4";
			
			if(fromId.length() == 3 && !fromId.contains("AT")){
				fromTransf = "";
			}
			if(toId.length() == 3){
				toTransf = "";
			}
			
			//get the geometries mapped to the keys specified in the relation
			//this should be municipalities
			Geometry from = munId2Geometry.get(fromId);
			Geometry to = munId2Geometry.get(toId);
			
			//if still any geometry should be null, skip this entry
			if(from != null && to != null){
				
				//create as many persons as are specified in the commuter relation
				for(int i = 0; i < relation.getCommuters(); i++){
					
					Person person = factory.createPerson(Id.createPersonId(fromId + "_" + toId + "_" + i));
					
					int age = EgapPopulationUtils.setAge(20, 79);
					agentAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
					int sex = EgapPopulationUtils.setSex(age);
					agentAttributes.putAttribute(person.getId().toString(), Global.SEX, sex);
					
					MiDPersonGroupData data = personGroupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));

					if(data == null){
						
						i--;
						continue;
						
					}
					
					boolean hasLicense = setBooleanAttribute(person.getId().toString(), data.getpLicense(), Global.LICENSE);
					boolean carAvail = setBooleanAttribute(person.getId().toString(), data.getpCarAvail(), Global.CAR_AVAIL);
					
					agentAttributes.putAttribute(person.getId().toString(), Global.EMPLOYED, true);
					agentAttributes.putAttribute(person.getId().toString(), Global.LICENSE, hasLicense);
					agentAttributes.putAttribute(person.getId().toString(), Global.CAR_AVAIL, carAvail);
					
					if(fromId.contains("09180")){
						agentAttributes.putAttribute(person.getId().toString(), Global.INHABITANT, true);
					}
					
					if(hasLicense){
						
						agentAttributes.putAttribute(person.getId().toString(), Global.POTENTIAL_CARSHARING_USER, true);
						
						if(carAvail){
							
							agentAttributes.putAttribute(person.getId().toString(), Global.COMMUTER, true);
							
						}
						
					}
					
					List<MiDSurveyPerson> templatePersons = templates.getPersonGroups().get(EgapHashGenerator.generatePersonHash(age, sex, carAvail, hasLicense, true));
					
					if(templatePersons == null){
						
						i--;
						continue;
						
					}
					
					if(templatePersons.size() < 1){
						
						i--;
						continue;
						
					}
					
					int randomIndex = (int)(GAPMain.random.nextDouble() * templatePersons.size());
					
					MiDSurveyPerson templatePerson = templatePersons.get(randomIndex);
					
					if(templatePerson == null){
						
						i--;
						continue;
						
					}
					
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
					
					if(fromId.length() < 8 && !fromId.contains("A")){
						
						Coord c = GAPMain.UTM32NtoGK4.transform(homeCoord);
						Geometry nearestToHome = builtAreaQT.get(c.getX(), c.getY());
						homeCoord = GAPMain.gk4ToUTM32N.transform(shoot(nearestToHome));
						
					}
					
					if(toId.length() < 8 && !toId.contains("A")){
						
						Coord c = GAPMain.UTM32NtoGK4.transform(workCoord);
						Geometry nearestToWork = builtAreaQT.get(c.getX(), c.getY());
						workCoord = GAPMain.gk4ToUTM32N.transform(shoot(nearestToWork));
						if(toId.startsWith("09180")){
							workCoord = workLocations.get(workCoord.getX(), workCoord.getY()).getCoord();
						}
						
					}
					
//					DayPlanCreator.createArea(templatePerson.getPlan(), homeCoord, workCoord);
					
					Activity actHome = factory.createActivityFromCoord("home", homeCoord);
					actHome.setStartTime(0.);
					
					Leg firstLeg = (Leg) templatePerson.getPlan().getPlanElements().get(0);
					
					double timeShift = 0.;
					
					if(firstLeg.getDepartureTime() != Time.UNDEFINED_TIME){

						do{

							timeShift = createRandomEndTime();

							if(firstLeg.getDepartureTime() + timeShift > 0){
								
								actHome.setEndTime(firstLeg.getDepartureTime() + timeShift);
								
							} else{
								
								timeShift = 0;
								
							}
							
						} while(timeShift == 0);
						
					}
					
					plan.addActivity(actHome);
					
					int index = 1;
					
					for(PlanElement pe : templatePerson.getPlan().getPlanElements()){
						
						if(pe instanceof Activity){
							
							Activity act = (Activity)pe;
							
							String type = act.getType();
							double startTime = act.getStartTime();
							double endTime = act.getEndTime();
							
							Coord c = null;
							
							Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
							
							if(type.equals(Global.ActType.home.name())){
								
								c = homeCoord;
								
							} else if(type.equals(Global.ActType.other.name())){ //if the act type equals "other" or "work", shoot a random coordinate
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								double rndX = GAPMain.random.nextDouble();
								int signX = rndX >= 0.5 ? -1 : 1;
								double rndY = GAPMain.random.nextDouble();
								int signY = rndY >= 0.5 ? -1: 1;
								
								double x = signX * GAPMain.random.nextDouble() * distance;
								double y = signY * Math.sqrt(distance * distance - x * x);
								
								c = new CoordImpl(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
								c = GAPMain.UTM32NtoGK4.transform(c);
								Geometry nearest = builtAreaQT.get(c.getX(), c.getY());
								c = GAPMain.gk4ToUTM32N.transform(shoot(nearest));
									
								
							} else if(type.equals(Global.ActType.work.name())){
								
								c = workCoord;
								
							}
							else{ //for all activities apart from "other" and "home", shoot a random coordinate and get the nearest activity facility
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								ActivityFacility facility = null;
								
								do{
									
									double rndX = GAPMain.random.nextDouble();
									int signX = rndX >= 0.5 ? -1 : 1;
									double rndY = GAPMain.random.nextDouble();
									int signY = rndY >= 0.5 ? -1: 1;
									
									double x = signX * GAPMain.random.nextDouble() * distance;
									double y = signY * Math.sqrt(distance * distance - x * x);
									
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
							
							//create a new activity at the position
							Activity newAct = factory.createActivityFromCoord(type, c);
							
							if(lastAct.getEndTime() > startTime + timeShift){
								
								plan.getPlanElements().remove(plan.getPlanElements().size() - 1);
								break;
								
							}
							
							newAct.setStartTime(startTime + timeShift);
							
							//the end time has either not been read correctly or there simply was none given in the mid survey file
							if(endTime <= 0. || endTime + timeShift <= 0){
								
								endTime = startTime + timeShift + 1800;
								
							}
							
							//acts must not have zero or negative duration, a minimum duration of 0.5 hours is assumed...
							if(endTime - startTime <= 0){
								
								timeShift += 1800;
								
								if(endTime + timeShift - newAct.getStartTime() <= 0){
									
									newAct.setEndTime(24 * 3600);
									plan.addActivity(newAct);
									break;
									
								}
								
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
					person.setSelectedPlan(plan);
					
					scenario.getPopulation().addPerson(person);
					
				}
				
			} else{
				
				log.warn("Could not find geometries for:" + fromId + " (" + fromName + "), " + toId + " (" + toName + ").");
				log.warn("Continuing with next relation...");
				
			}
			
	}
	
	/**
	 * 
	 * @param mName name of the municipality
	 * @param a0 lower bound of agents' age
	 * @param aX upper bound of agents' age
	 * @param aR for computation reasons, this is needed to access the MiD survey persons, since their age categories do not fit those used in the census
	 * @param amount number of persons to be created
	 * @param scenario
	 * @param personGroupData
	 * @param templates
	 */
	private static void createPersonsFromPersonGroup(String mName, int a0, int aX, int aR, int amount, Scenario scenario, Map<String, MiDPersonGroupData> personGroupData, MiDPersonGroupTemplates templates){
		
		PopulationFactoryImpl factory = (PopulationFactoryImpl) scenario.getPopulation().getFactory();
		
		for(int i = 0; i < amount; i++){
			
			Person person = factory.createPerson(Id.createPersonId(mName + "_" + a0 + "_" + (aX) + "_" + i));
			Plan plan = factory.createPlan();
			
			int age = EgapPopulationUtils.setAge(a0, aR);
			agentAttributes.putAttribute(person.getId().toString(), Global.AGE, age);
			int sex = EgapPopulationUtils.setSex(age);
			agentAttributes.putAttribute(person.getId().toString(), Global.SEX, sex);
			
			MiDPersonGroupData data = personGroupData.get(EgapHashGenerator.generatePersonGroupHash(age, sex));
			
			if(data == null){
				i--;
				continue;
			}
			
			boolean isEmployed = false;
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
					Coord homeCoord = GAPMain.gk4ToUTM32N.transform(shoot(munId2Geometry.get(mName)));
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
							
							Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
							
							if(type.equals(Global.ActType.home.name())){
								
								c = homeCoord;
								
							} else if(type.equals(Global.ActType.other.name()) || type.equals(Global.ActType.work.name())){ //if the act type equals "other" or "work", shoot a random coordinate
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								double rndX = GAPMain.random.nextDouble();
								int signX = rndX >= 0.5 ? 1 : -1;
								double rndY = GAPMain.random.nextDouble();
								int signY = rndY >= 0.5 ? 1: -1;
								
								double x = signX * GAPMain.random.nextDouble() * distance;
								double y = signY * Math.sqrt(distance * distance - x * x);
								
								c = new CoordImpl(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
								if(type.equals(Global.ActType.work.name())){
									
									c = workLocations.get(c.getX(), c.getY()).getCoord();
									
								} else{
									
									c = GAPMain.UTM32NtoGK4.transform(c);
									Geometry nearest = builtAreaQT.get(c.getX(), c.getY());
									c = GAPMain.gk4ToUTM32N.transform(shoot(nearest));
									
								}
								
							} else{ //for all activities apart from "other" and "home", shoot a random coordinate and get the nearest activity facility
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								ActivityFacility facility = null;
								
								do{
									
									double rndX = GAPMain.random.nextDouble();
									int signX = rndX >= 0.5 ? 1 : -1;
									double rndY = GAPMain.random.nextDouble();
									int signY = rndY >= 0.5 ? 1: -1;
									
									double x = signX * GAPMain.random.nextDouble() * distance;
									double y = signY * Math.sqrt(distance * distance - x * x);
									
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
							
							//create a new activity at the position
							Activity newAct = factory.createActivityFromCoord(type, c);
							
							if(lastAct.getEndTime() > startTime + timeShift){
								
								plan.getPlanElements().remove(plan.getPlanElements().size() - 1);
								break;
								
							}
							
							newAct.setStartTime(startTime + timeShift);
							
							//the end time has either not been read correctly or there simply was none given in the mid survey file
							if(endTime <= 0. || endTime + timeShift <= 0){
								
								endTime = startTime + timeShift + 1800;
								
							}
							
							//acts must not have zero or negative duration, a minimum duration of 0.5 hours is assumed...
							if(endTime - startTime <= 0){
								
								timeShift += 1800;
								
								if(endTime + timeShift - newAct.getStartTime() <= 0){
									
									newAct.setEndTime(24 * 3600);
									plan.addActivity(newAct);
									break;
									
								}
								
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
	
	private static void createPersonsFromPersonGroup(int a0, int amount, Scenario scenario, Map<String, MiDPersonGroupData> groupData, MiDPersonGroupTemplates templates){
		
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
							
							Activity lastAct = (Activity)plan.getPlanElements().get(plan.getPlanElements().size() - 2);
							
							if(type.equals(Global.ActType.home.name())){
								
								c = homeCoord;
								
							} else if(type.equals(Global.ActType.other.name()) || type.equals(Global.ActType.work.name())){ //if the act type equals "other" or "work", shoot a random coordinate
								
								double distance = ((Leg)templatePerson.getPlan().getPlanElements().get(index - 2)).getRoute().getDistance();
								
								double x = GAPMain.random.nextDouble() * distance;
								double y = Math.sqrt(distance * distance - x * x);
								
								c = new CoordImpl(lastAct.getCoord().getX() + x, lastAct.getCoord().getY() + y);
								
							} else{ //for all activities apart from "other" and "home", shoot a random coordinate and get the nearest activity facility
								
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
							
							//create a new activity at the position
							Activity newAct = factory.createActivityFromCoord(type, c);
							
							if(lastAct.getEndTime() > startTime + timeShift){
								
								timeShift += 24 * 3600;
								
							}
							
							newAct.setStartTime(startTime + timeShift);
							
							//the end time has either not been read correctly or there simply was none given in the mid survey file
							if(endTime <= 0. || endTime + timeShift <= 0){
								
								endTime = startTime + timeShift + 1800;
								
							}
							
							//acts must not have zero or negative duration, a minimum duration of 0.5 hours is assumed...
							if(endTime - startTime <= 0){
								
								timeShift += 1800;
								
								if(endTime + timeShift - newAct.getStartTime() <= 0){
									
									newAct.setEndTime(24 * 3600);
									plan.addActivity(newAct);
									break;
									
								}
								
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
