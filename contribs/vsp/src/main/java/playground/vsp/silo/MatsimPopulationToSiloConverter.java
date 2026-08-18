package playground.vsp.silo;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.util.Random;

// todo: read in zonal system and add to correct zone

public class MatsimPopulationToSiloConverter {

	static final String outputFolder = "/Users/jakob/git/silo/useCases/berlinBrandenburg/scenario/microdata/";


	static final IntColumn id = IntColumn.create("id");
	static final IntColumn hhid = IntColumn.create("hhid");
	static final IntColumn age = IntColumn.create("age");
	static final IntColumn gender = IntColumn.create("gender");
	static final StringColumn relationShip = StringColumn.create("relationShip");
	static final StringColumn race = StringColumn.create("race");
	static final IntColumn occupation = IntColumn.create("occupation");
	static final StringColumn driversLicense = StringColumn.create("driversLicense");
	static final IntColumn workplace = IntColumn.create("workplace");
	static final IntColumn income = IntColumn.create("income");
	static final StringColumn nationality = StringColumn.create("nationality");
	static final IntColumn education = IntColumn.create("education");
	static final IntColumn homeZone = IntColumn.create("homeZone");
	static final IntColumn disability = IntColumn.create("disability");
	static final IntColumn schoolId = IntColumn.create("schoolId");

	static final IntColumn ones = IntColumn.create("ones");

	static final DoubleColumn coordX = DoubleColumn.create("coordX");
	static final DoubleColumn coordY = DoubleColumn.create("coordY");

	static final StringColumn dd_type = StringColumn.create("type");

	static final Random rnd = new Random(42);


	public static void main(String[] args) {



		Config config = ConfigUtils.loadConfig("../matsim-berlin/input/v6.4/berlin-v6.4.config.xml");
//
		config.network().setInputFile(null);
		config.facilities().setInputFile(null);
		config.vehicles().setVehiclesFile(null);
//
//		Config config = ConfigUtils.createConfig();
		config.plans().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v6.4/input/berlin-v6.4-0.1pct.plans.xml.gz");

		Scenario scenario = ScenarioUtils.loadScenario(config);


		Int2ObjectMap<Id<Person>> siloToMatsimIdMap = new Int2ObjectAVLTreeMap<>();
		int personIdCounter = 0;

		//for each person
		for(Person person : scenario.getPopulation().getPersons().values()){

			if(person.getAttributes().getAttribute("subpopulation") == null || !person.getAttributes().getAttribute("subpopulation").equals("person")){
				continue;
			}

			// Create a unique Silo ID for each person
			personIdCounter++;
			siloToMatsimIdMap.put(personIdCounter, person.getId());

			if(person.getAttributes().getAttribute("age") == null) {
				throw new RuntimeException("Person " + person.getId() + " has no age attribute.");
			}


			int zone = rnd.nextInt(3500) + 1;



			addPpRow(
				personIdCounter,
				personIdCounter, //assume living alone in household --> thus hh id = person id
				(int) person.getAttributes().getAttribute("age"),
				getSex(person),
				"single",
				"unknown",
				getOccupation(person),
				getHasLicense(person),
				getWorkplaceVal(person),
				(int) ((double) person.getAttributes().getAttribute("income")),
				"UNKNOWN",
				getEducationLevel(),
				zone,
				getDisability(person),
				0
			);

			dd_type.append("SFD");
			ones.append(1);
			coordX.append((Double) person.getAttributes().getAttribute("home_x"));
			coordY.append((Double) person.getAttributes().getAttribute("home_y"));




		}

		// Build the pp table
		String ppPath = outputFolder + "pp_2011.csv";
		Table tablePp = Table.create("pp")
			.addColumns(id, hhid, age, gender, relationShip, race, occupation,
				driversLicense, workplace, income, nationality,
				education, homeZone, disability, schoolId);

		tablePp.write().csv(ppPath);

		// Build hh table
		String hhPath = outputFolder + "hh_2011.csv";
		Table tableHh = Table.create("hh")
			.addColumns(id, id.copy().setName("dwelling"), homeZone.copy().setName("zone"), ones.copy().setName("hhSize"), ones.copy().setName("autos"));

		tableHh.write().csv(hhPath);

		// Build dd table
		String ddPath = outputFolder + "dd_2011.csv";
		Table tableDd = Table.create("dd")
			.addColumns(id, homeZone.copy().setName("zone"), dd_type, id.copy().setName("hhID"), ones.copy().setName("bedrooms"), ones.copy().setName("quality"), ones.copy().multiply(500).asIntColumn().setName("monthlyCost"), ones.copy().multiply(1995).asIntColumn().setName("yearBuilt"), coordX, coordY);

		tableDd.write().csv(ddPath);


		// build siloId2MatsimId Table
		String idKeyPath = outputFolder + "matsimToSiloIdsKey.csv";
		IntColumn siloId = IntColumn.create("map_key");
		StringColumn matsimId = StringColumn.create("person_id");

		siloToMatsimIdMap.forEach((key, id) -> {
			siloId.append(key);
			matsimId.append(String.valueOf(id));
		});

		Table.create("persons", siloId, matsimId)
			.write()
			.csv(idKeyPath);
	}

