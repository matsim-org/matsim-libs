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
 */
public class MATSimSimulator<U extends DecisionVariable> implements Simulator<U> {

	// private final Set<? extends DecisionVariable> decisionVariables;
    private final MATSimStateFactory stateFactory;
    private final Scenario scenario;

	private int nextControlerRun = 0;

	private final AbstractModule[] modules;
	
	private boolean modulesHaveBeenSet = false;
	
    public MATSimSimulator(// Set<? extends DecisionVariable> decisionVariables, 
    		MATSimStateFactory stateFactory, Scenario scenario, AbstractModule... modules) {
        // this.decisionVariables = decisionVariables;
        this.stateFactory = stateFactory;
        this.scenario = scenario;
		String outputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		this.scenario.getConfig().controler().setOutputDirectory(outputDirectory + "_0");
		this.modules = modules;
    }

    @Override
	public SimulatorState run(TrajectorySampler<U> evaluator) {
//				evaluator.addStatistic("./mylog.txt", new InterpolatedObjectiveFunctionValue());
//				evaluator.addStatistic("./mylog.txt", new AlphaStatistic(decisionVariables));
		String outputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		outputDirectory = outputDirectory.substring(0, outputDirectory.lastIndexOf("_")) + "_" + nextControlerRun;
		this.scenario.getConfig().controler().setOutputDirectory(outputDirectory);
		final MATSimDecisionVariableSetEvaluator predictor
				= new MATSimDecisionVariableSetEvaluator(evaluator, 
						// decisionVariables, 
						stateFactory);
		predictor.setMemory(1);
		predictor.setBinSize_s(10 * 60);
		predictor.setStartBin(6 * 5);
		predictor.setBinCnt(6 * 20);

		final Controler controler = new Controler(scenario);

		// Michael, ich weiss nicht ob das hier ideal ist, aber ich brauchte es, um irgendwie das
		// roadpricing-modul einzusetzen. Gunnar 2015-12-12
		if (this.modules != null && !this.modulesHaveBeenSet) {
			controler.setModules(this.modules);
			this.modulesHaveBeenSet = true;
		}
        
		controler.addControlerListener(predictor);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				binder().requestInjection(stateFactory);
			}
		});
		controler.setTerminationCriterion(new Controler.TerminationCriterion() {
			@Override
			public boolean continueIterations(int iteration) {
				return !predictor.foundSolution();
			}
		});
		controler.run();
		nextControlerRun++;
		return predictor.getFinalState();
	}

    @Override
	public SimulatorState run(TrajectorySampler<U> evaluator, SimulatorState initialState) {
		if (initialState != null) {
			((MATSimState) initialState).setPopulation(scenario.getPopulation());
			initialState.implementInSimulation();
		}
		return run(evaluator);
	}
}
