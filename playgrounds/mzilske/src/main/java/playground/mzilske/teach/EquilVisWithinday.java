package playground.mzilske.teach;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.AgentFactory;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.ptproject.qsim.QSimFactory;
import org.matsim.ptproject.qsim.helpers.QPersonAgent;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis2.OTFVisClient;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifier;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;

public class EquilVisWithinday implements Runnable {
	
	private final class MyWithinDayDuringLegReplanner extends
			WithinDayDuringLegReplanner {
		private MyWithinDayDuringLegReplanner(Id id, Scenario scenario) {
			super(id, scenario);
		}

		@Override
		public WithinDayDuringLegReplanner clone() {
			MyWithinDayDuringLegReplanner clone = new MyWithinDayDuringLegReplanner(id, scenario);
			clone.setReplanner(planAlgorithm);
			return clone;
		}

		@Override
		public boolean doReplanning(PersonDriverAgent driverAgent) {
			planAlgorithm.run(driverAgent.getPerson().getSelectedPlan());
			return true;
		}
	}

	public static void main(String[] args) {
		EquilVisWithinday potsdamRun = new EquilVisWithinday();
		potsdamRun.run();
	}
	
	@Override
	public void run() {
		String configFileName = "./examples/tutorial/config/example5-config.xml";
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(configFileName);
		loader.loadScenario();
		Scenario scenario = loader.getScenario();
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		qSimConfigGroup.setSnapshotPeriod(1);
		scenario.getConfig().setQSimConfigGroup(qSimConfigGroup);
		EventsManager events = new EventsManagerImpl();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		
		final WithinDayDuringLegReplanner duringLegReplanner = new MyWithinDayDuringLegReplanner(ReplanningIdGenerator.getNextId(), scenario);
		duringLegReplanner.setReplanner(new PlanAlgorithm() {

			@Override
			public void run(Plan plan) {
				System.out.println("Replanning "+plan.getPerson().getId());
			}
			
		});

		// WithinDayQSim queueSimulation = (WithinDayQSim) new WithinDayQSim(scenario, events);
		
		QSim queueSimulation = (QSim) new QSimFactory().createMobsim(scenario, events);
		
		queueSimulation.addSnapshotWriter(server.getSnapshotReceiver());
		queueSimulation.setAgentFactory(new AgentFactory(queueSimulation) {

			@Override
			public QPersonAgent createPersonAgent(Person p) {
				WithinDayPersonAgent agent = new WithinDayPersonAgent((PersonImpl) p, this.simulation);
				agent.addWithinDayReplanner(duringLegReplanner);
				return agent;
			}
			
		});
		
		ReplanningManager replanningManager = new ReplanningManager();
		ParallelDuringLegReplanner parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(1);
		
		FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();
		
	
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
		queueSimulation.addQueueSimulationListeners(foqsl);

		
		
		
		LeaveLinkIdentifier leaveLinkIdentifier = new LeaveLinkIdentifier(queueSimulation);
		duringLegReplanner.addAgentsToReplanIdentifier(leaveLinkIdentifier);
		parallelLeaveLinkReplanner.addWithinDayReplanner(duringLegReplanner);

		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);
		replanningManager.doLeaveLinkReplanning(true);

		
		
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(configFileName, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(true);
		client.run();
		
		
		queueSimulation.run();
	}
}