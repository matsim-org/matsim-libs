package opdytsintegration;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.scoring.ScoringFunctionFactory;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * Created by michaelzilske on 08/10/15.
 * 
 * Modified by Gunnar, starting in December 2015.
 */
public class MATSimSimulator<U extends DecisionVariable> implements
		Simulator<U> {

	// -------------------- MEMBERS --------------------

	private final MATSimStateFactory<U> stateFactory;

	private final Scenario scenario;

	private final TimeDiscretization timeDiscretization;

	private final Set<Id<Link>> relevantLinkIds;

	private AbstractModule[] modules = null;

	private int nextControlerRun = 0;

	private ScoringFunctionFactory scoringFunctionFactory = null;

	// -------------------- CONSTRUCTOR --------------------

	public MATSimSimulator(final MATSimStateFactory<U> stateFactory,
			final Scenario scenario,
			final TimeDiscretization timeDiscretization,
			final Set<Id<Link>> relevantLinkIds,
			final AbstractModule... modules) {
		this.stateFactory = stateFactory;
		this.scenario = scenario;
		this.timeDiscretization = timeDiscretization;
		this.relevantLinkIds = relevantLinkIds;
		this.modules = modules;

		final String outputDirectory = this.scenario.getConfig().controler()
				.getOutputDirectory();
		this.scenario.getConfig().controler()
				.setOutputDirectory(outputDirectory + "_0");
	}

	// TODO NEW
	public void setScoringFunctionFactory(final ScoringFunctionFactory factory) {
		this.scoringFunctionFactory = factory;
	}

	// --------------- IMPLEMENTATION OF Simulator INTERFACE ---------------

	@Override
	public SimulatorState run(final TrajectorySampler<U> trajectorySampler) {

		/*
		 * (1) This function is called in many iterations. Each time, it
		 * executes a complete MATSim run. To avoid that the MATSim output files
		 * are overwritten each time, set iteration-specific output directory
		 * names.
		 */
		String outputDirectory = this.scenario.getConfig().controler()
				.getOutputDirectory();
		outputDirectory = outputDirectory.substring(0,
				outputDirectory.lastIndexOf("_"))
				+ "_" + this.nextControlerRun;
		this.scenario.getConfig().controler()
				.setOutputDirectory(outputDirectory);

		/*
		 * (2) Create the MATSimDecisionVariableSetEvaluator that is supposed to
		 * "optimize along" the MATSim run of this iteration.
		 */
		final MATSimDecisionVariableSetEvaluator<U> matsimDecisionVariableEvaluator = new MATSimDecisionVariableSetEvaluator<>(
				trajectorySampler, this.stateFactory, this.timeDiscretization,
				this.relevantLinkIds);
		matsimDecisionVariableEvaluator.setMemory(1); // TODO make configurable
		// matsimDecisionVariableEvaluator.setStandardLogFileName("./opdyts.log");

		/*
		 * (3) Create, configure, and run a new MATSim Controler.
		 * 
		 * TODO Is this done correctly?
		 */
		final Controler controler = new Controler(this.scenario);
		if (this.modules != null) {
			// controler.setModules(new
			// ControlerDefaultsWithRoadPricingModule());
			controler.setModules(this.modules);
			// this.modules = null; // ???
		}
		controler.addControlerListener(matsimDecisionVariableEvaluator);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().requestInjection(stateFactory);
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().requestInjection(matsimDecisionVariableEvaluator);
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().requestInjection(
						trajectorySampler.getObjectiveFunction());
			}
		});
		controler.setTerminationCriterion(new TerminationCriterion() {
			@Override
			public boolean continueIterations(int iteration) {
				return (!matsimDecisionVariableEvaluator.foundSolution());
			}
		});

		// >>>>>>>>>> TODO NEW >>>>>>>>>>
		if (this.scoringFunctionFactory != null) {
			controler.setScoringFunctionFactory(this.scoringFunctionFactory);
		}
		// <<<<<<<<<< TODO NEW <<<<<<<<<<

		controler.run();
		this.nextControlerRun++;
		return matsimDecisionVariableEvaluator.getFinalState();
	}

	@Override
	public SimulatorState run(final TrajectorySampler<U> evaluator,
			final SimulatorState initialState) {
		if (initialState != null) {
			initialState.implementInSimulation();
		}
		return this.run(evaluator);
	}

}
