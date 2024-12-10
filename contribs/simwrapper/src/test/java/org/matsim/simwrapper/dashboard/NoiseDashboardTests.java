package org.matsim.simwrapper.dashboard;

import com.opencsv.CSVReader;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.TestScenario;
import org.matsim.testcases.MatsimTestUtils;


import java.io.FileReader;
import java.net.URL;
import java.nio.file.Path;

public class NoiseDashboardTests {


	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void generate() {
		Path out = Path.of(utils.getOutputDirectory(), "analysis", "noise");

		Config config = TestScenario.loadConfig(utils);

		config.global().setCoordinateSystem("EPSG:25832");

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		URL kelheim = ExamplesUtils.getTestScenarioURL("kelheim");

		simWrapperConfigGroup.defaultParams().shp = IOUtils.extendUrl(kelheim, "area/area.shp").toString();

		SimWrapper sw = SimWrapper.create(config).addDashboard(new NoiseDashboard(config.global().getCoordinateSystem()));
		Controler controler = MATSimApplication.prepare(new TestScenario(sw), config);

		controler.run();

		Assertions.assertThat(out)
			.isDirectoryContaining("glob:**emission_per_day.csv")
			.isDirectoryContaining("glob:**immission_per_day.avro")
			.isDirectoryContaining("glob:**immission_per_hour.avro")
			.isDirectoryContaining("glob:**damages_receiverPoint_per_hour.avro")
			.isDirectoryContaining("glob:**damages_receiverPoint_per_day.avro")
			.isDirectoryContaining("glob:**noise_stats.csv");

		double totalDamages;
		double totalImmissions;
		try {
			CSVReader reader = new CSVReader(new FileReader(utils.getOutputDirectory() + "analysis/noise/noise_stats.csv"));
			reader.skip(1);
			totalDamages = Double.parseDouble(reader.readNext()[1]);
			totalImmissions = Double.parseDouble(reader.readNext()[1]);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Assertions.assertThat(totalDamages == 3573114.25);
		Assertions.assertThat( totalImmissions == 2.688);

	}
}
