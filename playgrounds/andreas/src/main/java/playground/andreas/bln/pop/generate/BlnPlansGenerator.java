package playground.andreas.bln.pop.generate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;


/**
 *
 * @author aneumann
 *
 */
public class BlnPlansGenerator {

	private static final Logger log = Logger.getLogger(TabReader.class);
	private static final int spreadingTime = 900; // 15min
	private static final boolean setAllLegsToCar = false;

	// statistics
	private int[] actTypes = new int[14];
	private int[] modalSplit = new int[9];

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Gbl.createConfig(new String[] { "./src/playground/andreas/bln/config.xml" });

			BlnPlansGenerator myBlnPlanGenerator = new BlnPlansGenerator();

			HashMap<Id, PersonImpl> personMap;
			// Create a person for everyone in file and store them in a map
			personMap = myBlnPlanGenerator.generatePersons("Z:/population/input/PERSONEN.csv");

			// Same here, but trips
			HashMap<Id, ArrayList<String[]>> tripMap = myBlnPlanGenerator.readTrips("Z:/population/input/WEGE.csv");

			// Print statistics for raw data
			myBlnPlanGenerator.countPersonsPlans(personMap, tripMap);
			// If a person has only one trip, delete that trip
			tripMap = myBlnPlanGenerator.filterPersonsWithOneTrip(tripMap);

			// Some trips have invalid coordinates. Most of them are situated outside the survey area and weren't localized
			// But some weren't localized because of faulty survey data, e.x. misspelling, spaces in Name, or a different federal state
			// Those could be reconstructed by defining a common entry point to the survey area
			//			tripMap = myBlnPlanGenerator.filterTripsWithoutCoordButEntryInCoordMap(tripMap, "z:/population/input/zero_coordinates_trips.csv");
			tripMap = myBlnPlanGenerator.filterTripsWithoutCoord(tripMap);
			// Again statistics
			tripMap = myBlnPlanGenerator.filterPersonsWithOneTrip(tripMap);

			// Add trips to persons and create a plan
			myBlnPlanGenerator.addPlansToPersons(personMap, tripMap);

			// Remove all persons without a plan
			personMap = myBlnPlanGenerator.removePersonsWithoutPlan(personMap, tripMap);

			// Write output to file
			myBlnPlanGenerator.writePopulationToFile(personMap.values(), "z:/population/output/plans.xml.gz");

			// Print additional statistics
			myBlnPlanGenerator.printStatistic();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private HashMap<Id,PersonImpl> generatePersons(String personsFileName) throws IOException{

		HashMap<Id, PersonImpl> personList = new HashMap<Id, PersonImpl>();

		log.info("setAllLegsToCar is set to " + setAllLegsToCar);

		log.info("Start reading file " + personsFileName);
		ArrayList<String[]> personData = TabReader.readFile(personsFileName);
		log.info("...finished reading " + personData.size() + " entries.");

//			log.info("Start generating persons...");
		for (String[] data : personData) {

			PersonImpl person = new PersonImpl(new IdImpl(data[0]));
			personList.put(person.getId(), person);

			// approximation: yearOfSurvey - yearOfBirth
			person.setAge(98 - Integer.parseInt(data[2]));

			// 1 = no, 2 occasionally, 3 yes
			// TODO [an] any string can be written to file, but PersonReader expects
			// "a value from the list always never sometimes"
			if (data[19].equalsIgnoreCase("1")){
				person.setCarAvail("never");
			} else {
				person.setCarAvail("always");
			}

			// filter unemployed persons and data without entry
			if (Integer.parseInt(data[12]) != 6 && Integer.parseInt(data[12]) != 0){
				person.setEmployed("yes");
			}

			// person.setHousehold(hh)(new IdImpl(data[1]));

			if(Integer.parseInt(data[18]) == 2){
				person.setLicence("yes");
			} else if(Integer.parseInt(data[18]) == 1){
				person.setLicence("no");
			} // else don't know

			// TODO [an] same as setCarAvail. Any string can be written to file, but PersonReader expects
			// "a value from the list "f m "."
			if (Integer.parseInt(data[3]) == 2 ) {
				person.setSex("f");
			} else if (Integer.parseInt(data[3]) == 1){
				person.setSex("m");
			}

		}
		log.info("...finished generating " + personList.size() + " persons.");

		return personList;
	}

