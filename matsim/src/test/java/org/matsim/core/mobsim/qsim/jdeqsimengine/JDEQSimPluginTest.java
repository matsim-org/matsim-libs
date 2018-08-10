package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Created by michaelzilske on 19/03/14.
 */
public class JDEQSimPluginTest extends MatsimTestCase {
	private QSim prepareQSim(Scenario scenario, EventsManager eventsManager) {
        return new QSimBuilder(scenario.getConfig()) //
        	.addQSimModule(new JDEQSimModule()) //
        	.configureComponents(components -> {
        		components.activeMobsimEngines.add(JDEQSimModule.JDEQ_ENGINE_NAME);
        		components.activeActivityHandlers.add(JDEQSimModule.JDEQ_ENGINE_NAME);
        	}) //
        	.build(scenario, eventsManager);
	}

    public void testRunsAtAll() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
        
        QSim qsim = prepareQSim(scenario, eventsManager);
        qsim.run();
    }

    public void testRunsEquil() {
        Scenario scenario = ScenarioUtils.loadScenario(loadConfig("test/scenarios/equil/config.xml"));
        EventsManager eventsManager = EventsUtils.createEventsManager(scenario.getConfig());
        eventsManager.initProcessing();
        PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
        
        QSim qsim = prepareQSim(scenario, eventsManager);
        qsim.run();
    }

}
