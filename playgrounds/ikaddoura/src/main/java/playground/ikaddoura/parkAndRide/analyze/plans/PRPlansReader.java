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
	static String zoneFile;
	static String outputPath;
	static double tolerance;
	
	private Scenario scenario1;
	private Scenario scenario2;
	
	private List<Person> personsHomeWork = new ArrayList<Person>(); // persons who have a home and work activity
	
	private List<Person> allPersonsPR = new ArrayList<Person>(); // all persons who have a park-and-ride activity in the selected plan
	private List<Person> improvedPersonsPR = new ArrayList<Person>();
	private List<Person> worsePersonsPR = new ArrayList<Person>();
	private List<Person> equalPersonsPR = new ArrayList<Person>();
	
	private Map<Id, ParkAndRideFacility> id2PRFacilities;
	private List <Person> improvedPlanPersons = new ArrayList<Person>();
	private List <Person> worsePlanPersons = new ArrayList<Person>();
	private List <Person> equalPlanPersons = new ArrayList<Person>();
	
	private MyShapeFileWriter shapeFileWriter = new MyShapeFileWriter();
	private TextFileWriter writer = new TextFileWriter();
			
	public static void main(String[] args) throws IOException {
		
		plansFile1 = "/Users/Ihab/Desktop/test/population1.xml";
		plansFile2 = "/Users/Ihab/Desktop/test/population2.xml";
		netFile = "/Users/Ihab/Desktop/test/network.xml";
		prFacilitiesFile = "/Users/Ihab/Desktop/test/prFacilities.txt";
		zoneFile = "/Users/Ihab/Desktop/test/dlm_gemeinden.shp";
		outputPath = "/Users/Ihab/Desktop/analyseOutput/";
		tolerance = 1.0;

		// ****************************
		
//		plansFile1 = args[0];
//		plansFile2 = args[1];
//		netFile = args[2];
//		prFacilitiesFile = args[3];
//		outputPath = args[4];
//		tolerance = Double.parseDouble(args[5]);
//		
		PRPlansReader analysis = new PRPlansReader();
		analysis.run();
	}
	
	public void run() {
		
		this.scenario1 = getScenario(netFile, plansFile1);
		this.scenario2 = getScenario(netFile, plansFile2);
		
		PRFileReader prFileReader = new PRFileReader(prFacilitiesFile);		
		this.id2PRFacilities = prFileReader.getId2prFacility();
		
		System.out.println();
		System.out.println("Starting Analyzing the plan files...");
		
		analyzeSelectedPlans_scores();
		analyzeSelectedPlans_activities();
		setImprovedPRPersons();
		setWorsePRPersons();
		setEqualPRPersons();
		
		try {
			analyzeZones(zoneFile);
		} catch (IOException e) {
			e.printStackTrace();
		}

		File directory = new File(outputPath);
		directory.mkdirs();
		
		writer.writeFile1(this.equalPlanPersons.size(), this.improvedPlanPersons.size(), this.worsePlanPersons.size(), this.improvedPersonsPR.size(), this.worsePersonsPR.size(), this.equalPersonsPR.size(), outputPath + "scoreComparison.txt");		
		writer.writeFile2(this.allPersonsPR.size(), this.personsHomeWork.size(), outputPath+"parkAndRideShare.txt");
		
		shapeFileWriter.writeShapeFileLines(this.scenario2, outputPath + "network.shp");
		
		this.writeShapeFiles_homeWorkCoord(this.allPersonsPR, "allPRUsers");
		this.writeShapeFiles_PRUsage(this.allPersonsPR, "allPRUsers", "allPRUsers.txt");
		
		this.writeShapeFiles_homeWorkCoord(this.improvedPersonsPR, "pRUsers_higherScore");
		this.writeShapeFiles_PRUsage(this.improvedPersonsPR, "pRUsers_higherScore", "pRUsers_higherScore.txt");

		this.writeShapeFiles_homeWorkCoord(this.worsePersonsPR, "prUsers_lowerScore");
		this.writeShapeFiles_PRUsage(this.worsePersonsPR, "prUsers_lowerScore", "pRUsers_lowerScore.txt");

		this.writeShapeFiles_homeWorkCoord(this.equalPersonsPR, "prUsers_equalScore");
		this.writeShapeFiles_PRUsage(this.equalPersonsPR, "prUsers_equalScore", "pRUsers_equalScore.txt");

		writeShapeFiles_homeWorkCoord(this.personsHomeWork, "allHomeWorkPersons");

		System.out.println("Done.");
	}

	private void analyzeZones(String zoneFile) throws IOException {
		
		Map<Integer, Geometry> nr2zoneGeometry = new HashMap<Integer, Geometry>();
		Map<Integer, Integer> nr2homeAll = new HashMap<Integer, Integer>();	
		Map<Integer, Integer> nr2homePR = new HashMap<Integer, Integer>();
		Map<Integer, Double> nr2PRUsersHomeShare = new HashMap<Integer, Double>();	

		ShapeFileReader reader = new ShapeFileReader();
		Set<Feature> features;
		features = reader.readFileAndInitialize(zoneFile);
		for (Feature feature : features) {
			nr2zoneGeometry.put(Integer.parseInt((String)feature.getAttribute("NR")), feature.getDefaultGeometry());
		}
				
		SortedMap<Id,Coord> id2homeCoordAll = getCoordinates(this.personsHomeWork, "home");
		
		for (Coord coord : id2homeCoordAll.values()) {
			for (Integer nr : nr2zoneGeometry.keySet()) {
				Geometry geometry = nr2zoneGeometry.get(nr);
				Point p = MGC.coord2Point(coord); 
				
				if (p.within(geometry)){
					if (nr2homeAll.get(nr) == null){
						nr2homeAll.put(nr, 1);
					} else {
						int homes = nr2homeAll.get(nr);
						nr2homeAll.put(nr, homes + 1);
					}
				}
			}
		}
		
		SortedMap<Id,Coord> id2homeCoordPRUsers = getCoordinates(this.allPersonsPR, "home");
		
		for (Coord coord : id2homeCoordPRUsers.values()) {
			for (Integer nr : nr2zoneGeometry.keySet()) {
				Geometry geometry = nr2zoneGeometry.get(nr);
				if (geometry.getEnvelopeInternal().contains(MGC.coord2Coordinate(coord))){
					if (nr2homePR.get(nr) == null){
						nr2homePR.put(nr, 1);
					} else {
						int homes = nr2homePR.get(nr);
						nr2homePR.put(nr, homes + 1);
					}
				}
			}
		}
		
		for (Integer nr : nr2homeAll.keySet()){
			double share = (double) nr2homePR.get(nr) / (double) nr2homeAll.get(nr);
			nr2PRUsersHomeShare.put(nr, share);
			System.out.println("Zonen-Nr: " + nr + " PR home share: " + share);
		}
		
		shapeFileWriter.writeShapeFileGeometry(nr2zoneGeometry, nr2PRUsersHomeShare, outputPath + "/prUsersHomeShare.shp");
		
	}

	private void setImprovedPRPersons() {
		
		for (Person person : this.allPersonsPR){
			if (this.improvedPlanPersons.contains(person)){
				this.improvedPersonsPR.add(person);
			}
		}
	}
	
	private void setEqualPRPersons() {
		
		for (Person person : this.allPersonsPR){
			if (this.equalPlanPersons.contains(person)){
				this.equalPersonsPR.add(person);
			}
		}
	}
	
	private void setWorsePRPersons() {
		
		for (Person person : this.allPersonsPR){
			if (this.worsePlanPersons.contains(person)){
				this.worsePersonsPR.add(person);
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
		
		File directory = new File(outputPath + outputDir);
		directory.mkdirs();
		
		if (persons.isEmpty()){
			// do nothing
		} else {
			shapeFileWriter.writeShapeFilePoints(scenario2, homeCoordinates, outputPath + outputDir + "/homeCoordinates.shp");
			shapeFileWriter.writeShapeFilePoints(scenario2, workCoordinates, outputPath + outputDir + "/workCoordinates.shp");	
		}
	}
	
	private void writeShapeFiles_PRUsage(List<Person> persons, String outputDir, String outputFile) {
		Map<Id, Integer> prLinkId2prActs = new HashMap<Id, Integer>();
		SortedMap<Id,Coord> prCoordinates = new TreeMap<Id,Coord>();
		prCoordinates = getCoordinates(persons, ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE);
		
		File directory = new File(outputPath + outputDir);
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
		
			writer.writeFile3(prLinkId2prActs, this.id2PRFacilities, outputPath + "prUsage_" + outputFile);
			shapeFileWriter.writeShapeFilePoints(scenario2, prCoordinates, outputPath + outputDir + "/prCoordinates.shp");
			shapeFileWriter.writeShapeFilePRUsage(scenario2, this.id2PRFacilities, prLinkId2prActs, outputPath + outputDir + "/prUsage.shp");
	
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
				allPersonsPR.add(person);
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
