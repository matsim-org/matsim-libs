package playground.mzilske.deteval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.households.Household;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsWriterV10;
import org.matsim.households.Income.IncomePeriod;

import com.vividsolutions.jts.geom.Point;

public class GeneratePopulation {
	
	private static Logger logger = Logger.getLogger(GeneratePopulation.class);

	private static final String MID_PERSONENDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Personendatensatz_MUC.csv";
	
	private static final String MID_HAUSHALTSDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Haushaltsdatensatz_MUC.csv";
	
	private static final String MID_WEGEDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Wegedatensatz_MUC.csv";
	
	private static final String MID_WEGEKODIERUNG = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Wegekodierung_MUC-LH.csv";
		
	private static final String MID_VERKEHRSZELLEN = "../detailedEval/Net/shapeFromVISUM/Verkehrszellen_Umrisse_zone.shp";

	private static final String PLANS = "../detailedEval/pop/plans.xml";
	
	private static final String HOUSEHOLDS_FILE = "../detailedEval/pop/households.xml";
	
	private static final Integer NUMBER_OF_SIMULATED_PEOPLE = 140000;

	private Scenario scenario = new ScenarioImpl();
	
	private Map<String, Case> cases = new HashMap<String, Case>();
	
	private Households households = new HouseholdsImpl();
	
	private Map<Id, Person> persons = new HashMap<Id, Person>();
	
	private Map<Id, Plan> plans = new HashMap<Id, Plan>();
	
	private Map<Integer, Feature> verkehrszellen = new HashMap<Integer, Feature>();
	
	private Map<Activity, Integer> activity2verkehrszelle = new HashMap<Activity, Integer>();

	public static void main(String[] args) throws IOException {
		GeneratePopulation generatePopulation = new GeneratePopulation();
		generatePopulation.parseVerkehrszellen();
		generatePopulation.parseHouseholds();
		generatePopulation.parsePersons();
		generatePopulation.parsePlans();
		generatePopulation.addPlans();
		generatePopulation.dropPlanlessPeople();
		// generatePopulation.multiplyPopulation();
		generatePopulation.addPopulationToScenario();
		generatePopulation.addAndWriteHouseholds();
		generatePopulation.writePlans();
	}	

	private void addAndWriteHouseholds() throws IOException {
		for (String caseid : cases.keySet()) {
			IdImpl householdId = new IdImpl(caseid);
			Household household = households.getFactory().createHousehold(householdId);
			Case caze = cases.get(caseid);
			for (Person person : caze.members) {
				household.getMemberIds().add(person.getId());
			}
			household.setIncome(households.getFactory().createIncome(caze.income, IncomePeriod.year));
			households.getHouseholds().put(householdId, household);
		}
		HouseholdsWriterV10 writer = new HouseholdsWriterV10(households);
		writer.writeFile(HOUSEHOLDS_FILE);
	}