	private HashMap<Id,ArrayList<String[]>> readTrips(String filename) throws IOException{

		HashMap<Id, ArrayList<String[]>> tripData = new HashMap<Id, ArrayList<String[]>>();

		log.info("Start reading file " + filename);
		ArrayList<String[]> unsortedTripData = TabReader.readFile(filename);
		log.info("...finished reading " + unsortedTripData.size() + " entries in trip file.");

		for (String[] tripDataString : unsortedTripData) {
			IdImpl personId = new IdImpl(tripDataString[1]);
			if(tripData.get(personId) != null){
				tripData.get(personId).add(tripDataString);
			} else {
				ArrayList<String[]> newArrayList = new ArrayList<String[]>();
				newArrayList.add(tripDataString);
				tripData.put(personId, newArrayList);
			}
		}

		return tripData;
	}

	private void countPersonsPlans(HashMap<Id,PersonImpl> personList, HashMap<Id,ArrayList<String[]>> tripData){

		int numberOfPersonsWithoutTrip = 0;
		int numberOfTripsWithoutPerson = 0;

		// check if every person has at least one trip
		for (Id personId : personList.keySet()) {
			if(tripData.get(personId) == null){
				numberOfPersonsWithoutTrip++;
			}
		}

		// check if every trip has a person
		for (Id personId : tripData.keySet()){
			if(personList.get(personId) == null){
				numberOfTripsWithoutPerson++;
			}
		}

		log.info(numberOfPersonsWithoutTrip + " persons have no trip");
		log.info(numberOfTripsWithoutPerson + " trips have no correspnding person in raw data.");
	}

	private HashMap<Id,ArrayList<String[]>> filterPersonsWithOneTrip(HashMap<Id,ArrayList<String[]>> unfilteredTripData){
		HashMap<Id,ArrayList<String[]>> filteredTripData = new HashMap<Id,ArrayList<String[]>>();
		int numberOfPersonsWithOnlyOneTrip = 0;

		for (Id personsId : unfilteredTripData.keySet()) {
			if(unfilteredTripData.get(personsId).size() > 1){
				filteredTripData.put(personsId, unfilteredTripData.get(personsId));
			} else {
				numberOfPersonsWithOnlyOneTrip++;
			}
		}

		log.info("Filtered " + numberOfPersonsWithOnlyOneTrip + " persons with only one trip.");
		return filteredTripData;
	}

	private HashMap<Id, ArrayList<String[]>> filterTripsWithoutCoord(HashMap<Id, ArrayList<String[]>> unfilteredTripData) {
		HashMap<Id, ArrayList<String[]>> filteredTripData = new HashMap<Id, ArrayList<String[]>>();
		int numberOfTripsWithInvalidCoord = 0;

		for (Id personsId : unfilteredTripData.keySet()) {
			ArrayList<String[]> filteredArrayList = new ArrayList<String[]>();

			for (String[] dataString : unfilteredTripData.get(personsId)) {

				// Ignore trips with invalid coordinates
				if (Double.parseDouble(dataString[11]) > 1.0 && Double.parseDouble(dataString[12]) > 1.0){
					filteredArrayList.add(dataString);
				} else {
					numberOfTripsWithInvalidCoord++;
				}
			}
			filteredTripData.put(personsId, filteredArrayList);
		}

		log.info("Filtered " + numberOfTripsWithInvalidCoord + " trips, cause coords weren't specified.");
		return filteredTripData;
	}

