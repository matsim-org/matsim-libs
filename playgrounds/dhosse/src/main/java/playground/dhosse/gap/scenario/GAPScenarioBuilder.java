package playground.dhosse.gap.scenario;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

import playground.dhosse.gap.Global;
import playground.dhosse.gap.analysis.SpatialAnalysis;
import playground.dhosse.gap.scenario.config.ConfigCreator;
import playground.dhosse.gap.scenario.network.NetworkCreator;
import playground.dhosse.gap.scenario.population.Municipalities;

/**
 * 
 * This class creates a scenario for
 * Garmisch-Partenkirchen, Bavaria, Germany.
 * 
 * @author dhosse
 *
 */

public class GAPScenarioBuilder {
	
	private static final Logger log = Logger.getLogger(GAPScenarioBuilder.class);
	
	private static Map<String, Geometry> munId2Geometry = new HashMap<>();
	
	private static QuadTree<Geometry> builtAreaQT;
	
	private static QuadTree<ActivityFacility> workLocations;
	private static QuadTree<ActivityFacility> educationQT;
	private static QuadTree<ActivityFacility> shopQT;
	private static QuadTree<ActivityFacility> leisureQT;
	
	//this attributes object stores subpopulation attributes
	private static ObjectAttributes subpopulationAttributes = new ObjectAttributes();
	//this attributes object stores data about age, sex, car availability etc.
//	private static ObjectAttributes demographicAttributes = new ObjectAttributes();
	
	public static void main(String args[]){
		
		//initialize everything
		MatsimRandom.reset(4711);
		
		log.info("Creating scenario for Garmisch-Partenkirchen...");
		
		Config config = ConfigUtils.createConfig();
		ConfigCreator.configureConfig(config);
		
		log.info("Config created and modified...");
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		//create network from osm data
		NetworkCreator.createAndAddNetwork(scenario, Global.networkDataDir + "survey-network.osm");
		new NetworkWriter(scenario.getNetwork()).write(Global.matsimInputDir + "Netzwerk/merged-network.xml.gz");
		SpatialAnalysis.writeNetworkToShape(Global.matsimInputDir + "Netzwerk/merged-network.xml.gz", "/home/danielhosse/Dokumente/net.shp");
		
//		//create public transport
//		TransitCreator.createTransit(scenario);
//		
//		//create counting stations
//		Counts counts = CountsCreator.createCountingStations(scenario.getNetwork());
//		new CountsWriter(counts).write(Global.matsimInputDir + "Counts/counts.xml.gz");
//		
//		//init administrative boundaries
//		initMunicipalities(scenario);
//		
//		initQuadTrees(scenario);
//		
//		//createFacilities
//		FacilitiesCreator.initAmenities(scenario);
//		FacilitiesCreator.readWorkplaces(scenario, Global.dataDir + "20150112_Unternehmen_Adressen_geokoordiniert.csv");
//		new FacilitiesWriter(scenario.getActivityFacilities()).write(Global.matsimInputDir + "facilities/facilities.xml.gz");
//		new ObjectAttributesXmlWriter(scenario.getActivityFacilities().getFacilityAttributes()).writeFile(Global.matsimInputDir + "facilities/facilityAttributes.xml.gz");
//		
//		//create plans
//		PlansCreator.createPlans(scenario, Global.matsimInputDir + "Argentur_für_Arbeit/Garmisch_Einpendler.csv", Global.matsimInputDir + "Argentur_für_Arbeit/Garmisch_Auspendler.csv");
//		Global.setN(PlansCreator.getInhabitantsCounter());
//		
//		//create activity parameters for all types of activities
//		AddingActivitiesInPlans aaip = new AddingActivitiesInPlans(scenario);
//		aaip.run();
//		
//		SortedMap<String, Tuple<Double, Double>> acts = aaip.getActivityType2TypicalAndMinimalDuration();
//		
//		for(String act : acts.keySet()){
//			
//			ActivityParams params = new ActivityParams();
//			params.setActivityType(act);
//			params.setTypicalDuration(acts.get(act).getFirst());
//			params.setMinimalDuration(acts.get(act).getSecond());
//			params.setClosingTime(Time.UNDEFINED_TIME);
//			params.setEarliestEndTime(Time.UNDEFINED_TIME);
//			params.setLatestStartTime(Time.UNDEFINED_TIME);
//			params.setOpeningTime(Time.UNDEFINED_TIME);
//			config.planCalcScore().addActivityParams(params);
//			
//		}
//		
//		ConfigCreator.configureQSimAndCountsConfigGroups(config);
//		
//		new PopulationWriter(aaip.getOutPop()).write(Global.matsimInputDir + "Pläne/plans_mid.xml.gz");
//		
//		new ConfigWriter(config).write(Global.matsimInputDir + "config.xml");
		
	}

