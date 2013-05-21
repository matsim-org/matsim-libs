package playground.dziemke.potsdam.analysis.disaggregated.adapted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;

public class PotsdamDisaggregatedAnalysis implements Runnable {
	
	// private PolylineFeatureFactory polylineFeatureFactory;
	private PointFeatureFactory pointFeatureFactory;
	// private FeatureType featureType;

	// private GeometryFactory geometryFactory = new GeometryFactory();
	
	// String configBase = "./input/potsdam/Config_run_11.xml";
	String configBase = "D:/Workspace/container/potsdam-pg/config/Config_run_11.xml";
	// String networkBase = "./input/potsdam/potsdamNetwork.xml";
	String networkBase = "D:/Workspace/container/potsdam-pg/data/potsdamNetwork.xml";
	// String eventsBase = "./output/potsdam/run_11/ITERS/it.500/500.events.xml";
	String eventsBase = "D:/Workspace/container/potsdam-pg/output/run_11/ITERS/it.500/500.events.xml";
	// String plansBase =  "./output/potsdam/run_11/ITERS/it.500/500.plans.xml";
	String plansBase =  "D:/Workspace/container/potsdam-pg/output/run_11/ITERS/it.500/500.plans.xml";
	
	// String configAnalysis =  "./input/potsdam/Config_run_x11b.xml";
	String configAnalysis =  "D:/Workspace/container/potsdam-pg/config/Config_run_x11b.xml";
	// String networkAnalysis = "./input/potsdam/potsdamNetworkBridgeClosed3.xml";
	String networkAnalysis = "D:/Workspace/container/potsdam-pg/data/potsdamNetworkBridgeClosed3.xml";
	// String eventsAnalysis = "./output/potsdam/run_x11b/ITERS/it.150/150.events.xml";
	String eventsAnalysis = "D:/Workspace/container/potsdam-pg/output/run_x11b/ITERS/it.150/150.events.xml";
	// String plansAnalysis = "./output/potsdam/run_x11b/ITERS/it.150/150.plans.xml";
	String plansAnalysis = "D:/Workspace/container/potsdam-pg/output/run_x11b/ITERS/it.150/150.plans.xml";

