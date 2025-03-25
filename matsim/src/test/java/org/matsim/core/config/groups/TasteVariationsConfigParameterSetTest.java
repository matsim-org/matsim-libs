package org.matsim.core.config.groups;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.scoring.functions.ModeUtilityParameters;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TasteVariationsConfigParameterSetTest {

	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testCreation() {
		TasteVariationsConfigParameterSet set = new TasteVariationsConfigParameterSet();
		assertThat(set.getName()).as("Parameter set name").isEqualTo("tasteVariations");
		assertThat(set.getIncomeExponent()).as("Default income exponent").isZero();
		assertThat(set.getVariationsOf()).as("Initial variations").isEmpty();
	}

	@Test
	void testGetterSetter() {
		TasteVariationsConfigParameterSet set = new TasteVariationsConfigParameterSet();

		// Test income exponent
		set.setIncomeExponent(1.5);
		assertThat(set.getIncomeExponent()).as("Income exponent").isEqualTo(1.5);

		// Test variations
		set.setVariationsOf(Set.of(ModeUtilityParameters.Type.constant, ModeUtilityParameters.Type.dailyUtilityConstant));

		assertThat(set.getVariationsOf())
				.as("Utility parameter variations")
				.containsExactlyInAnyOrder(ModeUtilityParameters.Type.constant, ModeUtilityParameters.Type.dailyUtilityConstant);
	}

	@Test
	void testIntegrationWithScoringConfigGroup() {
		ScoringConfigGroup scoringConfig = new ScoringConfigGroup();

		// Initially there should be no taste variations
		assertThat(scoringConfig.getScoringParameters(null).getTasteVariationsParams())
				.as("Initial taste variations")
				.isNull();

		// Test getOrAddTasteVariationsParams
		TasteVariationsConfigParameterSet params = scoringConfig.getScoringParameters(null).getOCreateTasteVariationsParams();
		assertThat(params).as("Created taste variations").isNotNull();
		assertThat(params.getIncomeExponent()).as("Default income exponent").isZero();
		assertThat(params.getVariationsOf()).as("Initial variations").isEmpty();

		// Verify the same instance is returned on second call
		TasteVariationsConfigParameterSet params2 = scoringConfig.getScoringParameters(null).getOCreateTasteVariationsParams();
		assertThat(params2).as("Retrieved taste variations").isSameAs(params);

		// Test modifying the params
		params.setIncomeExponent(2.5);
		params.setVariationsOf(Set.of(ModeUtilityParameters.Type.constant));

		// Verify we get the modified params
		TasteVariationsConfigParameterSet retrieved = scoringConfig.getScoringParameters(null).getTasteVariationsParams();
		assertThat(retrieved.getIncomeExponent()).as("Modified income exponent").isEqualTo(2.5);
		assertThat(retrieved.getVariationsOf())
				.as("Modified variations")
				.hasSize(1)
				.contains(ModeUtilityParameters.Type.constant);

		// Test setting a new instance
		TasteVariationsConfigParameterSet newParams = new TasteVariationsConfigParameterSet();
		newParams.setIncomeExponent(3.5);
		scoringConfig.getScoringParameters(null).setTasteVariationsParams(newParams);

		retrieved = scoringConfig.getScoringParameters(null).getTasteVariationsParams();
		assertThat(retrieved.getIncomeExponent()).as("New income exponent").isEqualTo(3.5);
		assertThat(retrieved.getVariationsOf()).as("New variations").isEmpty();
	}

	@Test
	void testConfigWriteRead() {
		String outFile = utils.getOutputDirectory() + "/config.xml";

		// Create config with taste variations
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup scoringConfig = config.scoring();

		TasteVariationsConfigParameterSet tasteParams = scoringConfig.getScoringParameters(null).getOCreateTasteVariationsParams();
		tasteParams.setIncomeExponent(2.0);
		tasteParams.setVariationsOf(Set.of(ModeUtilityParameters.Type.constant, ModeUtilityParameters.Type.dailyUtilityConstant));

		// Write to file
		new ConfigWriter(config).write(outFile);

		// Read from file
		Config readConfig = ConfigUtils.createConfig();
		new ConfigReader(readConfig).readFile(outFile);

		// Verify the taste variations were correctly read
		TasteVariationsConfigParameterSet readParams = readConfig.scoring().getScoringParameters(null).getTasteVariationsParams();
		assertThat(readParams).as("Read taste variations").isNotNull();
		assertThat(readParams.getIncomeExponent()).as("Read income exponent").isEqualTo(2.0);
		assertThat(readParams.getVariationsOf())
				.as("Read variations")
				.containsExactlyInAnyOrder(ModeUtilityParameters.Type.constant, ModeUtilityParameters.Type.dailyUtilityConstant);
	}

	@Test
	void testMultipleSubpopulations() {
		ScoringConfigGroup scoringConfig = new ScoringConfigGroup();

		// Add taste variations to a specific subpopulation
		TasteVariationsConfigParameterSet subPopParams = scoringConfig.getOrCreateScoringParameters("subpop1").getOCreateTasteVariationsParams();
		subPopParams.setIncomeExponent(2.0);

		assertThat(scoringConfig.getScoringParameters("subpop1").getTasteVariationsParams().getIncomeExponent())
				.as("Subpopulation income exponent")
				.isEqualTo(2.0);
	}

	@Test
	void testMultipleTasteVariationSetsError() {
		ScoringConfigGroup.ScoringParameterSet scoringParams = new ScoringConfigGroup().getScoringParameters(null);

		// Add the first taste variations parameter set through the proper API
		scoringParams.getOCreateTasteVariationsParams();

		// Directly add a second parameter set (bypassing the check in getOrAddTasteVariationsParams)
		TasteVariationsConfigParameterSet secondSet = new TasteVariationsConfigParameterSet();

		// Trying to get the taste variations should throw an exception
		assertThatThrownBy(() -> scoringParams.addParameterSet(secondSet))
				.as("Adding second taste variations set")
				.isInstanceOf(RuntimeException.class);
	}
}
