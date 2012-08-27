package playground.ikaddoura.parkAndRide.analyze.plans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.config.ConfigUtils;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import playground.ikaddoura.parkAndRide.pR.PRFileReader;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

public class PRPlansReader {
	
	private static final Logger log = Logger.getLogger(PRPlansReader.class);

	static String plansFile1; // initial Plans
	static String plansFile2; // output Plans
	static String netFile;
	static String prFacilitiesFile;
	static String zoneInputFile;
	static String outputPath;
	static double tolerance;
	
	private Scenario scenario1;
	private Scenario scenario2;
	private Map<Integer, Geometry> zoneNr2zoneGeometry = new HashMap<Integer, Geometry>();
	
	private List<Person> personsHomeWork = new ArrayList<Person>(); // persons who have a home and work activity
	
	private List<Person> prUsers_all = new ArrayList<Person>(); // all persons who have a park-and-ride activity in the selected plan
	private List<Person> prUsers_higherScore = new ArrayList<Person>();
	private List<Person> prUsers_lowerScore = new ArrayList<Person>();
	private List<Person> prUsers_equalScore = new ArrayList<Person>();
	
	private Map<Id, ParkAndRideFacility> id2PRFacilities;
	private List <Person> improvedPlanPersons = new ArrayList<Person>();
	private List <Person> worsePlanPersons = new ArrayList<Person>();
	private List <Person> equalPlanPersons = new ArrayList<Person>();
	
	private MyShapeFileWriter shapeFileWriter = new MyShapeFileWriter();
	private TextFileWriter writer = new TextFileWriter();
			
	public static void main(String[] args) throws IOException {
		
//		plansFile1 = "/Users/Ihab/Desktop/test/population1.xml";
//		plansFile2 = "/Users/Ihab/Desktop/test/population2.xml";
//		netFile = "/Users/Ihab/Desktop/test/network.xml";
//		prFacilitiesFile = "/Users/Ihab/Desktop/test/prFacilities.txt";
//		zoneInputFile = "/Users/Ihab/Desktop/test/rasterBB.shp";
//		outputPath = "/Users/Ihab/Desktop/analyseOutput/";
//		tolerance = 1.0;

		// ****************************
		
		plansFile1 = args[0];
		plansFile2 = args[1];
		netFile = args[2];
		prFacilitiesFile = args[3];
		zoneInputFile = args[4];
		outputPath = args[5];
		tolerance = Double.parseDouble(args[6]);
		
		PRPlansReader analysis = new PRPlansReader();
		analysis.run();
	}
	
	public void run() throws IOException {
		
		// read zone Data
		ShapeFileReader reader = new ShapeFileReader();
		Set<Feature> features;
		features = reader.readFileAndInitialize(zoneInputFile);
		for (Feature feature : features) {
			this.zoneNr2zoneGeometry.put(Integer.parseInt((String)feature.getAttribute("NR")), feature.getDefaultGeometry());
		}
		
		this.scenario1 = getScenario(netFile, plansFile1);
		this.scenario2 = getScenario(netFile, plansFile2);
		
		PRFileReader prFileReader = new PRFileReader(prFacilitiesFile);		
		this.id2PRFacilities = prFileReader.getId2prFacility();
		
		System.out.println();
		System.out.println("Starting Analyzing the plan files...");
		
		// level 1
		
		analyzeSelectedPlans_scores();
		analyzeSelectedPlans_activities();
		setImprovedPRPersons();
		setWorsePRPersons();
		setEqualPRPersons();
		
		// level 2

		File directory = new File(outputPath);
		directory.mkdirs();
		
		writer.writeFile1(this.equalPlanPersons.size(), this.improvedPlanPersons.size(), this.worsePlanPersons.size(), this.prUsers_higherScore.size(), this.prUsers_lowerScore.size(), this.prUsers_equalScore.size(), outputPath + "scoreComparison.txt");		
		writer.writeFile2(this.prUsers_all.size(), this.personsHomeWork.size(), outputPath+"parkAndRideShare.txt");
		
		shapeFileWriter.writeShapeFileLines(this.scenario2, outputPath + "network/", "network.shp");
		
		this.writeShapeFiles_homeWorkCoord(this.prUsers_all, "prUsers_all");
		this.writeShapeFiles_PRUsage(this.prUsers_all, "pRUsers_all", "prUsers_all.txt");
		
		this.writeShapeFiles_homeWorkCoord(this.prUsers_higherScore, "pRUsers_higherScore");
		this.writeShapeFiles_PRUsage(this.prUsers_higherScore, "pRUsers_higherScore", "pRUsers_higherScore.txt");

		this.writeShapeFiles_homeWorkCoord(this.prUsers_lowerScore, "prUsers_lowerScore");
		this.writeShapeFiles_PRUsage(this.prUsers_lowerScore, "prUsers_lowerScore", "pRUsers_lowerScore.txt");

		this.writeShapeFiles_homeWorkCoord(this.prUsers_equalScore, "prUsers_equalScore");
		this.writeShapeFiles_PRUsage(this.prUsers_equalScore, "prUsers_equalScore", "pRUsers_equalScore.txt");

		this.writeShapeFiles_homeWorkCoord(this.personsHomeWork, "allHomeWorkPersons");
		
		this.writeShapeFiles_zones(this.prUsers_all, this.personsHomeWork, outputPath + "prUsers_all/", "zones_prUsers_all.shp");
		this.writeShapeFiles_zones(this.prUsers_higherScore, this.personsHomeWork, outputPath + "prUsers_higherScore/", "zones_prUsers_higherScore.shp");
		this.writeShapeFiles_zones(this.prUsers_equalScore, this.personsHomeWork, outputPath + "prUsers_equalScore/", "zones_prUsers_equalScore.shp");
		this.writeShapeFiles_zones(this.prUsers_lowerScore, this.personsHomeWork, outputPath + "prUsers_lowerScore/", "zones_prUsers_lowerScore.shp");

		System.out.println();
		System.out.println("Done.");
	}

