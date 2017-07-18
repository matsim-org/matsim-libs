package org.matsim.contrib.opdyts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * Created by michaelzilske on 08/10/15.
 * 
 * Modified by Gunnar, starting in December 2015.
 */
public class MATSimSimulator2<U extends DecisionVariable> implements Simulator<U> {

	// -------------------- MEMBERS --------------------

	private final MATSimStateFactory<U> stateFactory;

	private final Scenario scenario;

	private AbstractModule[] replacingModules = null;
	private AbstractModule overrides = AbstractModule.emptyModule();

	private int nextControlerRun = 0;

	private ScoringFunctionFactory scoringFunctionFactory = null;

	private int stateMemory = 1;

	// TODO not elegant
	// a list because the order matters in the state space vector
	private final List<SimulationStateAnalyzerProvider> simulationStateAnalyzers = new ArrayList<>();

	// TODO exists also in MATSimDecisionVariableSetEvaluator2
	public void addSimulationStateAnalyzer(final SimulationStateAnalyzerProvider analyzer) {
		if (this.simulationStateAnalyzers.contains(analyzer)) {
			throw new RuntimeException("Analyzer " + analyzer + " has already been added.");
		}
		this.simulationStateAnalyzers.add(analyzer);
	}

	// -------------------- CONSTRUCTOR --------------------

	public MATSimSimulator2(final MATSimStateFactory<U> stateFactory, final Scenario scenario) {
		this.stateFactory = stateFactory;
		this.scenario = scenario;

		final String outputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		this.scenario.getConfig().controler().setOutputDirectory(outputDirectory + "_0");

		// because this systematically changes the simulation dynamics
		this.scenario.getConfig().strategy().setFractionOfIterationsToDisableInnovation(Double.POSITIVE_INFINITY);
		this.scenario.getConfig().planCalcScore().setFractionOfIterationsToStartScoreMSA(Double.POSITIVE_INFINITY);
	}

	public final void setReplacingModules(final AbstractModule... replacingModules) {
		this.replacingModules = replacingModules;
	}

	public final void addOverridingModule(AbstractModule abstractModule) {
		this.overrides = AbstractModule.override(Arrays.asList(this.overrides), abstractModule);
	}

	// TODO NEW
	// In principle this can also be done via a module. So this additional
	// syntax is not really necessary (and bloats the design). kai, sep'16
	public void setScoringFunctionFactory(final ScoringFunctionFactory factory) {
		this.scoringFunctionFactory = factory;
	}

	// TODO NEW
	public void setStateMemory(final int stateMemory) {
		this.stateMemory = stateMemory;
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
		String outputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		outputDirectory = outputDirectory.substring(0, outputDirectory.lastIndexOf("_")) + "_" + this.nextControlerRun;
		this.scenario.getConfig().controler().setOutputDirectory(outputDirectory);

		/*
		 * (2) Create the MATSimDecisionVariableSetEvaluator that is supposed to
		 * "optimize along" the MATSim run of this iteration.
		 */
		final MATSimDecisionVariableSetEvaluator2<U> matsimDecisionVariableEvaluator = new MATSimDecisionVariableSetEvaluator2<>(
				trajectorySampler, this.stateFactory);
		
		for (SimulationStateAnalyzerProvider analyzer : this.simulationStateAnalyzers) {
			matsimDecisionVariableEvaluator.addSimulationStateAnalyzer(analyzer);
		}
		
		matsimDecisionVariableEvaluator.setMemory(this.stateMemory);

		/*
		 * (3) Create, configure, and run a new MATSim Controler.
		 * 
		 * TODO Is this done correctly?
		 */
		final Controler controler = new Controler(this.scenario);
		if ((this.replacingModules != null) && (this.replacingModules.length > 0)) {
			controler.setModules(this.replacingModules);

			// I would say that with this syntax it is quite prone to
			// misunderstandings: If this.modules is != null, this syntax will
			// _replace_ the complete controler infrastructure. This is a
			// possibility, but in matsim it seems more normal to
			// add "overriding" modules, i.e. to replace default functionality
			// by other functionality and/or add functionality. kai, sep'16
			// Fixed by differentiating between setReplacingModules and
			// addOverridingModules. kai, sep'16
		}
		controler.addOverridingModule(this.overrides);

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
				binder().requestInjection(trajectorySampler.getObjectiveFunction());
			}
		});
		controler.setTerminationCriterion(new TerminationCriterion() {
			@Override
			public boolean continueIterations(int iteration) {
				return (!matsimDecisionVariableEvaluator.foundSolution());
			}
		});

		if (this.scoringFunctionFactory != null) {
			controler.setScoringFunctionFactory(this.scoringFunctionFactory);
		}

		this.stateFactory.registerControler(controler);

		controler.run();
		this.nextControlerRun++;
		return matsimDecisionVariableEvaluator.getFinalState();
	}

	@Override
	public SimulatorState run(final TrajectorySampler<U> evaluator, final SimulatorState initialState) {
		if (initialState != null) {
			initialState.implementInSimulation();
		}
		return this.run(evaluator);
	}
}
