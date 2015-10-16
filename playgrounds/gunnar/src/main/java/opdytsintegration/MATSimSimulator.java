package opdytsintegration;

import floetteroed.opdyts.DecisionVariable;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import floetteroed.opdyts.SimulatorState;
import floetteroed.opdyts.searchalgorithms.Simulator;
import floetteroed.opdyts.trajectorysampling.TrajectorySampler;

/**
 * Created by michaelzilske on 08/10/15.
 */
public class MATSimSimulator implements Simulator {

	// private final Set<? extends DecisionVariable> decisionVariables;
    private final MATSimStateFactory stateFactory;
    private final Scenario scenario;

    public MATSimSimulator(// Set<? extends DecisionVariable> decisionVariables, 
    		MATSimStateFactory stateFactory, Scenario scenario) {
        // this.decisionVariables = decisionVariables;
        this.stateFactory = stateFactory;
        this.scenario = scenario;
    }

    @Override
	public SimulatorState run(TrajectorySampler evaluator) {
//				evaluator.addStatistic("./mylog.txt", new InterpolatedObjectiveFunctionValue());
//				evaluator.addStatistic("./mylog.txt", new AlphaStatistic(decisionVariables));

		final MATSimDecisionVariableSetEvaluator predictor
				= new MATSimDecisionVariableSetEvaluator(evaluator, 
						// decisionVariables, 
						stateFactory);
		predictor.setMemory(1);
		predictor.setBinSize_s(10 * 60);
		predictor.setStartBin(6 * 5);
		predictor.setBinCnt(6 * 20);

		final Controler controler = new Controler(scenario);
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

		return predictor.getFinalState();
	}

    @Override
	public SimulatorState run(TrajectorySampler evaluator, SimulatorState initialState) {
		if (initialState != null) {
			((MATSimState) initialState).setPopulation(scenario.getPopulation());
			initialState.implementInSimulation();
		}
		return run(evaluator);
	}
}
