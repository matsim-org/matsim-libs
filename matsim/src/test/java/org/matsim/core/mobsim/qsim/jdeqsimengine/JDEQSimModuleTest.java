package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Created by michaelzilske on 19/03/14.
 */
public class JDEQSimModuleTest extends MatsimTestCase {

    public void testRunsAtAll() {
    	Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
        MobsimTimer mobsimTimer = new MobsimTimer(config);
        ActiveQSimBridge activeQSimBridge = new ActiveQSimBridge();
        QSim qsim = new QSim(scenario, eventsManager, agentCounterFixture, mobsimTimer, activeQSimBridge);
        JDEQSimModule.configure(qsim, config, scenario, eventsManager, agentCounterFixture);
        qsim.run();
    }

    public void testRunsEquil() {
    	Config config = loadConfig("test/scenarios/equil/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
        MobsimTimer mobsimTimer = new MobsimTimer(config);
        ActiveQSimBridge activeQSimBridge = new ActiveQSimBridge();
        QSim qsim = new QSim(scenario, eventsManager, agentCounterFixture, mobsimTimer, activeQSimBridge);
        JDEQSimModule.configure(qsim, config, scenario, eventsManager, agentCounterFixture);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), new DefaultAgentFactory(scenario, eventsManager, mobsimTimer), config, scenario, qsim);
        qsim.addAgentSource(agentSource);
        qsim.run();
    }
    
	final private static AgentCounter agentCounterFixture = new AgentCounter() {
		@Override
		public boolean isLiving() {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public void incLost() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void incLiving() {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public int getLost() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public int getLiving() {
			// TODO Auto-generated method stub
			return 0;
		}
		
		@Override
		public void decLiving() {
			// TODO Auto-generated method stub
			
		}
	};

}
