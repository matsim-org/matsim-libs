package playground.mzilske.withinday;

import java.util.Arrays;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;

import playground.mzilske.city2000w.GridCreator;
import playground.mzilske.logevents.LogOutputEventHandler;

public class Run {
	
	private static final class AdapterAgentFactory implements AgentFactory, MobsimEngine {
		private final QSim qSim;
		
		private InternalInterface internalInterface;

		private AdapterAgentFactory(QSim qSim) {
			this.qSim = qSim;
		}

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			AdapterAgent adapterAgent = new AdapterAgent(p.getSelectedPlan(), this.internalInterface);
			qSim.addQueueSimulationListeners(adapterAgent);
			return adapterAgent;
		}

		@Override
		public void doSimStep(double time) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onPrepareSim() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void afterSim() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setInternalInterface(InternalInterface internalInterface) {
			this.internalInterface = internalInterface;
		}
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.createConfig();
		config.qsim().setEndTime(8*60*60);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		GridCreator gridCreator = new GridCreator(scenario);
		gridCreator.createGrid(8);
		
		Person person = scenario.getPopulation().getFactory().createPerson(Id.create("1", Person.class));
		Plan plan = scenario.getPopulation().getFactory().createPlan();
		
		Activity activity1 = scenario.getPopulation().getFactory().createActivityFromLinkId("wurst", Id.create("i(1,1)", Link.class));
		activity1.setEndTime(6*60*60);
		plan.addActivity(activity1);
		
		
		
		
		Leg leg = scenario.getPopulation().getFactory().createLeg("car");
		// GenericRouteImpl route = new GenericRouteImpl(Id.create("i(1,1)"), Id.create("i(8,8)"));
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(Id.create("i(1,1)", Link.class), Id.create("i(8,1)", Link.class));
		route.setLinkIds(Id.create("i(1,1)", Link.class), 
				Arrays.asList(
						Id.create("i(2,1)", Link.class), 
						Id.create("i(3,1)", Link.class),
						Id.create("i(4,1)", Link.class), 
						Id.create("i(5,1)", Link.class),
						Id.create("i(6,1)", Link.class), 
						Id.create("i(7,1)", Link.class)), 
						Id.create("i(8,1)", Link.class));
		leg.setTravelTime(3600);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(scenario.getPopulation().getFactory().createActivityFromLinkId("wurst", Id.create("i(8,1)", Link.class)));
		
		
		
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
		
		
		final EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new LogOutputEventHandler());
		QSim qSim1 = new QSim(scenario, events);
		ActivityEngine activityEngine = new ActivityEngine();
		qSim1.addMobsimEngine(activityEngine);
		qSim1.addActivityHandler(activityEngine);
        QNetsimEngineModule.configure(qSim1);
		TeleportationEngine teleportationEngine = new TeleportationEngine();
		qSim1.addMobsimEngine(teleportationEngine);
		final QSim qSim = qSim1;

		
		AdapterAgentFactory fac = new AdapterAgentFactory(qSim);
		qSim.addMobsimEngine(fac);
		
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), fac, qSim);
		qSim.addAgentSource(agentSource);
		
		
//		ControlerIO controlerIO = new ControlerIO(scenario.getConfig().controler().getOutputDirectory());
//		OTFVisMobsimFeature queueSimulationFeature = new OTFVisMobsimFeature(queueSimulation);
//		queueSimulation.addFeature(queueSimulationFeature);
//		queueSimulationFeature.setVisualizeTeleportedAgents(scenario.getConfig().otfVis().isShowTeleportedAgents());
//		queueSimulation.setControlerIO(controlerIO);
//		queueSimulation.setIterationNumber(scenario.getConfig().controler().getLastIteration());

		
//		OTFVisLiveServer server = new OTFVisLiveServer(scenario, events);
//		OTFHostConnectionManager hostConnectionManager = new OTFHostConnectionManager("wurst", server);
//		OTFVisClient client = new OTFVisClient();
//		queueSimulation.addSnapshotWriter(server.getSnapshotReceiver());
//		client.setHostConnectionManager(hostConnectionManager);
//		client.setSwing(false);
//		client.run();
		
		qSim.run();
		
	}

}
