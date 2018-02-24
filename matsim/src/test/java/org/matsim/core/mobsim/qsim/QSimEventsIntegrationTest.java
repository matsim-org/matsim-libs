package org.matsim.core.mobsim.qsim;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEngineModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class QSimEventsIntegrationTest {

	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Rule
	public Timeout globalTimeout= new Timeout(2000);

	@Test
	public void netsimEngineHandlesExceptionCorrectly() {
		Config config = utils.loadConfig("test/scenarios/equil/config_plans1.xml");
//		config.qsim().setUsingThreadpool(false);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new LinkLeaveEventHandler() {
			@Override
			public void handleEvent(LinkLeaveEvent event) {
				throw new RuntimeException("Haha, I hope the QSim exits cleanly.");
			}

			@Override
			public void reset(int iteration) {

			}
		});
		MobsimTimer mobsimTimer = new MobsimTimer(scenario.getConfig());
		AgentCounter agentCounter = new AgentCounterImpl();
		ActiveQSimBridge activeQSimBridge = new ActiveQSimBridge();
		QSim qSim = new QSim(scenario, events, agentCounter, mobsimTimer, activeQSimBridge);
		AgentFactory agentFactory = new DefaultAgentFactory(scenario, events, mobsimTimer);
		PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), agentFactory, scenario.getConfig(), scenario, qSim);
		qSim.addAgentSource(agentSource);
		ActivityEngine activityEngine = new ActivityEngine(events, agentCounter, mobsimTimer);
		qSim.addMobsimEngine(activityEngine);
		qSim.addActivityHandler(activityEngine);
		QNetsimEngineModule.configure(qSim, scenario.getConfig(), scenario, events, mobsimTimer, agentCounter);
		try {
			qSim.run();
		} catch (RuntimeException e) {
			// That's fine. Only timeout is bad, which would mean qsim would hang on an Exception in an EventHandler.
		}
	}

	@Test
	public void controlerHandlesExceptionCorrectly() {
		Config config = utils.loadConfig("test/scenarios/equil/config_plans1.xml");
		//		config.qsim().setUsingThreadpool(false);
		Controler controler = new Controler(config);
		controler.getEvents().addHandler(new LinkLeaveEventHandler() {
			@Override
			public void handleEvent(LinkLeaveEvent event) {
				throw new RuntimeException("Haha, I hope the QSim exits cleanly.");
			}

			@Override
			public void reset(int iteration) {

			}
		});
		try {
			controler.run();
		} catch (RuntimeException e) {
			// That's fine. Only timeout is bad, which would mean qsim would hang on an Exception in an EventHandler.
		}
	}

}