	private static void initQuadTrees(Scenario scenario) {
		
		//create quad trees for all activity types apart from home and other which are shot randomly
		log.info("Building amenity quad trees for...");
		
		double[] bbox = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
		
		double[] boundary = NetworkUtils.getBoundingBox(scenario.getNetwork().getNodes().values());
		setWorkLocations(new QuadTree<ActivityFacility>(boundary[0], boundary[1], boundary[2], boundary[3]));
		
		log.info("...education");
		setEducationQT(new QuadTree<ActivityFacility>(bbox[0], bbox[1], bbox[2], bbox[3]));
		for(ActivityFacility af : scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.education.name()).values()){
			getEducationQT().put(af.getCoord().getX(), af.getCoord().getY(), af);
		}
		log.info("...shop");
		setShopQT(new QuadTree<ActivityFacility>(bbox[0], bbox[1], bbox[2], bbox[3]));
		for(ActivityFacility af : scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.shop.name()).values()){
			getShopQT().put(af.getCoord().getX(), af.getCoord().getY(), af);
		}
		log.info("...leisure");
		setLeisureQT(new QuadTree<ActivityFacility>(bbox[0], bbox[1], bbox[2], bbox[3]));
		for(ActivityFacility af : scenario.getActivityFacilities().getFacilitiesForActivityType(Global.ActType.leisure.name()).values()){
			getLeisureQT().put(af.getCoord().getX(), af.getCoord().getY(), af);
		}
		
		log.info("...Done.");
		
	}
	
	/**
	 * 
	 * Parses shape files in order to extract their geometries.
	 * This is needed to shoot activity coordinates in municipalities or counties.
	 * 
	 */
	private static void initMunicipalities(Scenario scenario){
		
		double fractionOfStudents = 0.614213;
		Municipalities.addEntry("09180112", (int) (446*fractionOfStudents), 1559, 526);						//Bad Kohlgrub
		Municipalities.addEntry("09180113", (int) (169*fractionOfStudents), 772, 223);						//Bad Bayersoien
		Municipalities.addEntry("09180114", (int) (269*fractionOfStudents), 908, 368);						//Eschenlohe
		Municipalities.addEntry("09180115", (int) (103*fractionOfStudents), 534, 368);						//Ettal
		Municipalities.addEntry("09180116", (int) (636*fractionOfStudents), 2288, 759);						//Farchant
		Municipalities.addEntry("09180117", (int) (3744*fractionOfStudents), 15016, 7308);					//Garmisch-Partenkirchen
		Municipalities.addEntry("09180118", (int) (501*fractionOfStudents), 2193, 841);						//Grainau
		Municipalities.addEntry("09180119", (int) (281*fractionOfStudents), 895, 280);						//Großweil
		Municipalities.addEntry("09180122", (int) (283*fractionOfStudents), 1193, 446);						//Krün
		Municipalities.addEntry("09180123", (int) (978*fractionOfStudents), 4622, 1834);					//Mittenwald
		Municipalities.addEntry("09180124", (int) (2003*fractionOfStudents), 7508, 2751);					//Murnau a. Staffelsee
		Municipalities.addEntry("09180125", (int) (890*fractionOfStudents), 3085, 1253);					//Oberammergau
		Municipalities.addEntry("09180126", (int) (528*fractionOfStudents), 1787, 691);						//Oberau
		Municipalities.addEntry("09180127", (int) (596*fractionOfStudents), 1982, 632);						//Ohlstadt
		Municipalities.addEntry("09180128", (int) (206*fractionOfStudents), 683, 266);						//Riegsee
		Municipalities.addEntry("09180129", (int) (313*fractionOfStudents), 1034, 301);						//Saulgrub
		Municipalities.addEntry("09180131", (int) (111*fractionOfStudents), 335, 155);						//Schwaigen
		Municipalities.addEntry("09180132", (int) (486*fractionOfStudents), 1415, 600);						//Seehausen a. Staffelsee
		Municipalities.addEntry("09180133", (int) (151*fractionOfStudents), 491, 123);						//Spatzenhausen
		Municipalities.addEntry("09180134", (int) (582*fractionOfStudents), 1813, 583);						//Uffing a. Staffelsee
		Municipalities.addEntry("09180135", (int) (294*fractionOfStudents), 857, 315);						//Unterammergau
		Municipalities.addEntry("09180136", (int) (265*fractionOfStudents), 823, 306);						//Wallgau
		
		setBuiltAreaQT(new QuadTree<Geometry>(4070000, 5190000, 4730000, 6106925));
		
		Collection<SimpleFeature> builtAreas = new ShapeFileReader().readFileAndInitialize(Global.adminBordersDir + "Gebietsstand_2007/gemeinden_2007_bebaut.shp");
		
		log.info("Processing built areas...");
		
		for(SimpleFeature f : builtAreas){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Long identifier = (Long) f.getAttribute("GEM_KENNZ");
			
			String id = "0" + Long.toString(identifier);
			
			getMunId2Geometry().put(id, geometry);
			
			Coord c = MGC.point2Coord(geometry.getCentroid());
			getBuiltAreaQT().put(c.getX(), c.getY(), geometry);
			
		}
		
		log.info("Processing administrative boundaries...");
		
		Collection<SimpleFeature> counties = new ShapeFileReader().readFileAndInitialize(Global.adminBordersDir + "Gebietsstand_2007/kreise_2007_12.shp");
		
		for(SimpleFeature f : counties){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			String identifier = (String) f.getAttribute("KENNZAHL");

			getMunId2Geometry().put(identifier, geometry);
			
		}
		
		Collection<SimpleFeature> regBez = new ShapeFileReader().readFileAndInitialize("/home/dhosse/Downloads/germany/DEU_adm/DEU_adm2.shp");
		
		for(SimpleFeature f : regBez){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Long identifier = (Long) f.getAttribute("ID_2");
			
			getMunId2Geometry().put("0" + Long.toString(identifier), geometry);
			
		}
		
		Collection<SimpleFeature> c = new ShapeFileReader().readFileAndInitialize(Global.adminBordersDir + "bundeslaender.shp");
		
		for(SimpleFeature f : c){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			Long identifier = (Long) f.getAttribute("LAND");
			
			getMunId2Geometry().put("0" + Long.toString(identifier), geometry);
			
		}
		
		Collection<SimpleFeature> countries = new ShapeFileReader().readFileAndInitialize(Global.adminBordersDir + "europa_staaten.shp");
		
		for(SimpleFeature f : countries){
			
			Geometry geometry = (Geometry) f.getDefaultGeometry();
			String identifier = (String) f.getAttribute("NUTS0");
			
			getMunId2Geometry().put("0" + identifier, geometry);
			
		}
		
	}

	public static Map<String, Geometry> getMunId2Geometry() {
		return munId2Geometry;
	}

	public static void setMunId2Geometry(Map<String, Geometry> munId2Geometry) {
		GAPScenarioBuilder.munId2Geometry = munId2Geometry;
	}

	public static ObjectAttributes getSubpopulationAttributes() {
		return subpopulationAttributes;
	}

	public static void setSubpopulationAttributes(ObjectAttributes subpopulationAttributes) {
		GAPScenarioBuilder.subpopulationAttributes = subpopulationAttributes;
	}

	public static QuadTree<Geometry> getBuiltAreaQT() {
		return builtAreaQT;
	}

	public static void setBuiltAreaQT(QuadTree<Geometry> builtAreaQT) {
		GAPScenarioBuilder.builtAreaQT = builtAreaQT;
	}

	public static QuadTree<ActivityFacility> getWorkLocations() {
		return workLocations;
	}

	public static void setWorkLocations(QuadTree<ActivityFacility> workLocations) {
		GAPScenarioBuilder.workLocations = workLocations;
	}

	public static QuadTree<ActivityFacility> getEducationQT() {
		return educationQT;
	}

	public static void setEducationQT(QuadTree<ActivityFacility> educationQT) {
		GAPScenarioBuilder.educationQT = educationQT;
	}

	public static QuadTree<ActivityFacility> getShopQT() {
		return shopQT;
	}

	public static void setShopQT(QuadTree<ActivityFacility> shopQT) {
		GAPScenarioBuilder.shopQT = shopQT;
	}

	public static QuadTree<ActivityFacility> getLeisureQT() {
		return leisureQT;
	}

	public static void setLeisureQT(QuadTree<ActivityFacility> leisureQT) {
		GAPScenarioBuilder.leisureQT = leisureQT;
	}
	
}
