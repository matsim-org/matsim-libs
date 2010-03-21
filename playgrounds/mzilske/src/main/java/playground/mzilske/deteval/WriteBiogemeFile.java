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

	private static final String CLUSTERING_FILE = "../detailedEval/pop/befragte-personen/clustering.txt";

	private static final String NET = "../detailedEval/pop/befragte-personen/cropped-network.xml";

	private static final String SURVEY_PLANS = "../detailedEval/pop/befragte-personen/plans.xml";

	private static final String ROUTED_PLANS = "../detailedEval/pop/befragte-personen/routed-plans.xml";

	private static final String PLANS = "../detailedEval/pop/befragte-personen/plans.xml";

	private static final String HOUSEHOLDS_FILE = "../detailedEval/pop/befragte-personen/households.xml";

	private static final String MID_WEGEDATENSATZ = "../detailedEval/eingangsdaten/MidMUC_2002/MiD2002_Wegedatensatz_MUC.csv";

	private static final int D_CASEID = 0;

	private static final int D_PID = 1;
	
	private static final int D_WEG = 2;
	
	private static final int D_W08 = 49; // Wegstrecke in km

	private Scenario scenarioWithSurveyData = new ScenarioImpl();
	
	private Scenario scenarioWithRoutedPlans = new ScenarioImpl();

	private Households households = new HouseholdsImpl();

	private BiogemeWriter biogemeWriter = new BiogemeWriter(scenarioWithSurveyData.getPopulation(), scenarioWithRoutedPlans.getPopulation(),
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
						Person sourcePerson = scenarioWithSurveyData.getPopulation()
								.getPersons().get(scenarioWithSurveyData.createId(row[0]));
						if (sourcePerson == null) {
							return;
						}
						int sourceLegIdx = Integer.parseInt(row[1]);
						Leg sourceLeg = (Leg) sourcePerson.getPlans()
								.iterator().next().getPlanElements().get(
										sourceLegIdx);
						Person targetPerson = scenarioWithSurveyData.getPopulation()
								.getPersons().get(scenarioWithSurveyData.createId(row[2]));
						if (targetPerson == null) {
							return;
						}
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
			Person person = scenarioWithSurveyData.getPopulation().getPersons().get(personId);
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
		return scenarioWithRoutedPlans.createId(caseid + "." + pid);
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
		PopulationReaderMatsimV4 populationReader1 = new PopulationReaderMatsimV4(scenarioWithSurveyData);
		populationReader1.readFile(SURVEY_PLANS);
		NetworkReaderMatsimV1 networkReader = new NetworkReaderMatsimV1(scenarioWithRoutedPlans);
		networkReader.parse(NET);
		PopulationReaderMatsimV4 populationReader2 = new PopulationReaderMatsimV4(scenarioWithRoutedPlans);
		populationReader2.readFile(ROUTED_PLANS);
		HouseholdsReaderV10 householdsReader = new HouseholdsReaderV10(households);
		householdsReader.readFile(HOUSEHOLDS_FILE);
	}

}
