package playground.ikaddoura.parkAndRide.analyze.plans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
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
import org.matsim.core.config.ConfigUtils;

import playground.ikaddoura.parkAndRide.pR.PRFileReader;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideConstants;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

public class PRPlansReader {
	
	private static final Logger log = Logger.getLogger(PRPlansReader.class);

	static String plansFile1; // initial Plans
	static String plansFile2; // output Plans
	static String netFile;
	static String prFacilitiesFile;
	static String outputPath;
	static double tolerance;
	
	private Scenario scenario1;
	private Scenario scenario2;
	
	private List<Person> personsHomeWork = new ArrayList<Person>();
	private List<Person> allPersonsPR = new ArrayList<Person>();
	private List<Person> improvedPersonsPR = new ArrayList<Person>();
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
//		outputPath = "/Users/Ihab/Desktop/analyseOutput/";
//		tolerance = 1.0;

		// ****************************
		
		plansFile1 = args[0];
		plansFile2 = args[1];
		netFile = args[2];
		prFacilitiesFile = args[3];
		outputPath = args[4];
		tolerance = Double.parseDouble(args[5]);
		
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
		
		File directory = new File(outputPath);
		directory.mkdirs();
		
		writer.writeFile1(this.equalPlanPersons.size(), this.improvedPlanPersons.size(), this.worsePlanPersons.size(), this.improvedPersonsPR.size(), outputPath + "scoreComparison.txt");		
		writer.writeFile2(this.allPersonsPR.size(), this.personsHomeWork.size(), outputPath+"parkAndRideShare.txt");
		
		shapeFileWriter.writeShapeFileLines(scenario2, outputPath + "network.shp");			
		writeShapeFiles(allPersonsPR, "prUsage_allPersons", "prUsage_all.txt");
		writeShapeFiles(improvedPersonsPR, "prUsage_improvedPersons", "prUsage_improvedPersons.txt");
		
		System.out.println("Done.");
	}

	private void setImprovedPRPersons() {
		
		for (Person person : this.allPersonsPR){
			if (this.improvedPlanPersons.contains(person)){
				this.improvedPersonsPR.add(person);
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
							equalPlanPersons.add(person2);
						}

					}
				}
			}
		}		
	}

	private void writeShapeFiles(List<Person> persons, String outputDir, String outputFile) {

		Map<Id, Integer> prLinkId2prActs = new HashMap<Id, Integer>();
		SortedMap<Id,Coord> homeCoordinates = new TreeMap<Id,Coord>();
		SortedMap<Id,Coord> workCoordinates = new TreeMap<Id,Coord>();
		SortedMap<Id,Coord> prCoordinates = new TreeMap<Id,Coord>();
		
		homeCoordinates = getCoordinates(persons, "home");
		workCoordinates = getCoordinates(persons, "work");
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
		
			writer.writeFile3(prLinkId2prActs, this.id2PRFacilities, outputPath + outputFile);

			shapeFileWriter.writeShapeFilePoints(scenario2, homeCoordinates, outputPath + outputDir + "/homeCoordinates.shp");
			shapeFileWriter.writeShapeFilePoints(scenario2, workCoordinates, outputPath + outputDir + "/workCoordinates.shp");
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
