package org.matsim.core.mobsim.qsim.jdeqsimengine;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.ActivityEnginePlugin;
import org.matsim.core.mobsim.qsim.PopulationPlugin;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.TeleportationPlugin;
import org.matsim.core.mobsim.qsim.agents.DefaultAgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsPlugin;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.mobsim.qsim.messagequeueengine.MessageQueuePlugin;
import org.matsim.core.mobsim.qsim.pt.TransitEnginePlugin;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetsimEnginePlugin;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * Created by michaelzilske on 19/03/14.
 */
public class JDEQSimPluginTest extends MatsimTestCase {
	private QSim prepareQSim(Scenario scenario, EventsManager eventsManager) {
        Collection<AbstractQSimPlugin> plugins = new LinkedList<>();
		plugins.add(new JDEQSimPlugin(scenario.getConfig()));
		
		QSimComponents components = new QSimComponents();
		components.activeMobsimEngines.add(JDEQSimPlugin.JDEQ_ENGINE);
		components.activeActivityHandlers.add(JDEQSimPlugin.JDEQ_ENGINE);
		
		List<AbstractModule> modules = new LinkedList<>();
		modules.add(new AbstractModule() {
			@Override
			public void install() {
				bind(QSimComponents.class).toInstance(components);
			}
		});
        
        return QSimUtils.createQSim(scenario, eventsManager, modules, plugins);
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