	private void dropPlanlessPeople() {
		Iterator<Map.Entry<String, Case>> i = cases.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<String, Case> household = i.next();
			Iterator<Person> ii = household.getValue().members.iterator();
			while (ii.hasNext()) {
				Person person = ii.next();
				if (person.getPlans().isEmpty()) {
					ii.remove();
					persons.remove(person.getId());
					if (!ii.hasNext()) {
						i.remove();
					}
				}
			}
		}
	}

	private void addPopulationToScenario() {
		for (Case haushalt : cases.values()) {
			for (Person person : haushalt.members) {
				scenario.getPopulation().addPerson(person);
			}
		}
	}
	
	private void parseVerkehrszellen() {
		FeatureSource fts;
		try {
			fts = ShapeFileReader.readDataFile(MID_VERKEHRSZELLEN);
			System.out.println(fts.getFeatures().size());
			for (Feature feature : (Collection<Feature>) fts.getFeatures()) {
				Integer no = (Integer) feature.getAttribute("NO");
				verkehrszellen.put(no, feature);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static Point getRandomPointInFeature(Random rnd, Feature ft) {
		Point p = null;
		double x, y;
		do {
			x = ft.getBounds().getMinX() + rnd.nextDouble() * (ft.getBounds().getMaxX() - ft.getBounds().getMinX());
			y = ft.getBounds().getMinY() + rnd.nextDouble() * (ft.getBounds().getMaxY() - ft.getBounds().getMinY());
			p = MGC.xy2Point(x, y);
		} while (ft.getDefaultGeometry().contains(p));
		return p;
	}
	
	private void addPlans() {
		for (Map.Entry<Id, Plan> entry : plans.entrySet()) {
			Id personId = entry.getKey();
			Plan plan = entry.getValue();
			boolean isGood = true;
			for (PlanElement planElement : plan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					if (activity.getCoord() == null) {
						logger.warn("Dumped a plan because of a coordinateless activity.");
						isGood = false;
					}
				}
			}
			if (isGood) {
				Person person = persons.get(personId);
				if (person != null) {
					person.addPlan(plan);
				} else {
					logger.warn("Plan for a person without a person record: " + personId);
				}
			}
		}
	}

	private void parsePlans()
			throws IOException {
		final List<String[]> wegekodierungRows = new ArrayList<String[]>();
		final List<String[]> wegedatensatzRows = new ArrayList<String[]>();
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_WEGEKODIERUNG);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					wegekodierungRows.add(row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

		});
		tabFileParserConfig.setFileName(MID_WEGEDATENSATZ);
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					wegedatensatzRows.add(row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

		});
		
		Iterator<String[]> iKodierung = wegekodierungRows.iterator();
		Iterator<String[]> iDatensatz = wegedatensatzRows.iterator();
		while (iKodierung.hasNext()) {
			String[] kRow = iKodierung.next();
			String[] dRow = iDatensatz.next();
			parseAndAddLeg(kRow, dRow);
		}
	}


	private static final int K_CASEID = 0;

	private static final int K_PID = 1;
	
	private static final int K_VONVBEZ = 6;
	
	private static final int K_NACHVBEZ = 7;
	
	private static final int D_W03_HS = 51; 
	
	private static final int D_W03_MS = 52; 
	
	private static final int D_W04 = 6; // Wegzweck
	
	private static final int D_W05 = 61; // Hauptverkehrsmittel
	
	private static final int D_WEGDAUER = 56;

	private Random rnd = new Random();

	private void parseAndAddLeg(String[] kRow, String[] dRow) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		String caseid = kRow[K_CASEID];
		String pid = kRow[K_PID];
		Id personId = createPersonId(caseid, pid);
		Plan plan;
		Activity previousActivity;
		if (!plans.containsKey(personId)) {
			plan = factory.createPlan();
			plans.put(personId, plan);
			String cellNumberString = kRow[K_VONVBEZ];
			int cellNumber = Integer.parseInt(cellNumberString);
			Coord coord = createCentroidCoordIfAvailable(cellNumber);
			Activity firstHomeActivity = factory.createActivityFromCoord("home", coord);
			activity2verkehrszelle.put(firstHomeActivity, cellNumber);
			plan.addActivity(firstHomeActivity);
			previousActivity = firstHomeActivity;
		} else {
			plan = plans.get(personId);
			previousActivity = lastActicity(plan);
		}
		int h = Integer.parseInt(dRow[D_W03_HS]);
		int m = Integer.parseInt(dRow[D_W03_MS]);
		previousActivity.setEndTime(m * 60 + h * 60 * 60);
		Leg leg = factory.createLeg(parseLegMode(dRow[D_W05]));
		final double travelTime = Double.parseDouble(dRow[D_WEGDAUER]);
		if (travelTime > 999990) {
			System.out.println("Zeit falsch.");
		} else {
			leg.setTravelTime(travelTime * 60);
		}
		plan.addLeg(leg);
		String cellNumberString = kRow[K_NACHVBEZ];
		int cellNumber = Integer.parseInt(cellNumberString);
		Coord coord = createCentroidCoordIfAvailable(cellNumber);
		Activity activity = factory.createActivityFromCoord(parseActivityType(dRow[D_W04], plan), coord);
		activity2verkehrszelle.put(activity, cellNumber);
		plan.addActivity(activity);
	}

	private TransportMode parseLegMode(String hauptverkehrsmittel) {
		if (hauptverkehrsmittel.equals("1")) {
			return TransportMode.walk;
		} else if (hauptverkehrsmittel.equals("2")) {
			return TransportMode.bike;
		} else if (hauptverkehrsmittel.equals("3")) {
			// Mofa, Moped
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("4")) {
			// Motorrad
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("5")) {
			// Mitfahrer
			return TransportMode.ride;
		} else if (hauptverkehrsmittel.equals("8")) {
			// PT
			// return TransportMode.pt;
			return TransportMode.pt;
		} else if (hauptverkehrsmittel.equals("6")) {
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("7")) {
			// LKW
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("9")) {
			// Taxi
			return TransportMode.ride;
		} else if (hauptverkehrsmittel.equals("10")) {
			// Schiff, Bahn, Bus, Flugzeug
			// return TransportMode.pt;
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("11")) {
			// other
			return TransportMode.undefined;
		} else if (hauptverkehrsmittel.equals("97")) {
			return TransportMode.undefined;
		} else {
			logger.warn(hauptverkehrsmittel);
			return TransportMode.undefined;
		}
	}

	private String parseActivityType(String wegzweck, Plan plan) {
		if (wegzweck.equals("1")) {
			return "work";
		} else if (wegzweck.equals("2")) {
			return "business";
		} else if (wegzweck.equals("3")) {
			return "education";
		} else if (wegzweck.equals("4")) {
			return "shopping";
		} else if (wegzweck.equals("5")) {
			return "private";
		} else if (wegzweck.equals("6")) {
			return "pickup";
		} else if (wegzweck.equals("7")) {
			return "leisure";
		} else if (wegzweck.equals("8")) {
			return "home";
		} else if (wegzweck.equals("9")) {
			Activity previousActivity = previousActivity(plan);
			return previousActivity.getType();
		} else if (wegzweck.equals("10")) {
			return "other";
		} else if (wegzweck.equals("11")) {
			return "with adult";
		} else if (wegzweck.equals("31")) {
			return "education";
		} else if (wegzweck.equals("32")) {
			return "education";
		} else if (wegzweck.equals("32")) {
			return "education";
		} else if (wegzweck.equals("40")) {
			return "sports";
		} else if (wegzweck.equals("41")) {
			return "friends";
		} else if (wegzweck.equals("97")) {
			return "unknown";
		} else if (wegzweck.equals("98")) {
			return "unknown";
		} else if (wegzweck.equals("99")) {
			return "unknown";
		} else {
			return "unknown";
		}
	}

	private Activity previousActivity(Plan plan) {
		Activity previousActivity;
		int nPlanElements = plan.getPlanElements().size();
		if (nPlanElements >= 4) {
			previousActivity = (Activity) plan.getPlanElements().get(nPlanElements - 4);
		} else {
			previousActivity = (Activity) plan.getPlanElements().get(nPlanElements - 2);
			logger.warn("Bad round trip.");
		}
		return previousActivity;
	}

	private Activity lastActicity(Plan plan) {
		int nPlanElements = plan.getPlanElements().size();
		Activity lastActivity = (Activity) plan.getPlanElements().get(nPlanElements - 1);
		return lastActivity;
	}
	
	private void parseHouseholds() throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_HAUSHALTSDATENSATZ);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			private static final int CASEID = 0;
			
			private static final int H_H07 = 120;
			
			private static final int H_H071 = 121; // Einkommen in DM
			
			private static final int H_H072 = 125; // Einkommen in Euro
			
			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					parseAndAddHousehold(scenario, row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

			private void parseAndAddHousehold(Scenario scenario, String[] row) {
				Case household = new Case();
				String caseid = row[CASEID];
				int DMorEuro = Integer.parseInt(row[H_H07]);
				if (DMorEuro == 1) {
					int incomeInTDM = Integer.parseInt(row[H_H071]);
					if (incomeInTDM > 0 && incomeInTDM < 95) {
						household.income = (incomeInTDM * 1000) / 2; // TDM to Euro
					} else {
						household.income = -1;
					}
				} else if (DMorEuro == 2) {
					int incomeInTEur = Integer.parseInt(row[H_H072]);
					if (incomeInTEur > 0 && incomeInTEur < 95) {
						household.income = (incomeInTEur * 1000); // TDM to Euro
					} else {
						household.income = -1;
					}
				} else {
					household.income = -1;
				}
				cases.put(caseid, household);
			}
			
		});
	}

	private void parsePersons()
			throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_PERSONENDATENSATZ);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {
			
			private static final int CASEID = 0;

			private static final int PID = 1;
			
			private static final int PALTER = 112;

			private static final int PSEX = 113;
			
			@Override
			public void startRow(String[] row) {
				check(row);
				if(!first) {
					parseAndAddPerson(scenario, row);
				} else {
					// This is the header. Nothing to do.
				}
				first = false;
			}

			private void parseAndAddPerson(final Scenario scenario, String[] row) {
				String caseid = row[CASEID];
				String pid = row[PID];
				Id id = createPersonId(caseid, pid);
				PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(id);
				String palter = row[PALTER];
				if (palter.equals("997")) {
					// Verweigert
				} else if (palter.equals("998")) {
					// Weiss nicht
				} else if (palter.equals("999")) {
					// Keine Angabe
				} else {
					person.setAge(Integer.parseInt(palter));
				}
				String psex = row[PSEX];
				if (psex.equals("1")) {
					person.setSex("m");
				} else if (psex.equals("2")) {
					person.setSex("f");
				} else {
					// unknown
				}
				
				Case household = cases.get(caseid);
				household.members.add(person);
				persons.put(id, person);
			}
			
		});
	}
	
	private void multiplyPopulation() {
		Integer householdId = 0;
		Integer personId = 0;
		Map<String, Case> householdSeeds = new HashMap<String, Case>(cases);
		cases.clear();
		persons.clear();
		HashMap<String, Case> newHaushalte = new HashMap<String, Case>(); 
		while (personId < NUMBER_OF_SIMULATED_PEOPLE) {
			for (Map.Entry<String, Case> haushalt : householdSeeds.entrySet()) {
				Integer homeCell = determineHomeCell(haushalt.getValue());
				Coord homeCoord = createRandomCoord(homeCell);
				Case haushaltCopy = new Case();
				for (Person person : haushalt.getValue().members) {
					Person newPerson = copyPersonWithNewLocationsInSameCell(homeCoord, person, (personId++).toString());
					haushaltCopy.members.add(newPerson);
					persons.put(newPerson.getId(), newPerson);
				}
				newHaushalte.put((householdId++).toString(), haushaltCopy);
			}
		}
		cases.putAll(newHaushalte);
	}

	private Integer determineHomeCell(Case haushalt) {
		for (Person person : haushalt.members) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement planElement : plan.getPlanElements()) {
					if (planElement instanceof Activity) {
						Activity activity = (Activity) planElement;
						if (activity.getType().equals("home")) {
							Integer homeCell = activity2verkehrszelle.get(activity);
							if (homeCell != null) {
								return homeCell;
							}
						}
					}
				}
			}
		}
		throw new RuntimeException();
	}

	private Person copyPersonWithNewLocationsInSameCell(Coord homeCoord, Person person, String cloneId) {
		PersonImpl oldPerson = (PersonImpl) person;
		PersonImpl newPerson = (PersonImpl) scenario.getPopulation().getFactory().createPerson(scenario.createId(person.getId().toString() + "#" + cloneId));
		newPerson.setAge(oldPerson.getAge());
		newPerson.setSex(oldPerson.getSex());
		for(Plan oldPlan : oldPerson.getPlans()) {
			Plan newPlan = scenario.getPopulation().getFactory().createPlan();
			for (PlanElement planElement : oldPlan.getPlanElements()) {
				if (planElement instanceof Activity) {
					Activity activity = (Activity) planElement;
					Coord activityCoord;
					if (activity.getType().equals("home")) {
						activityCoord = homeCoord;
					} else {
						activityCoord = createRandomCoord(activity2verkehrszelle.get(activity));
					}
					Activity newActivity = scenario.getPopulation().getFactory().createActivityFromCoord(activity.getType(), activityCoord);
					newActivity.setEndTime(activity.getEndTime());
					newPlan.addActivity(newActivity);
				} else if (planElement instanceof Leg) {
					Leg leg = (Leg) planElement;
					Leg newLeg = scenario.getPopulation().getFactory().createLeg(leg.getMode());
					newPlan.addLeg(newLeg);
				}
			}
			newPerson.addPlan(newPlan);
		}
		return newPerson;
	}

	private Coord createRandomCoord(Integer integer) {
		Point point = getRandomPointInFeature(rnd, verkehrszellen.get(integer));
		return scenario.createCoord(point.getX(), point.getY());
	}
	
	private Coord createCentroidCoordIfAvailable(Integer integer) {
		Feature verkehrszelle = verkehrszellen.get(integer);
		if (verkehrszelle != null) {
			Point point = verkehrszelle.getDefaultGeometry().getCentroid();
			return scenario.createCoord(point.getX(), point.getY());
		} else {
			return null;
		}
	}

	private void writePlans() {
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(PLANS);
	}

	private Id createPersonId(String caseid, String pid) {
		return scenario.createId(caseid + "." + pid);
	}

}