	Map <Id, Integer> differenceMap = new HashMap <Id, Integer>();
	Map <Id, Integer> agentsForShape = new HashMap <Id, Integer>();
	
	
	public static void main(String[] args) {
		PotsdamDisaggregatedAnalysis potsdamAnalysis = new PotsdamDisaggregatedAnalysis();
		potsdamAnalysis.run();
	}

	
	@Override
	public void run() {
		analysis();
	}

	
	private void analysis() {
		
		List <Id> bridgeUsers = new ArrayList <Id>(); 
		List <Id> allAgents = new ArrayList <Id>(); 
		
		Map <Id, Double> tenPercentMap = new HashMap <Id, Double>();
		List <Id> tenPercentList = new ArrayList <Id>();
		
		Map <Id, Coord> homeCoords = new HashMap <Id, Coord>();
		Map <Id, Coord> workCoords = new HashMap <Id, Coord>();
		
		Map <Id, String> legModesBase = new HashMap <Id, String>();
		Map <Id, String> legModesAnalysis = new HashMap <Id, String>();
		
		List <Id> changedLegModeToCar = new ArrayList<Id>();
		List <Id> changedLegModeToPt = new ArrayList<Id>();
		
		Map <Id, Coord> homeCoordsCar = new HashMap <Id, Coord>();
		Map <Id, Coord> workCoordsCar = new HashMap <Id, Coord>();
		
		Map <Id, Coord> homeCoordsPt = new HashMap <Id, Coord>();
		Map <Id, Coord> workCoordsPt = new HashMap <Id, Coord>();
		
		Map <Id, Double> travelTimesBase = new HashMap<Id, Double>();
		Map <Id, Double> travelTimesMeasure = new HashMap<Id, Double>();
		
		List <DoubleId> travelTimeDifference = new ArrayList<DoubleId>();
		
		Map <Id, Double> travelTimesBaseAll = new HashMap<Id, Double>();
		Map <Id, Double> travelTimesAnalysisAll = new HashMap<Id, Double>();

		List <DoubleId> travelTimeDifferenceAll = new ArrayList<DoubleId>();
		
		Config config1 = ConfigUtils.loadConfig(configBase);
		config1.network().setInputFile(networkBase);
		config1.plans().setInputFile(plansBase);
		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		
		Config config2 = ConfigUtils.loadConfig(configAnalysis);
		config2.network().setInputFile(networkAnalysis);
		config2.plans().setInputFile(plansAnalysis);
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		
		// A list containing the IDs of all agents who used the bridge in the base case
		bridgeUsers = PotsdamEventsFileReaderBridgeUsers.EventFileReader(configBase, eventsBase);
		Collections.sort(bridgeUsers);
		// can later be removed, when if clause in EventHandler is used
		RemoveDuplicates.remove(bridgeUsers);
				
		// get travel times of first leg of base case and measure case for bridge users
		travelTimesBase = getTravelTime(bridgeUsers, scenario1);
		travelTimesMeasure = getTravelTime(bridgeUsers, scenario2);
		
		
		// travel time differences between base and measure case for bridge users
		double sumTravelTimesDifference1 = 0.0;
		for(Id id : travelTimesBase.keySet() ){
			double difference = travelTimesMeasure.get(id) - travelTimesBase.get(id);
			sumTravelTimesDifference1 = sumTravelTimesDifference1 + difference;
			DoubleId aa = new DoubleId(difference, id);
			// why write in list and not in map?
			travelTimeDifference.add(aa);
		}
		
		
		System.out.println(travelTimesBase);
		System.out.println(travelTimesMeasure);

		
		// Formel korrekt?
		double average = (sumTravelTimesDifference1)/(travelTimesMeasure.size()+ travelTimesBase.size());  
		System.out.println("################################################################################################################" + "\n"
								+ "Die durchschnittliche Reisezeitdifferenz f�r einen Agenten der die Br�cke benutzt hat betr�gt: " 
								+ average + " s." + "\n" +
								"################################################################################################################");
		

		// 10% of population, which is affected the most
		// schreibe diese in eine Liste
		for (Id id: scenario1.getPopulation().getPersons().keySet() ){
			allAgents.add(id);
		}
		System.out.println("Wir haben " + allAgents.size() + " Agenten.");
		
		
		// get travel times of firt leg of base case and measure case for all agents
		travelTimesBaseAll = getTravelTime(allAgents, scenario1);
		travelTimesAnalysisAll = getTravelTime(allAgents, scenario2);

		
		// travel time differences between base and measure case for all agents
		for (Id id: travelTimesBaseAll.keySet()){
			double difference = travelTimesAnalysisAll.get(id) - travelTimesBaseAll.get(id);
			DoubleId aa = new DoubleId(difference, id);
			// why write in list and not in map?
			travelTimeDifferenceAll.add(aa);
		}

		
		// ?????????????????????????
		Comparator <DoubleId> comparator = new DoubleIdComparator();
		
		Collections.sort(travelTimeDifferenceAll, comparator);
	
		int amount = allAgents.size();
		int tenPercent = amount/10;
		int rest = amount - tenPercent;
		
		
		System.out.println("10% von " + allAgents.size() + " Agenten sind " + tenPercent);
		
		for (int i=rest-1; i<allAgents.size(); i++){
			tenPercentMap.put(travelTimeDifferenceAll.get(i).id, travelTimeDifferenceAll.get(i).time);
			tenPercentList.add(allAgents.get(i));
		}
		// ??????????????????????????????????????
				
		// TravelTimeDifferenceWriter.writeToFile("./output/potsdam/diss_ana/TravelTimeDifference10Percent.csv", tenPercentMap);
		TravelTimeDifferenceWriter.writeToFile("D:/Workspace/container/potsdam-pg/analysis/TravelTimeDifference10Percent.csv", tenPercentMap);

		
		// Generate Shape Files
		homeCoords = getHomeCoordFromId(tenPercentList, scenario1);
		workCoords = getWorkCoordFromId(tenPercentList, scenario1);
		
		// writeShapeFilePoints(scenario1, "./output/potsdam/diss_ana/homeCoords10Percent.shp", homeCoords);
		writeShapeFilePoints(scenario1, "D:/Workspace/container/potsdam-pg/analysis/homeCoords10Percent.shp", homeCoords);
		// writeShapeFilePoints(scenario1, "./output/potsdam/diss_ana/workCoords10Percent.shp", workCoords);
		writeShapeFilePoints(scenario1, "D:/Workspace/container/potsdam-pg/analysis/workCoords10Percent.shp", workCoords);

		
		// Modal Split
		legModesBase = getLegMode(allAgents, scenario1);
		legModesAnalysis = getLegMode(allAgents, scenario2);
		
		changedLegModeToCar = changedLegModetTo(legModesBase, legModesAnalysis, "car");
		changedLegModeToPt = changedLegModetTo(legModesBase, legModesAnalysis, "pt");
		
		homeCoordsCar = getHomeCoordFromId(changedLegModeToCar, scenario1);
		workCoordsCar = getWorkCoordFromId(changedLegModeToCar, scenario1);
		
		homeCoordsPt = getHomeCoordFromId(changedLegModeToPt, scenario1);
		workCoordsPt = getWorkCoordFromId(changedLegModeToPt, scenario1);
		
		// writeShapeFilePoints(scenario1, "./output/potsdam/diss_ana/homeCoordsCar.shp", homeCoordsCar);
		writeShapeFilePoints(scenario1, "D:/Workspace/container/potsdam-pg/analysis/homeCoordsCar.shp", homeCoordsCar);
		// writeShapeFilePoints(scenario1, "./output/potsdam/diss_ana/workCoordsCar.shp", workCoordsCar);
		writeShapeFilePoints(scenario1, "D:/Workspace/container/potsdam-pg/analysis/workCoordsCar.shp", workCoordsCar);
		
		// writeShapeFilePoints(scenario1, "./output/potsdam/diss_ana/homeCoordsPt.shp", homeCoordsPt);
		writeShapeFilePoints(scenario1, "D:/Workspace/container/potsdam-pg/analysis/homeCoordsPt.shp", homeCoordsPt);
		// writeShapeFilePoints(scenario1, "./output/potsdam/diss_ana/workCoordsPt.shp", workCoordsPt);
		writeShapeFilePoints(scenario1, "D:/Workspace/container/potsdam-pg/analysis/workCoordsPt.shp", workCoordsPt);
		
	}
	
	
	private void writeShapeFilePoints(Scenario scenario, String outputShapeFile, Map <Id,Coord> coords) {
		if (coords.isEmpty()==true) {
			System.out.println("Map ist leer!");
		} else {
			initFeatureType();
			// Collection <Feature> features = createFeatures(scenario, coords);
			Collection <SimpleFeature> features = createFeatures(scenario, coords);
		ShapeFileWriter.writeGeometries(features, outputShapeFile);
		System.out.println("ShapeFile geschrieben (Points) in "+outputShapeFile);
		}
	}

	
	private void initFeatureType() {		
//		AttributeType [] attribs = new AttributeType[2];
//		attribs[0] = DefaultAttributeTypeFactory.newAttributeType("Point",Point.class, true, null, null, MGC.getCRS(TransformationFactory.WGS84_UTM35S));
//		attribs[1] = AttributeTypeFactory.newAttributeType("PersonID", String.class);	
		
		// this.polylineFeatureFactory = new PolylineFeatureFactory.Builder().
		this.pointFeatureFactory = new PointFeatureFactory.Builder().
				setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
				setName("points").
				addAttribute("PersonID", String.class).
				// addAttribute("Length", Double.class).
				// addAttribute("Difference", Integer.class).
				create();
//		try {
//			this.featureType = FeatureTypeBuilder.newFeatureType(attribs, "point");
//		} catch (FactoryRegistryException e) {
//			e.printStackTrace();
//		} catch (SchemaException e) {
//			e.printStackTrace();
//		}
	}	
	
	
	// private Collection<Feature> createFeatures(Scenario scenario, Map<Id,Coord> coords) {
	private Collection <SimpleFeature> createFeatures(Scenario scenario, Map<Id,Coord> coords) {
		// ArrayList <Feature> list = new ArrayList <Feature>();
		List <SimpleFeature> features = new ArrayList <SimpleFeature>();
		for (Id id : coords.keySet()){
			features.add(getFeature(coords.get(id), id));
		}
		return features;
	}
	
	
	// from aggregated analysis; for comparison
//	private List <SimpleFeature> createFeatures(Scenario scenario) {
//		List <SimpleFeature> features = new ArrayList <SimpleFeature>() ; 
//		// ArrayList<Feature> features = new ArrayList <Feature>() ; 
//		
//		for (Link link : scenario.getNetwork().getLinks().values()){
//			// features.add(getFeature1(link,differenceMap.get(link.getId())));
//			features.add(getFeature(link));
//			}
//
//		return features;
//	}

	
	

	
	
