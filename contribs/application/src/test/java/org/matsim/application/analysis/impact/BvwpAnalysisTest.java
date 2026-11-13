package org.matsim.application.analysis.impact;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class BvwpAnalysisTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void writesComputedPlanValuesAndPlaceholdersWithoutReferenceRun() throws IOException {

		Path runDirectory = Path.of(utils.getOutputDirectory());
		Path legs = runDirectory.resolve("legs.csv");
		Path missingEmissions = runDirectory.resolve("analysis").resolve("emissions").resolve("emissions_per_network_mode.csv");

		Files.writeString(legs, """
			person,trip_id,dep_time,trav_time,wait_time,distance,mode,network_mode
			p1,t1,08:00:00,10:00:00,00:00:00,1000000,car,car
			f1,t2,09:00:00,10:00:00,00:00:00,60000000,freight,freight
			""");

		new BvwpAnalysis().execute(
			"--input-legs", legs.toString(),
			"--input-emissions-per-network-mode", missingEmissions.toString(),
			"--run-directory", runDirectory.toString(),
			"--sample-size", "1.0",
			"--output-bvwp-central-traffic-effects", runDirectory.resolve("bvwp_central_traffic_effects.csv").toString(),
			"--output-bvwp-emissions", runDirectory.resolve("bvwp_emissions.csv").toString(),
			"--output-bvwp-cost-benefit-analysis", runDirectory.resolve("bvwp_cost_benefit_analysis.csv").toString(),
			"--output-bvwp-costs", runDirectory.resolve("bvwp_costs.csv").toString(),
			"--output-bvwp-summary", runDirectory.resolve("bvwp_summary.csv").toString()
		);

		Assertions.assertThat(Files.readString(runDirectory.resolve("bvwp_central_traffic_effects.csv")))
			.contains("Veraenderung der Betriebsleistung im Personenverkehr (PV)")
			.contains("0.334")
			.contains("placeholder_missing_reference_run")
			.contains("placeholder_not_in_standard_matsim_output");

		Assertions.assertThat(Files.readString(runDirectory.resolve("bvwp_cost_benefit_analysis.csv")))
			.contains("NB")
			.contains("placeholder_requires_bvwp_parameters");
	}
}
