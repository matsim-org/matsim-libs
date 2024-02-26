package org.matsim.application;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ConfigYamlUpdateTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void params() {

		Path input = Path.of(utils.getClassInputDirectory());

		Config config = ConfigUtils.loadConfig(input.resolve("config.xml").toString());

		ApplicationUtils.applyConfigUpdate(
			config, input.resolve("params.yml")
		);

		ScoringConfigGroup scoring = ConfigUtils.addOrGetModule(config, ScoringConfigGroup.class);

		assertThat(scoring.getModes())
			.hasSize(7);

		assertThat(scoring.getPerforming_utils_hr())
			.isEqualTo(6.88);

		ScoringConfigGroup.ModeParams car = scoring.getModes().get(TransportMode.car);

		assertThat(car.getConstant()).isEqualTo(-0.62);
		assertThat(car.getMarginalUtilityOfTraveling()).isEqualTo(0);

	}

	@Test
	void standard() {

		Config config = ConfigUtils.createConfig();
		Path input = Path.of(utils.getClassInputDirectory());

		ApplicationUtils.applyConfigUpdate(
			config, input.resolve("standard.yml")
		);

		assertThat(config.controller().getRunId()).isEqualTo("567");
		assertThat(config.global().getNumberOfThreads()).isEqualTo(8);

		assertThat(config.scoring().getOrCreateModeParams("car").getConstant()).isEqualTo(-1);
		assertThat(config.scoring().getOrCreateModeParams("bike").getConstant()).isEqualTo(-2);
	}

	@Test
	void createParamSet() {

		Config config = ConfigUtils.createConfig();
		Path input = Path.of(utils.getClassInputDirectory());

		TestConfigGroup testGroup = ConfigUtils.addOrGetModule(config, TestConfigGroup.class);

		ApplicationUtils.applyConfigUpdate(
			config, input.resolve("multiLevel.yml")
		);

		testGroup.addParam("values", "1, 2, 3");

		assertThat(testGroup.values)
			.containsExactly(1, 2, 3);

		Collection<? extends ConfigGroup> params = testGroup.getParameterSets("params");

		assertThat(params).hasSize(2);

		Iterator<? extends ConfigGroup> it = params.iterator();
		TestParamSet next = (TestParamSet) it.next();

		assertThat(next.getParams().get("mode")).isEqualTo("car");
		assertThat(next.getParams().get("values")).isEqualTo("-1.0, -2.0");
		assertThat(next.values).containsExactly(-1d, -2d);

		next = (TestParamSet) it.next();

		assertThat(next.getParams().get("mode")).isEqualTo("bike");
		assertThat(next.getParams().get("values")).isEqualTo("3.0, 4.0");
		assertThat(next.getParams().get("extra")).isEqualTo("extra");
	}

	@Test
	void createGroup() {
		Config config = ConfigUtils.createConfig();
		Path input = Path.of(utils.getClassInputDirectory());

		ApplicationUtils.applyConfigUpdate(
			config, input.resolve("multiLevel.yml")
		);


		TestConfigGroup test = ConfigUtils.addOrGetModule(config, TestConfigGroup.class);

		assertThat(test.values).containsExactly(1, 2, 3);

	}


	@Test
	void multiLevel() {

		Config config = ConfigUtils.createConfig();
		Path input = Path.of(utils.getClassInputDirectory());

		TestConfigGroup testGroup = ConfigUtils.addOrGetModule(config, TestConfigGroup.class);

		testGroup.addParameterSet(new TestParamSet("car", "person", "work"));
		testGroup.addParameterSet(new TestParamSet("bike", "person", "work"));

		ApplicationUtils.applyConfigUpdate(
			config, input.resolve("multiLevel.yml")
		);

		Collection<? extends ConfigGroup> params = testGroup.getParameterSets("params");
		assertThat(params).hasSize(2);

		Iterator<? extends ConfigGroup> it = params.iterator();
		ConfigGroup next = it.next();

		// These parameters are recognized as lists correctly
		assertThat(next.getParams().get("values")).isEqualTo("-1.0, -2.0");

		next = it.next();
		assertThat(next.getParams().get("values")).isEqualTo("3.0, 4.0");
		assertThat(next.getParams().get("extra")).isEqualTo("extra");

	}

	@Test
	void updateOne() {

		Config config = ConfigUtils.createConfig();
		Path input = Path.of(utils.getClassInputDirectory());

		TestConfigGroup testGroup = ConfigUtils.addOrGetModule(config, TestConfigGroup.class);

		testGroup.addParameterSet(new TestParamSet("car", "person", "work"));

		ApplicationUtils.applyConfigUpdate(
			config, input.resolve("multiLevel.yml")
		);

		Collection<? extends ConfigGroup> params = testGroup.getParameterSets("params");
		assertThat(params).hasSize(2);

		Iterator<? extends ConfigGroup> it = params.iterator();
		ConfigGroup next = it.next();

		assertThat(next.getParams().get("mode")).isEqualTo("car");
		assertThat(next.getParams().get("values")).isEqualTo("-1.0, -2.0");

		next = it.next();
		assertThat(next.getParams().get("mode")).isEqualTo("bike");
		assertThat(next.getParams().get("values")).isEqualTo("3.0, 4.0");
		assertThat(next.getParams().get("extra")).isEqualTo("extra");
	}

	@Test
	void ambiguous() {

		Config config = ConfigUtils.createConfig();
		Path input = Path.of(utils.getClassInputDirectory());

		TestConfigGroup testGroup = ConfigUtils.addOrGetModule(config, TestConfigGroup.class);

		testGroup.addParameterSet(new TestParamSet("car", "person", "work"));
		testGroup.addParameterSet(new TestParamSet("car", "person", "home"));

		// This should fail because the parameter set is ambiguous
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
			ApplicationUtils.applyConfigUpdate(
				config, input.resolve("multiLevel.yml")
			);
		});
	}


	public static final class TestConfigGroup extends ReflectiveConfigGroup {

		@Parameter
		private List<Integer> values;

		public TestConfigGroup() {
			super("test");
		}

		@Override
		public ConfigGroup createParameterSet(String type) {
			if (type.equals("params")) {
				return new TestParamSet();
			}

			return super.createParameterSet(type);
		}
	}


	public static final class TestParamSet extends ReflectiveConfigGroup {

		@Parameter
		private List<Double> values;

		public TestParamSet() {
			super("params", true);
		}

		public TestParamSet(String mode, String subpopulation, String activity) {
			super("params", true);

			this.addParam("mode", mode);
			this.addParam("subpopulation", subpopulation);
			this.addParam("activity", activity);
		}
	}

}