	// private Feature getFeature(Coord coord, Id id) {
	private SimpleFeature getFeature(Coord coord, Id id) {
		Coordinate coordinates = new Coordinate(coord.getX(), coord.getY());
//		Point p = this.geometryFactory.createPoint(homeCoordinate);
//		Object [] attribs = new Object[2];
//		attribs[0] = p;
//		attribs[1] = id;
		
		
		
		Object[] attributes = new Object [] {id};
		
		return this.pointFeatureFactory.createPoint(coordinates, attributes, null);
		
//		try {
//			return this.featureType.create(attribs);
//		} catch (IllegalAttributeException e) {
//			throw new RuntimeException(e);
//		}
	}
	
	
//	// from aggregated analysis; for comparison
//	private SimpleFeature getFeature(Link link) {
//		// private Feature getFeature1(Link link, Integer difference) {
////			LineString ls = this.geometryFactory.createLineString(new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()), MGC.coord2Coordinate(link.getToNode().getCoord())});
////			Object [] attribs = new Object[3];
////			attribs[0] = ls;
////			attribs[1] = link.getId().toString();
////			attribs[2] = difference;
//			Coordinate[] coordinates = new Coordinate[] {MGC.coord2Coordinate(link.getFromNode().getCoord()),
//					MGC.coord2Coordinate(link.getToNode().getCoord())};
//			Object[] attributes = new Object [] {link.getId().toString(), link.getLength(), this.differenceMap.get(link.getId())};
//			
//			return this.polylineFeatureFactory.createPolyline(coordinates, attributes, null);
//	}
//
////		try {
////			return this.featureType.create(attribs);
////		} catch (IllegalAttributeException e) {
////				throw new RuntimeException(e);
////		}
////		}

		
		
		
	
	
	
	
	private Map<Id, Coord> getHomeCoordFromId (List <Id> agents, Scenario scenario){
		Map <Id, Coord> idWithCoord = new HashMap <Id, Coord>();
		for (int i=0; i<agents.size(); i++){
			Person person = scenario.getPopulation().getPersons().get(agents.get(i));
			Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(0);
			idWithCoord.put(agents.get(i), activity.getCoord());
		}
			return idWithCoord;
	}
	
	
	private Map<Id, Coord> getWorkCoordFromId (List <Id> agents, Scenario scenario){
		Map <Id, Coord> idWithCoord = new HashMap <Id, Coord>();
		for (int i=0; i<agents.size(); i++){
			Person person = scenario.getPopulation().getPersons().get(agents.get(i));
			Activity activity = (Activity) person.getSelectedPlan().getPlanElements().get(2);
			idWithCoord.put(agents.get(i), activity.getCoord());
		}
		return idWithCoord;
	}
	
	
	private Map <Id, String> getLegMode (List <Id> agents, Scenario scenario){
		Map <Id, String> legModes = new HashMap <Id, String>();
		for(int i=0; i<agents.size(); i++){
			Person person = scenario.getPopulation().getPersons().get(agents.get(i));
			Leg leg = (Leg) person.getSelectedPlan().getPlanElements().get(1);
			legModes.put (agents.get(i), leg.getMode());
		}
		return legModes;
	}
	
	
	private List <Id> changedLegModetTo (Map <Id, String> legModesB, Map <Id, String> legModesM, String legMode){
		List <Id> changedLeg = new ArrayList <Id>();
		for(Id id: legModesB.keySet()){
			if (legModesB.get(id).equals(legMode)) {
				if (legModesB.get(id).equals(legModesM.get(id))){
				}else{
					changedLeg.add(id);
				}
			}
		}
		return changedLeg;
	}
	
	
	private Map <Id, Double> getTravelTime (List <Id> agents, Scenario scenario){
		Map <Id, Double> travelTimes = new HashMap <Id, Double>();
		for(int i=0; i<agents.size(); i++){
			Person p = scenario.getPopulation().getPersons().get(agents.get(i));
			Leg leg = (Leg) p.getSelectedPlan().getPlanElements().get(1);
			travelTimes.put(agents.get(i), leg.getTravelTime());
		}
		return travelTimes;
	}
	
}
