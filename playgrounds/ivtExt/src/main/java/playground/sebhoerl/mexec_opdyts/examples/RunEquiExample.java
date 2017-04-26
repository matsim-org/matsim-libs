package playground.sebhoerl.mexec_opdyts.examples;

import floetteroed.opdyts.convergencecriteria.ConvergenceCriterion;
import floetteroed.opdyts.convergencecriteria.FixedIterationNumberConvergenceCriterion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import playground.sebhoerl.mexec.Controller;
import playground.sebhoerl.mexec.Environment;
import playground.sebhoerl.mexec.Scenario;
import playground.sebhoerl.mexec.Simulation;
import playground.sebhoerl.mexec.local.LocalEnvironment;
import playground.sebhoerl.mexec.local.os.LinuxDriver;
import playground.sebhoerl.mexec_opdyts.execution.OpdytsRunner;
import playground.sebhoerl.mexec_opdyts.execution.OpdytsRunnerConfig;
import playground.sebhoerl.mexec_opdyts.optimization.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RunEquiExample {
    static public class WalkShareObjectiveHandler implements PersonDepartureEventHandler, IterationEventHandler {
        final private double targetShare;

        private double totalLegs;
        private double walkLegs;

        public WalkShareObjectiveHandler(double targetShare) {
            this.targetShare = targetShare;
            reset(0);
        }

        @Override
        public double getValue() {
            double walkShare = walkLegs / totalLegs;
            return Math.pow(targetShare - walkShare, 2.0);
        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            totalLegs += 1.0;

            if (event.getLegMode().equals("walk")) {
                walkLegs += 1.0;
            }
        }

        @Override
        public void reset(int iteration) {
            totalLegs = 0;
            walkLegs = 0;
        }
    }

    static public class CarTravelUtilityProposal implements Proposal {
        final private double utility;

        public CarTravelUtilityProposal(double utility) {
            this.utility = utility;
        }

        public double getUtility() {
            return utility;
        }

        @Override
        public void implement(Simulation simulation) {
            // General setup (for mexec)
            simulation.getConfig().setParameter("network", "inputNetworkFile", "%{scenario}/network.xml");
            simulation.getConfig().setParameter("plans", "inputPlansFile", "%{scenario}/plans100.xml");
            simulation.getConfig().setParameter("controler", "outputDirectory", "%{output}");
            simulation.getConfig().setParameter("controler", "overwriteFiles", "deleteDirectoryIfExists");

            // Scenario setup (add walk mode)
            simulation.getConfig().setParameter("planCalcScore", "traveling_walk", "-6");
            simulation.getConfig().setParameter("strategy", "ModuleProbability_3", "0.05");
            simulation.getConfig().setParameter("strategy", "Module_3", "SubtourModeChoice");
            simulation.getConfig().setParameter("subtourModeChoice", "chainBasedModes", "car");
            simulation.getConfig().setParameter("subtourModeChoice", "modes", "car,walk");

            // Proposal specific setup
            simulation.getConfig().setParameter("planCalcScore", "traveling", String.valueOf(utility));
        }

        @Override
        public String toString() {
            return String.valueOf(utility);
        }
    }

    static public class CustomProposalDistribution implements ProposalDistribution {
        @Override
        public Proposal draw() {
            return new CarTravelUtilityProposal(-8.0);
        }

        @Override
        public Proposal draw(Proposal priorProposal, Simulation priorSimulation) {
            CarTravelUtilityProposal proposal = (CarTravelUtilityProposal) priorProposal;
            NormalDistribution normal = new NormalDistribution(proposal.getUtility(), 0.1);
            return new CarTravelUtilityProposal(normal.sample());
        }
    }

    static private Environment getEnvironment() {
        File environmentPath = new File(SystemUtils.getUserHome(), "mexecenv");
        String localControllerPath = "/home/sebastian/Downloads/matsim";
        String localScenarioPath = "/home/sebastian/Downloads/matsim/examples/equil";

        Environment environment = new LocalEnvironment(environmentPath.toString(), new LinuxDriver());

        if (!environment.hasScenario("equil")) {
            environment.createScenario("equil", localScenarioPath);
        }

        if (!environment.hasController("standard")) {
            environment.createController("standard", localControllerPath, "matsim-0.8.0.jar", "org.matsim.run.Controler");
        }

        return environment;
    }

    static public void main(String[] args) {
        Environment environment = getEnvironment();
        Scenario scenario = environment.getScenario("equil");
        Controller controller = environment.getController("standard");

        OpdytsRunnerConfig config = new OpdytsRunnerConfig();
        config.setLogPath("/home/sebastian/mexec/opdyts_logs");

        config.setCandidatePoolSize(5);
        config.setMatsimStepsPerTransition(1);

        config.setMaximumNumberOfTransitions(1000);
        config.setMaximumNumberOfIterations(100);

        int iterationsToConvergence = 20;
        int averagingIterations = 5;

        IterationObjectiveFunction objective = new EventsBasedObjectiveFunction(new WalkShareObjectiveHandler(0.75));
        ProposalDistribution proposalDistribution = new CustomProposalDistribution();
        ConvergenceCriterion convergenceCriterion = new FixedIterationNumberConvergenceCriterion(iterationsToConvergence, averagingIterations);

        new OpdytsRunner(
            environment, scenario, controller,
            proposalDistribution,
            convergenceCriterion,
            objective,
            config
        ).run();
    }
}
