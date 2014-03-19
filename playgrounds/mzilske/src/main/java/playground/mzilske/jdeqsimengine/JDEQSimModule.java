package playground.mzilske.jdeqsimengine;

import org.matsim.core.mobsim.jdeqsim.MessageQueue;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * Created by michaelzilske on 19/03/14.
 */
public class JDEQSimModule {

    private JDEQSimModule() {}

    public static void configure(QSim qsim) {
        SteppableScheduler scheduler = new SteppableScheduler(new MessageQueue());
        JDEQSimEngine jdeqSimEngine = new JDEQSimEngine(qsim.getScenario(), qsim.getEventsManager(), scheduler);
        qsim.addMobsimEngine(jdeqSimEngine);
        qsim.addActivityHandler(jdeqSimEngine);
    }

}
