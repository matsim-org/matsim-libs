package playground.sebhoerl.euler_opdyts;

import com.jcraft.jsch.JSchException;
import floetteroed.opdyts.DecisionVariableRandomizer;
import floetteroed.opdyts.ObjectiveFunction;
import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import floetteroed.opdyts.searchalgorithms.RandomSearch;
import floetteroed.opdyts.searchalgorithms.SelfTuner;
import org.apache.commons.io.FileUtils;
import playground.sebhoerl.remote_exec.RemoteController;
import playground.sebhoerl.remote_exec.RemoteEnvironment;
import playground.sebhoerl.remote_exec.RemoteScenario;
import playground.sebhoerl.remote_exec.local.LocalConfiguration;
import playground.sebhoerl.remote_exec.local.LocalEnvironment;
import playground.sebhoerl.remote_exec.local.LocalInterface;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RunSiouxFallsCalibration {
    private static String localPath;
    private static String environmentName;

    private static int numberOfOpdytsIterations;
    private static int candidatePoolSize;
    private static int numberOfSimulationIterationsPerOpdytsRun;
    private static int numberOfSimulationIterationsPerTransition;

    static public void main(String[] args) throws JSchException, IOException, InterruptedException {
        localPath = args[0];
        environmentName = args[1];
        numberOfOpdytsIterations = Integer.parseInt(args[2]);
        candidatePoolSize = Integer.parseInt(args[3]);
        numberOfSimulationIterationsPerOpdytsRun = Integer.parseInt(args[4]);
        numberOfSimulationIterationsPerTransition = Integer.parseInt(args[5]);

        /*JSch jsch = new JSch();
        jsch.addIdentity("~/.ssh/eth");
        jsch.setKnownHosts("~/.ssh/known_hosts");

        Session session = jsch.getSession("shoerl", "euler", 22);*/

        /*EulerConfiguration config = new EulerConfiguration();
        config.setOutputPath("/cluster/scratch/shoerl/calibration");
        config.setScenarioPath("/cluster/home/shoerl/calibration");
        config.setJobPrefix("ca_");

        RemoteEnvironment environment = new EulerEnvironment(new EulerInterface(session, config));*/

        LocalConfiguration config = new LocalConfiguration();
        config.setOutputPath(localPath + "/env/" + environmentName + "/output");
        config.setScenarioPath(localPath + "/env/" + environmentName + "/scenario");

        RemoteEnvironment environment = new LocalEnvironment(new LocalInterface(config));

        setupEnvironment(environment);
        runCalibration(environment);

        /*try {
            session.connect();

            setupEnvironment(environment);
            runCalibration(environment);
        } finally {
            session.disconnect();
        }*/
    }

    static void setupEnvironment(RemoteEnvironment environment) {
        if (!environment.hasController("standard")) {
            environment.createController("standard", localPath + "/controller", "matsim-0.8.0.jar", "org.matsim.run.Controler");
        }

        if (!environment.hasScenario("sioux2016")) {
            environment.createScenario("sioux2016", localPath + "/scenario");
        }
    }

    static void runCalibration(RemoteEnvironment environment) throws IOException {
        /*final int candidatePoolSize = 5;

        final int numberOfOpdytsIterations = 4;

        final int numberOfSimulationIterationsPerOpdytsRun = 20;
        final int numberOfSimulationIterationsPerTransition = 5;*/

        final int numberOfTransitionsPerOpdytsRun = numberOfSimulationIterationsPerOpdytsRun / numberOfSimulationIterationsPerTransition;
        final int numberOfOpdytsTransitions = numberOfOpdytsIterations * numberOfTransitionsPerOpdytsRun;

        RemoteController controller = environment.getController("standard");
        RemoteScenario scenario = environment.getScenario("sioux2016");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("car_costs", String.valueOf((-40.0 / 100.0) / 1000.0));
        RemoteDecisionVariable initial = new RemoteDecisionVariable(parameters);

        RemoteStateHandler handler = new MyObjectiveHandler();
        ObjectiveFunction objectiveFunction = new RemoteObjectiveFunction();

        DecisionVariableRandomizer<RemoteDecisionVariable> randomizer = new MyDecisionVariableRandomizer(candidatePoolSize);

        ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(numberOfTransitionsPerOpdytsRun, numberOfTransitionsPerOpdytsRun);

        RemoteSimulationFactory simulationFactory = new RemoteSimulationFactory(environment, scenario, controller, numberOfSimulationIterationsPerOpdytsRun, numberOfSimulationIterationsPerTransition);
        RemoteSimulator simulator = new RemoteSimulator(simulationFactory, handler);

        RandomSearch<RemoteDecisionVariable> randomSearch = new RandomSearch<>(
                simulator,
                randomizer,
                initial,
                convergenceCriterion,
                numberOfOpdytsIterations,
                numberOfOpdytsTransitions * candidatePoolSize + candidatePoolSize * numberOfOpdytsIterations,
                candidatePoolSize,
                new Random(),
                true,
                objectiveFunction,
                false
        );

        String logPath = localPath + "/opdyts_logs/" + environmentName;
        randomSearch.setLogPath(logPath);

        randomSearch.run(new SelfTuner(0.95));
    }
}
