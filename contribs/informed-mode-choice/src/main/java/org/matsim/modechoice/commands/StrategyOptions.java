package org.matsim.modechoice.commands;


import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import org.apache.logging.log4j.util.TriConsumer;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.replanning.choosers.ForceInnovationStrategyChooser;
import org.matsim.core.replanning.choosers.StrategyChooser;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.InformedModeChoiceModule;
import org.matsim.modechoice.constraints.RelaxedMassConservationConstraint;
import org.matsim.modechoice.estimators.ActivityEstimator;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Command line parameters that can be included in any {@link org.matsim.application.MATSimApplication} to configurable mode-choice strategies.
 *
 * <code>
 *      {@code @CommandLine.ArgGroup(exclusive} = false, multiplicity = "0..1", heading = "Strategy Options\n")
 *      StrategyOptions strategy = new StrategyOptions();
 * </code>
 */
public final class StrategyOptions {

	@CommandLine.Option(names = {"--mode-choice", "--mc"}, description = "Mode choice strategy: ${COMPLETION-CANDIDATES}")
	private ModeChoice modeChoice;
	private final String subpopulation;

	@CommandLine.Option(names = "--weight", defaultValue = "0.10", description = "Mode-choice strategy weight")
	private double weight;

	@CommandLine.Option(names = "--top-k", defaultValue = "5", description = "Top k options for some of the strategies")
	private int k;

	@CommandLine.Option(names = "--avoid-k", defaultValue = "10", description = "Avoid using recent mode types again")
	private int avoidK;

	// picocli has strange behaviour regarding default values of these boolean options
	// Like this the default will be true
	@CommandLine.Option(names = "--no-time-mutation", defaultValue = "true", description = "Enable time mutation strategy", negatable = true)
	private boolean timeMutation;

	@CommandLine.Option(names = "--mass-conservation", defaultValue = "false", description = "Enable mass conservation constraint", negatable = true)
	private boolean massConservation;

	@CommandLine.Option(names = "--act-est", defaultValue = "false", description = "Enable activity estimation", negatable = true)
	private boolean actEst;

	@CommandLine.Option(names = "--force-innovation", defaultValue = "10", description = "Force innovative strategy with %% of agents")
	private int forceInnovation;

	@CommandLine.Option(names = "--inv-beta", defaultValue = "1", description = "Inv beta parameter (0 = best choice)")
	private double invBeta;

	@CommandLine.Option(names = "--prune", description = "Name of pruner to enable")
	private String prune;

	@CommandLine.Option(names = "--anneal", defaultValue = "off", description = "Parameter annealing")
	private InformedModeChoiceConfigGroup.Schedule anneal = InformedModeChoiceConfigGroup.Schedule.off;

	public StrategyOptions(ModeChoice modeChoice, String subpopulation) {
		this.modeChoice = modeChoice;
		this.subpopulation = subpopulation;
	}

	public ModeChoice getModeChoice() {
		return modeChoice;
	}

	/**
	 * Apply specified config and run options.
	 *
	 * @param log usually method reference to {@link org.matsim.application.MATSimApplication#addRunOption(Config, String, Object)}
	 */
	public void applyConfig(Config config, TriConsumer<Config, String, Object> log) {

		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);
		imc.setTopK(k);
		imc.setAvoidK(avoidK);
		imc.setInvBeta(invBeta);
		imc.setAnneal(anneal);
		imc.setPruning(prune);

		log.accept(config, "mc", modeChoice);

		if (modeChoice == ModeChoice.selectBestKPlanModes || modeChoice == ModeChoice.informedModeChoice) {
			log.accept(config, "k", k);
		}

		if (avoidK != 10) {
			log.accept(config, "ak", avoidK);
		}

		if (prune != null) {
			log.accept(config, "prune", prune);
		}

		if (massConservation) {
			log.accept(config, "mass-conv", "");
		} else
			config.subtourModeChoice().setChainBasedModes(new String[0]);

		if (actEst)
			log.accept(config, "act-est", "");

		if (!timeMutation)
			log.accept(config, "no-tm", "");


		if (anneal != InformedModeChoiceConfigGroup.Schedule.off)
			log.accept(config, "anneal", anneal);

		if (invBeta != 1)
			log.accept(config, "invBeta", invBeta);

		if (forceInnovation != 10)
			log.accept(config, "f-inv", forceInnovation);


		// Depends on number of pre generated plans
		if (modeChoice == ModeChoice.none || modeChoice == ModeChoice.informedModeChoice)
			config.strategy().setMaxAgentPlanMemorySize(Math.max(config.strategy().getMaxAgentPlanMemorySize(), k) + 5);

	}

	/**
	 * Apply and bind the {@link InformedModeChoiceModule}.
	 * @param setup procedure to configure the module
	 *
	 * @return prepared module that needs to be installed with guice
	 */
	public Module applyModule(Binder binder, Config config, Consumer<InformedModeChoiceModule.Builder> setup) {

		// Always collect all strategies (without the common MCs first)
		List<StrategyConfigGroup.StrategySettings> strategies = config.strategy().getStrategySettings().stream()
				.filter(s -> !s.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice) &&
						!s.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode) &&
						!s.getStrategyName().equals(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
				).collect(Collectors.toList());


		if (timeMutation) {
			strategies.add(new StrategyConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator)
					.setSubpopulation(subpopulation)
					.setWeight(0.025)
			);
		}

		if (modeChoice != ModeChoice.none) {

			strategies.add(new StrategyConfigGroup.StrategySettings()
					.setStrategyName(modeChoice.name)
					.setSubpopulation(subpopulation)
					.setWeight(weight)
			);
		}

		// reset und set new strategies
		config.strategy().clearStrategySettings();
		strategies.forEach(s -> config.strategy().addStrategySettings(s));

		if (forceInnovation > 0)
			binder.bind(new TypeLiteral<StrategyChooser<Plan, Person>>() {
			}).toInstance(new ForceInnovationStrategyChooser<>(forceInnovation, ForceInnovationStrategyChooser.Permute.yes));


		InformedModeChoiceModule.Builder builder = InformedModeChoiceModule.newBuilder();

		setup.accept(builder);

		if (massConservation)
			builder.withConstraint(RelaxedMassConservationConstraint.class);

		if (!actEst)
			builder.withActivityEstimator(ActivityEstimator.None.class);

		return builder.build();

	}

	public enum ModeChoice {

		none("none"),
		changeSingleTrip(DefaultPlanStrategiesModule.DefaultStrategy.ChangeSingleTripMode),
		subTourModeChoice(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice),
		selectSingleTripMode(InformedModeChoiceModule.SELECT_SINGLE_TRIP_MODE_STRATEGY),
		selectBestKPlanModes(InformedModeChoiceModule.SELECT_BEST_K_PLAN_MODES_STRATEGY),
		selectSubtourMode(InformedModeChoiceModule.SELECT_SUBTOUR_MODE_STRATEGY),
		informedModeChoice(InformedModeChoiceModule.INFORMED_MODE_CHOICE);

		private final String name;

		ModeChoice(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

}

