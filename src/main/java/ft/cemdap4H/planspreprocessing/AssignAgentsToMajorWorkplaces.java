package ft.cemdap4H.planspreprocessing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.IOUtils;
import org.opengis.feature.simple.SimpleFeature;
import com.vividsolutions.jts.geom.Geometry;

public class AssignAgentsToMajorWorkplaces {
	// A map which connects a workPlaces with the number of workers, stored as
	// Integer
	static Map<String, Integer> mapWorkPlaceToWorkers = new HashMap<>();
	// 1st string = work place | 2nd string = linkId
	static Map<String, String> mapWorkPlaceToLinks = new HashMap<>();
	static Scenario scenario = null;
	static String pathInputPlanFile = null;
	static String pathNetworkFile = null;
	static String pathOutputPlanFile = null;
	static Double sigmaDistance;
	static Double parkingDistanceInMeters;
	static Set<Id<Person>> personBlackList = new HashSet<Id<Person>>();
	static String pathToAssignedWorkers = null;
	static List<String> assignedWorkersStringList = new ArrayList<String>();
	static int assigendWorkers;
	static ArrayList<Id<Person>> keys = new ArrayList<Id<Person>>();
	static Map<String, String> mapWorkPlaceToZoneID = new HashMap<>();

	static Population population = null;
	static Set<String> zones = new HashSet<>();
	static Map<String, Geometry> zoneMap = new HashMap<>();
	static String shapeFeature = null;
	static String shapeFile = null;

	static int testedAgentsSinceLastAssignmend = 0;

	private static final Logger LOG = Logger.getLogger(AssignAgentsToMajorWorkplaces.class);

	public static void main(String[] args) throws MathException {

		mapWorkPlaceToWorkers.put("vw", 13920);
		mapWorkPlaceToWorkers.put("conti", 7800);
		mapWorkPlaceToWorkers.put("klinikum", 8500);
		mapWorkPlaceToWorkers.put("mhh", 7557);
		mapWorkPlaceToWorkers.put("db", 6000);
		mapWorkPlaceToWorkers.put("wabco", 2000);

		mapWorkPlaceToLinks.put("vw", "105858");
		mapWorkPlaceToLinks.put("conti", "105986");
		mapWorkPlaceToLinks.put("klinikum", "18056");
		mapWorkPlaceToLinks.put("mhh", "143958");
		mapWorkPlaceToLinks.put("db", "324469");
		mapWorkPlaceToLinks.put("wabco", "135896");

		mapWorkPlaceToZoneID.put("vw", "350");
		mapWorkPlaceToZoneID.put("conti", "350");
		mapWorkPlaceToZoneID.put("klinikum", "35");
		mapWorkPlaceToZoneID.put("mhh", "25");
		mapWorkPlaceToZoneID.put("db", "1");
		mapWorkPlaceToZoneID.put("wabco", "34");

		pathToAssignedWorkers = "E:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\assignedWorkers.txt";

		shapeFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\add_data\\shp\\Statistische_Bezirke_Hannover_Region.shp";
		shapeFeature = "NO";

		pathInputPlanFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\mergedPlans_dur_dropped_Stud_Ren.xml.gz";
		pathOutputPlanFile = "E:\\Thiel\\Programme\\MatSim\\00_HannoverModel_1.0\\Input\\Cemdap\\cemdap_output\\Hannover_big_wchildren\\mergedPlans_dur_dropped_Work.xml.gz";
		pathNetworkFile = "E:\\Thiel\\Programme\\MatSim\\01_HannoverModel_2.0\\Network\\00_Final_Network\\network.xml.gz";

		AssignAgentsToMajorWorkplaces.run(pathToAssignedWorkers,shapeFile,shapeFeature,pathInputPlanFile,pathOutputPlanFile,pathNetworkFile);

	}

