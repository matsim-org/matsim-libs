package org.matsim.simwrapper.dashboard;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;

public class DashboardTests {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	private void run(Dashboard... dashboards) {

		Config config = TestScenario.loadConfig(utils);
		config.controller().setLastIteration(2);

		SimWrapperConfigGroup group = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);
		group.defaultParams().sampleSize = 0.001;

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
				.isDirectoryContaining("glob:**mode_share.csv");
	}

	@Test
	void tripRef() {

		Path out = Path.of(utils.getOutputDirectory(), "analysis", "population");

		run(new TripDashboard("mode_share_ref.csv", "mode_share_per_dist_ref.csv", "mode_users_ref.csv"));
		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**trip_stats.csv")
			.isDirectoryContaining("glob:**mode_share.csv");

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

}
