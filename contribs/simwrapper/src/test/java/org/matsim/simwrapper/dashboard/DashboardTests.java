package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.CsvOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.simwrapper.viz.TransitViewer;
import org.matsim.testcases.MatsimTestUtils;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class DashboardTests {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private void run(Dashboard... dashboards) {

		Config config = TestScenario.loadConfig(utils);
		config.controller().setLastIteration(2);

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.sampleSize = 0.001;

		SimWrapper sw = SimWrapper.create(config);
		for (Dashboard d : dashboards) {
			sw.addDashboard(d);
		}

		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);
		controler.run();
	}

	@Test
	void defaults() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run();

		// Ensure default dashboards have been added
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**stuck_agents.csv");
	}

	@Test
	void stuckAgents() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new StuckAgentDashboard());

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**stuck_agents.csv");

	}

	@Test
	void trip() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new TripDashboard());
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv")
			.isDirectoryContaining("glob:**mode_shift.csv");
	}

	@Test
	void tripPersonFilter() throws IOException {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new TripDashboard().setAnalysisArgs("--person-filter", "subpopulation=person"));
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv");

		Table tripStats = Table.read().csv(CsvReadOptions.builder(IOUtils.getBufferedReader(Path.of(utils.getOutputDirectory(), "analysis", "population", "trip_stats.csv").toString()))
			.sample(false)
			.separator(CsvOptions.detectDelimiter(Path.of(utils.getOutputDirectory(), "analysis", "population", "mode_share.csv").toString())).build());

		Assertions.assertThat(tripStats.containsColumn("freight")).isFalse();
	}

	@Test
	void tripRef() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		TripDashboard dashboard = new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv")
			.withGroupedRefData("mode_share_per_group_dist_ref.csv")
			.withDistanceDistribution("mode_share_distance_distribution.csv")
			.withChoiceEvaluation(true);

		run(dashboard);
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv")
			.isDirectoryContaining("glob:**mode_choices.csv")
			.isDirectoryContaining("glob:**mode_choice_evaluation.csv")
			.isDirectoryContaining("glob:**mode_confusion_matrix.csv");

	}

	@Test
	void populationAttribute() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new PopulationAttributeDashboard());
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**amount_per_age_group.csv")
			.isDirectoryContaining("glob:**amount_per_sex_group.csv")
			.isDirectoryContaining("glob:**total_agents.csv");


	}

	@Test
	void traffic() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "traffic");

		run(new TrafficDashboard());

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**traffic_stats_by_link_daily.csv")
			.isDirectoryContaining("glob:**traffic_stats_by_road_type_and_hour.csv")
			.isDirectoryContaining("glob:**traffic_stats_by_road_type_daily.csv");


	}

	@Test
	void odTrips() {
		run(new ODTripDashboard(Set.of("car", "pt", "walk", "bike", "ride"), "EPSG:25832"));

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trips_per_mode_car.csv")
			.isDirectoryContaining("glob:**trips_per_mode_bike.csv")
			.isDirectoryContaining("glob:**trips_per_mode_ride.csv");

	}

	@Test
	void ptCustom() {
		PublicTransitDashboard pt = new PublicTransitDashboard();

		// bus
		TransitViewer.CustomRouteType crt = new TransitViewer.CustomRouteType();
		crt.label = "Bus";
		crt.color = "#109192";
		crt.addMatchGtfsRouteType(3);

		// rail
		TransitViewer.CustomRouteType crtRail = new TransitViewer.CustomRouteType();
		crtRail.label = "Rail";
		crtRail.color = "#EC0016";
		crtRail.addMatchGtfsRouteType(2);

		pt.withCustomRouteTypes(crt, crtRail);

		run(pt);
	}
}