	private HashMap<Id, ArrayList<String[]>> filterTripsWithoutCoordButEntryInCoordMap(HashMap<Id, ArrayList<String[]>> unfilteredTripData, String filename) throws IOException {
		HashMap<Id, ArrayList<String[]>> filteredTripData = new HashMap<Id, ArrayList<String[]>>();
		int numberOfTripsWithInvalidCoord = 0;
		int numberOfReplacedCoords = 0;

		log.info("Start reading file " + filename);
		ArrayList<String[]> unsortedCoordMapData = TabReader.readFile(filename);
		log.info("...finished reading " + unsortedCoordMapData.size() + " entries in " + filename + " file.");

		HashMap<Id, ArrayList<String[]>> sortedCoordMapData = new HashMap<Id, ArrayList<String[]>>();
		for (String[] coordMapEntry : unsortedCoordMapData) {
			IdImpl personId = new IdImpl(coordMapEntry[0]);
			if(sortedCoordMapData.get(personId) != null){
				sortedCoordMapData.get(personId).add(coordMapEntry);
			} else {
				ArrayList<String[]> newArrayList = new ArrayList<String[]>();
				newArrayList.add(coordMapEntry);
				sortedCoordMapData.put(personId, newArrayList);
			}
		}

		for (Id personsId : unfilteredTripData.keySet()) {
			ArrayList<String[]> filteredArrayList = new ArrayList<String[]>();

			for (String[] dataString : unfilteredTripData.get(personsId)) {

				// Ignore trips with invalid coordinates
				if (Double.parseDouble(dataString[11]) > 1.0 && Double.parseDouble(dataString[12]) > 1.0){
					filteredArrayList.add(dataString);
				} else {

					if (sortedCoordMapData.get(personsId) != null){
						for (String[] coordMapEntry : sortedCoordMapData.get(personsId)) {

							// replace coord, if possible
							if (dataString[2].equalsIgnoreCase(coordMapEntry[1])){
								dataString[11] = coordMapEntry[3];
								dataString[12] = coordMapEntry[4];
								filteredArrayList.add(dataString);
								numberOfReplacedCoords++;
							} else {
								numberOfTripsWithInvalidCoord++;
							}
						}
					} else {
						numberOfTripsWithInvalidCoord++;
					}
				}
			}
			filteredTripData.put(personsId, filteredArrayList);
		}

		log.info("Filtered " + numberOfTripsWithInvalidCoord + " trips, cause coords weren't specified.");
		return filteredTripData;
	}

	private void addPlansToPersons(HashMap<Id,PersonImpl> personList, HashMap<Id,ArrayList<String[]>> tripData) {

		double numberOfPlansFound = 0;

		log.info("Adding Plans to Person, spreading time is " + BlnPlansGenerator.spreadingTime + "s");
		for (Id personId : tripData.keySet()) {

			ArrayList<String[]> curTripList = tripData.get(personId);
			Person curPerson = personList.get(personId);
			Plan curPlan;

			if(curPerson.getSelectedPlan() == null){
				curPlan = ((PersonImpl) curPerson).createAndAddPlan(true);
			} else {
				curPlan = curPerson.getSelectedPlan();
			}

			ActivityImpl lastAct = null;

			for (Iterator<String[]> iterator = curTripList.iterator(); iterator.hasNext();) {
				String[] tripEntry = iterator.next();

				// Read Activity from survey
				ActivityImpl newAct = new ActivityImpl(this.getActType(tripEntry), new CoordImpl(Double.parseDouble(tripEntry[11]), Double.parseDouble(tripEntry[12])));

				Time.setDefaultTimeFormat(Time.TIMEFORMAT_HHMM);

				double startTime = Time.parseTime(tripEntry[5]);

				if(startTime == 60.0){
					numberOfPlansFound++;
					if(lastAct != null){
						startTime = lastAct.getEndTime();
					} else {
						log.error("LastAct is null - Shouldn't be possible - " + curPerson.getId());
					}
				}

				// Since data is from survey, it is more likely to get x:00, x:15, x:30, x:45 as answer,
				// and therefore arbitrary peaks in departure and arrival time histogram -> spread it
				if (startTime % (15*60) == 0){
					startTime = (startTime - (spreadingTime / 2)) + Math.random() * spreadingTime;
					startTime = Math.max(0.0, startTime);
					newAct.setStartTime(startTime);
				} else {
					newAct.setStartTime(startTime);
				}

				if(lastAct != null){
					// Since data is from survey, it is more likely to get x:00, x:15, x:30, x:45 as answer,
					// and therefore arbitrary peaks in departure and arrival time histogram -> spread it
					double endTime = Time.parseTime(tripEntry[0]);
					if(endTime == 60.0){
						endTime = lastAct.getStartTime();
					}

					if (endTime % (15*60) == 0){
						endTime = (endTime - (spreadingTime / 2)) + Math.random() * spreadingTime;
						endTime = Math.max(0.0, endTime);
						lastAct.setEndTime(endTime);
					} else {
						lastAct.setEndTime(endTime);
					}

					curPlan.addLeg(this.createLeg(tripEntry));

					if(!iterator.hasNext()){
						newAct.setEndTime(86400.0);
					}
				}

				curPlan.addActivity(newAct);
				lastAct = newAct;
			}
		}
	}