	private void writeShapeFiles_zones(List<Person> personsPR, List<Person> personsAll, String outputPath, String outputFile) throws IOException {
		
		if (personsPR.isEmpty()){
			// do nothing!
		} else {
						
			Map<Integer, Integer> zoneNr2home_all = getZoneNr2activityLocations("home", personsAll, this.zoneNr2zoneGeometry);	
			Map<Integer, Integer> zoneNr2home_prUsers = getZoneNr2activityLocations("home", personsPR, this.zoneNr2zoneGeometry);
			Map<Integer, Integer> zoneNr2work_all = getZoneNr2activityLocations("work", personsAll, this.zoneNr2zoneGeometry);	
			Map<Integer, Integer> zoneNr2work_prUsers = getZoneNr2activityLocations("work", personsPR, this.zoneNr2zoneGeometry);
								
			Map<Integer, Double> zoneNr2activityShare_home = getZoneNr2activityShare(zoneNr2home_prUsers, zoneNr2home_all);
			Map<Integer, Double> zoneNr2activityShare_work = getZoneNr2activityShare(zoneNr2work_prUsers, zoneNr2work_all);
			
			String path = outputPath + "shapeFiles/";
			File directory = new File(path);
			directory.mkdirs();
			shapeFileWriter.writeShapeFileGeometry(this.zoneNr2zoneGeometry, zoneNr2activityShare_home, zoneNr2activityShare_work, zoneNr2home_prUsers, zoneNr2work_prUsers, zoneNr2home_all, zoneNr2work_all, path + outputFile);
		}
	}
	
	private Map<Integer, Double> getZoneNr2activityShare(Map<Integer, Integer> zoneNr2activity_prUsers, Map<Integer, Integer> zoneNr2activity_all) {
		Map<Integer, Double> zoneNr2activityShare = new HashMap<Integer, Double>();
		
		for (Integer nr : zoneNr2activity_all.keySet()){
		
			double prUsers = 0.;
			double allUsers = 0.;
			double share = 0.;

			if (zoneNr2activity_prUsers.get(nr) == null) {
				// 0 PRusers in this zone
			} else {
				prUsers = zoneNr2activity_prUsers.get(nr);
			}
			
			if (zoneNr2activity_all.get(nr) == null) {
				// 0 users in this zone
			} else {
				allUsers = zoneNr2activity_all.get(nr);
			}
			
			if (allUsers == 0.){
				System.out.println("No coordinates in this zone.");
			} else {
				share = prUsers / allUsers;
			}
			zoneNr2activityShare.put(nr, share);
		}
		
		return zoneNr2activityShare;
	}

	private Map<Integer, Integer> getZoneNr2activityLocations(String activity, List<Person> persons, Map<Integer, Geometry> zoneNr2zoneGeometry) {
		Map<Integer, Integer> zoneNr2activity = new HashMap<Integer, Integer>();	

		SortedMap<Id,Coord> id2activityCoord = getCoordinates(persons, activity);
		
		for (Coord coord : id2activityCoord.values()) {
			for (Integer nr : zoneNr2zoneGeometry.keySet()) {
				Geometry geometry = zoneNr2zoneGeometry.get(nr);
				Point p = MGC.coord2Point(coord); 
				
				if (p.within(geometry)){
					if (zoneNr2activity.get(nr) == null){
						zoneNr2activity.put(nr, 1);
					} else {
						int activityCounter = zoneNr2activity.get(nr);
						zoneNr2activity.put(nr, activityCounter + 1);
					}
				}
			}
		}
		return zoneNr2activity;
	}

	private void setImprovedPRPersons() {
		
		for (Person person : this.prUsers_all){
			if (this.improvedPlanPersons.contains(person)){
				this.prUsers_higherScore.add(person);
			}
		}
	}
	
	private void setEqualPRPersons() {
		
		for (Person person : this.prUsers_all){
			if (this.equalPlanPersons.contains(person)){
				this.prUsers_equalScore.add(person);
			}
		}
	}
	
	private void setWorsePRPersons() {
		
		for (Person person : this.prUsers_all){
			if (this.worsePlanPersons.contains(person)){
				this.prUsers_lowerScore.add(person);
			}
		}
	}

