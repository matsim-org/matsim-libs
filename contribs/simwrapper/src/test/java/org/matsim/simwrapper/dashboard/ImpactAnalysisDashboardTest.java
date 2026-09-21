package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DefaultDashboardProvider;
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
			.contains("title: Impact Analysis")
			.contains("Absolute traffic, physical and environmental impacts of the scenario.")
			.contains("title: \"Traffic, Physical and Environmental Effects\"")
			.contains("type: csv")
			.contains("dataset: analysis/impact/impact_overview.csv")
			.contains("height: 15.0");

		Assertions.assertThat(runDirectory.resolve("analysis/impact/impact_overview.csv"))
			.exists()
			.content()
			.contains("Category,Mode,Factor,per Day,Unit,per Year,Unit")
			.contains("Traffic / Physical Effects,car,Trips,1.00\u2060,trips/day,334.00\u2060,trips/year")
			.contains("Traffic / Physical Effects,freight,Trips,1.00\u2060,trips/day,302.00\u2060,trips/year")
			.contains("Score,all,Monetized executed score,15.00\u2060,monetary units/day,\"5,010.00\u2060\",monetary units/year")
			.doesNotContain("Persons with executed score");

		Assertions.assertThat(impact)
			.exists()
			.content()
			.contains("section,metric,component,mode,period,unit,reference,scenario,difference,relative_change,status,source")
			.contains("Personenverkehr,Reisezeit Personen,Reisezeit Personen,car,year,million person-hours/year")
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

		Config config = ConfigUtils.createConfig();
		config.global().setBaseCasePathName(referenceDirectory.toString());
		SimWrapper sw = SimWrapper.create(config);
		Dashboard impactDashboard = new DefaultDashboardProvider().getDashboards(config, sw).stream()
			.filter(ImpactAnalysisDashboard.class::isInstance)
			.findFirst()
			.orElseThrow();
		sw.addDashboard(impactDashboard);
		sw.generate(scenarioDirectory);
		sw.run(scenarioDirectory);

		Path dashboard = scenarioDirectory.resolve("dashboard-1.yaml");
		Path impact = scenarioDirectory.resolve("analysis").resolve("impact").resolve("impact.csv");

		Assertions.assertThat(dashboard)
			.content()
			.contains("Absolute impacts of the policy case and changes relative to the base")
			.contains("case.")
			.contains("dataset: analysis/impact/impact_overview.csv");

		Assertions.assertThat(impact)
			.content()
			.contains("Personenverkehr,Verkehrsleistung Personen,Verkehrsleistung Personen,car,year,million person-km/year,0.003340,0.002672,-0.000668")
			.contains("Agentenvergleich,Verbleiber")
			.contains("Agentenvergleich,Mittlere Scoredifferenz gemeinsamer Personen");

		Assertions.assertThat(scenarioDirectory.resolve("analysis/impact/impact_overview.csv"))
			.content()
			.contains("Traffic / Physical Effects,car,Person Travel Distance,8.00\u2060,person-km/day,0.00\u2060,mio. person-km/year");
	}

	private void writeRunInputs(Path runDirectory, String legs, String trips) throws IOException {
		Files.createDirectories(runDirectory);
		Files.writeString(runDirectory.resolve("legs.csv"), legs);
		Files.writeString(runDirectory.resolve("trips.csv"), trips);
		Files.writeString(runDirectory.resolve("persons.csv"), "person,executed_score\np1,10\nf1,5\n");
	}
}
