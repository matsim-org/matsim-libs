package playground.mzilske.teach;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.LinkNetworkRoute;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.framework.PersonAgent;
import org.matsim.core.mobsim.framework.PersonDriverAgent;
import org.matsim.core.mobsim.framework.listeners.SimulationListener;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.helpers.QPersonAgent;
import org.matsim.vis.otfvis.gui.OTFHostConnectionManager;
import org.matsim.vis.otfvis2.OTFVisClient;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

import playground.christoph.events.algorithms.FixedOrderQueueSimulationListener;
import playground.christoph.withinday.mobsim.DuringLegReplanningModule;
import playground.christoph.withinday.mobsim.ReplanningManager;
import playground.christoph.withinday.mobsim.WithinDayAgentFactory;
import playground.christoph.withinday.mobsim.WithinDayPersonAgent;
import playground.christoph.withinday.mobsim.WithinDayQSim;
import playground.christoph.withinday.mobsim.WithinDayQSimFactory;
import playground.christoph.withinday.replanning.ReplanningIdGenerator;
import playground.christoph.withinday.replanning.WithinDayDuringLegReplanner;
import playground.christoph.withinday.replanning.identifiers.LeaveLinkIdentifier;
import playground.christoph.withinday.replanning.identifiers.LinkReplanningMap;
import playground.christoph.withinday.replanning.parallel.ParallelDuringLegReplanner;

public class EquilVisWithinday implements Runnable {
	
	private Scenario scenario;

	private final class MyWithinDayDuringLegReplanner extends
			WithinDayDuringLegReplanner {
		private MyWithinDayDuringLegReplanner(Id id, Scenario scenario) {
			super(id, scenario);
		}

		@Override
		public WithinDayDuringLegReplanner clone() {
			MyWithinDayDuringLegReplanner clone = new MyWithinDayDuringLegReplanner(id, scenario);
			return clone;
		}

		@Override
		public boolean doReplanning(PersonAgent driverAgent) {
			Id personId = new IdImpl("98");
			if (personId.equals(driverAgent.getPerson().getId())) {
				List<Id> route = ((LinkNetworkRoute) driverAgent.getCurrentLeg().getRoute()).getLinkIds();
				Id id = ((PersonDriverAgent)driverAgent).getCurrentLinkId();
				if (new IdImpl("6").equals(id)) {
					stellAb(driverAgent);
					((QPersonAgent) driverAgent).resetCaches();
				}
			}
			return true;
		}

		private void stellAb(PersonAgent driverAgent) {
			Id currentLinkId = ((PersonDriverAgent)driverAgent).getCurrentLinkId();
			PlanImpl plan = (PlanImpl) driverAgent.getPerson().getSelectedPlan();
			Leg currentLeg = driverAgent.getCurrentLeg();
			Route route = currentLeg.getRoute();
			NetworkFactoryImpl networkFactory = (NetworkFactoryImpl) scenario.getNetwork().getFactory();
			
			Route newRoute = networkFactory.createRoute(TransportMode.car, route.getStartLinkId(), currentLinkId);
			currentLeg.setRoute(newRoute);
			Activity activity = scenario.getPopulation().getFactory().createActivityFromLinkId("umsteigen", currentLinkId);
			activity.setEndTime(0);
			// Route newWalkRoute = networkFactory.createRoute(TransportMode.walk, currentLinkId, route.getEndLinkId());
			Leg newLeg = scenario.getPopulation().getFactory().createLeg(TransportMode.walk);
			// newLeg.setRoute(newWalkRoute);
			newLeg.setTravelTime(30);
			
			plan.getPlanElements().add(plan.getPlanElements().indexOf(currentLeg)+1, activity);
			plan.getPlanElements().add(plan.getPlanElements().indexOf(activity)+1, newLeg);
			System.out.println("Replanned.");
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
		scenario = loader.getScenario();
		QSimConfigGroup qSimConfigGroup = new QSimConfigGroup();
		qSimConfigGroup.setSnapshotPeriod(1);
		scenario.getConfig().setQSimConfigGroup(qSimConfigGroup);
		EventsManager events = new EventsManagerImpl();
		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
		
		final WithinDayDuringLegReplanner duringLegReplanner = new MyWithinDayDuringLegReplanner(ReplanningIdGenerator.getNextId(), scenario);

		
		WithinDayQSim queueSimulation = new WithinDayQSimFactory().createMobsim(scenario, events);
//		QSim queueSimulation = (QSim) new QSimFactory().createMobsim(scenario, events);
		
		queueSimulation.addSnapshotWriter(server.getSnapshotReceiver());
		queueSimulation.setAgentFactory(new WithinDayAgentFactory(queueSimulation) {

			@Override
			public QPersonAgent createPersonAgent(Person p) {
				WithinDayPersonAgent agent = (WithinDayPersonAgent) super.createPersonAgent(p);
				agent.addWithinDayReplanner(duringLegReplanner);
				return agent;
			}
			
		});
		
		List<SimulationListener> listeners = new ArrayList<SimulationListener>();
		ReplanningManager replanningManager = new ReplanningManager();
		ParallelDuringLegReplanner parallelLeaveLinkReplanner = new ParallelDuringLegReplanner(1, listeners);
		for (SimulationListener listener : listeners) queueSimulation.addQueueSimulationListeners(listener);
		
		FixedOrderQueueSimulationListener foqsl = new FixedOrderQueueSimulationListener();
		
	
		foqsl.addQueueSimulationInitializedListener(replanningManager);
		foqsl.addQueueSimulationBeforeSimStepListener(replanningManager);
		queueSimulation.addQueueSimulationListeners(foqsl);

		
		
		LinkReplanningMap linkReplanningMap = new LinkReplanningMap(queueSimulation);
		LeaveLinkIdentifier leaveLinkIdentifier = new LeaveLinkIdentifier(linkReplanningMap);
		duringLegReplanner.addAgentsToReplanIdentifier(leaveLinkIdentifier);
		parallelLeaveLinkReplanner.addWithinDayReplanner(duringLegReplanner);

		DuringLegReplanningModule leaveLinkReplanning = new DuringLegReplanningModule(parallelLeaveLinkReplanner);

		replanningManager.setLeaveLinkReplanningModule(leaveLinkReplanning);
		replanningManager.doLeaveLinkReplanning(true);

		
		
		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager(configFileName, server);
		OTFVisClient client = new OTFVisClient();
		client.setHostConnectionManager(hostConnectionManager);
		client.setSwing(false);
		client.run();
		
		
		queueSimulation.run();
	}
}