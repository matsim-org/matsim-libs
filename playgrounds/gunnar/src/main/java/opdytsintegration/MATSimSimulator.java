package opdytsintegration;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import floetteroed.opdyts.DecisionVariable;
import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * Created by michaelzilske on 08/10/15.
 * 
 * Modified by Gunnar in December 2015.
 */
public class MATSimSimulator<U extends DecisionVariable> implements
		Simulator<U> {

	// -------------------- MEMBERS --------------------

	private final MATSimStateFactory<U> stateFactory;

	private final Scenario scenario;

	private AbstractModule[] modules = null;

	private int nextControlerRun = 0;

	private final TimeDiscretization timeDiscretization;

	// -------------------- CONSTRUCTOR --------------------

	public MATSimSimulator(final MATSimStateFactory<U> stateFactory,
			final Scenario scenario,
			final TimeDiscretization timeDiscretization,
			final AbstractModule... modules) {
		this.stateFactory = stateFactory;
		this.scenario = scenario;
		this.timeDiscretization = timeDiscretization;
		String outputDirectory = this.scenario.getConfig().controler()
				.getOutputDirectory();
		this.scenario.getConfig().controler()
				.setOutputDirectory(outputDirectory + "_0");
		this.modules = modules;
	}

	// --------------- IMPLEMENTATION OF Simulator INTERFACE ---------------

	@Override
	public SimulatorState run(final TrajectorySampler<U> trajectorySampler) {
		String outputDirectory = this.scenario.getConfig().controler()
				.getOutputDirectory();
		outputDirectory = outputDirectory.substring(0,
				outputDirectory.lastIndexOf("_"))
				+ "_" + this.nextControlerRun;
		this.scenario.getConfig().controler()
				.setOutputDirectory(outputDirectory);
		final MATSimDecisionVariableSetEvaluator<U> matsimDecisionVariableEvaluator = new MATSimDecisionVariableSetEvaluator<U>(
				trajectorySampler, this.stateFactory, this.timeDiscretization);
		matsimDecisionVariableEvaluator.setMemory(1); // TODO make configurable
		matsimDecisionVariableEvaluator.setStandardLogFileName(outputDirectory
				+ "/optimization.log");
		// predictor.setStartTime_s(this.startTime_s);
		// predictor.setBinSize_s(this.binSize_s);
		// predictor.setBinCnt(this.binCnt);

		final Controler controler = new Controler(this.scenario);

		// Michael, ich weiss nicht ob das hier so richtig ist. Ich brauche
		// etwas in der Art, um das roadpricing-modul einzusetzen.
		// Gunnar 2015-12-12
		if (this.modules != null) {
			controler.setModules(this.modules);
			this.modules = null;
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
		controler.setTerminationCriterion(new Controler.TerminationCriterion() {
			@Override
			public boolean continueIterations(int iteration) {
				return !matsimDecisionVariableEvaluator.foundSolution();
			}
		});
		controler.run();
		this.nextControlerRun++;
		return matsimDecisionVariableEvaluator.getFinalState();
	}

	@Override
	public SimulatorState run(final TrajectorySampler<U> evaluator,
			final SimulatorState initialState) {
		if (initialState != null) {
			// ((MATSimState) initialState).setPopulation(this.scenario
			// .getPopulation());
			initialState.implementInSimulation();
		}
		return this.run(evaluator);
	}
}