	private static int getEducationLevel() {

		//    no (0),
		//    low (1),
		//    medium (2),
		//    high(3);
		return 2;
	}

	private static int getDisability(Person person) {

		//    WITHOUT (0),
		//    MENTAL(1),
		//    PHYSICAL(2);

		boolean restrictedMobility = (boolean) person.getAttributes().getAttribute("restricted_mobility");
		if (restrictedMobility) {
			return 2; // physical disability
		} else {
			return 0;
		}
	}

	private static int getOccupation(Person person) {
//		String employment = (String) person.getAttributes().getAttribute("employment");
//
//		if ("child".equals(employment)) {
//			return 0; // toddler
//		} else if (List.of("student", "school").contains(employment)) {
//			return 3; // student
//		} else if (List.of("job_full_time", "job_part_time", "homemaker","trainee","other").contains(employment)) {
//			return 1; // employed
//		} else if ("unemployed".equals(employment)) {
//			return 2; // unemployed
//		} else if ("retiree".equals(employment)) {
//			return 4; // retiree
//		} else {
//			throw new RuntimeException("employment not specified");
//		}

		// everyone unemployed for now
		return 2;

	}

	private static String getHasLicense(Person person) {
		String hasLicense = (String) person.getAttributes().getAttribute("hasLicense");

		if(hasLicense.equals("yes")) {
			return "true";
		} else if(hasLicense.equals("no")) {
			return "false";
		} else {
			throw new RuntimeException("hasLicense not specified");
		}
	}

	private static int getSex(Person person) {
		String gender = (String) person.getAttributes().getAttribute("sex");
		if(gender.equals("f"))
			return 1;
		else if (gender.equals("m"))
			return 2;
		else
			throw new RuntimeException("gender not specified");

	}

	private static int getWorkplaceVal(Person person) {

//		Activity workLocation = person
//			.getSelectedPlan()
//			.getPlanElements()
//			.stream()
//			.filter(x -> x instanceof Activity)
//			.map(x -> (Activity) x)
//			.filter(x -> x.getType().toString().startsWith("work"))
//			.findFirst().orElse(null);
//
//		if(workLocation == null) {
//			return "none";
//		}
//
//		return workLocation.getFacilityId().toString();

		return -1;
	}


	public static void addPpRow(int idVal, int hhidVal, int ageVal, int genderVal, String relationShipVal,
	                            String raceVal, int occupationVal, String driversLicenseVal, int workplaceVal,
	                            int incomeVal, String nationalityVal, int educationVal, int homeZoneVal,
	                            int disabilityVal, int schoolIdVal) {

		id.append(idVal);
		hhid.append(hhidVal);
		age.append(ageVal);
		gender.append(genderVal);
		relationShip.append(relationShipVal);
		race.append(raceVal);
		occupation.append(occupationVal);
		driversLicense.append(driversLicenseVal);
		workplace.append(workplaceVal);
		income.append(incomeVal);
		nationality.append(nationalityVal);
		education.append(educationVal);
		homeZone.append(homeZoneVal);
		disability.append(disabilityVal);
		schoolId.append(schoolIdVal);
	}
}
