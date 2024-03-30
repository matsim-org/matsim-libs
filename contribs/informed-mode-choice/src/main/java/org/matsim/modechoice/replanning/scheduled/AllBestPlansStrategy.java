package org.matsim.modechoice.replanning.scheduled;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;
import org.matsim.modechoice.ScheduledModeChoiceConfigGroup;
import org.matsim.modechoice.replanning.GeneratorContext;
import org.matsim.modechoice.replanning.scheduled.solver.ModeSchedulingSolver;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Strategy that tries all best plans deterministically while keeping the mode share fixed.
 */
public class AllBestPlansStrategy extends AbstractMultithreadedModule implements PersonAlgorithm {

	private static final Logger log = LogManager.getLogger(AllBestPlansStrategy.class);

	private final Config config;
	private final ScheduledModeChoiceConfigGroup scheduleConfig;
	private final Scenario scenario;
	private final ThreadLocal<GeneratorContext> ctx;

	/**
	 * Scheduled plans for all relevant persons.
	 */
	private Map<Id<Person>, List<PlanCandidate>> plans = new ConcurrentHashMap<>();

	/**
	 * If greater than -1, the strategy will apply next plan from the schedule.
	 */
	private int applyIdx = -1;

	public AllBestPlansStrategy(Config config, Scenario scenario,
								Provider<GeneratorContext> generator) {
		super(config.global());
		this.config = config;
		this.scheduleConfig = ConfigUtils.addOrGetModule(config, ScheduledModeChoiceConfigGroup.class);
		this.scenario = scenario;
		this.ctx = ThreadLocal.withInitial(generator::get);
	}

	@Override
	protected void beforePrepareReplanningHook(ReplanningContext replanningContextTmp) {
		applyIdx = ScheduledStrategyChooser.isScheduledIteration(replanningContextTmp, scheduleConfig);

		if (applyIdx < 0)
			return;

		// Check if plans are already created
		if (!plans.isEmpty())
			return;

		log.info("Creating plan candidates.");
		ParallelPersonAlgorithmUtils.run(scenario.getPopulation(), config.global().getNumberOfThreads(), this);

		log.info("Creating plan schedule.");
		InformedModeChoiceConfigGroup imc = ConfigUtils.addOrGetModule(config, InformedModeChoiceConfigGroup.class);

		ModeSchedulingSolver solver = new ModeSchedulingSolver(scheduleConfig.getScheduleIterations(), imc.getTopK(),
			scheduleConfig.getModeTargetParameters(), scheduleConfig.getTargetSwitchShare());

		plans = solver.solve(plans);
	}

	@Override
	public void run(Person person) {

		PlanModel model = PlanModel.newInstance(person.getSelectedPlan());
		List<PlanCandidate> candidates = ctx.get().generator.generate(model);
		plans.put(person.getId(), candidates);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm();
	}


	private final class Algorithm implements PlanAlgorithm {

		@Override
		public void run(Plan plan) {

			// Does nothing if schedule is not advancing
			if (applyIdx < 0) {
				return;
			}

			Id<Person> personId = plan.getPerson().getId();

			List<PlanCandidate> schedule = plans.get(personId);
			if (schedule != null) {
				PlanCandidate candidate = schedule.get(applyIdx);
				candidate.applyTo(plan, true);

				plan.setType(candidate.getPlanType());
			}
		}
	}


}
