package playground.staheale.matsim2030;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class PopulationActChainGenerator {

	private static Logger log = Logger.getLogger(PopulationActChainGenerator.class);
	boolean isWorking = false;
	int count = 0;
	int countPop = 0;
	private Random random = new Random(37835409);
	char home = 'h';
	char work = 'w';
	char shop = 's';
	char leisure = 'l';
	char education = 'e';
	String workX = null;
	double workCoordX;
	String workY = null;
	double workCoordY;
	Coord coordsWork = null;
	double latestStartTime = 0;
	Id laId = null;
	//List<Id> idList = new ArrayList<Id>();
	int idCount = 1; //new ids will start from this number
	

	public PopulationActChainGenerator() {
		super();		
	}

	public static void main(String[] args) throws Exception {
		PopulationActChainGenerator generator = new PopulationActChainGenerator();
		generator.run();
	}

	public void run() throws Exception {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population mz_population = sc.getPopulation();

		Scenario scWrite = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population popWrite = scWrite.getPopulation();

		Coord sampleCoords = new Coord((double) 500000, (double) 150000);
		
		SortedMap<String,Integer> workingList = new TreeMap<String,Integer>();

		//////////////////////////////////////////////////////////////////////
		// preparing output file for population not found in MZ population

		final String header0="Unknown_Person_id";
		final BufferedWriter out0 =
				IOUtils.getBufferedWriter("./output/NonExisting_ids2030.txt");
		out0.write(header0);
		out0.newLine();


		//////////////////////////////////////////////////////////////////////
		// preparing output file for population with no working activity even though specified in csv file

		final String header="Person_id";
		final BufferedWriter out =
				IOUtils.getBufferedWriter("./output/NonWorking_ids2030.txt");
		out.write(header);
		out.newLine();


		//////////////////////////////////////////////////////////////////////
		// read in MZ population

		log.info("Reading MZ plans...");	
		MatsimPopulationReader PlansReader = new MatsimPopulationReader(sc); 
		PlansReader.readFile("./input/population.16.xml"); //contains also agents without a plan, activity types HWELS
		log.info("Reading MZ plans...done.");
		log.info("MZ population size is " +mz_population.getPersons().size());


		//////////////////////////////////////////////////////////////////////
		// read in population csv file

		PopulationWriter pw = new PopulationWriter(popWrite, null);
		pw.writeStartPlans("./output/population2030combined_without_facilities.xml.gz");

		log.info("Reading population csv file...");		
		File file0 = new File("./input/pop.combined2030.csv");
		BufferedReader bufRdr = new BufferedReader(new FileReader(file0));
		String curr_line0 = bufRdr.readLine();
		log.info("Start line iteration through mz population csv file");
		while ((curr_line0 = bufRdr.readLine()) != null) {
			count += 1;
			String[] entries = curr_line0.split(",");
			String re10record = entries[0].trim();
			Id<Person> id = Id.create(idCount, Person.class);
			//String nr = entries[1].trim();
			String homeX = entries[2].trim();
			double homeCoordX = Double.parseDouble(homeX)-2000000; //change from CH1903+ to CH1903
			String homeY = entries[3].trim();
			double homeCoordY = Double.parseDouble(homeY)-1000000; //change from CH1903+ to CH1903
			Coord coordsHome = new Coord(homeCoordX, homeCoordY);
			//String age = entries[4].trim();
			String mzId = entries[5].trim();
			Id<Person> MZid = Id.create(mzId, Person.class);
			String working = entries[6].trim();
			int isWorking = Integer.parseInt(working);
			String nrWorking = entries[7].trim();
			if (isWorking > 0) {
				workX = entries[8].trim();
				workCoordX = Double.parseDouble(workX)-2000000; //change from CH1903+ to CH1903
				workY = entries[9].trim();
				workCoordY = Double.parseDouble(workY)-1000000; //change from CH1903+ to CH1903
				coordsWork = new Coord(workCoordX, workCoordY);
				
			}
			String gender = entries[10].trim();
			String sex = null;
			if (gender.startsWith("M")) {
				sex = "m";
			}
			else {
				sex = "f";
			}
			String actchain = entries[11].trim();
			
			//////////////////////////////////////////////////////////////////////
			// analyse work locations
						
			if (isWorking > 0) {
				String coordsW = coordsWork.toString();
				if (workingList.containsKey(coordsW)) {
					int i = workingList.get(coordsW)+1;
					workingList.put(coordsW, i);
				}
				else {
					workingList.put(coordsW, 1);
				}
			}

			//////////////////////////////////////////////////////////////////////
			// assign activity chain

			// get corresponding person from mz
			Person mz_p = mz_population.getPersons().get(MZid);
			if (mz_population.getPersons().get(MZid) == null) {
				out0.write(re10record.toString());
				out0.newLine();
			}
			else {
				PersonImpl mz_person = (PersonImpl) mz_p;

				// copy mz_person
				PersonImpl person = mz_person;
				person.setId(id);
				idCount += 1;

				// check if gender information is consistent, otherwise set it to csv info
				if (mz_person.getSex().equals(sex) != true) {
					log.warn("gender information for person " +re10record+ " is not consistent");
					person.setSex(sex);
				}

				// create home activity as first activity if necessary
				List<PlanElement> pes = person.getSelectedPlan().getPlanElements();
				Activity firstAct = ((Activity) pes.get(0));
				if (firstAct.getType().startsWith("h") != true) {
					Activity homeFirstAct = new ActivityImpl("home", coordsHome);
					double travelTime = 1800; // assume travel time 0.5h
					double secondEndTime = firstAct.getEndTime();
					if (pes.size() == 1) {
						secondEndTime = 9*3600;
					}
					double startSecondAct = secondEndTime - 3600; // assume activity duration 1h;
					firstAct.setStartTime(startSecondAct);
					double firstEndTime = startSecondAct - travelTime;
					homeFirstAct.setEndTime(firstEndTime);
					//person.getSelectedPlan().addActivity(homeFirstAct);
					
					pes.add(0, homeFirstAct);
					String firstLegMode = null;
					if (pes.size() > 2) {
						Leg firstLeg = ((Leg) pes.get(2));
						firstLegMode = firstLeg.getMode();
					}
					else {
						if (mz_person.hasLicense()) {
								firstLegMode = "car";
						}
						else {
								firstLegMode = "pt";
						}
					}
					Leg newLeg = new LegImpl(firstLegMode);
					newLeg.setDepartureTime(firstEndTime);
					newLeg.setTravelTime(travelTime);
					
					//person.getSelectedPlan().addLeg(newLeg);
					pes.add(1, newLeg);
					
				}

				// create home activity as last activity if necessary
				Activity lastAct = ((Activity) pes.get( pes.size() -1 ));
				if (lastAct.getType().startsWith("h") != true) {
					Activity homeLastAct = new ActivityImpl("home", coordsHome);
					Leg lastLeg = ((Leg) pes.get(pes.size() -2));
					double travelTime = 1800; // assume travel time 0.5h
					double endSecondAct = lastAct.getStartTime() + 3600; // assume activity duration 1h;
					double newStartTime = endSecondAct + travelTime;
					lastAct.setEndTime(endSecondAct);

					String lastLegMode = lastLeg.getMode();
					Leg newLeg = new LegImpl(lastLegMode);
					newLeg.setDepartureTime(endSecondAct);
					newLeg.setTravelTime(travelTime);
					person.getSelectedPlan().addLeg(newLeg);

					homeLastAct.setStartTime(newStartTime);
					person.getSelectedPlan().addActivity(homeLastAct);
				}
				
				// change home and work location
				Iterator<PlanElement> iter = pes.iterator();
				while (iter.hasNext()) {
					PlanElement pe = iter.next();
					if (pe instanceof Activity) {
						ActivityImpl a = (ActivityImpl) pe;
						if (a.getType().equals("home")) {
							a.setCoord(coordsHome);
						}
						else if (a.getType().equals("work")) {
							a.setCoord(coordsWork);
						}
						else if (a.getType().equals("airport: home")) {
							a.setType("home");
						}
						else if (a.getType().equals("airport: shopping")) {
							a.setType("shopping");
						}
					}
					//deal with leg mode "other" and "abroad_teleport" --> if license = yes, change to car, otherwise to pt
					if (pe instanceof Leg) {
						LegImpl l = (LegImpl) pe;
						if (l.getMode().equals("other") || l.getMode().equals("abroad_teleport") || l.getMode().equals("pseudoetappe")) {
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

				//////////////////////////////////////////////////////////////////////
				// check if license holder is over 18
				if (person.hasLicense() && person.getAge() < 18) {
					log.warn("person " +person.getId()+ " has a license, but is under 18");
				}

				//////////////////////////////////////////////////////////////////////
				// add person to population
				if (person.getSelectedPlan().getPlanElements().size() > 1) {
					countPop += 1;
					pw.writePerson(person);
				}
			}

		}
		bufRdr.close();
		
		//////////////////////////////////////////////////////////////////////
		// preparing output file for work analysis
		
		final String head="workingNr;frequency";
		final BufferedWriter out2 =
		IOUtils.getBufferedWriter("./output/workingLocations2030.txt");
		out2.write(head);
		out2.newLine();

		for (String treeKey : workingList.keySet()) {
			out2.write(treeKey.toString()+ ";" +workingList.get(treeKey));
			out2.newLine();
		}
		
		out2.flush();
		out2.close();
		
		out0.flush();
		out0.close();
		out.flush();
		out.close();
		log.info("End line iteration");
		log.info("population size in csv file is: " +count);
		pw.writeEndPlans();
		log.info("Writing plans...done");
		log.info("final population size is: " +countPop);
	}
}
