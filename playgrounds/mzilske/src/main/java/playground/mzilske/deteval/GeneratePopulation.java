package playground.mzilske.deteval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class GeneratePopulation {
	
	private static Logger logger = Logger.getLogger(GeneratePopulation.class);

	private static final String MID_PERSONENDATENSATZ = "../detailedEval/MidMUC_2002/MiD2002_Personendatensatz_MUC.csv";
	
	private static final String MID_WEGEDATENSATZ = "../detailedEval/MidMUC_2002/MiD2002_Wegedatensatz_MUC.csv";
	
	private static final String MID_WEGEKODIERUNG = "../detailedEval/MidMUC_2002/MiD2002_Wegekodierung_MUC-LH.csv";
	
	private static final String MID_SCHWERPUNKTE = "../detailedEval/Muenchen_Schwerpunkte-Verkehrszellen.csv";
	
	private static final String PLANS = "../detailedEval/pop/plans.xml";

	private Scenario scenario = new ScenarioImpl();
	
	private Map<Id, Person> persons = new HashMap<Id, Person>();
	
	private Map<Id, Plan> plans = new HashMap<Id, Plan>();

	private Map<String, Coord> schwerpunkte = new HashMap<String, Coord>();
	
	public static void main(String[] args) throws IOException {
		GeneratePopulation generatePopulation = new GeneratePopulation();
		generatePopulation.parsePersons();
		generatePopulation.parseSchwerpunkte();
		generatePopulation.parsePlans();
		generatePopulation.addPlans();
		generatePopulation.writePlans();
	}

	private void parseSchwerpunkte() throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_SCHWERPUNKTE);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new CheckingTabularFileHandler() {

			@Override
			public void startRow(String[] row) {
				Coord coord = scenario.createCoord(Double.parseDouble(row[1]), Double.parseDouble(row[2]));
				schwerpunkte.put(row[0], coord);
			}
		
		});
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
					scenario.getPopulation().addPerson(person);
				} else {
					logger.warn("Plan for a person without a person record: " + personId);
				}
			}
		}
	}

	private void writePlans() {
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(PLANS);
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
	
	private void parseAndAddLeg(String[] kRow, String[] dRow) {
		PopulationFactory factory = scenario.getPopulation().getFactory();
		String caseid = kRow[K_CASEID];
		String pid = kRow[K_PID];
		Id personId = createPersonId(scenario, caseid, pid);
		Plan plan;
		Activity previousActivity;
		if (!plans.containsKey(personId)) {
			plan = factory.createPlan();
			plans.put(personId, plan);
			Coord coord = makeCoordinate(kRow[K_VONVBEZ]);
			Activity firstHomeActivity = factory.createActivityFromCoord("home", coord);
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
		plan.addLeg(leg);
		Coord coord = makeCoordinate(kRow[K_NACHVBEZ]);
		Activity activity = factory.createActivityFromCoord(parseActivityType(dRow[D_W04], plan), coord);
		plan.addActivity(activity);
	}

	private Coord makeCoordinate(String cellNumber) {
		return schwerpunkte.get(cellNumber);
	}

	private TransportMode parseLegMode(String hauptverkehrsmittel) {
		if (hauptverkehrsmittel.equals("1")) {
			return TransportMode.walk;
		} else if (hauptverkehrsmittel.equals("2")) {
			return TransportMode.bike;
		} else if (hauptverkehrsmittel.equals("3")) {
			// Mofa, Moped
			return TransportMode.motorbike;
		} else if (hauptverkehrsmittel.equals("4")) {
			// Motorrad
			return TransportMode.motorbike;
		} else if (hauptverkehrsmittel.equals("5")) {
			// Mitfahrer
			return TransportMode.miv;
		} else if (hauptverkehrsmittel.equals("8")) {
			return TransportMode.pt;
		} else if (hauptverkehrsmittel.equals("6")) {
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("7")) {
			// LKW
			return TransportMode.car;
		} else if (hauptverkehrsmittel.equals("9")) {
			// Taxi
			return TransportMode.miv;
		} else if (hauptverkehrsmittel.equals("10")) {
			// Schiff, Bahn, Bus, Flugzeug
			return TransportMode.pt;
		} else if (hauptverkehrsmittel.equals("11")) {
			return TransportMode.other;
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
				Id id = createPersonId(scenario, caseid, pid);
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
					person.setSex("w");
				} else {
					// unknown
				}
				persons.put(id, person);
			}

			
			
		});
	}
	
	private static abstract class CheckingTabularFileHandler implements TabularFileHandler {
		
		boolean first = true;
		
		int numColumns = -1;
		
		protected void check(String[] row) {
			if (first) {
				numColumns = row.length;
				System.out.println("Header: ");
				for (String entry : row) {
					System.out.print(entry);
					System.out.print(" ");
				}
				System.out.println();
			}
			if (numColumns != row.length) {
				throw new RuntimeException("Parse error. Expected: "+numColumns+" Got: "+row.length);
			}
		}
		
	}

	private static Id createPersonId(final Scenario scenario, String caseid, String pid) {
		return scenario.createId(caseid + "." + pid);
	}

}
