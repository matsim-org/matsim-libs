package playground.mzilske.jdeqsimengine;

import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.QSim;

public class JDEQSimModule {

    private JDEQSimModule() {}

    public static void configure(QSim qsim) {
        SteppableScheduler scheduler = new SteppableScheduler(new MessageQueue());
        JDEQSimEngine jdeqSimEngine = new JDEQSimEngine(qsim.getScenario(), qsim.getEventsManager(), qsim.getAgentCounter(), scheduler);
        qsim.addMobsimEngine(jdeqSimEngine);
        qsim.addActivityHandler(jdeqSimEngine);
    }

}
