package playground.mzilske.deteval;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

public class GeneratePopulation {

	private static final String MID_PERSONENDATENSATZ = "../detailedEval/MidMUC_2002/MiD2002_Personendatensatz_MUC.csv";
	
	private static final String PLANS = "../detailedEval/pop/plans.xml";
	
	private static final int CASEID = 0;

	private static final int PID = 1;
	
	private static final int PALTER = 112;

	private static final int PSEX = 113;
	
	public static void main(String[] args) throws IOException {
		final Scenario scenario = new ScenarioImpl();
		parseAndAddPlans(scenario);
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(PLANS);
	}

	private static void parseAndAddPlans(final Scenario scenario)
			throws IOException {
		TabularFileParserConfig tabFileParserConfig = new TabularFileParserConfig();
		tabFileParserConfig.setFileName(MID_PERSONENDATENSATZ);
		tabFileParserConfig.setDelimiterTags(new String[] {";"});
		new TabularFileParser().parse(tabFileParserConfig, new TabularFileHandler() {
			
			boolean first = true;
			int numColumns = -1;
			
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
				Id id = scenario.createId(caseid + "." + pid);
				PersonImpl person = (PersonImpl) scenario.getPopulation().getFactory().createPerson(id);
				String palter = row[PALTER];
				if (palter.equals("997")) {
					// Verweigert
				} else if (palter.equals("998")) {
					// Wei§ nicht
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
				scenario.getPopulation().addPerson(person);
			}

			private void check(String[] row) {
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
			
		});
	}

}
