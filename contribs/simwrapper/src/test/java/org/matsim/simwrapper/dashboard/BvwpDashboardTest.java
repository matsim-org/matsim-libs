package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class BvwpDashboardTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generate() throws IOException {

		Path planRunDirectory = Path.of(utils.getOutputDirectory());
		Path referenceRunDirectory = planRunDirectory.resolve("reference");

		writeRunInputs(planRunDirectory,
			"""
				person,leg_id,mode,network_mode,distance,trav_time
				car-plan,0,car,car,20000,00:30:00
				car-plan,1,car,car,15000,00:20:00
				truck-plan,0,freight,freight,40000,01:00:00
				truck-plan,1,freight,freight,80000,02:00:00
				""",
			1_200_000,
			2_100_000
		);

		writeRunInputs(referenceRunDirectory,
			"""
				person,leg_id,mode,network_mode,distance,trav_time
				car-ref,0,car,car,18000,00:36:00
				car-ref,1,car,car,12000,00:24:00
				truck-ref,0,freight,freight,30000,01:10:00
				truck-ref,1,freight,freight,70000,02:20:00
				""",
			1_000_000,
			1_800_000
		);

		SimWrapper sw = SimWrapper.create()
			.addDashboard(new BvwpDashboard(Set.of("car"), Set.of("freight"), referenceRunDirectory));

		sw.generate(planRunDirectory);
		sw.run(planRunDirectory);

		Path dashboard = planRunDirectory.resolve("dashboard-1.yaml");
		Path centralTrafficEffects = planRunDirectory.resolve("analysis").resolve("impact").resolve("bvwp_central_traffic_effects.csv");
		Path emissions = planRunDirectory.resolve("analysis").resolve("impact").resolve("bvwp_emissions.csv");
		Path costBenefitAnalysis = planRunDirectory.resolve("analysis").resolve("impact").resolve("bvwp_cost_benefit_analysis.csv");
		Path costs = planRunDirectory.resolve("analysis").resolve("impact").resolve("bvwp_costs.csv");
		Path summary = planRunDirectory.resolve("analysis").resolve("impact").resolve("bvwp_summary.csv");

		Assertions.assertThat(dashboard)
			.exists()
			.content()
			.contains("title: BVWP")
			.contains("bvwp_central_traffic_effects.csv")
			.contains("bvwp_cost_benefit_analysis.csv");

		Assertions.assertThat(centralTrafficEffects)
			.exists()
			.content()
			.contains("computed_from_matsim")
			.contains("Veraenderung der Betriebsleistung im Personenverkehr (PV)");

		Assertions.assertThat(emissions)
			.exists()
			.content()
			.contains("computed_from_matsim")
			.contains("Kohlendioxid-Emissionen (CO2)");

		Assertions.assertThat(costBenefitAnalysis)
			.exists()
			.content()
			.contains("placeholder_requires_bvwp_parameters");

		Assertions.assertThat(costs).exists();
		Assertions.assertThat(summary).exists();
	}

	private void writeRunInputs(Path runDirectory, String legs, double carCo2, double freightCo2) throws IOException {

		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("legs.csv"), legs);

		Path emissionsDirectory = runDirectory.resolve("analysis").resolve("emissions");
		Files.createDirectories(emissionsDirectory);
		Files.writeString(emissionsDirectory.resolve("emissions_per_network_mode.csv"),
			"""
				vehicleType,pollutant,value
				car,NOx,1000
				car,CO,2000
				car,CO2_TOTAL,%.0f
				car,HC,3000
				car,PM,4000
				car,SO2,5000
				freight,NOx,6000
				freight,CO,7000
				freight,CO2_TOTAL,%.0f
				freight,HC,8000
				freight,PM,9000
				freight,SO2,10000
				""".formatted(carCo2, freightCo2)
		);
	}
}
