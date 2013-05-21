package playground.dziemke.potsdam.analysis.disaggregated.revised;

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
	
	private PointFeatureFactory pointFeatureFactory;
	
	// Input file paths
	String configFileBase = "D:/Workspace/container/potsdam-pg/config/Config_run_11.xml";
	String networkFileBase = "D:/Workspace/container/potsdam-pg/data/potsdamNetwork.xml";
	String eventsFileBase = "D:/Workspace/container/potsdam-pg/output/run_11/ITERS/it.500/500.events.xml";
	String plansFileBase =  "D:/Workspace/container/potsdam-pg/output/run_11/ITERS/it.500/500.plans.xml";
	
	String configFileMeasure =  "D:/Workspace/container/potsdam-pg/config/Config_run_x11b.xml";
	String networkFileMeasure = "D:/Workspace/container/potsdam-pg/data/potsdamNetworkBridgeClosed3.xml";
	String eventsFileMeasure = "D:/Workspace/container/potsdam-pg/output/run_x11b/ITERS/it.150/150.events.xml";
	String plansFileMeasure = "D:/Workspace/container/potsdam-pg/output/run_x11b/ITERS/it.150/150.plans.xml";
	
	// Output file paths
	String mostAffectedTravelTimeDifferenceFile = "D:/Workspace/container/potsdam-pg/analysis/TravelTimeDifference10Percent.csv";
	String mostAffectedHomeCoordsFile = "D:/Workspace/container/potsdam-pg/analysis/homeCoords10Percent.shp";
	String mostAffectedWorkCoordsFile = "D:/Workspace/container/potsdam-pg/analysis/workCoords10Percent.shp";
	
	String toCarHomeCoordsFile = "D:/Workspace/container/potsdam-pg/analysis/homeCoordsCar.shp";
	String toCarWorkCoordsFile = "D:/Workspace/container/potsdam-pg/analysis/workCoordsCar.shp";
	
	String toPtHomeCoordsFile = "D:/Workspace/container/potsdam-pg/analysis/homeCoordsPt.shp";
	String toPtWorkCoordsFile = "D:/Workspace/container/potsdam-pg/analysis/workCoordsPt.shp";
	
	
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
		
		Map <Id, Double> travelTimesBaseBridgeUsers = new HashMap<Id, Double>();
		Map <Id, Double> travelTimesMeasureBridgeUsers = new HashMap<Id, Double>();
		List <DoubleId> travelTimeDifferenceBridgeUsers = new ArrayList<DoubleId>();
				
		Map <Id, Double> travelTimesBaseAll = new HashMap<Id, Double>();
		Map <Id, Double> travelTimesAnalysisAll = new HashMap<Id, Double>();
		List <DoubleId> travelTimeDifferenceAll = new ArrayList<DoubleId>();
						
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
		
		Config configBase = ConfigUtils.loadConfig(configFileBase);
		configBase.network().setInputFile(networkFileBase);
		configBase.plans().setInputFile(plansFileBase);
		Scenario scenarioBase = ScenarioUtils.loadScenario(configBase);
		
		Config configMeasure = ConfigUtils.loadConfig(configFileMeasure);
		configMeasure.network().setInputFile(networkFileMeasure);
		configMeasure.plans().setInputFile(plansFileMeasure);
		Scenario scenarioMeasure = ScenarioUtils.loadScenario(configMeasure);
		
		
		// A list containing the IDs of all agents who used the bridge in the base case
		bridgeUsers = PotsdamEventsFileReaderBridgeUsers.EventFileReader(configFileBase, eventsFileBase);
		Collections.sort(bridgeUsers);
		
		
		// Get travel times of first leg of base case and measure case for bridge users
		travelTimesBaseBridgeUsers = getTravelTime(bridgeUsers, scenarioBase);
		travelTimesMeasureBridgeUsers = getTravelTime(bridgeUsers, scenarioMeasure);
		System.out.println(travelTimesBaseBridgeUsers);
		System.out.println(travelTimesMeasureBridgeUsers);
		
		
		// Travel time differences between base and measure case for bridge users
		double sumDifference = 0.0;
		for(Id id : travelTimesBaseBridgeUsers.keySet() ){
			double currentDifference = travelTimesMeasureBridgeUsers.get(id) - travelTimesBaseBridgeUsers.get(id);
			sumDifference = sumDifference + currentDifference;
			DoubleId timeId = new DoubleId(currentDifference, id);
			// why write in list and not in map? Sorting?
			travelTimeDifferenceBridgeUsers.add(timeId);
		}
		
		double average = sumDifference/travelTimesBaseBridgeUsers.size();  
		System.out.println("################################################################################################################" + "\n"
								+ "Die durchschnittliche Reisezeitdifferenz der Agenten, die die Bruecke benutzt hat betraegt: " 
								+ average + " s." + "\n" +
								"################################################################################################################");
		

		// All agents
		for (Id id: scenarioBase.getPopulation().getPersons().keySet() ){
			allAgents.add(id);
		}
		System.out.println("Wir haben " + allAgents.size() + " Agenten.");
		
		travelTimesBaseAll = getTravelTime(allAgents, scenarioBase);
		travelTimesAnalysisAll = getTravelTime(allAgents, scenarioMeasure);

		for (Id id: travelTimesBaseAll.keySet()){
			double difference = travelTimesAnalysisAll.get(id) - travelTimesBaseAll.get(id);
			DoubleId timeId = new DoubleId(difference, id);
			// why write in list and not in map?
			travelTimeDifferenceAll.add(timeId);
		}

		
		// 10% of agents who are affected the most
		Collections.sort(travelTimeDifferenceAll, new Comparator<DoubleId>() {
			public int compare(DoubleId arg0, DoubleId arg1) {
			    return (int) (arg0.getTime() - arg1.getTime());
	        }
	    });
			
		int amount = allAgents.size();
		int tenPercent = amount/10;
		int rest = amount - tenPercent;
				
		System.out.println("10% von " + allAgents.size() + " Agenten sind " + tenPercent);
		
		for (int i=rest-1; i<allAgents.size(); i++){
			tenPercentMap.put(travelTimeDifferenceAll.get(i).id, travelTimeDifferenceAll.get(i).time);
			tenPercentList.add(allAgents.get(i));
		}
		
		TravelTimeDifferenceWriter.writeToFile(mostAffectedTravelTimeDifferenceFile, tenPercentMap);

		
		// Generate Shape Files
		homeCoords = getHomeCoordFromId(tenPercentList, scenarioBase);
		workCoords = getWorkCoordFromId(tenPercentList, scenarioBase);
	
		writeShapeFilePoints(scenarioBase, mostAffectedHomeCoordsFile, homeCoords);
		writeShapeFilePoints(scenarioBase, mostAffectedWorkCoordsFile, workCoords);

		
		// Modal Split
		legModesBase = getLegMode(allAgents, scenarioBase);
		legModesAnalysis = getLegMode(allAgents, scenarioMeasure);
		
		changedLegModeToCar = changedLegModetTo(legModesBase, legModesAnalysis, "car");
		changedLegModeToPt = changedLegModetTo(legModesBase, legModesAnalysis, "pt");
		
		homeCoordsCar = getHomeCoordFromId(changedLegModeToCar, scenarioBase);
		workCoordsCar = getWorkCoordFromId(changedLegModeToCar, scenarioBase);
		
		homeCoordsPt = getHomeCoordFromId(changedLegModeToPt, scenarioBase);
		workCoordsPt = getWorkCoordFromId(changedLegModeToPt, scenarioBase);
		
		writeShapeFilePoints(scenarioBase, toCarHomeCoordsFile, homeCoordsCar);
		writeShapeFilePoints(scenarioBase, toCarWorkCoordsFile, workCoordsCar);
		
		writeShapeFilePoints(scenarioBase, toPtHomeCoordsFile, homeCoordsPt);
		writeShapeFilePoints(scenarioBase, toPtWorkCoordsFile, workCoordsPt);
		
	}
	
	
	private void writeShapeFilePoints(Scenario scenario, String outputShapeFile, Map <Id,Coord> coords) {
		if (coords.isEmpty()==true) {
			System.out.println("Map ist leer!");
		} else {
			initFeatureType();
			Collection <SimpleFeature> features = createFeatures(scenario, coords);
		ShapeFileWriter.writeGeometries(features, outputShapeFile);
		System.out.println("ShapeFile geschrieben (Points) in "+outputShapeFile);
		}
	}

	
	private void initFeatureType() {		
		this.pointFeatureFactory = new PointFeatureFactory.Builder().
			setCrs(MGC.getCRS(TransformationFactory.WGS84_UTM35S)).
			setName("points").
			addAttribute("PersonID", String.class).
			create();
	}	
	
	
	private Collection <SimpleFeature> createFeatures(Scenario scenario, Map<Id,Coord> coords) {
		List <SimpleFeature> features = new ArrayList <SimpleFeature>();
		for (Id id : coords.keySet()){
			features.add(getFeature(coords.get(id), id));
		}
		return features;
	}
	
	

	private SimpleFeature getFeature(Coord coord, Id id) {
		Coordinate coordinates = new Coordinate(coord.getX(), coord.getY());
		Object[] attributes = new Object [] {id};
		return this.pointFeatureFactory.createPoint(coordinates, attributes, null);
	}
	
	
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
