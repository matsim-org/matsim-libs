package playground.andreas.bln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.ActImpl;
import org.matsim.population.LegImpl;
import org.matsim.population.PersonImpl;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationWriter;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

/**
 * 
 * @author aneumann
 *
 */
public class GenerateBlnPlan {

	private static final Logger log = Logger.getLogger(TabReader.class);
	private static final String plansOutFile = "z:/raw_plans_out.xml";
	private static final int spreadingTime = 900; // 15min
	
	HashMap<Id, PersonImpl> personList = new HashMap<Id, PersonImpl>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Gbl.createConfig(new String[] { "./src/playground/andreas/bln/config.xml" });
		
		GenerateBlnPlan myGenerator = new GenerateBlnPlan();
		myGenerator.generatePersons();
		myGenerator.generatePlans();
		
		ArrayList<PersonImpl> persons = myGenerator.getPersonList();
		
		persons = myGenerator.filterPersonsWithZeroPlans(persons);
		persons = myGenerator.filterPersonsWithOneAct(persons);
		myGenerator.writePopulation(persons);

	}
	
	private ArrayList<PersonImpl> filterPersonsWithOneAct(ArrayList<PersonImpl> inPersons) {
		
		int numberOfRemovedPersons = 0;
		ArrayList<PersonImpl> outPersons  = new ArrayList<PersonImpl>();
		
		for (PersonImpl person : inPersons) {
			if (person.getSelectedPlan().getActsLegs().size() == 1){
				numberOfRemovedPersons++;
//				log.info("Removed person due to no selected plan");
			} else {
				outPersons.add(person);
			}
		}
		
		log.info("Removed " + numberOfRemovedPersons + " persons with one Activity");
		
		return outPersons;		
	}

	private ArrayList<PersonImpl> filterPersonsWithZeroPlans(ArrayList<PersonImpl> inPersons){
		
		int numberOfRemovedPersons = 0;
		ArrayList<PersonImpl> outPersons  = new ArrayList<PersonImpl>();
		
		for (PersonImpl person : inPersons) {
			if (person.getSelectedPlan() == null){
				numberOfRemovedPersons++;
//				log.info("Removed person due to no selected plan");
			} else {
				outPersons.add(person);
			}
		}
		
		log.info("Removed " + numberOfRemovedPersons + " persons with zero Plans");
		
		return outPersons;		
	}
	
	private ArrayList<PersonImpl> getPersonList(){
		ArrayList<PersonImpl> persons  = new ArrayList<PersonImpl>();
		for (PersonImpl person : this.personList.values()) {
			persons.add(person);
		}
		this.personList = null;
		return persons;
	}
	
	
	private void writePopulation(ArrayList<PersonImpl> filteredPersons){
		
		log.info("Generating population");
		PopulationImpl pop = new PopulationImpl();
		for (PersonImpl person : filteredPersons) {
			pop.addPerson(person);
		}
		log.info("Writing Population to " + GenerateBlnPlan.plansOutFile);
		PopulationWriter writer = new PopulationWriter(pop, GenerateBlnPlan.plansOutFile, "v4");
		writer.write();
		log.info("Finished.");
	}
	
	private void generatePlans() {
		
		int numberOfPlansAdded = 0;
		int numberOfTripsUsed = 0;
		
		try {

			log.info("Start reading file...");
			ArrayList<String[]> tripData = TabReader.readFile("Z:/WEGE.csv");
			log.info("...finished reading " + tripData.size() + " entries.");

			log.info("Start generating Plans...");
//			for (Iterator iterator = tripData.iterator(); iterator.hasNext();) {
//				String[] data = (String[]) iterator.next();
				
//			}
			
			ActImpl lastAct = null;
			LegImpl lastLeg = null;
			Id lastPersonId = null;
			
			int workCounter = 0;
			int homeCounter = 0;
			int educationCounter = 0;
			int shoppingcounter = 0;
			int leisureCounter = 0;
			int otherCounter = 0;
			int[] modalSplit = new int[8];
			
			for (String[] data : tripData) {

				// Ignore all trips with coord -1.0
				if (Double.parseDouble(data[11]) != -1.0 && Double.parseDouble(data[12]) != -1.0){

					// Ignore all trips with coord 0.0
					if (Double.parseDouble(data[11]) != 0.0 && Double.parseDouble(data[12]) != 0.0){

						PersonImpl actPerson= this.personList.get(new IdImpl(data[1]));

						Plan actPlan;

						if (actPerson.getSelectedPlan() == null){
							actPlan = actPerson.createPlan(true);
							numberOfPlansAdded++;
						} else {
							actPlan = actPerson.getSelectedPlan();
						}

						ActImpl newAct = null;

						// set every non home activity to work
//						if (data[10].equalsIgnoreCase("WAHR")){
//							newAct = new Act("home", new CoordImpl(Double.parseDouble(data[11]), Double.parseDouble(data[12])));
//						} else {
//							newAct = new Act("work", new CoordImpl(Double.parseDouble(data[11]), Double.parseDouble(data[12])));
//						}
						
						// Register ModalSplit
						if (!data[49].equalsIgnoreCase("")){
							modalSplit[Integer.parseInt(data[48])]++;
						}
						
						// Read Activity from survey
						String actType;
						int actNr = Integer.parseInt(data[38]);
						
						switch (actNr) {
						case 0:
							if (data[10].equalsIgnoreCase("WAHR")){
								actType = "home";
								homeCounter++;
							} else {
								// "keine Angabe"
								actType = "not specified";
								otherCounter++;
							}
							break;
						case 1:
							// "Arbeitsplatz"
							actType = "work";
							workCounter++;
							break;
						case 2:
							// "Schule / Ausbildung"
							actType = "education";
							educationCounter++;
							break;
						case 3:
							// "dienstl./geschaeftl."
							actType = "business";
							workCounter++;
							break;
						case 4:
							// "Einkauf taegl.Bedarf"
							actType = "shopping";
							shoppingcounter++;
							break;
						case 5:
							// "Einkauf sonstiges"
							actType = "shopping";
							shoppingcounter++;
							break;
						case 6:
							// "Freizeit (Kino,Rest.,usw.)"
							actType = "leisure";
							leisureCounter++;
							break;
						case 7:
							// "Freizeit (sonstiges incl.Sport)"
							actType = "leisure";
							leisureCounter++;
							break;
						case 8:
							// "nach Hause"
							actType = "home";
							homeCounter++;
							break;
						case 9:
							// "zum Arzt"
							actType = "see a doctor";
							otherCounter++;
							break;
						case 10:
							// "Urlaub / Reise"
							actType = "holiday / journey";
							leisureCounter++;
							break;
						case 11:
							// "Anderes"
							actType = "other";
							otherCounter++;
							break;
						case 12:
							// "Mehrfachnennung"
							actType = "multiple";
							otherCounter++;
							break;
						default:
							log.error("ActType not defined");
							actType = "not defined";
						}
						
						newAct = new ActImpl(actType, new CoordImpl(Double.parseDouble(data[11]), Double.parseDouble(data[12])));
				
						numberOfTripsUsed++;

						Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMM);
						
						// Since data is from survey, it is more likely to get x:00, x:15, x:30, x:45 as answer,
						// and therefore arbitrary peaks in departure and arrival time histogram -> spread it
						double startTime = Time.parseTime(data[5]);
						if (startTime % (15*60) == 0){
							startTime = (startTime - (spreadingTime / 2)) + Math.random() * spreadingTime;
							startTime = Math.max(0.0, startTime);
							newAct.setStartTime(startTime);
						} else {
							newAct.setStartTime(startTime);
						}

						if(lastPersonId == null){

							lastPersonId = actPerson.getId();
							lastAct = newAct;
							
							if (lastLeg == null){
								log.error("First leg");
								lastLeg = makeLeg(data);
							}

						} else {

							if(lastPersonId.equals(actPerson.getId())){

								if(lastAct == null){
									log.error("This should not happen");
								} else {
									// Since data is from survey, it is more likely to get x:00, x:15, x:30, x:45 as answer,
									// and therefore arbitrary peaks in departure and arrival time histogram -> spread it
									double endTime = Time.parseTime(data[0]);
									if (endTime % (15*60) == 0){
										endTime = (endTime - (spreadingTime / 2)) + Math.random() * spreadingTime;
										endTime = Math.max(0.0, endTime);
										lastAct.setEndTime(endTime);
									} else {
										lastAct.setEndTime(endTime);
									}
									
//									lastAct.setEndTime(Time.parseTime(data[0]));
									lastAct = newAct;
									
									// TODO [an] Reihenfolge stimmt eventuell nicht -> Test
									actPlan.addLeg(lastLeg);
									lastLeg = makeLeg(data);
									
								}
							} else {
								if (lastAct != null){
									lastAct.setEndTime(86400.0);
									lastPersonId = actPerson.getId();
									lastAct = newAct;
									lastLeg = makeLeg(data);
								} else {
									log.error("This should not happen");
								}

							}

						}

						actPlan.addAct(newAct);				
						//				log.info("hold");

					}
				}


			}
			log.info("...finished generating " + numberOfPlansAdded + " Plans.");
			log.info("...used " + numberOfTripsUsed + " trips.");
			log.info("...whereas " + homeCounter + " home, " + workCounter + " work, " + educationCounter + 
					" education, " + leisureCounter + " leisure, " + shoppingcounter + " shopping and " + 
					otherCounter + " other acts were counted.");
			log.info("...ModalSplit: " + modalSplit[0] + " keine Angabe, " + modalSplit[1] + " Fuss, " +
					modalSplit[2] +	" Rad, " + modalSplit[3] + " MIV, " + modalSplit[4] + " OEV, " +
					modalSplit[5] + " Rad/OEV, " + modalSplit[6] + " MIV/OEV, " + modalSplit[7] + " Sonst.");

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void generatePersons(){
				
		try {
			
			log.info("Start reading file...");
			ArrayList<String[]> personData = TabReader.readFile("Z:/PERSONEN.csv");
			log.info("...finished reading " + personData.size() + " entries.");
			
			log.info("Start generating BasicPersons...");
			for (String[] data : personData) {
				
				PersonImpl person = new PersonImpl(new IdImpl(data[0]));
				this.personList.put(person.getId(), person);
				
				// approximation: yearOfSurvey - yearOfBirth 
				person.setAge(98 - Integer.parseInt(data[2]));
				
				// 1 = no, 2 occasionally, 3 yes
				// TODO [an] any string can be written to file, but PersonReader expects
				// "a value from the list "always never sometimes"
				if (data[19].equalsIgnoreCase("1")){
					person.setCarAvail("never");
				} else {
					person.setCarAvail("always");
				}
				
				// filter unemployed persons and data without entry
				if (Integer.parseInt(data[12]) != 6 && Integer.parseInt(data[12]) != 0){
					person.setEmployed("yes");
				}

//				person.setHousehold(hh)(new IdImpl(data[1]));
				
				if(Integer.parseInt(data[18]) == 2){
					person.setLicence("yes");
				} else if(Integer.parseInt(data[18]) == 1){
					person.setLicence("no");
				}
				
				// TODO [an] same as setCarAvail. Any string can be written to file, but PersonReader expects
				// "a value from the list "f m "."
				if (Integer.parseInt(data[3]) == 2 ) {
					person.setSex("f");
				} else if (Integer.parseInt(data[3]) == 1){
					person.setSex("m");
				}				
				
			}
			log.info("...finished generating " + this.personList.size() + " BasicPersons.");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	private LegImpl makeLeg(String[] tripData){

		LegImpl leg = null;

		switch (Integer.parseInt(tripData[48])) {
		case 0:
			// "keine Angabe"
			leg = new LegImpl(BasicLeg.Mode.undefined);
			break;
		case 1:
			// "Fuss"
			leg = new LegImpl(BasicLeg.Mode.walk);
			break;
		case 2:
			// "Rad"
			leg = new LegImpl(BasicLeg.Mode.bike);
			break;
		case 3:
			// "MIV"
			leg = new LegImpl(BasicLeg.Mode.miv);
			break;
		case 4:
			// "OEV"
			leg = new LegImpl(BasicLeg.Mode.pt);
			break;
		case 5:
			// "Rad/OEV"
			leg = new LegImpl(BasicLeg.Mode.pt);
			break;
		case 6:
			// "IV/OEV"
			leg = new LegImpl(BasicLeg.Mode.pt);
			break;
		case 7:
			// "sonstiges"
			leg = new LegImpl(BasicLeg.Mode.undefined);
			break;
		default:
			log.error("transport mode not defined");
			leg = new LegImpl(BasicLeg.Mode.walk);
		}

		// Read travel trip time from survey (min) 
		if (!tripData[50].equalsIgnoreCase("")){
			leg.setTravelTime(Double.parseDouble(tripData[50]) * 60);
		} else {
//			log.info("empty String");
		}
		return leg;

	}

}

