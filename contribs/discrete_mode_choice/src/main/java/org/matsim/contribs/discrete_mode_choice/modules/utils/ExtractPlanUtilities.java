package org.matsim.contribs.discrete_mode_choice.modules.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.FileWriter;
import java.io.IOException;

public class ExtractPlanUtilities {
	public static void writePlanUtilities(Population population, String filePath) throws IOException {
		FileWriter writer = new FileWriter(filePath);
		writer.write(String.join(CSV_SEPARATOR, CSV_HEADER));
		writer.write("\n");

		for(Person person: population.getPersons().values()) {
			double utility = Double.NaN;
			Plan plan = person.getSelectedPlan();
			if(plan != null && plan.getAttributes().getAttribute("utility") != null) {
				utility = (double) plan.getAttributes().getAttribute("utility");
			}
			writer.write(String.join(CSV_SEPARATOR, new String[]{
				person.getId().toString(),
				String.valueOf(utility)
			}));
			writer.write("\n");
		}
		writer.close();
	}

	public static final String CMD_PLANS_PATH = "plans-path";
	public static final String CMD_OUTPUT_PATH = "output-path";
	public static final String CSV_SEPARATOR = ";";
	public static final String[] CSV_HEADER = new String[]{"person_id", "utility"};

	public static void main(String[] args) throws CommandLine.ConfigurationException, IOException {
		CommandLine commandLine = new CommandLine.Builder(args).requireOptions(CMD_PLANS_PATH, CMD_OUTPUT_PATH).build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(commandLine.getOptionStrict(CMD_PLANS_PATH));

		writePlanUtilities(scenario.getPopulation(), commandLine.getOptionStrict(CMD_OUTPUT_PATH));
	}
}