	private HashMap<Id, PersonImpl> removePersonsWithoutPlan(HashMap<Id, PersonImpl> personMap, HashMap<Id, ArrayList<String[]>> tripMap) {

		HashMap<Id, PersonImpl> filteredPersonMap = new HashMap<Id, PersonImpl>();
		for (Id personId : tripMap.keySet()) {
			filteredPersonMap.put(personId, personMap.get(personId));
		}

		return filteredPersonMap;
	}

	private void writePopulationToFile(Collection<PersonImpl> personList, String filename){
		PopulationImpl pop = new ScenarioImpl().getPopulation();
		int numberOfPersonWithPlans = 0;
		for (PersonImpl person : personList) {
			pop.addPerson(person);
			numberOfPersonWithPlans++;
		}
		new PopulationWriter(pop, null).writeFile(filename);
		log.info(numberOfPersonWithPlans + " persons were written to " + filename);
	}

	private String getActType(String[] tripData){
		String actType;
		int actNr = Integer.parseInt(tripData[38]);

		switch (actNr) {
		case 0:
			if (tripData[10].equalsIgnoreCase("WAHR")){
				actType = "home";
				this.actTypes[0]++;
			} else {
				// "keine Angabe"
				actType = "not specified";
				this.actTypes[13]++;
			}
			break;
		case 1:
			// "Arbeitsplatz"
			actType = "work";
			this.actTypes[1]++;
			break;
		case 2:
			// "Schule / Ausbildung"
			actType = "education";
			this.actTypes[2]++;
			break;
		case 3:
			// "dienstl./geschaeftl."
			actType = "business";
			this.actTypes[3]++;
			break;
		case 4:
			// "Einkauf taegl.Bedarf"
			actType = "shopping";
			this.actTypes[4]++;
			break;
		case 5:
			// "Einkauf sonstiges"
			actType = "shopping";
			this.actTypes[5]++;
			break;
		case 6:
			// "Freizeit (Kino,Rest.,usw.)"
			actType = "leisure";
			this.actTypes[6]++;
			break;
		case 7:
			// "Freizeit (sonstiges incl.Sport)"
			actType = "leisure";
			this.actTypes[7]++;
			break;
		case 8:
			// "nach Hause"
			actType = "home";
			this.actTypes[8]++;
			break;
		case 9:
			// "zum Arzt"
			actType = "see a doctor";
			this.actTypes[9]++;
			break;
		case 10:
			// "Urlaub / Reise"
			actType = "holiday / journey";
			this.actTypes[10]++;
			break;
		case 11:
			// "Anderes"
			actType = "other";
			this.actTypes[11]++;
			break;
		case 12:
			// "Mehrfachnennung"
			actType = "multiple";
			this.actTypes[12]++;
			break;
		default:
			log.error("ActType not defined");
		actType = "not defined";
		this.actTypes[13]++;
		}
		return actType;
	}

