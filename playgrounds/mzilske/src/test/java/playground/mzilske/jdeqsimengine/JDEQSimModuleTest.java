package playground.mzilske.jdeqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Created by michaelzilske on 19/03/14.
 */
public class JDEQSimModuleTest extends MatsimTestCase {

    public void testRunsAtAll() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        QSim qsim = new QSim(scenario, eventsManager);
        JDEQSimModule.configure(qsim);
        qsim.run();
    }

    public void testRunsEquil() {
        Scenario scenario = ScenarioUtils.loadScenario(loadConfig("test/scenarios/equil/config.xml"));
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        QSim qsim = new QSim(scenario, eventsManager);
        JDEQSimModule.configure(qsim);
        PopulationAgentSource agentSource = new PopulationAgentSource(scenario.getPopulation(), new DefaultAgentFactory(qsim), qsim);
        qsim.addAgentSource(agentSource);
        qsim.run();
    }

}