	private void analyzeSelectedPlans_scores() {
				
		for (Person person1 : this.scenario1.getPopulation().getPersons().values()){
			Plan plan1 = person1.getSelectedPlan();
			if (plan1.getScore() == null){
				log.info("Plan1 has no Score. Skipping this plan...");
			} else {
				double score1 = plan1.getScore();
				Id personId1 = person1.getId();
				if (this.scenario2.getPopulation().getPersons().get(personId1) == null){
				} else {
					Person person2 = this.scenario2.getPopulation().getPersons().get(personId1);
					Plan plan2 = person2.getSelectedPlan();
					if (plan2.getScore() == null){
						log.info("Plan2 has no Score. Skipping this plan...");
					} else {
						double score2 = plan2.getScore();
						if (score2 > score1 + tolerance) {
							improvedPlanPersons.add(person2);
						} else if ( score1 > score2 + tolerance){
							worsePlanPersons.add(person2);
						} else {
							// approx. the same score (within tolerance)
							equalPlanPersons.add(person2);
						}

					}
				}
			}
		}		
	}

	private void writeShapeFiles_homeWorkCoord(List<Person> persons, String outputDir) {

		SortedMap<Id,Coord> homeCoordinates = new TreeMap<Id,Coord>();
		SortedMap<Id,Coord> workCoordinates = new TreeMap<Id,Coord>();
		
		homeCoordinates = getCoordinates(persons, "home");
		workCoordinates = getCoordinates(persons, "work");
		
		String path = outputPath + outputDir + "/shapeFiles";
		File directory = new File(path);
		directory.mkdirs();
		
		if (persons.isEmpty()){
			// do nothing
		} else {
			shapeFileWriter.writeShapeFilePoints(scenario2, homeCoordinates, path + "/homeCoordinates.shp");
			shapeFileWriter.writeShapeFilePoints(scenario2, workCoordinates, path + "/workCoordinates.shp");	
		}
	}
	
	private void writeShapeFiles_PRUsage(List<Person> persons, String outputDir, String outputFile) {
		Map<Id, Integer> prLinkId2prActs = new HashMap<Id, Integer>();
		SortedMap<Id,Coord> prCoordinates = new TreeMap<Id,Coord>();
		prCoordinates = getCoordinates(persons, ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE);
		
		String path = outputPath + outputDir + "/shapeFiles";
		File directory = new File(path);
		directory.mkdirs();
		
		if (persons.isEmpty()){
			// do nothing
		} else {
			for (Person person : persons){
				for (PlanElement pe: person.getSelectedPlan().getPlanElements()) {
					if (pe instanceof ActivityImpl) {
						ActivityImpl act = (ActivityImpl)pe;
						if (act.getType().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
							Id linkId = act.getLinkId();
							if (prLinkId2prActs.get(linkId) == null){
								prLinkId2prActs.put(linkId, 1);
							} else {
								int increasedPrActs = prLinkId2prActs.get(linkId) + 1;
								prLinkId2prActs.put(linkId, increasedPrActs);
							}
						}
					}
				}
			}
		
			writer.writeFile3(prLinkId2prActs, this.id2PRFacilities, outputPath + outputDir + "/prUsage_" + outputFile);
			shapeFileWriter.writeShapeFilePoints(scenario2, prCoordinates, path + "/prCoordinates.shp");
			shapeFileWriter.writeShapeFilePRUsage(scenario2, this.id2PRFacilities, prLinkId2prActs, path + "/prUsage.shp");
	
		}
	}

	private void analyzeSelectedPlans_activities() {

		for (Person person : this.scenario2.getPopulation().getPersons().values()){
			Plan selectedPlan = person.getSelectedPlan();
			
			boolean hasPR = false;
			boolean hasHome = false;
			boolean hasWork = false;

			for (PlanElement pe: selectedPlan.getPlanElements()) {
				if (pe instanceof ActivityImpl) {
					ActivityImpl act = (ActivityImpl)pe;
					if (act.getType().equals(ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE)){
						hasPR = true;
					}
					if (act.getType().equals("home")){
						hasHome = true;
					}
					if (act.getType().equals("work")){
						hasWork = true;
					}
				}
			}
			
			if (hasPR) {
				prUsers_all.add(person);
			}
			
			if (hasHome && hasWork){
				personsHomeWork.add(person);
			}
		}		
	}

	private SortedMap<Id, Coord> getCoordinates(List<Person> persons, String activity) {
		SortedMap<Id,Coord> id2koordinaten = new TreeMap<Id,Coord>();
		for(Person person : persons){
			for (PlanElement pE : person.getSelectedPlan().getPlanElements()){
				if (pE instanceof Activity){
					Activity act = (Activity) pE;
					if (act.getType().equals(activity)){
						Coord coord = act.getCoord();
						id2koordinaten.put(person.getId(), coord);
					}
					else {}
				}
			}
		}
		return id2koordinaten;
	}
		
	private Scenario getScenario(String netFile, String plansFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

}