	private LegImpl createLeg(String[] tripData){

		LegImpl leg = null;

		if(BlnPlansGenerator.setAllLegsToCar == true){

			leg = new LegImpl(TransportMode.car);
			this.modalSplit[3]++;

		} else {

			switch (Integer.parseInt(tripData[48])) {
			case 0:
				// "keine Angabe"
				leg = new LegImpl(TransportMode.undefined);
				this.modalSplit[0]++;
				break;
			case 1:
				// "Fuss"
//				leg = new LegImpl(TransportMode.pt);
//				this.modalSplit[4]++;
//				break;
				leg = new LegImpl(TransportMode.walk);
				this.modalSplit[1]++;
				break;
			case 2:
				// "Rad"
				leg = new LegImpl(TransportMode.bike);
				this.modalSplit[2]++;
				break;
			case 3:
				// "MIV" TODO [an] BasicLeg.Mode.miv cannot be handled by PersonPrepareForSim.1
				leg = new LegImpl(TransportMode.car);
				this.modalSplit[3]++;
				break;
			case 4:
				// "OEV"
				leg = new LegImpl(TransportMode.pt);
				this.modalSplit[4]++;
				break;
			case 5:
				// "Rad/OEV"
				leg = new LegImpl(TransportMode.pt);
				this.modalSplit[5]++;
				break;
			case 6:
				// "IV/OEV"
				leg = new LegImpl(TransportMode.pt);
				this.modalSplit[6]++;
				break;
			case 7:
				// "sonstiges"
				leg = new LegImpl(TransportMode.undefined);
				this.modalSplit[7]++;
				break;
			default:
				log.error("transport mode not defined");
			leg = new LegImpl(TransportMode.walk);
			this.modalSplit[8]++;
			}

			// Read travel trip time from survey (min)
			if (!tripData[50].equalsIgnoreCase("")){
				leg.setTravelTime(Double.parseDouble(tripData[50]) * 60);
			} else {
//			log.info("empty String");
			}

		}
		return leg;
	}

	private void printStatistic() {
		int totalNumActs = 0;
		for (int i = 0; i < this.actTypes.length; i++) {
			totalNumActs = totalNumActs + this.actTypes[i];
		}
		log.info("Number of Acts: " + totalNumActs + "\nActivity Split:" + (this.actTypes[0] + this.actTypes[8]) + " home | "
				+ this.actTypes[1] + " work | " + this.actTypes[2] + " education | "
				+ this.actTypes[3] + " business | " + (this.actTypes[4] + this.actTypes[5]) + " shopping | "
				+ (this.actTypes[6] + this.actTypes[7]) + " leisure | "
				+ this.actTypes[9] + " see a doctor | " + this.actTypes[10] + " holiday | "
				+ (this.actTypes[11] + this.actTypes[12] + this.actTypes[13]) + " other, multiple or not defined");

		int totalNumLegs = 0;
		for (int i = 0; i < this.modalSplit.length; i++) {
			totalNumLegs = totalNumLegs + this.modalSplit[i];
		}
		log.info("Number of Legs: " + totalNumLegs + "\nModal Split: " + this.modalSplit[3] + " car | "
				+ (this.modalSplit[4] + this.modalSplit[5] + this.modalSplit[6]) + " public transport | "
				+ this.modalSplit[2] +	" bike | " + this.modalSplit[1] + " walk | "
				+ (this.modalSplit[0] + this.modalSplit[7]) + " not definied | "
				+ this.modalSplit[8] + " no data");
	}

}