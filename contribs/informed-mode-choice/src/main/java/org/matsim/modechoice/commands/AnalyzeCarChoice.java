package org.matsim.modechoice.commands;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.CsvOptions;
import org.matsim.application.options.ScenarioOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.search.TopKChoicesGenerator;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommandLine.Command(
		name = "analyze-car-choices",
		description = "Analyze current options and compare to car choice."
)
public class AnalyzeCarChoice implements MATSimAppCommand, PersonAlgorithm {

	@CommandLine.Mixin
	private ScenarioOptions scenario;

	@CommandLine.Mixin
	private CsvOptions csv = new CsvOptions(CSVFormat.Predefined.TDF);

	@CommandLine.Option(names = "--output", description = "Path to output tsv", required = true)
	private Path output;

	@Inject
	private TopKChoicesGenerator generator;

	@Inject
	private ScoringParametersForPerson scoring;

	private CSVPrinter printer;

	public static void main(String[] args) {
		new AnalyzeCarChoice().execute(args);
	}

	@Override
	public Integer call() throws Exception {

		Config config = scenario.getConfig();

		config.controller().setLastIteration(0);
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		Controler controler = scenario.createControler();

		Injector injector = controler.getInjector();

		controler.run();

		injector.injectMembers(this);

		try (CSVPrinter printer = csv.createPrinter(output)) {

			this.printer = printer;
			printer.printRecord("person", "can_use_car", "car_costs", "n_trips", "modes", "estimate", "car_estimate", "walk_estimate", "bike_estimate", "ride_estimate");
			ParallelPersonAlgorithmUtils.run(controler.getScenario().getPopulation(), 1, this);
		}

		return 0;
	}

	@Override
	public void run(Person person) {

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());

		List<String[]> modes = new ArrayList<>();
		modes.add(model.getCurrentModes());

		for (String m : List.of(TransportMode.car, TransportMode.walk, TransportMode.bike, TransportMode.ride)) {
			String[] current = model.getCurrentModes();
			Arrays.fill(current, m);
			modes.add(current);
		}

		List<PlanCandidate> estimate = generator.generatePredefined(model, modes);

		ScoringParameters params = scoring.getScoringParameters(person);

		double car_cost = params.modeParams.get(TransportMode.car).dailyMoneyConstant * params.marginalUtilityOfMoney;
		boolean can_use = PersonUtils.canUseCar(person);

		try {
			printer.printRecord(
					person.getId(),
					can_use,
					car_cost,
					model.trips(),
					estimate.get(0).getPlanType(),
					estimate.get(0).getUtility(),
					estimate.get(1).getUtility(),
					estimate.get(2).getUtility(),
					estimate.get(3).getUtility(),
					estimate.get(4).getUtility()
			);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

	}
}