	public static void run(String pathToAssignedWorkers, String shapeFile, String shapeFeature, String pathInputPlanFile,String  pathOutputPlanFile,String pathNetworkFile  ) throws MathException {

		int buffer = 0;
		int maxAllowedBuffer = 5000;
		int deltaBuffer = 1000;

		readShape(shapeFile, shapeFeature);

		// Initialize empty scenario
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		// Fill scenario
		new MatsimNetworkReader(scenario.getNetwork()).readFile(pathNetworkFile);
		new PopulationReader(scenario).readFile(pathInputPlanFile);

		keys.addAll(scenario.getPopulation().getPersons().keySet());
		population = scenario.getPopulation();
		
		
		//Delete unselected plans
		for (Entry<Id<Person>, ? extends Person> personEntry : population.getPersons().entrySet())
		{
			Person person = personEntry.getValue();
			PersonUtils.removeUnselectedPlans(person);
		}

		int totalPopulation = population.getPersons().size();

		for (Entry<String, Integer> workPlaceEntry : mapWorkPlaceToWorkers.entrySet()) {
			assigendWorkers = 0;
			int workersToBeAssigned = workPlaceEntry.getValue();
			System.out.println("Assigning " + workPlaceEntry.getKey());

			testedAgentsSinceLastAssignmend = 0;

			while (assigendWorkers < workersToBeAssigned) {

				// Dynamically adjust buffer
				if (testedAgentsSinceLastAssignmend > totalPopulation * 0.5) {
					LOG.warn("Tested 50 % of the total population and no suitable worker has been found");

					// Increase buffer around zone geometry in order to find a
					// suitable worker

					buffer = buffer + deltaBuffer;
					for (Iterator<Entry<String, Geometry>> iterator = zoneMap.entrySet().iterator(); iterator
					        .hasNext();) {

						Entry<String, Geometry> entry = iterator.next();

						

						if (buffer > maxAllowedBuffer) {
							throw new RuntimeException(
							        "Less working agentes in simulation than required. Check supply!");

						}

						// Store old zone
						Geometry oldZone = zoneMap.get(entry.getKey());
						zoneMap.replace(entry.getKey(), oldZone.buffer(buffer));
//						zoneMap.remove(entry.getKey());

						// Add a new zone with an increased buffer
//						zoneMap.put(entry.getKey(), oldZone.buffer(buffer));

					}

					LOG.warn("Adjusted buffer arround zones to "+ buffer + " meters");
					// Reset testedAgentsSinceLastAssignmend
					testedAgentsSinceLastAssignmend = 0;

				}

				Person workerToBeAssigned = getRandomPersonNotinBlackList();
				modifyWorkLocation(workerToBeAssigned, workPlaceEntry.getKey());
				testedAgentsSinceLastAssignmend++;
			}

		}

		writeAssignedAgents(pathToAssignedWorkers, assignedWorkersStringList);
		new PopulationWriter(population).write(pathOutputPlanFile);

	}

	public static boolean planWithSingleWorkPlace(Plan plan) {
		if (countDistinctWorkLocationsPerPlan(plan) == 1) {
			return true;
		} else
			return false;
	}

	public static Coord getParkingCoordWithinDistance(Id<Link> linkId) {

		parkingDistanceInMeters = 300.0;
		Coord shiftedRandomParkingCoord = null;
		Random r = new Random();
		double spatialShiftX = r.nextGaussian() * parkingDistanceInMeters + 0;
		double spatialShiftY = r.nextGaussian() * parkingDistanceInMeters + 0;

		Network network = scenario.getNetwork();

		Link originalParkinLink = network.getLinks().get(linkId);
		Double originalParkinLinkXCoord = originalParkinLink.getCoord().getX();
		Double originalParkinLinkYCoord = originalParkinLink.getCoord().getY();

		shiftedRandomParkingCoord = new Coord(originalParkinLinkXCoord + spatialShiftX,
		        originalParkinLinkYCoord + spatialShiftY);

		// Random Parking distance within tolerance
		return shiftedRandomParkingCoord;

	}
	

	public static Link getclosestParkingLinkFromCoord(Coord parkingCoord) {
		return NetworkUtils.getNearestLink(scenario.getNetwork(), parkingCoord);
	}

	public static int countDistinctWorkLocationsPerPlan(Plan plan) {
		Set<Double> locationSet = new HashSet<Double>();

		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
		        EmptyStageActivityTypes.INSTANCE);

		for (Activity act : activities) {

			if (act.getType().contains("work")) {
				Double x = act.getCoord().getX();
				locationSet.add(x);
			}

		}

