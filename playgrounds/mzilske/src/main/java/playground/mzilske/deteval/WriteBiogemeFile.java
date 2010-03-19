package playground.mzilske.deteval;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.network.NetworkReaderMatsimV1;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;
import org.matsim.households.Households;
import org.matsim.households.HouseholdsImpl;
import org.matsim.households.HouseholdsReaderV10;
import org.xml.sax.SAXException;

public class WriteBiogemeFile {

	private static final String CLUSTERING_FILE = "../detailedEval/pop/clustering.txt";

	private static final String NET = "../detailedEval/pop/befragte-personen/cropped-network.xml";
	
	private static final String PLANS = "../detailedEval/pop/befragte-personen/routed-plans.xml";

	private static final String HOUSEHOLDS_FILE = "../detailedEval/pop/households.xml";

	private static final String MID_WEGEDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Wegedatensatz_MUC.csv";

	private static final int D_CASEID = 0;

	private static final int D_PID = 1;
	
	private static final int D_WEG = 2;
	
	private static final int D_W08 = 49; // Wegstrecke in km

	private Scenario scenario = new ScenarioImpl();

	private Households households = new HouseholdsImpl();

	private BiogemeWriter biogemeWriter = new BiogemeWriter(scenario,
			households);

	private void parseClustering() throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(CLUSTERING_FILE);
		tabFileParserConfig.setDelimiterTags(new String[] { ";" });
		new TabularFileParser().parse(tabFileParserConfig,
				new CheckingTabularFileHandler() {

					@Override
					public void startRow(String[] row) {
						check(row);
						if (!first) {
							parseAndAddClustering(row);
						} else {
							// This is the header. Nothing to do.
						}
						first = false;
					}

					private void parseAndAddClustering(String[] row) {
						Person sourcePerson = scenario.getPopulation()
								.getPersons().get(scenario.createId(row[0]));
						int sourceLegIdx = Integer.parseInt(row[1]);
						Leg sourceLeg = (Leg) sourcePerson.getPlans()
								.iterator().next().getPlanElements().get(
										sourceLegIdx);
						Person targetPerson = scenario.getPopulation()
								.getPersons().get(scenario.createId(row[2]));
						int targetLegIdx = Integer.parseInt(row[3]);
						Leg targetLeg = (Leg) targetPerson.getPlans()
								.iterator().next().getPlanElements().get(
										targetLegIdx);
						biogemeWriter.putProxyLeg(sourceLeg, targetLeg);
					}
				});
	}

	private void parseAndAddTravelDistanceToLeg(String[] dRow) {
		try {
			Double dist = NumberFormat.getInstance(Locale.GERMAN).parse(dRow[D_W08]).doubleValue();
			String caseid = dRow[D_CASEID];
			String pid = dRow[D_PID];
			int weg = Integer.parseInt(dRow[D_WEG]);
			Id personId = createPersonId(caseid, pid);
			Person person = scenario.getPopulation().getPersons().get(personId);
			if (person != null) {
				Plan plan = person.getPlans().iterator().next();
				Leg leg = (Leg) plan.getPlanElements().get(2*weg-1);
				biogemeWriter.putTravelDistance(leg, dist);
			}
		} catch (ParseException e) {
			throw new RuntimeException();
		}
	}

	private void parsePlans() throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setDelimiterTags(new String[] { ";" });
		tabFileParserConfig.setFileName(MID_WEGEDATENSATZ);
		new TabularFileParser().parse(tabFileParserConfig,
				new CheckingTabularFileHandler() {

					@Override
					public void startRow(String[] row) {
						check(row);
						if (!first) {
							parseAndAddTravelDistanceToLeg(row);
						} else {
							// This is the header. Nothing to do.
						}
						first = false;
					}

				});
	}
	
	private Id createPersonId(String caseid, String pid) {
		return scenario.createId(caseid + "." + pid);
	}

	public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
		WriteBiogemeFile writeBiogemeFile = new WriteBiogemeFile();
		writeBiogemeFile.readPopulation();
		writeBiogemeFile.parsePlans();
		writeBiogemeFile.parseClustering();
		writeBiogemeFile.writeBiogemeFile();
	}

	private void writeBiogemeFile() throws IOException {
		biogemeWriter.writeBiogemeFile();
	}

	private void readPopulation() throws SAXException, ParserConfigurationException, IOException {
		NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(scenario);
		networkReader.parse(NET);
		PopulationReaderMatsimV4 populationReader = new PopulationReaderMatsimV4(scenario);
		populationReader.readFile(PLANS);
		HouseholdsReaderV10 householdsReader = new HouseholdsReaderV10(households);
		householdsReader.readFile(HOUSEHOLDS_FILE);
	}

}
