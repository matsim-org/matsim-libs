package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class ImpactAnalysisDashboardTest {

	@RegisterExtension
	private final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generatesAbsoluteImpactsWithoutReference() throws IOException {

		Path runDirectory = Path.of(utils.getOutputDirectory());
		writeRunInputs(runDirectory,
			"""
				person,leg_id,mode,network_mode,distance,trav_time
				p1,0,car,car,10000,00:30:00
				p1,1,walk,,1000,00:15:00
				f1,0,freight,freight,50000,01:00:00
				""",
			"""
				person,trip_number,trip_id,trav_time,traveled_distance,main_mode
				p1,1,p1_1,00:30:00,10000,car
				p1,2,p1_2,00:15:00,1000,walk
				f1,1,f1_1,01:00:00,50000,freight
				"""
		);

		SimWrapper sw = SimWrapper.create().addDashboard(new ImpactAnalysisDashboard());
		sw.generate(runDirectory);
		sw.run(runDirectory);

		Path dashboard = runDirectory.resolve("dashboard-1.yaml");
		Path impact = runDirectory.resolve("analysis").resolve("impact").resolve("impact.csv");

		Assertions.assertThat(dashboard)
			.exists()
			.content()
			.contains("title: Wirkungsanalyse")
			.contains("impact.csv")
			.contains("Absolute Szenariowirkungen");

		Assertions.assertThat(impact)
			.exists()
			.content()
			.contains("section,metric,component,mode,period,unit,reference,scenario,difference,benefit,status,source")
			.contains("Personenverkehr,Reisezeit Personen,Reisezeit Personen,car,year,Mio. Personen-h/a")
			.contains("Score,Summe ausgefuehrter Score")
			.contains("missing_emissions")
			.contains("missing_investment_cost");
	}

	@Test
	void comparesScenarioWithReference() throws IOException {

		Path scenarioDirectory = Path.of(utils.getOutputDirectory());
		Path referenceDirectory = scenarioDirectory.resolve("reference");

		writeRunInputs(scenarioDirectory,
			"""
				person,leg_id,mode,network_mode,distance,trav_time
				p1,0,car,car,8000,00:20:00
				""",
			"""
				person,trip_number,trip_id,trav_time,traveled_distance,main_mode
				p1,1,p1_1,00:20:00,8000,car
				"""
		);
		writeRunInputs(referenceDirectory,
			"""
				person,leg_id,mode,network_mode,distance,trav_time
				p1,0,car,car,10000,00:30:00
				""",
			"""
				person,trip_number,trip_id,trav_time,traveled_distance,main_mode
				p1,1,p1_1,00:30:00,10000,car
				"""
		);

		SimWrapper sw = SimWrapper.create().addDashboard(new ImpactAnalysisDashboard(referenceDirectory));
		sw.generate(scenarioDirectory);
		sw.run(scenarioDirectory);

		Path dashboard = scenarioDirectory.resolve("dashboard-1.yaml");
		Path impact = scenarioDirectory.resolve("analysis").resolve("impact").resolve("impact.csv");

		Assertions.assertThat(dashboard)
			.content()
			.contains("Szenario und Bezugsfall")
			.contains("Szenario minus Bezugsfall");

		Assertions.assertThat(impact)
			.content()
			.contains("Personenverkehr,Verkehrsleistung Personen,Verkehrsleistung Personen,car,year,Mio. Personen-km/a,0.003340,0.002672,-0.000668")
			.contains("Agentenvergleich,Verbleiber")
			.contains("Agentenvergleich,Mittlere Scoredifferenz gemeinsamer Personen");
	}

	private void writeRunInputs(Path runDirectory, String legs, String trips) throws IOException {
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("legs.csv"), legs);
		Files.writeString(runDirectory.resolve("trips.csv"), trips);
		Files.writeString(runDirectory.resolve("persons.csv"), "person,executed_score\np1,10\nf1,5\n");
	}
}