		return locationSet.size();
	}

	public static Coord getHomeCoord(Plan plan) {

		Coord homeCoord = null;

		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
		        EmptyStageActivityTypes.INSTANCE);

		for (Activity act : activities) {

			if (act.getType().contains("home")) {
				return act.getCoord();
			}
		}

		return homeCoord;

	}

	public static Coord getWorkCoord(Plan plan) {

		Coord workCoord = null;

		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
		        EmptyStageActivityTypes.INSTANCE);

		for (Activity act : activities) {

			if (act.getType().contains("work")) {
				return act.getCoord();
			}
		}

		return workCoord;

	}

	public static int getActWorkIdxInZone(Plan plan, String zoneId) {

		List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
		        EmptyStageActivityTypes.INSTANCE);

		int actIdx = 0;
		for (Activity act : activities) {

			Geometry zone = zoneMap.get(String.valueOf(zoneId));

			// Agent works in target zone
			if (act.getType().contains("work") && zone.contains(MGC.coord2Point(act.getCoord()))) {

				return actIdx;

			}
			actIdx++;
		}

		return -99;

	}

	public static double getNormalPropability(double metricalDistanceToWork) throws MathException {
		double muMeter = 0.0;
		double sigma = 35000.0;
		NormalDistribution d;
		d = new NormalDistributionImpl(muMeter, sigma);
		double prop = d.cumulativeProbability(metricalDistanceToWork);

		return prop;
	}

	public static Person getRandomPersonNotinBlackList() {

		boolean stopLoop = false;

		while (stopLoop == false) {

			Random random = new Random();

			Id<Person> randomPerson = keys.get(random.nextInt(keys.size()));

			if (!personBlackList.contains(randomPerson)) {
				return population.getPersons().get(randomPerson);
			}

		}

		return null;

	}

	public static boolean personAlreadyWorksinZone(Plan plan, String workingplace) {
		Geometry necessaryWorkZone = zoneMap.get(mapWorkPlaceToZoneID.get(workingplace));
		Coord workCoord = getWorkCoord(plan);

		return necessaryWorkZone.contains(MGC.coord2Point(workCoord));
	}

	public static void modifyWorkLocation(Person person, String workingplace) throws MathException {
		// Plan has only one working location
		Plan plan = person.getSelectedPlan();
		PersonUtils.removeUnselectedPlans(person);

		// Index of Activity, working in target zone
		String zoneId = mapWorkPlaceToZoneID.get(workingplace);

		if (getActWorkIdxInZone(plan, zoneId) != -99) {

			// if (planWithSingleWorkPlace(plan) &&
			// personAlreadyWorksinZone(plan, workingplace)) {

			// Calculate distance between new work and home location
			Coord homeCoord = getHomeCoord(plan);
			Coord newWorkCoord = scenario.getNetwork().getLinks()
			        .get(Id.createLinkId(mapWorkPlaceToLinks.get(workingplace))).getCoord();
			Double metricalDistanceHomeToWork = NetworkUtils.getEuclideanDistance(homeCoord, newWorkCoord);

			// Calculate probability to modify this person
			Double probabilityToModifyThisPerson = getNormalPropability(metricalDistanceHomeToWork);

			Random r = new Random();
			double probToBeExceeded = r.nextDouble();

			// Distance based probability
			if (probabilityToModifyThisPerson > probToBeExceeded) {

				// Create new parking coordinate
				Coord newParkingCoord = getParkingCoordWithinDistance(
				        Id.createLinkId(mapWorkPlaceToLinks.get(workingplace)));

				List<Activity> activities = TripStructureUtils.getActivities(plan.getPlanElements(),
				        EmptyStageActivityTypes.INSTANCE);

				int actIdx = getActWorkIdxInZone(plan, mapWorkPlaceToZoneID.get(workingplace));

				activities.get(actIdx).setCoord(newParkingCoord);
				// for (Activity act : activities) {
				//
				// if (act.getType().contains("work")) {
				//
				// act.setCoord(newParkingCoord);
				//
				// }
				//
				// }

				personBlackList.add(person.getId());
				assigendWorkers++;
				// Reset testedAgentsSinceLastAssignmend because we found
				// already a new worker that could be assigned
				testedAgentsSinceLastAssignmend = 0;
				//System.out.println("Assigned " + assigendWorkers);
				assignedWorkersStringList.add(person.getId().toString() + "\t" + workingplace + "\t"
				        + newParkingCoord.getX() + "\t" + newParkingCoord.getY());

			}
		}

	}

	public static void readShape(String shapeFile, String featureKeyInShapeFile) {
		Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(shapeFile);
		for (SimpleFeature feature : features) {
			String id = feature.getAttribute(featureKeyInShapeFile).toString();
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			zones.add(id);
			zoneMap.put(id, geometry);
		}
	}

	static void writeAssignedAgents(String filename, List<String> assignedWorkersStringList) {
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		String sep = "\t";
		try {
			bw.append("personId" + sep + "workplaceName" + sep + "X" + sep + "Y");
			bw.newLine();

			for (String line : assignedWorkersStringList)

			{
				bw.append(line);
				bw.newLine();
			}

			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
