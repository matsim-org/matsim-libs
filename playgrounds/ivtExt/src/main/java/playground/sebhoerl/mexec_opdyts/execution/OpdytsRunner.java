package playground.sebhoerl.mexec_opdyts.execution;

import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec_opdyts.optimization.IterationObjectiveFunction;
import playground.sebhoerl.mexec_opdyts.optimization.ProposalDistribution;

import java.util.Random;

public class OpdytsRunner {
    final private RandomSearch<ProposalDecisionVariable> searchAlgorithm;
    final private OpdytsRunnerConfig config;

    public OpdytsRunner(Environment environment, Scenario scenario, Controller controller, ProposalDistribution proposalDistribution, ConvergenceCriterion convergenceCriterion, IterationObjectiveFunction objectiveFunction, OpdytsRunnerConfig config) {
        this.config = config;

        OpdytsExecutor executor = new OpdytsExecutor(environment, controller, scenario, config.getMatsimStepsPerTransition(), config.getSimulationPrefix());
        OpdytsRandomizer randomizer = new OpdytsRandomizer(executor, config.getCandidatePoolSize(), proposalDistribution);
        OpdytsSimulator simulator = new OpdytsSimulator(executor);

        ProposalDecisionVariable initialProposal = new ProposalDecisionVariable(executor, proposalDistribution.draw());

        searchAlgorithm = new RandomSearch<ProposalDecisionVariable>(
                simulator,
                randomizer,
                initialProposal,
                convergenceCriterion,
                (int) config.getMaximumNumberOfIterations(),
                (int) config.getMaximumNumberOfTransitions(),
                (int) config.getCandidatePoolSize(),
                new Random(),
                config.isInterpolate(),
                new IterationObjectiveFunctionWrapper(objectiveFunction),
                config.isIncludeCurrentBest()
        );

        if (config.getLogPath() != null) {
            searchAlgorithm.setLogPath(config.getLogPath());
        }
    }

    public void run() {
        searchAlgorithm.run(new SelfTuner(config.getSelfTunerInertia()));
    }
}
