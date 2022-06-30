package playground.vsp;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.ModeOptions;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.pt.fare.PtTripFareEstimator;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URISyntaxException;
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

		File f;
		try {
			f = new File(ExamplesUtils.getTestScenarioURL("kelheim/config.xml").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}

		Config config = ConfigUtils.loadConfig(f.toString());
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		return config;
	}

	@Override
	protected void prepareControler(Controler controler) {

		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "bike", "walk")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderYesAndNo.class, "car")
				.withTripEstimator(PtTripFareEstimator.class, ModeOptions.AlwaysAvailable.class, "pt");

		controler.addOverridingModule(builder.build());

	}
}
