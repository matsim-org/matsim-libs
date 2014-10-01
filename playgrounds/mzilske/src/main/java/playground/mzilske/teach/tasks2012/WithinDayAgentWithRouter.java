package playground.mzilske.teach.tasks2012;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.TeleportationEngine;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class WithinDayAgentWithRouter {
	
	private static class MyAgent implements MobsimDriverAgent {

		private PersonDriverAgentImpl delegate;

		public MyAgent(Person p, Plan unmodifiablePlan, QSim qSim) {
			delegate = new PersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), qSim);
		}

		public final void endActivityAndComputeNextState(double now) {
			delegate.endActivityAndComputeNextState(now);
		}

		public final void endLegAndComputeNextState(double now) {
			delegate.endLegAndComputeNextState(now);
		}

		public final void setStateToAbort(double now) {
			delegate.setStateToAbort(now);
		}

		public boolean equals(Object obj) {
			return delegate.equals(obj);
		}

		public final double getActivityEndTime() {
			return delegate.getActivityEndTime();
		}

		public final Id getCurrentLinkId() {
			return delegate.getCurrentLinkId();
		}

		public final Double getExpectedTravelTime() {
			return delegate.getExpectedTravelTime();
		}

		public final String getMode() {
			return delegate.getMode();
		}

		public final Id getDestinationLinkId() {
			return delegate.getDestinationLinkId();
		}

		public final Id getId() {
			return delegate.getId();
		}

		public State getState() {
			return delegate.getState();
		}

		public final void notifyArrivalOnLinkByNonNetworkMode(Id linkId) {
			delegate.notifyArrivalOnLinkByNonNetworkMode(linkId);
		}

		@Override
		public Id chooseNextLinkId() {
			return delegate.chooseNextLinkId();
		}

		@Override
		public void notifyMoveOverNode(Id newLinkId) {
			System.out.println("I moved over a node!");
			delegate.notifyMoveOverNode(newLinkId);
		}

		@Override
		public void setVehicle(MobsimVehicle veh) {
			delegate.setVehicle(veh);
		}

		@Override
		public MobsimVehicle getVehicle() {
			return delegate.getVehicle();
		}

		@Override
		public Id getPlannedVehicleId() {
			return delegate.getPlannedVehicleId();
		}


	}

	private static class MyAgentFactory implements AgentFactory {

		private QSim qSim;

		public MyAgentFactory(QSim qSim) {
			this.qSim = qSim;
		}

		@Override
		public MobsimAgent createMobsimAgentFromPerson(Person p) {
			MobsimAgent agent;
			if (p.getId().toString().equals("50")) {
				agent = new MyAgent(p, PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.qSim);
			} else {
			    agent = new PersonDriverAgentImpl(PopulationUtils.unmodifiablePlan(p.getSelectedPlan()), this.qSim); 
			}
			return agent;
		}

	}

	private static class MyMobsimFactory implements MobsimFactory {

		@Override
		public Mobsim createMobsim(Scenario sc, EventsManager eventsManager) {
	        QSim qSim1 = new QSim(sc, eventsManager);
			ActivityEngine activityEngine = new ActivityEngine();
			qSim1.addMobsimEngine(activityEngine);
			qSim1.addActivityHandler(activityEngine);
            QNetsimEngineModule.configure(qSim1);
			TeleportationEngine teleportationEngine = new TeleportationEngine();
			qSim1.addMobsimEngine(teleportationEngine);
			QSim qSim = qSim1;
	        PopulationAgentSource agentSource = new PopulationAgentSource(sc.getPopulation(), new MyAgentFactory(qSim), qSim);
	        qSim.addAgentSource(agentSource);
	        return qSim;
		}

	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig("examples/equil/config.xml");
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(1);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);
		MobsimFactory mobsimFactory = new MyMobsimFactory();
		controler.setMobsimFactory(mobsimFactory );
		controler.run();
		
	}

}
