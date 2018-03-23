package org.matsim.core.mobsim.qsim.jdeqsimengine;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.jdeqsim.JDEQSimConfigGroup;
import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.AgentCounter;

public class JDEQSimModule {

    private JDEQSimModule() {}

    public static void configure(QSim qsim, Config config, Scenario scenario, EventsManager eventsManager, AgentCounter agentCounter) {
        SteppableScheduler scheduler = new SteppableScheduler(new MessageQueue());
        JDEQSimEngine jdeqSimEngine = new JDEQSimEngine(ConfigUtils.addOrGetModule(config, JDEQSimConfigGroup.NAME, JDEQSimConfigGroup.class), scenario, eventsManager, agentCounter, scheduler);
        qsim.addMobsimEngine(jdeqSimEngine);
        qsim.addActivityHandler(jdeqSimEngine);
    }

}
