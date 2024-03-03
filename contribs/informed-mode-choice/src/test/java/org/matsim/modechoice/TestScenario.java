package org.matsim.modechoice;

import com.google.common.collect.Sets;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.estimators.ComplexTripEstimator;
import org.matsim.modechoice.estimators.DefaultLegScoreEstimator;
import org.matsim.modechoice.estimators.FixedCostsEstimator;
import org.matsim.modechoice.estimators.PtTripEstimator;
import org.matsim.testcases.MatsimTestUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A test scenario based on kelheim example.
 */
public class TestScenario extends MATSimApplication {

	@CommandLine.Option(names = "--mc", description = "Mass-conservation constraint", defaultValue = "false")
	private boolean mc;

	@CommandLine.Option(names = "--complex", description = "Use the complex estimator", defaultValue = "false")
	private boolean complex;

	/**
	 * Hand-picked agents that have trajectories fully within scenario area.
	 */
	public static final List<Id<Person>> Agents = List.of(
			Id.createPersonId("17187"),
			Id.createPersonId("10548"),
			Id.createPersonId("37842"),
			Id.createPersonId("40079"),
			Id.createPersonId("11074"),
			Id.createPersonId("13864"),
			Id.createPersonId("909")
	);

	public TestScenario(@Nullable Config config) {
		super(config);
	}

	/**
	 * Load scenario config.
	 * @param utils needed to set correct output directory {@code @RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils();}
	 */
	public static Config loadConfig(MatsimTestUtils utils) {

		URL context = ExamplesUtils.getTestScenarioURL("kelheim");
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(context, "config.xml"));
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		return config;
	}

	@Override
	protected Config prepareConfig(Config config) {

		for (long ii = 600; ii <= 97200; ii += 600) {

			for (String act : List.of("home", "restaurant", "other", "visit", "errands", "accomp_other", "accomp_children",
					"educ_higher", "educ_secondary", "educ_primary", "educ_tertiary", "educ_kiga", "educ_other")) {
				config.scoring()
						.addActivityParams(new ScoringConfigGroup.ActivityParams(act + "_" + ii).setTypicalDuration(ii));
			}

			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("work_" + ii).setTypicalDuration(ii)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("business_" + ii).setTypicalDuration(ii)
					.setOpeningTime(6. * 3600.).setClosingTime(20. * 3600.));
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("leisure_" + ii).setTypicalDuration(ii)
					.setOpeningTime(9. * 3600.).setClosingTime(27. * 3600.));

			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("shop_daily_" + ii).setTypicalDuration(ii)
					.setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
			config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("shop_other_" + ii).setTypicalDuration(ii)
					.setOpeningTime(8. * 3600.).setClosingTime(20. * 3600.));
		}

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("car interaction").setTypicalDuration(60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("other").setTypicalDuration(600 * 3));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_start").setTypicalDuration(60 * 15));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_end").setTypicalDuration(60 * 15));

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setModes(Set.of("car", "ride", "bike", "walk", "pt"));

//		config.planCalcScore().setExplainScores(true);

		if (mc){
			config.subtourModeChoice().setCoordDistance(50);
			config.subtourModeChoice().setChainBasedModes(new String[]{TransportMode.car, TransportMode.bike});
			config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5);
		}
		else
			config.subtourModeChoice().setChainBasedModes(new String[0]);

		return config;
	}

	@Override
	protected void prepareScenario(Scenario scenario) {

		for (Link link : scenario.getNetwork().getLinks().values()) {
			Set<String> modes = link.getAllowedModes();

			// allow freight traffic together with cars
			if (modes.contains("car")) {
				HashSet<String> newModes = Sets.newHashSet(modes);
				newModes.add("freight");

				link.setAllowedModes(newModes);
			}
		}

		// reset all scores
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				plan.setScore(null);
			}
		}
	}

	@Override
	protected void prepareControler(Controler controler) {

		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder()
				.withFixedCosts(FixedCostsEstimator.DailyConstant.class, "car")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.AlwaysAvailable.class, "ride", "bike", "walk")
				.withLegEstimator(DefaultLegScoreEstimator.class, ModeOptions.ConsiderIfCarAvailable.class, "car")
				.withTripEstimator(complex ? ComplexTripEstimator.class : PtTripEstimator.class, ModeOptions.AlwaysAvailable.class, "pt");

		if (mc)
			builder.withConstraint(RelaxedMassConservationConstraint.class);

		controler.addOverridingModule(builder.build());

	}
}
