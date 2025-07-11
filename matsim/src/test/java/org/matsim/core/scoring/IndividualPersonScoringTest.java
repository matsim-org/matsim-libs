package org.matsim.core.scoring;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.TasteVariationsConfigParameterSet;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class IndividualPersonScoringTest {

    @RegisterExtension
    private MatsimTestUtils utils = new MatsimTestUtils();

    // Base car constant defined in the config
    private static final double BASE_CAR_CONSTANT = 5.0;

    // Small tolerance for floating point comparisons
    private static final double EPSILON = 1e-10;

    @Test
    void testIndividualPersonScoringOutput() throws IOException {
        // Create scenario from equil example
        Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));

        // Set output directory
        config.controller().setOutputDirectory(utils.getOutputDirectory());

        // Set up a small simulation
        config.controller().setLastIteration(1);
        config.controller().setWriteEventsInterval(1);
        config.controller().setDumpDataAtEnd(true);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        // Enable individual person scoring
        ScoringConfigGroup.ScoringParameterSet scoringParams = config.scoring().getScoringParameters(null);
        TasteVariationsConfigParameterSet tasteVariationsParams = new TasteVariationsConfigParameterSet();

        // Enable taste variations for mode constants and marginal utility of traveling
        tasteVariationsParams.setVariationsOf(Set.of(
            ModeUtilityParameters.Type.constant,
            ModeUtilityParameters.Type.dailyUtilityConstant
        ));

        scoringParams.setTasteVariationsParams(tasteVariationsParams);
		scoringParams.getOrCreateModeParams(TransportMode.car).setConstant(BASE_CAR_CONSTANT);
		scoringParams.getOrCreateModeParams(TransportMode.car).setMarginalUtilityOfDistance(0);

        // Load the scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        for (Person person : scenario.getPopulation().getPersons().values()) {

			// Use the person ID to calculate the variation, the ids needs to be an integer
			int count = Integer.parseInt(person.getId().toString());

            // Set taste variations for car mode
            // Add some variation to the constant and marginal utility of travel time
            Map<ModeUtilityParameters.Type, Double> carVariations = Map.of(
                ModeUtilityParameters.Type.constant, -1.0 + count * 0.5,
                ModeUtilityParameters.Type.dailyUtilityConstant,  count * 0.05
            );

            Map<String, Map<ModeUtilityParameters.Type, Double>> modeTasteVariations = Map.of(
				TransportMode.car, carVariations
            );

            PersonUtils.setModeTasteVariations(person, modeTasteVariations);
        }

        // Create and run the controler
        Controler controler = new Controler(scenario);
        controler.run();

        // Check that the output file exists
        String outputFile = utils.getOutputDirectory() + "person_util_variations.csv.gz";
        File outputCsvFile = new File(outputFile);

        // Check if the file exists with a descriptive error message
        assertThat(outputCsvFile)
            .exists();

        // Parse the CSV file and check its contents
        try (CSVParser parser = CSVParser.parse(
                IOUtils.getBufferedReader(outputFile),
                CSVFormat.DEFAULT.builder().setHeader().build())) {

            // Check that there are entries for the modified persons
            Map<String, CSVRecord> records = parser.getRecords().stream()
                .collect(java.util.stream.Collectors.toMap(
                    record -> record.get(0), // person ID
                    record -> record
                ));

            // First verify that all expected columns exist
            assertThat(parser.getHeaderMap().keySet())
                .as("CSV file should contain all required columns")
                .contains("car-constant", "car-dailyUtilityConstant");

            // Verify that the records contain the expected values for modified persons
            for (int i = 1; i < 50; i++) {

                String personId = Integer.toString(i);
                // Check that the record for this person exists
                assertThat(records)
                    .as("Person %s should have an entry in the output file", personId)
                    .containsKey(personId);

                CSVRecord record = records.get(personId);

                // Calculate expected values based on the formula we used when setting the variations
                double expectedConstant = BASE_CAR_CONSTANT + (-1.0 + i * 0.5);
                double expectedMarginalUtility = i * 0.05;

                // Check that the values match our calculations
                double constantValue = Double.parseDouble(record.get("car-constant"));
                assertThat(constantValue)
                    .as("car-constant value for person %s should be %f", personId, expectedConstant)
                    .isCloseTo(expectedConstant, within(EPSILON));

                double marginalUtilityValue = Double.parseDouble(record.get("car-dailyUtilityConstant"));
                assertThat(marginalUtilityValue)
                    .as("car-dailyUtilityConstant value for person %s should be %f", personId, expectedMarginalUtility)
                    .isCloseTo(expectedMarginalUtility, within(EPSILON));
            }
        }
    }
}
