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
import org.matsim.api.core.v01.population.Population;
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
	
	TextFileWriter writer = new TextFileWriter();
			
	public static void main(String[] args) throws IOException {
		
//		plansFile1 = "/Users/Ihab/Desktop/test/population1.xml";
//		plansFile2 = "/Users/Ihab/Desktop/test/population2.xml";
//		netFile = "/Users/Ihab/Desktop/test/network.xml";
//		prFacilitiesFile = "/Users/Ihab/Desktop/test/prFacilities.txt";
//		outputPath = "/Users/Ihab/Desktop/test/";

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
		
		Scenario scenario1 = getScenario(netFile, plansFile1);
		Scenario scenario2 = getScenario(netFile, plansFile2);
		PRFileReader prFileReader = new PRFileReader(prFacilitiesFile);		
		Map<Id, ParkAndRideFacility> id2PRFacilities = prFileReader.getId2prFacility();
		
		System.out.println("-------------------------------------------------------");
		
		compareScores(scenario1.getPopulation(), scenario2.getPopulation(), tolerance);
		analyzePR(scenario2, id2PRFacilities);

	}

	private void analyzePR(Scenario scenario2, Map<Id, ParkAndRideFacility> id2prFacilities) {
		
		List<Person> personsPR = new ArrayList<Person>();
		List<Person> personsHomeWork = new ArrayList<Person>();

		for (Person person : scenario2.getPopulation().getPersons().values()){
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
				personsPR.add(person);
			}
			
			if (hasHome && hasWork){
				personsHomeWork.add(person);
			}
		}
		log.info("PRPlans: " + personsPR.size());
		log.info("WorkPlans: " + personsHomeWork.size());
		log.info("Park'n'Ride-Anteil: " + (double) personsPR.size() / (double) personsHomeWork.size()*100+"%");
		
		writer.writeFile2(personsPR, personsHomeWork, outputPath+"prPlans.txt");
		
		SortedMap<Id,Coord> homeCoordinates = getCoordinates(personsPR, "home");
		SortedMap<Id,Coord> workCoordinates = getCoordinates(personsPR, "work");
		SortedMap<Id,Coord> prCoordinates = getCoordinates(personsPR, ParkAndRideConstants.PARKANDRIDE_ACTIVITY_TYPE);

		MyShapeFileWriter shapeFileWriter = new MyShapeFileWriter();
		
		File directory = new File(outputPath+"shapeFiles");
		directory.mkdirs();
		
		shapeFileWriter.writeShapeFilePoints(scenario2, homeCoordinates, outputPath + "shapeFiles/homeCoordinates.shp");
		shapeFileWriter.writeShapeFilePoints(scenario2, workCoordinates, outputPath + "shapeFiles/workCoordinates.shp");
		shapeFileWriter.writeShapeFilePoints(scenario2, prCoordinates, outputPath + "shapeFiles/prCoordinates.shp");
		shapeFileWriter.writeShapeFileLines(scenario2, outputPath + "shapeFiles/network.shp");
		
		Map<Id, Integer> prLinkId2prActs = new HashMap<Id, Integer>();

		for (Person person : personsPR){
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
		
		writer.writeFile3(prLinkId2prActs, id2prFacilities, outputPath+"prUsage.txt");
		shapeFileWriter.writeShapeFilePRUsage(scenario2, id2prFacilities, prLinkId2prActs, outputPath + "shapeFiles/prUsage.shp");
		
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
	
	
	//------------------------------------------------------------------------------------------------------------------------

	private void compareScores(Population population1, Population population2, double tolerance) {
		
		List <Id> improvedPlanPersonIDs = new ArrayList<Id>();
		List <Id> worsePlanPersonIDs = new ArrayList<Id>();
		List <Id> equalPlanPersonIDs = new ArrayList<Id>();

		// for selected plans
		
		for (Person person1 : population1.getPersons().values()){
			Plan plan1 = person1.getSelectedPlan();
			if (plan1.getScore() == null){
				log.info("Plan1 hat keinen Score. Skipping this plan...");
			} else {
				double score1 = plan1.getScore();
				Id personId1 = person1.getId();
				if (population2.getPersons().get(personId1) == null){
				} else {
					Person person2 = population2.getPersons().get(personId1);
					Plan plan2 = person2.getSelectedPlan();
					if (plan2.getScore()==null){
						log.info("Plan2 hat keinen Score. Skipping this plan...");
					} else {
						double score2 = plan2.getScore();
						if (score2 > score1 + tolerance) {
							improvedPlanPersonIDs.add(person2.getId());
						} else if ( score1 > score2 + tolerance){
							worsePlanPersonIDs.add(person2.getId());
						} else {
							equalPlanPersonIDs.add(person2.getId());
						}

					}
				}
			}
		}
		
		writer.writeFile1(improvedPlanPersonIDs, improvedPlanPersonIDs.size(), outputPath + "improvedPersons.txt");
		writer.writeFile1(worsePlanPersonIDs, worsePlanPersonIDs.size(), outputPath + "worsePersons.txt");
		writer.writeFile1(equalPlanPersonIDs, equalPlanPersonIDs.size(), outputPath + "equalPersons.txt");

//		List <Id> personIdsWithImprovedPlanWithoutPR = new ArrayList<Id>();
//		List <Id> personIdsWithImprovedPlanWithPR = new ArrayList<Id>();
//		List <Id> personIdsWithWorsePlan = new ArrayList<Id>();
//
//		int plansImprovedWithoutPR = 0;
//		int plansImprovedWithPR = 0;
//		int plansWorse = 0;
//		
//		for (Person person1 : population1.getPersons().values()){
//			for (Plan plan1 : person1.getPlans()){
//				if (plan1.getScore()==null){
//					log.info("Plan1 hat keinen Score...");
//				} else {
//					double score1 = plan1.getScore();
//					Id personId1 = person1.getId();
//					
//					if (population2.getPersons().get(personId1)==null){
//					} else {
//						Person person2 = population2.getPersons().get(personId1);
//						for (Plan plan2 : person2.getPlans()){
//							boolean plan2HasPR = false;
//							
//							if (plan2.getScore()==null){
//								log.info("Plan2 hat keinen Score...");
//							} else {
//								double score2 = plan2.getScore();
//								if (score2 > score1 + tolerance) {
//									for (PlanElement pE : plan2.getPlanElements()){
//										if (pE instanceof Activity){
//											Activity act = (Activity) pE;
//											if (act.toString().contains("parkAndRide")){
//												plan2HasPR = true;
//											}
//										}
//									}
//									if (plan2HasPR){
//										plansImprovedWithPR++;
//										if (personIdsWithImprovedPlanWithPR.contains(person2.getId())){
//										} else {
//											personIdsWithImprovedPlanWithPR.add(person2.getId());
//										}
//									} else {
//										plansImprovedWithoutPR++;
//										if (personIdsWithImprovedPlanWithoutPR.contains(person2.getId())){
//										} else {
//											personIdsWithImprovedPlanWithoutPR.add(person2.getId());
//										}
//									}
//								} else if(score1 > score2 + tolerance) {
//									plansWorse++;
//									if (personIdsWithWorsePlan.contains(person2.getId())){
//									} else {
//										personIdsWithWorsePlan.add(person2.getId());
//									}
//								} else {
//									// unchanged score
//								}
//							}	
//						}
//					}
//				}
//			}
//		}
//		writer.writeFile1(personIdsWithImprovedPlanWithoutPR, plansImprovedWithoutPR, outputPath+"improvedPlan_WithoutPR.txt");
//		writer.writeFile1(personIdsWithImprovedPlanWithPR, plansImprovedWithPR, outputPath+"improvedPlan_WithPR.txt");		
//		writer.writeFile1(personIdsWithWorsePlan, plansWorse, outputPath+"worsePlan.txt");		

	}

//------------------------------------------------------------------------------------------------------------------------
	
	private Scenario getScenario(String netFile, String plansFile){
		
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		config.plans().setInputFile(plansFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

}
