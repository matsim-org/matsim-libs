package playground.vsp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.contrib.vsp.pt.fare.PtTripWithDistanceBasedFareEstimator;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.List;

/**
 * A test scenario based on kelheim example.
 */
public class TestScenario extends MATSimApplication {

	/**
	 * Hand-picked agents that have trajectories fully within scenario area.
	 */
	public static final List<Id<Person>> Agents = List.of(
			Id.createPersonId("17187"),
			Id.createPersonId("10548"),
			Id.createPersonId("37842"),
			Id.createPersonId("40079"),
			Id.createPersonId("11074"),
			Id.createPersonId("13864")
	);

	public TestScenario(@Nullable Config config) {
		super(config);
	}

	public static Config loadConfig(MatsimTestUtils utils) {

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {

		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderYesAndNo.class, "car")
				.withTripEstimator(PtTripWithDistanceBasedFareEstimator.class, ModeOptions.AlwaysAvailable.class, "pt");

		controler.addOverridingModule(builder.build());

	}
}
