package playground.staheale.matsim2030;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class AdvancedPopulationGenerator {

	private static Logger log = Logger.getLogger(AdvancedPopulationGenerator.class);
	int idCounter = 1;	//new ids will start from this number
	Id idTemp = null;
	static List<Id<Person>> weekendList = new ArrayList<>(); //list containing all mz persons with activities reported on the weekend
	List<Id<Person>> educationList = new ArrayList<>(); //list containing all mz persons being educated
	List<Id<Person>> UnknownGroupList = new ArrayList<>(); //list containing all csv persons with an initially unknown mz group
	boolean isWorking = false;
	int count = 0;
	int countPop = 0;
	// TODO
	Random random = new Random(37835409);
	int sampleSize = 1000;
	int sampleCount = 0;
	boolean index = false;

	public AdvancedPopulationGenerator() {
		super();		
	}

	public static void main(String[] args) throws Exception {
		AdvancedPopulationGenerator advancedPopGenerator = new AdvancedPopulationGenerator();
		advancedPopGenerator.run(args);
	}

	public void run(String[] args) throws Exception {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population mz_population = sc.getPopulation();

		//////////////////////////////////////////////////////////////////////
		// preparing output file for population not found in MZ population

		final String header0="Unknown_Person_id";
		final BufferedWriter out0 =
				IOUtils.getBufferedWriter("./output/NonExisting_ids.txt");
		out0.write(header0);
		out0.newLine();


		//////////////////////////////////////////////////////////////////////
		// preparing output file for population with no working activity even though specified in csv file

		final String header="Person_id";
		final BufferedWriter out =
				IOUtils.getBufferedWriter("./output/NonWorking_ids.txt");
		out.write(header);
		out.newLine();

		//////////////////////////////////////////////////////////////////////
		// preparing output file for person with initially unknown group

		final String header1="Unknown_Group_id";
		final BufferedWriter out1 =
				IOUtils.getBufferedWriter("./output/UnknownGroup_ids.txt");
		out1.write(header1);
		out1.newLine();

		//////////////////////////////////////////////////////////////////////
		// read in MZ population

		log.info("Reading MZ plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		String filename = "./input/population.15.xml";
		if (args.length > 0)
			filename = args[0];
		PlansReader.readFile(filename); //contains also agents without a plan, activity types HWELS
		log.info("Reading MZ plans...done.");
		log.info("MZ population size is " +mz_population.getPersons().size());

		// create empty plan for persons without any activity
		PopulationFactory populationFactory = mz_population.getFactory();
		Plan plan = populationFactory.createPlan();
		for (Person p : mz_population.getPersons().values()) {
			if (p.getSelectedPlan() == null) {
				p.addPlan(plan);
			}
		}

		//////////////////////////////////////////////////////////////////////
		// read in mz person info csv file

		log.info("Reading mz person info csv file...");		
		File file0 = new File("./input/zielpersonenInfo.csv");
		BufferedReader bufRdr0 = new BufferedReader(new FileReader(file0));
		String curr_line0 = bufRdr0.readLine();
		log.info("Start line iteration through mz person info csv file");
		while ((curr_line0 = bufRdr0.readLine()) != null) {
			String[] entries = curr_line0.split(";");
			String hhnr = entries[0].trim();
			String zielpnr = entries[1].trim();
			Id<Person> id = Id.create(hhnr.concat(zielpnr), Person.class);
			String weight = entries[2].trim();
			double mzWeight = Double.parseDouble(weight);
			String weekend = entries[4].trim();
			int weekendTest = Integer.parseInt(weekend);
			String education = entries[5].trim();
			int educTest = Integer.parseInt(education);


			if (weekendTest == 1) {
				weekendList.add(id);
			}
			if (educTest == 1) {
				educationList.add(id);
			}
			//set score value to weight
			Id<Person> MZid = Id.create(hhnr.concat(zielpnr), Person.class);
			if (mz_population.getPersons().containsKey(MZid)) {
				Person p = mz_population.getPersons().get(MZid);
				PersonImpl person = (PersonImpl) p;
				if (person.getSelectedPlan() != null){
					person.getSelectedPlan().setScore(mzWeight);
				}
			}
		}
		bufRdr0.close();
		log.info("End line iteration");
		log.info("Reading mz person info csv file...done.");

		//////////////////////////////////////////////////////////////////////
		// create mz data structure according to balmermi

		// remove persons without plan and with weekend plan
		removePersonsWithWeekendPlan(mz_population);

		log.info("  creating mz data stucture... ");
		MicroCensus2010 mz = new MicroCensus2010(mz_population);
		log.info("  done.");
		mz.print();

		//////////////////////////////////////////////////////////////////////
		// read in population csv file

		PopulationWriter pw = new PopulationWriter(mz_population, null);
		pw.writeStartPlans("./output/population2010.xml.gz");

		// TODO
		PopulationWriter pwSample = new PopulationWriter(mz_population, null);
		pwSample.writeStartPlans("./output/population2010sample.xml.gz");

		log.info("Reading population csv file...");		
		File file = new File("./input/pop2010adjusted.csv");
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String curr_line = bufRdr.readLine();
		log.info("Start line iteration through population csv file");
		while ((curr_line = bufRdr.readLine()) != null) {
			count += 1;
			String[] entries = curr_line.split(",");
			String recordId = entries[0].trim();
			Id<Person> recId = Id.create(recordId, Person.class);
			//String nrHome = entries[1].trim();
			String homeXcoord = entries[2].trim();
			String homeYcoord = entries[3].trim();
			//String ageUnder25 = entries[4].trim();
			//String ageBetween25_54 = entries[5].trim();
			//String ageOver54 = entries[6].trim();
			String hhnr = entries[7].trim();
			String zielpnr = entries[8].trim();
			String working = entries[9].trim();
			int work = Integer.parseInt(working);
			//String nrWork = entries[10].trim();
			String workXcoord = entries[11].trim();
			String workYcoord = entries[12].trim();

			//////////////////////////////////////////////////////////////////////
			// assign activity chain

			//read corresponding MZ person out of population file
			Id<Person> MZid = Id.create(hhnr.concat(zielpnr), Person.class);
			Person p = mz_population.getPersons().get(MZid);
			PersonImpl person = (PersonImpl) p;
			if (mz_population.getPersons().get(MZid) == null) {
				//log.warn("person with MZid " +MZid+ " cannot be found in MZ population!");
				out0.write(recId.toString());
				out0.newLine();
				//TODO: assign an activity chain to those persons!
				//missing info from "registererhebung": age, gender, license (available?), education
			}
			else {
				//log.info("person p with MZid " +MZid+ " is: " +p);

				// check if person should be working according to csv
				boolean has_work = false;
				if (work == 1) {
					has_work = true;
				}

				// check if referenced mz person is being educated
				boolean has_educ = false;
				if (educationList.contains(MZid)) {
					has_educ = true;
				}

				//check if effective day is on weekend, otherwise assign an activity chain from mz
				if (weekendList.contains(MZid)) {
					if (weekendList.contains(MZid) && p.getSelectedPlan() != null) {
						Plan planRem = person.getSelectedPlan();
						person.removePlan(planRem);
					} 
					Person random_mz_p = mz.getRandomWeightedMZPerson(person.getAge(),person.getSex(),person.getLicense(), has_work, has_educ);
					if (random_mz_p == null) {
						if (UnknownGroupList.contains(recId) != true) {
							UnknownGroupList.add(recId);
							out1.write(recId.toString());
							out1.newLine();
						}
						//log.warn("pid="+person.getId()+": Person does not belong to a micro census group!");
						//case of person over 24
						if (person.getAge() > 24){
							random_mz_p = mz.getRandomWeightedMZPerson(person.getAge(),person.getSex(),person.getLicense(), has_work, false);
							//log.warn("=> Assigning same demographics (>24) except that education status is set false. NOTE: Works only for CH-Microcensus 2010.");
						}
						//case of person under 25
						else {
							random_mz_p = mz.getRandomWeightedMZPerson(person.getAge(),person.getSex(),"no", has_work, has_educ);
							//log.warn("=> Assigning same demographics (<24) except that license status is set false. NOTE: Works only for CH-Microcensus 2010.");
						}

						if (random_mz_p == null) {
							throw new RuntimeException("That should not happen!");
						}
					}
					// search for a random mz person until effective date is not on the weekend
					//					int count = 0;
					//					while (weekendList.contains(random_mz_p.getId())) {
					//						random_mz_p = mz.getRandomWeightedMZPerson(person.getAge(),person.getSex(),person.getLicense(), has_work, has_educ);
					//						if (count == 100){
					//							log.warn("pid="+person.getId()+": Person does not belong to a micro census group!");
					//							if (random_mz_p == null) {
					//								Gbl.errorMsg("In CH-Microcensus 2005: That should not happen!");
					//							}
					//						}
					//						count += 1;
					//					}

					person.addPlan(random_mz_p.getSelectedPlan());
					person.setSelectedPlan(random_mz_p.getSelectedPlan());
				}

				//////////////////////////////////////////////////////////////////////
				//change home location and add home activity as last activity if required

				if (p.getSelectedPlan() != null) {

					//Plan plan = person.getSelectedPlan();
					double xHome = Double.parseDouble(homeXcoord)-2000000; //change from CH1903+ to CH1903
					double yHome = Double.parseDouble(homeYcoord)-1000000; //change from CH1903+ to CH1903
					Coord coordsHome = new Coord(xHome, yHome);
					List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
					if (pes.size() > 0){
						Activity lastAct = ((Activity) pes.get( pes.size() -1 ));
						if (lastAct.getType().equals("home") != true) {	
							//log.warn("last activity for person " +recId+ " is not a home activity!");
							Leg lastLeg = ((Leg) pes.get(pes.size() -2));
							String lastLegMode = lastLeg.getMode();
							double lastTravelTime = lastLeg.getTravelTime();
							double lastStartTime = lastAct.getStartTime();
							double lastEndTime = lastStartTime + 3600;
							double newLastActStartTime = lastEndTime+lastTravelTime;
							Leg newLastLeg = new LegImpl(lastLegMode);
							newLastLeg.setDepartureTime(lastEndTime);
							newLastLeg.setTravelTime(lastTravelTime);
							p.getSelectedPlan().addLeg(newLastLeg);
							Activity homeLastAct = new ActivityImpl("home", coordsHome);
							homeLastAct.setStartTime(newLastActStartTime);
							p.getSelectedPlan().addActivity(homeLastAct);
							lastAct.setEndTime(lastEndTime);

						}
						Iterator<PlanElement> iter = pes.iterator();
						while (iter.hasNext()) {
							PlanElement pe = iter.next();
							if (pe instanceof Activity) {
								ActivityImpl a = (ActivityImpl) pe;
								if (a.getType().equals("home")) {
									a.setCoord(coordsHome);
									//log.info("home coords for person with recId " +recId+ " changed");
								}
							}
							//deal with leg mode "other" and "abroad_teleport" --> if license = yes, change to car, otherwise to pt
							if (pe instanceof Leg) {
								LegImpl l = (LegImpl) pe;
								if (l.getMode().equals("other") || l.getMode().equals("abroad_teleport")) {
									if (person.getLicense().equals("yes")) {
										l.setMode("car");
										//log.info("leg mode of person with recId " +recId+ " is changed to car");

									}
									else {
										l.setMode("pt");
										//log.info("leg mode of person with recId " +recId+ " is changed to pt");
									}
								}
							}

						}
					}

					//////////////////////////////////////////////////////////////////////
					// change work location

					if (work == 1) {
						//check if person has working activity reported MZ, if yes: change work location, if not: add it to "non-working" list				

						double xWork = Double.parseDouble(workXcoord)-2000000; //change from CH1903+ to CH1903
						double yWork = Double.parseDouble(workYcoord)-1000000; //change from CH1903+ to CH1903
						Coord coordsWork = new Coord(xWork, yWork);

						for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
							if (pe instanceof Activity) {
								ActivityImpl a = (ActivityImpl) pe;
								if (a.getType().equals("work")) {
									a.setCoord(coordsWork);
									//log.info("work coords for person with recId " +recId+ " changed");
									isWorking = true;
								}

							}	
							//remove route elements out of plan TODO: check if this works...
							if (pe instanceof Route) {
								person.getSelectedPlan().getPlanElements().remove(pe);
							}
						}

						if (isWorking != true) {
							out.write(recId.toString());
							out.newLine();
							//log.info("employed person with recId " +recId+ " has no working activity");

						}

						isWorking = false;
					}	
					else if (work == 0) {
						for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
							if (pe instanceof Activity) {
								ActivityImpl a = (ActivityImpl) pe;
								if (a.getType().equals("work")) {
									//log.warn("person with recId " +recId+ " has a working activity planned even though not specified in the csv file");
								}

							}	

						}
					}

				}

				//////////////////////////////////////////////////////////////////////
				//add person to population
				person.setId(recId);
				pw.writePerson(person);
				countPop += 1;
				// TODO
				if (sampleCount < sampleSize) {
					index = random.nextBoolean();
					if (index != false){
						if (person.getSelectedPlan().getPlanElements().isEmpty() != true) {
							pwSample.writePerson(person);
							sampleCount += 1;
						}
					}
				}
				else if (sampleCount == 1000) {
					//TODO
					pwSample.writeEndPlans();
					log.info("sample count is " +sampleCount);
					sampleCount += 1;
				}

				
			}


		}

		bufRdr.close();
		out0.flush();
		out0.close();
		out.flush();
		out.close();
		out1.flush();
		out1.close();
		log.info("End line iteration");
		log.info("population size in csv file is: " +count);
		pw.writeEndPlans();
		log.info("Writing plans...done");
		log.info("final population size is: " +countPop);

	}

	public static void removePersonsWithWeekendPlan(Population population) {
		final Map<Id<Person>, ? extends Person> persons = population.getPersons();
		ArrayList<Id<Person>> toRemove = new ArrayList<>(persons.size());

		for (Id<Person> id : persons.keySet()) {
			if(weekendList.contains(id))
				toRemove.add(id);
		}

		for (Id<Person> id : toRemove)
			persons.remove(id);

		log.info("Removed " + toRemove.size() + " persons with weekend plan.");
	}

}

