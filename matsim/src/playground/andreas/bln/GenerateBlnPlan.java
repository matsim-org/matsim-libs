package playground.andreas.bln;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
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
	private static final String plansOutFile = "z:/plans_out.xml";
	
	HashMap<Id, PersonImpl> personList = new HashMap<Id, PersonImpl>();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Gbl.createConfig(new String[] { "./src/playground/andreas/bln/config.xml" });
		
		GenerateBlnPlan myGenerator = new GenerateBlnPlan();
		myGenerator.generatePersons();
		myGenerator.generatePlans();
		myGenerator.filterPopulation();
		myGenerator.writePopulation();

	}
	
	private void filterPopulation(){
		
		int numberOfRemovedPersons = 0;
		ArrayList<PersonImpl> filteresPersons  = new ArrayList<PersonImpl>();
		
		for (PersonImpl person : this.personList.values()) {
			if (person.getSelectedPlan() == null){
				numberOfRemovedPersons++;
			} else {
				filteresPersons.add(person);
			}
		}
		
		log.info("Removed " + numberOfRemovedPersons + " persons with zero Plans");
		
	}
	
	
	private void writePopulation(){
		
		log.info("Generating population");
		Population pop = new Population();
		for (PersonImpl person : this.personList.values()) {
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
			
			Act lastAct = null;
			Id lastPersonId = null;
			
			for (String[] data : tripData) {
				
				if (Double.parseDouble(data[11]) != 0.0 || Double.parseDouble(data[12]) != 0.0){
				
				PersonImpl actPerson= this.personList.get(new IdImpl(data[1]));

				Plan actPlan;
				
				if (actPerson.getSelectedPlan() == null){
					actPlan = actPerson.createPlan(true);
					numberOfPlansAdded++;
				} else {
					actPlan = actPerson.getSelectedPlan();
				}
				
				Act newAct = null;
				
				
					
					// set every non home activity to work
					if (data[10].equalsIgnoreCase("WAHR")){
						newAct = new Act("home", new CoordImpl(Double.parseDouble(data[11]), Double.parseDouble(data[12])));
					} else {
						newAct = new Act("work", new CoordImpl(Double.parseDouble(data[11]), Double.parseDouble(data[12])));
					}
					
					numberOfTripsUsed++;
									
					Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMM);
					newAct.setStartTime(Time.parseTime(data[5]));
					
				
				
				
				
				if(lastPersonId == null){
					
					lastPersonId = actPerson.getId();
					lastAct = newAct;
					
				} else {
					
					if(lastPersonId.equals(actPerson.getId())){
				
						if(lastAct == null){
							log.error("This should not happen");
						} else {
							lastAct.setEndTime(Time.parseTime(data[0]));
							lastAct = newAct;
							actPlan.addLeg(new Leg(BasicLeg.Mode.car));
						}
					} else {
						if (lastAct != null){
							lastAct.setEndTime(86400.0);
							lastPersonId = actPerson.getId();
							lastAct = newAct;
						} else {
							log.error("This should not happen");
						}
						
					}
				
				}
				
				actPlan.addAct(newAct);				
//				log.info("hold");
				
				}


			}
			log.info("...finished generating " + numberOfPlansAdded + " Plans.");
			log.info("...used " + numberOfTripsUsed + " trips.");			

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

}

